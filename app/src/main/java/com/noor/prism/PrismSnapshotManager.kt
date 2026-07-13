package com.noor.prism

import android.content.Context
import android.webkit.MimeTypeMap
import android.webkit.WebResourceResponse
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Offline-first, atomic snapshot cache for the public Prism GitHub Pages repository.
 *
 * A complete Git tree is downloaded into a staging directory. The live snapshot is
 * swapped only after every selected runtime file is present and verified. Existing
 * snapshots remain untouched if a sync is interrupted or any required file fails.
 */
class PrismSnapshotManager(private val context: Context) {

    data class Progress(
        val completed: Int,
        val total: Int,
        val ready: Boolean,
        val message: String = "Loading Prism…"
    )

    data class SyncResult(
        val success: Boolean,
        val updated: Boolean,
        val hasUsableSnapshot: Boolean,
        val error: String? = null
    )

    private data class RemoteFile(
        val path: String,
        val sha: String,
        val size: Long,
        val priority: Int
    )

    private val cacheRoot = File(context.filesDir, CACHE_ROOT)
    private val activeDir = File(cacheRoot, ACTIVE_DIR)
    private val stagingDir = File(cacheRoot, STAGING_DIR)
    private val backupDir = File(cacheRoot, BACKUP_DIR)
    private val syncRunning = AtomicBoolean(false)
    private val ioPool = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "PrismSnapshotCoordinator").apply { isDaemon = true }
    }

    init {
        cacheRoot.mkdirs()
        recoverInterruptedSwap()
    }

    fun hasUsableSnapshot(): Boolean =
        File(activeDir, INDEX_PATH).isFile && File(activeDir, SNAPSHOT_MANIFEST).isFile

    fun activeIndexUrl(device: String): String =
        "$PAGES_ROOT$INDEX_PATH?runtime=android&device=$device"

    fun startSync(
        force: Boolean = false,
        onProgress: (Progress) -> Unit,
        onComplete: (SyncResult) -> Unit
    ) {
        if (!syncRunning.compareAndSet(false, true)) {
            onComplete(SyncResult(success = true, updated = false, hasUsableSnapshot = hasUsableSnapshot()))
            return
        }

        ioPool.execute {
            val hadSnapshot = hasUsableSnapshot()
            try {
                onProgress(Progress(0, 0, hadSnapshot))
                val tree = fetchRepositoryTree()
                val activeManifest = readManifest(File(activeDir, SNAPSHOT_MANIFEST))
                val currentTreeSha = activeManifest?.optString("treeSha").orEmpty()
                if (!force && hadSnapshot && currentTreeSha == tree.first) {
                    onComplete(SyncResult(true, false, true))
                    return@execute
                }

                val files = tree.second
                if (files.none { it.path == INDEX_PATH }) {
                    error("GitHub tree does not contain $INDEX_PATH")
                }

                prepareStaging()
                val previousFiles = activeManifest?.optJSONObject("files")
                val completed = AtomicInteger(0)
                val total = files.size
                val failure = ConcurrentLinkedQueue<String>()
                val queue = ConcurrentLinkedQueue(files.sortedWith(compareBy<RemoteFile> { it.priority }.thenBy { it.path }))
                val latch = CountDownLatch(DOWNLOAD_WORKERS)
                val workers = Executors.newFixedThreadPool(DOWNLOAD_WORKERS) { runnable ->
                    Thread(runnable, "PrismDownload").apply { isDaemon = true }
                }

                repeat(DOWNLOAD_WORKERS) {
                    workers.execute {
                        try {
                            while (failure.isEmpty()) {
                                val remote = queue.poll() ?: break
                                val target = safeFile(stagingDir, remote.path)
                                val previousSha = previousFiles?.optJSONObject(remote.path)?.optString("sha")
                                val activeFile = safeFile(activeDir, remote.path)

                                val ok = if (previousSha == remote.sha && activeFile.isFile && activeFile.length() > 0L) {
                                    copyVerified(activeFile, target, remote.size)
                                } else {
                                    downloadVerified(remote, target)
                                }

                                if (!ok) {
                                    failure.add(remote.path)
                                    break
                                }
                                val done = completed.incrementAndGet()
                                onProgress(Progress(done, total, hadSnapshot))
                            }
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await()
                workers.shutdownNow()
                if (failure.isNotEmpty()) {
                    stagingDir.deleteRecursively()
                    error("Snapshot download failed: ${failure.peek()}")
                }

                writeManifest(tree.first, files)
                validateStaging(files)
                activateStaging()
                onProgress(Progress(total, total, true))
                onComplete(SyncResult(true, true, true))
            } catch (t: Throwable) {
                stagingDir.deleteRecursively()
                onComplete(
                    SyncResult(
                        success = false,
                        updated = false,
                        hasUsableSnapshot = hasUsableSnapshot(),
                        error = t.message ?: t.javaClass.simpleName
                    )
                )
            } finally {
                syncRunning.set(false)
            }
        }
    }

    fun responseFor(url: String): WebResourceResponse? {
        val path = pagesPath(url) ?: return null
        val file = safeFile(activeDir, path)
        if (!file.isFile || file.length() <= 0L) return null
        return runCatching {
            val mime = mimeFor(path)
            val encoding = if (isTextMime(mime)) "UTF-8" else null
            WebResourceResponse(mime, encoding, FileInputStream(file)).apply {
                responseHeaders = mapOf(
                    "Access-Control-Allow-Origin" to "*",
                    "Cache-Control" to "public, max-age=31536000, immutable"
                )
            }
        }.getOrNull()
    }

    fun close() {
        ioPool.shutdownNow()
    }

    private fun fetchRepositoryTree(): Pair<String, List<RemoteFile>> {
        val connection = openConnection(API_TREE_URL)
        val body = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        val json = JSONObject(body)
        if (json.optBoolean("truncated", false)) error("GitHub tree response was truncated")
        val treeSha = json.optString("sha")
        if (treeSha.isBlank()) error("GitHub tree SHA missing")

        val files = mutableListOf<RemoteFile>()
        val array: JSONArray = json.getJSONArray("tree")
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            if (item.optString("type") != "blob") continue
            val path = item.optString("path")
            val size = item.optLong("size", -1L)
            val sha = item.optString("sha")
            if (!isRuntimeFile(path, size) || sha.isBlank()) continue
            files += RemoteFile(path, sha, size, priorityFor(path))
        }
        return treeSha to files
    }

    private fun isRuntimeFile(path: String, size: Long): Boolean {
        if (path.isBlank() || size == 0L || size > MAX_SINGLE_FILE_BYTES) return false
        val lower = path.lowercase(Locale.US)
        if (lower.startsWith(".github/") || lower.startsWith("node_modules/") || lower.startsWith("docs/")) return false
        if (lower.endsWith("generate-menu.js") || lower.endsWith("package-lock.json") || lower.endsWith("package.json")) return false
        val extension = lower.substringAfterLast('.', "")
        return extension in RUNTIME_EXTENSIONS
    }

    private fun priorityFor(path: String): Int {
        val lower = path.lowercase(Locale.US)
        val extension = lower.substringAfterLast('.', "")
        return when {
            lower == INDEX_PATH || lower.endsWith("/index.html") && !lower.contains("plugins/") -> 0
            lower.endsWith("menu.json") || lower.endsWith("manifest.json") || lower.endsWith("webmanifest") -> 0
            extension in setOf("css", "js", "json", "html") -> 1
            extension in IMAGE_EXTENSIONS && (lower.contains("tile") || lower.contains("icon") || lower.contains("background")) -> 2
            extension in AUDIO_EXTENSIONS && looksLikeQuran(path) -> 3
            extension in AUDIO_EXTENSIONS -> 3
            extension in IMAGE_EXTENSIONS -> 4
            else -> 5
        }
    }

    private fun looksLikeQuran(path: String): Boolean {
        val lower = path.lowercase(Locale.US)
        val filename = lower.substringAfterLast('/')
        return lower.contains("quran") || lower.contains("surah") ||
            Regex("^(a?\\d{1,3}([_-]\\d+)?)\\.(mp3|ogg|wav|m4a|aac)$").matches(filename)
    }

    private fun downloadVerified(remote: RemoteFile, target: File): Boolean {
        target.parentFile?.mkdirs()
        val partial = File(target.parentFile, target.name + PART_SUFFIX)
        partial.delete()
        return runCatching {
            val url = PAGES_ROOT + remote.path.split('/').joinToString("/") { encodePathSegment(it) }
            val connection = openConnection(url)
            BufferedInputStream(connection.inputStream).use { input ->
                BufferedOutputStream(FileOutputStream(partial)).use { output ->
                    input.copyTo(output, COPY_BUFFER)
                }
            }
            if (partial.length() <= 0L) error("Empty response for ${remote.path}")
            if (remote.size >= 0L && partial.length() != remote.size) {
                error("Size mismatch for ${remote.path}: ${partial.length()} != ${remote.size}")
            }
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
            target.isFile && target.length() > 0L
        }.getOrElse {
            partial.delete()
            false
        }
    }

    private fun copyVerified(source: File, target: File, expectedSize: Long): Boolean = runCatching {
        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = true)
        target.length() > 0L && (expectedSize < 0L || target.length() == expectedSize)
    }.getOrDefault(false)

    private fun openConnection(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.useCaches = false
        connection.setRequestProperty("Accept-Encoding", "identity")
        connection.setRequestProperty("User-Agent", "NoorPrismAndroid/${BuildConfig.VERSION_NAME}")
        connection.connect()
        if (connection.responseCode !in 200..299) {
            error("HTTP ${connection.responseCode} for $url")
        }
        return connection
    }

    private fun writeManifest(treeSha: String, files: List<RemoteFile>) {
        val root = JSONObject()
        root.put("treeSha", treeSha)
        root.put("createdAt", System.currentTimeMillis())
        val fileMap = JSONObject()
        files.forEach { remote ->
            fileMap.put(remote.path, JSONObject().apply {
                put("sha", remote.sha)
                put("size", remote.size)
            })
        }
        root.put("files", fileMap)
        File(stagingDir, SNAPSHOT_MANIFEST).writeText(root.toString(), StandardCharsets.UTF_8)
    }

    private fun readManifest(file: File): JSONObject? = runCatching {
        if (!file.isFile) return null
        JSONObject(file.readText(StandardCharsets.UTF_8))
    }.getOrNull()

    private fun validateStaging(files: List<RemoteFile>) {
        if (!File(stagingDir, INDEX_PATH).isFile) error("Staged index is missing")
        if (!File(stagingDir, SNAPSHOT_MANIFEST).isFile) error("Staged manifest is missing")
        files.forEach { remote ->
            val file = safeFile(stagingDir, remote.path)
            if (!file.isFile || file.length() <= 0L) error("Missing staged file: ${remote.path}")
            if (remote.size >= 0L && file.length() != remote.size) error("Invalid staged size: ${remote.path}")
        }
    }

    @Synchronized
    private fun activateStaging() {
        backupDir.deleteRecursively()
        if (activeDir.exists() && !activeDir.renameTo(backupDir)) {
            activeDir.copyRecursively(backupDir, overwrite = true)
            activeDir.deleteRecursively()
        }
        if (!stagingDir.renameTo(activeDir)) {
            stagingDir.copyRecursively(activeDir, overwrite = true)
            stagingDir.deleteRecursively()
        }
        if (!hasUsableSnapshot()) {
            activeDir.deleteRecursively()
            if (backupDir.exists()) backupDir.renameTo(activeDir)
            error("Snapshot activation failed")
        }
        backupDir.deleteRecursively()
    }

    private fun prepareStaging() {
        stagingDir.deleteRecursively()
        stagingDir.mkdirs()
    }

    private fun recoverInterruptedSwap() {
        if (!activeDir.exists() && backupDir.exists()) backupDir.renameTo(activeDir)
        stagingDir.deleteRecursively()
        if (activeDir.exists()) backupDir.deleteRecursively()
    }

    private fun pagesPath(url: String): String? = runCatching {
        val uri = URI(url)
        if (!uri.scheme.equals("https", true) || !uri.host.equals(PAGES_HOST, true)) return null
        val prefix = "/$REPOSITORY_NAME/"
        val rawPath = uri.path ?: return null
        if (!rawPath.startsWith(prefix)) return null
        val relative = rawPath.removePrefix(prefix).ifBlank { INDEX_PATH }
        if (relative.contains("..")) return null
        relative
    }.getOrNull()

    private fun safeFile(root: File, relativePath: String): File {
        val candidate = File(root, relativePath).canonicalFile
        val rootPath = root.canonicalFile.path + File.separator
        require(candidate.path.startsWith(rootPath)) { "Unsafe cache path" }
        return candidate
    }

    private fun mimeFor(path: String): String {
        val extension = path.substringAfterLast('.', "").lowercase(Locale.US)
        return when (extension) {
            "js" -> "application/javascript"
            "json", "webmanifest" -> "application/json"
            "css" -> "text/css"
            "html", "htm" -> "text/html"
            "svg" -> "image/svg+xml"
            "mp3" -> "audio/mpeg"
            "m4a", "aac" -> "audio/mp4"
            "ogg" -> "audio/ogg"
            "wav" -> "audio/wav"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
        }
    }

    private fun isTextMime(mime: String): Boolean =
        mime.startsWith("text/") || mime.contains("javascript") || mime.contains("json") || mime.contains("svg")

    private fun encodePathSegment(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    companion object {
        private const val REPOSITORY_NAME = "Quran"
        private const val PAGES_HOST = "amtunnoor.github.io"
        private const val PAGES_ROOT = "https://$PAGES_HOST/$REPOSITORY_NAME/"
        private const val API_TREE_URL = "https://api.github.com/repos/AmtunNoor/Quran/git/trees/main?recursive=1"
        private const val CACHE_ROOT = "prism_snapshot"
        private const val ACTIVE_DIR = "active"
        private const val STAGING_DIR = "staging"
        private const val BACKUP_DIR = "backup"
        private const val SNAPSHOT_MANIFEST = ".snapshot.json"
        private const val INDEX_PATH = "index.html"
        private const val PART_SUFFIX = ".part"
        private const val DOWNLOAD_WORKERS = 8
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 60_000
        private const val COPY_BUFFER = 64 * 1024
        private const val MAX_SINGLE_FILE_BYTES = 150L * 1024L * 1024L

        private val AUDIO_EXTENSIONS = setOf("mp3", "ogg", "wav", "m4a", "aac")
        private val IMAGE_EXTENSIONS = setOf("webp", "png", "jpg", "jpeg", "gif", "svg", "avif")
        private val RUNTIME_EXTENSIONS = setOf(
            "html", "htm", "css", "js", "json", "webmanifest", "txt",
            "webp", "png", "jpg", "jpeg", "gif", "svg", "avif",
            "mp3", "ogg", "wav", "m4a", "aac",
            "woff", "woff2", "ttf", "otf"
        )
    }
}
