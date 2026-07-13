package com.noor.prism

import android.content.Context
import android.webkit.MimeTypeMap
import android.webkit.WebResourceResponse
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Manifest-driven, persistent and atomic offline store for Prism. */
class PrismSnapshotManager(private val context: Context) {

    data class Progress(val completed: Int, val total: Int)

    data class SyncResult(
        val success: Boolean,
        val updated: Boolean,
        val hasUsableSnapshot: Boolean,
        val error: String? = null
    )

    private data class RemoteFile(
        val path: String,
        val url: String,
        val size: Long,
        val sha256: String,
        val priority: Int,
        val lane: String
    )

    private data class RemoteSnapshot(
        val version: String,
        val entryPoint: String,
        val aliases: Map<String, String>,
        val files: List<RemoteFile>,
        val rawJson: String
    )

    private val cacheRoot = File(context.filesDir, CACHE_ROOT)
    private val activeDir = File(cacheRoot, ACTIVE_DIR)
    private val stagingDir = File(cacheRoot, STAGING_DIR)
    private val backupDir = File(cacheRoot, BACKUP_DIR)
    private val coordinator = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "PrismSnapshotCoordinator").apply { isDaemon = true }
    }
    private val syncRunning = AtomicBoolean(false)
    private val verifiedStagingPaths = Collections.synchronizedSet(mutableSetOf<String>())

    init {
        cacheRoot.mkdirs()
        recoverInterruptedSwap()
    }

    fun hasUsableSnapshot(): Boolean {
        val state = readJson(File(activeDir, LOCAL_STATE)) ?: return false
        val entry = state.optString("entryPoint", INDEX_PATH)
        return state.optInt("schema", 0) == 1 && safeFile(activeDir, entry).isFile
    }

    fun activeIndexUrl(device: String): String =
        "$PAGES_ROOT$INDEX_PATH?runtime=android&device=$device"

    fun startSync(
        force: Boolean = false,
        onReady: () -> Unit = {},
        onProgress: (Progress) -> Unit = {},
        onComplete: (SyncResult) -> Unit
    ) {
        if (!syncRunning.compareAndSet(false, true)) {
            onComplete(SyncResult(true, false, hasUsableSnapshot()))
            return
        }

        coordinator.execute {
            val readySent = AtomicBoolean(false)
            try {
                val remote = fetchManifest()
                val current = readJson(File(activeDir, LOCAL_STATE))
                val currentVersion = current?.optString("version").orEmpty()
                if (!force && hasUsableSnapshot() && currentVersion == remote.version) {
                    readySent.set(true)
                    onReady()
                    onComplete(SyncResult(true, false, true))
                    return@execute
                }

                prepareStaging(remote)
                val previousFiles = current?.optJSONArray("files")?.toPathMap().orEmpty()
                val ordered = buildFairDownloadOrder(remote.files)
                val criticalRemaining = AtomicInteger(ordered.count { it.priority <= CRITICAL_PRIORITY })
                if (criticalRemaining.get() == 0 && readySent.compareAndSet(false, true)) onReady()

                val queue = ConcurrentLinkedQueue(ordered)
                val completed = AtomicInteger(0)
                val failure = ConcurrentLinkedQueue<String>()
                val latch = CountDownLatch(DOWNLOAD_WORKERS)
                val workers = Executors.newFixedThreadPool(DOWNLOAD_WORKERS) { runnable ->
                    Thread(runnable, "PrismDownload").apply { isDaemon = true }
                }

                onProgress(Progress(0, ordered.size))
                repeat(DOWNLOAD_WORKERS) {
                    workers.execute {
                        try {
                            while (failure.isEmpty()) {
                                val item = queue.poll() ?: break
                                val target = safeFile(stagingDir, item.path)
                                val active = safeFile(activeDir, item.path)
                                val previous = previousFiles[item.path]

                                val ok = when {
                                    isVerified(target, item) -> true
                                    previous != null &&
                                        previous.optString("sha256") == item.sha256 &&
                                        previous.optLong("size", -1L) == item.size &&
                                        isVerified(active, item) -> copyVerified(active, target, item)
                                    else -> downloadWithRetry(item, target)
                                }

                                if (!ok) {
                                    failure.add(item.path)
                                    break
                                }

                                verifiedStagingPaths.add(item.path)
                                if (item.priority <= CRITICAL_PRIORITY &&
                                    criticalRemaining.decrementAndGet() == 0 &&
                                    readySent.compareAndSet(false, true)
                                ) {
                                    onReady()
                                }
                                onProgress(Progress(completed.incrementAndGet(), ordered.size))
                            }
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await()
                workers.shutdownNow()
                if (failure.isNotEmpty()) error("Could not download ${failure.peek()}")

                writeState(remote)
                validateStaging(remote)
                activateStaging()
                if (readySent.compareAndSet(false, true)) onReady()
                onComplete(SyncResult(true, true, true))
            } catch (error: Throwable) {
                onComplete(
                    SyncResult(
                        success = false,
                        updated = false,
                        hasUsableSnapshot = hasUsableSnapshot(),
                        error = error.message ?: error.javaClass.simpleName
                    )
                )
            } finally {
                syncRunning.set(false)
            }
        }
    }

    /** Serves active files first, then individually verified first-sync files. */
    fun responseFor(url: String, rangeHeader: String? = null): WebResourceResponse? {
        val requestedPath = pagesPath(url) ?: return null
        val state = readJson(File(activeDir, LOCAL_STATE))
        val resolvedPath = resolveAlias(requestedPath, state?.optJSONObject("aliases"))

        val active = safeFile(activeDir, resolvedPath)
        if (active.isFile && active.length() > 0L) return responseForFile(active, resolvedPath, rangeHeader)

        if (verifiedStagingPaths.contains(resolvedPath)) {
            val staged = safeFile(stagingDir, resolvedPath)
            if (staged.isFile && staged.length() > 0L) return responseForFile(staged, resolvedPath, rangeHeader)
        }
        return null
    }

    fun close() {
        coordinator.shutdownNow()
    }

    private fun fetchManifest(): RemoteSnapshot {
        var lastError: Throwable? = null
        var body: String? = null
        for (base in MANIFEST_URLS) {
            try {
                body = openConnection("$base?t=${System.currentTimeMillis()}")
                    .inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                break
            } catch (error: Throwable) {
                lastError = error
            }
        }
        val raw = body ?: throw (lastError ?: IllegalStateException("Snapshot manifest unavailable"))
        val root = JSONObject(raw)
        if (root.optInt("schema", 0) != 1) error("Unsupported snapshot schema")
        val version = root.optString("version")
        val entryPoint = root.optString("entryPoint", INDEX_PATH)
        if (!SHA256_REGEX.matches(version) || entryPoint.isBlank()) error("Invalid snapshot metadata")

        val aliases = mutableMapOf<String, String>()
        root.optJSONObject("aliases")?.let { json ->
            json.keys().forEach { from ->
                val to = json.optString(from)
                validatePath(from)
                validatePath(to)
                aliases[from] = to
            }
        }

        val files = mutableListOf<RemoteFile>()
        val seen = HashSet<String>()
        val array = root.optJSONArray("files") ?: error("Snapshot files missing")
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val path = item.optString("path")
            val sourceUrl = item.optString("url")
            val size = item.optLong("size", -1L)
            val hash = item.optString("sha256").lowercase(Locale.US)
            validatePath(path)
            validateSourceUrl(sourceUrl)
            if (!seen.add(path)) error("Duplicate snapshot path: $path")
            if (size <= 0L || size > MAX_SINGLE_FILE_BYTES) error("Invalid size: $path")
            if (!SHA256_REGEX.matches(hash)) error("Invalid hash: $path")
            files += RemoteFile(
                path = path,
                url = sourceUrl,
                size = size,
                sha256 = hash,
                priority = item.optInt("priority", 3).coerceIn(0, 9),
                lane = item.optString("lane", "general")
            )
        }
        if (files.none { it.path == entryPoint } || files.none { it.path == MENU_PATH }) {
            error("Snapshot is missing Prism core files")
        }
        return RemoteSnapshot(version, entryPoint, aliases, files, raw)
    }

    /** Interleaves Quran and module audio within each priority instead of serial categories. */
    private fun buildFairDownloadOrder(files: List<RemoteFile>): List<RemoteFile> {
        val output = mutableListOf<RemoteFile>()
        files.groupBy { it.priority }.toSortedMap().forEach { (_, group) ->
            val general = ArrayDeque(group.filter { it.lane == "general" }.sortedBy { it.path })
            val quran = ArrayDeque(group.filter { it.lane == "quran-audio" }.sortedBy { quranAudioRank(it.path) })
            val modules = ArrayDeque(group.filter { it.lane == "module-audio" }.sortedBy { it.path })
            while (general.isNotEmpty() || quran.isNotEmpty() || modules.isNotEmpty()) {
                if (general.isNotEmpty()) output += general.removeFirst()
                if (quran.isNotEmpty()) output += quran.removeFirst()
                if (modules.isNotEmpty()) output += modules.removeFirst()
                if (quran.isNotEmpty()) output += quran.removeFirst()
            }
        }
        return output
    }

    private fun quranAudioRank(path: String): String {
        val lower = path.lowercase(Locale.US)
        return when {
            lower == "a5.mp3" -> "000-$lower"
            lower.startsWith("a") -> "010-$lower"
            lower.startsWith("listen/") -> "020-$lower"
            lower.startsWith("learn/") -> "030-$lower"
            else -> "040-$lower"
        }
    }

    private fun downloadWithRetry(remote: RemoteFile, target: File): Boolean {
        repeat(DOWNLOAD_ATTEMPTS) { attempt ->
            if (downloadVerified(remote, target)) return true
            if (attempt + 1 < DOWNLOAD_ATTEMPTS) Thread.sleep(600L * (attempt + 1))
        }
        return false
    }

    private fun downloadVerified(remote: RemoteFile, target: File): Boolean {
        target.parentFile?.mkdirs()
        val partial = File(target.parentFile, target.name + PART_SUFFIX)
        partial.delete()
        return runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            val connection = openConnection(remote.url)
            BufferedInputStream(connection.inputStream).use { input ->
                BufferedOutputStream(FileOutputStream(partial)).use { output ->
                    val buffer = ByteArray(COPY_BUFFER)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        total += count
                        if (total > remote.size) error("Oversized response")
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                    }
                }
            }
            if (partial.length() != remote.size) error("Size mismatch")
            if (digest.digest().toHex() != remote.sha256) error("Hash mismatch")
            moveReplacing(partial, target)
            true
        }.getOrElse {
            partial.delete()
            false
        }
    }

    private fun copyVerified(source: File, target: File, remote: RemoteFile): Boolean = runCatching {
        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = true)
        isVerified(target, remote)
    }.getOrDefault(false)

    private fun isVerified(file: File, remote: RemoteFile): Boolean {
        if (!file.isFile || file.length() != remote.size) return false
        return runCatching { sha256(file) == remote.sha256 }.getOrDefault(false)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(COPY_BUFFER)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count > 0) digest.update(buffer, 0, count)
            }
        }
        return digest.digest().toHex()
    }

    private fun prepareStaging(remote: RemoteSnapshot) {
        verifiedStagingPaths.clear()
        val stagedManifest = readJson(File(stagingDir, STAGING_STATE))
        if (stagedManifest?.optString("version") != remote.version) {
            stagingDir.deleteRecursively()
            stagingDir.mkdirs()
        } else {
            stagingDir.mkdirs()
        }
        File(stagingDir, STAGING_STATE).writeText(
            JSONObject().put("version", remote.version).toString(),
            StandardCharsets.UTF_8
        )
    }

    private fun writeState(remote: RemoteSnapshot) {
        val parsed = JSONObject(remote.rawJson)
        parsed.put("activatedAt", System.currentTimeMillis())
        File(stagingDir, LOCAL_STATE).writeText(parsed.toString(), StandardCharsets.UTF_8)
        File(stagingDir, STAGING_STATE).delete()
    }

    private fun validateStaging(remote: RemoteSnapshot) {
        if (!safeFile(stagingDir, remote.entryPoint).isFile) error("Staged entry point missing")
        remote.files.forEach { item ->
            if (!isVerified(safeFile(stagingDir, item.path), item)) error("Invalid staged file: ${item.path}")
        }
        if (!File(stagingDir, LOCAL_STATE).isFile) error("Staged metadata missing")
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
        verifiedStagingPaths.clear()
    }

    private fun recoverInterruptedSwap() {
        if (!activeDir.exists() && backupDir.exists()) backupDir.renameTo(activeDir)
        if (activeDir.exists()) backupDir.deleteRecursively()
        stagingDir.mkdirs() // retained for resumable verified downloads
    }

    private fun responseForFile(file: File, path: String, rangeHeader: String?): WebResourceResponse? = runCatching {
        val mime = mimeFor(path)
        val encoding = if (isTextMime(mime)) "UTF-8" else null

        if (path == MENU_PATH) {
            val text = file.readText(StandardCharsets.UTF_8)
                .replace(LEGACY_FILE_BASE, PAGES_ROOT)
                .replace("${PAGES_ROOT}quran/index.html", "${PAGES_ROOT}index.html")
            val bytes = text.toByteArray(StandardCharsets.UTF_8)
            return@runCatching WebResourceResponse(
                mime, encoding, 200, "OK",
                mapOf(
                    "Access-Control-Allow-Origin" to "*",
                    "Cache-Control" to "no-store",
                    "Content-Length" to bytes.size.toString()
                ),
                ByteArrayInputStream(bytes)
            )
        }

        val length = file.length()
        val range = parseRange(rangeHeader, length)
        if (range != null) {
            val (start, end) = range
            val stream = FileInputStream(file)
            skipFully(stream, start)
            val count = end - start + 1L
            WebResourceResponse(
                mime, encoding, 206, "Partial Content",
                mapOf(
                    "Access-Control-Allow-Origin" to "*",
                    "Cache-Control" to "no-store",
                    "Accept-Ranges" to "bytes",
                    "Content-Range" to "bytes $start-$end/$length",
                    "Content-Length" to count.toString()
                ),
                LimitedInputStream(stream, count)
            )
        } else {
            WebResourceResponse(
                mime, encoding, 200, "OK",
                mapOf(
                    "Access-Control-Allow-Origin" to "*",
                    "Cache-Control" to "no-store",
                    "Accept-Ranges" to "bytes",
                    "Content-Length" to length.toString()
                ),
                FileInputStream(file)
            )
        }
    }.getOrNull()

    private fun parseRange(header: String?, length: Long): Pair<Long, Long>? {
        if (header.isNullOrBlank() || !header.startsWith("bytes=")) return null
        val value = header.removePrefix("bytes=").substringBefore(',').trim()
        val parts = value.split('-', limit = 2)
        if (parts.size != 2) return null
        val start = parts[0].toLongOrNull() ?: return null
        val requestedEnd = parts[1].toLongOrNull() ?: (length - 1L)
        if (start < 0L || start >= length) return null
        val end = requestedEnd.coerceIn(start, length - 1L)
        return start to end
    }

    private fun skipFully(stream: InputStream, bytes: Long) {
        var remaining = bytes
        while (remaining > 0L) {
            val skipped = stream.skip(remaining)
            if (skipped > 0L) {
                remaining -= skipped
            } else if (stream.read() >= 0) {
                remaining--
            } else {
                error("Unexpected end of file")
            }
        }
    }

    private class LimitedInputStream(
        private val source: InputStream,
        private var remaining: Long
    ) : InputStream() {
        override fun read(): Int {
            if (remaining <= 0L) return -1
            val value = source.read()
            if (value >= 0) remaining--
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (remaining <= 0L) return -1
            val allowed = minOf(length.toLong(), remaining).toInt()
            val count = source.read(buffer, offset, allowed)
            if (count > 0) remaining -= count.toLong()
            return count
        }

        override fun close() = source.close()
    }

    private fun resolveAlias(path: String, aliases: JSONObject?): String =
        aliases?.optString(path).orEmpty().ifBlank { path }

    private fun pagesPath(url: String): String? = runCatching {
        val uri = URI(url)
        if (!uri.scheme.equals("https", true) || !uri.host.equals(PAGES_HOST, true)) return null
        val prefix = "/$REPOSITORY_NAME/"
        val raw = uri.path ?: return null
        if (!raw.startsWith(prefix)) return null
        raw.removePrefix(prefix).ifBlank { INDEX_PATH }.also(::validatePath)
    }.getOrNull()

    private fun validatePath(path: String) {
        require(path.isNotBlank() && !path.startsWith("/") && !path.contains("..") && !path.contains('\\')) {
            "Unsafe snapshot path"
        }
    }

    private fun validateSourceUrl(value: String) {
        val uri = URI(value)
        val validRaw = uri.host.equals(RAW_HOST, true) && uri.path.startsWith("/$RAW_OWNER/$REPOSITORY_NAME/")
        val validPages = uri.host.equals(PAGES_HOST, true) && uri.path.startsWith("/$REPOSITORY_NAME/")
        require(uri.scheme.equals("https", true) && (validRaw || validPages)) { "Unsupported snapshot source" }
    }

    private fun safeFile(root: File, relativePath: String): File {
        val candidate = File(root, relativePath).canonicalFile
        val rootPath = root.canonicalFile.path + File.separator
        require(candidate.path.startsWith(rootPath)) { "Unsafe cache path" }
        return candidate
    }

    private fun openConnection(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.useCaches = false
        connection.setRequestProperty("Accept-Encoding", "identity")
        connection.setRequestProperty("Cache-Control", "no-cache")
        connection.setRequestProperty("User-Agent", "NoorPrismAndroid/${BuildConfig.VERSION_NAME}")
        connection.connect()
        if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
        return connection
    }

    private fun readJson(file: File): JSONObject? = runCatching {
        if (!file.isFile) return null
        JSONObject(file.readText(StandardCharsets.UTF_8))
    }.getOrNull()

    private fun JSONArray.toPathMap(): Map<String, JSONObject> {
        val map = HashMap<String, JSONObject>(length())
        for (index in 0 until length()) {
            val item = getJSONObject(index)
            map[item.optString("path")] = item
        }
        return map
    }

    private fun moveReplacing(source: File, target: File) {
        if (target.exists()) target.delete()
        if (!source.renameTo(target)) {
            source.copyTo(target, overwrite = true)
            source.delete()
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

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

    companion object {
        private const val REPOSITORY_NAME = "Quran"
        private const val RAW_OWNER = "AmtunNoor"
        private const val PAGES_HOST = "amtunnoor.github.io"
        private const val RAW_HOST = "raw.githubusercontent.com"
        private const val PAGES_ROOT = "https://$PAGES_HOST/$REPOSITORY_NAME/"
        private val MANIFEST_URLS = listOf(
            "https://$RAW_HOST/$RAW_OWNER/$REPOSITORY_NAME/main/snapshot.json",
            "${PAGES_ROOT}snapshot.json"
        )
        private const val LEGACY_FILE_BASE = "file:///data/user/0/com.noor.prism/files/"
        private const val CACHE_ROOT = "prism_snapshot_v3"
        private const val ACTIVE_DIR = "active"
        private const val STAGING_DIR = "staging"
        private const val BACKUP_DIR = "backup"
        private const val LOCAL_STATE = ".snapshot-state.json"
        private const val STAGING_STATE = ".staging-state.json"
        private const val INDEX_PATH = "index.html"
        private const val MENU_PATH = "menu.json"
        private const val PART_SUFFIX = ".part"
        private const val DOWNLOAD_WORKERS = 8
        private const val DOWNLOAD_ATTEMPTS = 3
        private const val CRITICAL_PRIORITY = 1
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 90_000
        private const val COPY_BUFFER = 64 * 1024
        private const val MAX_SINGLE_FILE_BYTES = 150L * 1024L * 1024L
        private val SHA256_REGEX = Regex("^[0-9a-f]{64}$")
    }
}
