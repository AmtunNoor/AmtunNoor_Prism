package com.noor.prism

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.MimeTypeMap
import android.webkit.ServiceWorkerController
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.zip.ZipInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val handler = Handler(Looper.getMainLooper())

    private val remoteBaseUrl = "https://amtunnoor.github.io/Quran/"
    private val remoteZipUrl = "https://github.com/AmtunNoor/Quran/archive/refs/heads/main.zip"
    private val updateCheckIntervalMs = 12L * 60L * 60L * 1000L

    private val prefs by lazy { getSharedPreferences("prism_app_state", MODE_PRIVATE) }
    private val bundleDir by lazy { File(filesDir, "prism_bundle_v${BuildConfig.VERSION_CODE}") }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setImmersiveFlags()

        webView = WebView(this)
        webView.setBackgroundColor(Color.rgb(15, 23, 42))
        setContentView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        configureWebView()
        ensureBundledSnapshotReady()
        pruneOldSnapshotsSafely()

        if (savedInstanceState == null) {
            webView.loadUrl(activeLocalIndexUrl())
            updateRemoteSnapshotInBackgroundIfDue()
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    private fun setImmersiveFlags() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
    }

    private fun activeSnapshotDir(): File {
        val activePath = prefs.getString("active_snapshot_path", null)
        return activePath?.let { File(it) }?.takeIf { File(it, "index.html").exists() } ?: bundleDir
    }

    private fun activeLocalIndexUrl(): String {
        return File(activeSnapshotDir(), "index.html").toURI().toString() + "?app=prism&local=1&apk=${BuildConfig.VERSION_CODE}"
    }

    private fun ensureBundledSnapshotReady() {
        val marker = File(bundleDir, ".bundle_ready")
        if (marker.exists() && File(bundleDir, "index.html").exists()) return
        try {
            if (bundleDir.exists()) bundleDir.deleteRecursively()
            bundleDir.mkdirs()
            copyAssetFolder("prism", bundleDir)
            marker.writeText("ready:${BuildConfig.VERSION_CODE}")
        } catch (_: Throwable) {
            // If copy fails, WebView will still attempt whatever exists.
        }
    }

    private fun copyAssetFolder(assetPath: String, dest: File) {
        val children = assets.list(assetPath) ?: emptyArray()
        if (children.isEmpty()) {
            dest.parentFile?.mkdirs()
            assets.open(assetPath).use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            return
        }
        dest.mkdirs()
        children.forEach { child ->
            copyAssetFolder("$assetPath/$child", File(dest, child))
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.mediaPlaybackRequiresUserGesture = false
        s.loadsImagesAutomatically = true
        s.blockNetworkImage = false
        s.allowFileAccess = true
        s.allowContentAccess = true
        s.allowFileAccessFromFileURLs = true
        s.allowUniversalAccessFromFileURLs = true
        s.javaScriptCanOpenWindowsAutomatically = false
        s.setSupportZoom(false)
        s.builtInZoomControls = false
        s.displayZoomControls = false
        s.loadWithOverviewMode = true
        s.useWideViewPort = true
        s.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            s.safeBrowsingEnabled = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val sw = ServiceWorkerController.getInstance().serviceWorkerWebSettings
                sw.allowContentAccess = true
                sw.allowFileAccess = true
                sw.blockNetworkLoads = false
                sw.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            } catch (_: Throwable) {}
        }

        WebView.setWebContentsDebuggingEnabled(false)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                view.loadUrl(request.url.toString())
                return true
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                return interceptMissingLocalAsset(request) ?: super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                runAppShellSelfHealChecks()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                super.onReceivedError(view, request, error)
            }
        }
    }

    /**
     * Keeps APK small: heavy MP3/APK files are not bundled.
     * If local HTML asks for a missing local asset, stream it from GitHub Pages.
     */
    private fun interceptMissingLocalAsset(request: WebResourceRequest): WebResourceResponse? {
        if (!isOnline()) return null
        val uri = request.url ?: return null
        if (uri.scheme != "file") return null
        val path = uri.path ?: return null
        val localFile = File(path)
        if (localFile.exists()) return null
        val rel = relativePathInsideKnownSnapshot(localFile) ?: return null
        if (rel.isBlank() || rel.contains("..")) return null
        return try {
            val remoteUrl = URL(remoteBaseUrl + rel.split('/').joinToString("/") { Uri.encode(it) })
            val connection = (remoteUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 30000
                instanceFollowRedirects = true
                useCaches = true
                setRequestProperty("Cache-Control", "max-age=86400")
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                connection.disconnect()
                null
            } else {
                WebResourceResponse(
                    guessMimeType(rel),
                    null,
                    connection.inputStream
                )
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun relativePathInsideKnownSnapshot(localFile: File): String? {
        val candidates = mutableListOf<File>()
        candidates.add(bundleDir)
        prefs.getString("active_snapshot_path", null)?.let { candidates.add(File(it)) }
        filesDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("prism_remote_") }?.let { candidates.addAll(it) }
        return candidates.distinctBy { it.absolutePath }.firstNotNullOfOrNull { root ->
            try {
                val rootPath = root.canonicalPath + File.separator
                val filePath = localFile.canonicalPath
                if (filePath.startsWith(rootPath)) filePath.removePrefix(rootPath).replace(File.separatorChar, '/') else null
            } catch (_: Throwable) { null }
        }
    }

    private fun guessMimeType(path: String): String {
        val ext = path.substringAfterLast('.', "").lowercase(Locale.US)
        return when (ext) {
            "js" -> "application/javascript"
            "css" -> "text/css"
            "json" -> "application/json"
            "html" -> "text/html"
            "mp3" -> "audio/mpeg"
            "webp" -> "image/webp"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "svg" -> "image/svg+xml"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        }
    }

    private fun runAppShellSelfHealChecks() {
        longArrayOf(250, 800, 1600, 3000, 5000, 8000).forEach { delay ->
            handler.postDelayed({ injectPrismAppShellFixes(webView) }, delay)
        }
    }

    private fun injectPrismAppShellFixes(view: WebView) {
        val js = """
            (function(){
              function shouldForceTopbar(){
                try{
                  var body = document.body;
                  if(!body) return false;
                  var cls = body.classList;
                  if(cls.contains('landing-mode')) return false;
                  if(cls.contains('plugin-letters') || cls.contains('plugin-angels') || cls.contains('plugin-pillars') || cls.contains('plugin-months') || cls.contains('plugin-numbers')) return false;
                  var topbar = document.getElementById('topbar') || document.querySelector('.topbar');
                  return !!topbar;
                }catch(e){ return false; }
              }
              function forceTopbarVisible(){
                try{
                  var topbar = document.getElementById('topbar') || document.querySelector('.topbar');
                  if(!topbar || !shouldForceTopbar()) return false;
                  if(window.__prismHealTopbar){ try{ window.__prismHealTopbar(); }catch(e){} }
                  var style = window.getComputedStyle(topbar);
                  var rect = topbar.getBoundingClientRect();
                  var hidden = style.display === 'none' || style.visibility === 'hidden' || Number(style.opacity) === 0 || rect.height < 8;
                  if(hidden){
                    topbar.style.setProperty('display','flex','important');
                    topbar.style.setProperty('visibility','visible','important');
                    topbar.style.setProperty('opacity','1','important');
                    topbar.style.setProperty('pointer-events','auto','important');
                    topbar.style.setProperty('z-index','999999','important');
                    document.body.classList.add('prism-android-topbar-healed','show-topbar');
                  }
                  return true;
                }catch(e){ return false; }
              }
              document.documentElement.classList.add('prism-android-app','prism-local-first-app');
              if(document.body) document.body.classList.add('prism-android-app','prism-local-first-app');
              forceTopbarVisible();
              if(!window.__prismAndroidTopbarWatchdog){
                window.__prismAndroidTopbarWatchdog = setInterval(forceTopbarVisible, 1500);
                setTimeout(function(){ try{ clearInterval(window.__prismAndroidTopbarWatchdog); window.__prismAndroidTopbarWatchdog = null; }catch(e){} }, 15000);
              }
            })();
        """.trimIndent()
        view.evaluateJavascript(js, null)
    }

    private fun updateRemoteSnapshotInBackgroundIfDue() {
        if (!isOnline()) return
        val now = System.currentTimeMillis()
        val last = prefs.getLong("last_snapshot_check", 0L)
        if (now - last < updateCheckIntervalMs) return
        prefs.edit().putLong("last_snapshot_check", now).apply()
        updateRemoteSnapshotInBackground()
    }

    private fun updateRemoteSnapshotInBackground() {
        Thread {
            try {
                val newDir = File(filesDir, "prism_remote_${System.currentTimeMillis()}")
                val tmpZip = File(cacheDir, "quran-main.zip")
                downloadToFile(remoteZipUrl, tmpZip)
                val tmpExtract = File(cacheDir, "prism_extract_${System.currentTimeMillis()}")
                if (tmpExtract.exists()) tmpExtract.deleteRecursively()
                tmpExtract.mkdirs()
                unzipRuntimeOnly(tmpZip, tmpExtract)
                val root = tmpExtract.listFiles()?.firstOrNull { it.isDirectory && File(it, "index.html").exists() }
                    ?: tmpExtract.takeIf { File(it, "index.html").exists() }
                    ?: return@Thread
                if (newDir.exists()) newDir.deleteRecursively()
                copyDirectoryRuntimeOnly(root, newDir)
                if (File(newDir, "index.html").exists()) {
                    prefs.edit().putString("active_snapshot_path", newDir.absolutePath).apply()
                } else {
                    newDir.deleteRecursively()
                }
                tmpExtract.deleteRecursively()
                tmpZip.delete()
                pruneOldSnapshotsSafely()
            } catch (_: Throwable) {}
        }.start()
    }

    private fun downloadToFile(urlText: String, out: File) {
        val connection = (URL(urlText).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 45000
            instanceFollowRedirects = true
        }
        connection.inputStream.use { input ->
            FileOutputStream(out).use { output -> input.copyTo(output) }
        }
        connection.disconnect()
    }

    private fun unzipRuntimeOnly(zip: File, dest: File) {
        ZipInputStream(zip.inputStream().buffered()).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                val normalized = entry.name.replace('\\', '/')
                val nameWithoutRoot = normalized.substringAfter('/', normalized)
                if (shouldExcludeFromLocalSnapshot(nameWithoutRoot) || entry.isDirectory) {
                    if (entry.isDirectory && !shouldExcludeFromLocalSnapshot(nameWithoutRoot)) {
                        File(dest, normalized).mkdirs()
                    }
                    zis.closeEntry()
                    continue
                }
                val outFile = File(dest, normalized)
                val canonicalDest = dest.canonicalPath + File.separator
                val canonicalOut = outFile.canonicalPath
                if (!canonicalOut.startsWith(canonicalDest)) {
                    zis.closeEntry()
                    continue
                }
                outFile.parentFile?.mkdirs()
                FileOutputStream(outFile).use { output -> zis.copyTo(output) }
                zis.closeEntry()
            }
        }
    }

    private fun copyDirectoryRuntimeOnly(src: File, dest: File) {
        if (src.isDirectory) {
            dest.mkdirs()
            src.listFiles()?.forEach { child -> copyDirectoryRuntimeOnly(child, File(dest, child.name)) }
        } else {
            val rel = src.name
            if (shouldExcludeFromLocalSnapshot(rel)) return
            dest.parentFile?.mkdirs()
            src.inputStream().use { input -> FileOutputStream(dest).use { output -> input.copyTo(output) } }
        }
    }

    private fun shouldExcludeFromLocalSnapshot(path: String): Boolean {
        val p = path.trimStart('/').lowercase(Locale.US)
        if (p.isBlank()) return false
        return p.startsWith(".git/") ||
            p.startsWith(".github/") ||
            p == "readme" || p.startsWith("readme.") ||
            p.endsWith(".apk") ||
            p.endsWith(".mp3")
    }

    private fun pruneOldSnapshotsSafely() {
        try {
            val active = prefs.getString("active_snapshot_path", null)
            filesDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("prism_remote_") && it.absolutePath != active }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(1)
                ?.forEach { it.deleteRecursively() }
        } catch (_: Throwable) {}
    }

    private fun isOnline(): Boolean {
        return try {
            val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Throwable) { false }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        try { webView.saveState(outState) } catch (_: Throwable) {}
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return if (webView.canGoBack()) {
                webView.goBack()
                true
            } else {
                moveTaskToBack(true)
                true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) setImmersiveFlags()
    }
}
