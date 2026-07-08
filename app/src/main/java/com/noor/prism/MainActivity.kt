package com.noor.prism

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.ServiceWorkerController
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val handler = Handler(Looper.getMainLooper())
    private val remoteZipUrl = "https://github.com/AmtunNoor/Quran/archive/refs/heads/main.zip"

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
            updateRemoteSnapshotInBackground()
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

    private fun activeLocalIndexUrl(): String {
        val activePath = prefs.getString("active_snapshot_path", null)
        val active = activePath?.let { File(it) }?.takeIf { File(it, "index.html").exists() }
            ?: bundleDir
        return File(active, "index.html").toURI().toString() + "?app=prism&local=1&apk=${BuildConfig.VERSION_CODE}"
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

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                runAppShellSelfHealChecks()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                super.onReceivedError(view, request, error)
            }
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

    private fun updateRemoteSnapshotInBackground() {
        if (!isOnline()) return
        Thread {
            try {
                val newDir = File(filesDir, "prism_remote_${System.currentTimeMillis()}")
                val tmpZip = File(cacheDir, "quran-main.zip")
                downloadToFile(remoteZipUrl, tmpZip)
                val tmpExtract = File(cacheDir, "prism_extract_${System.currentTimeMillis()}")
                if (tmpExtract.exists()) tmpExtract.deleteRecursively()
                tmpExtract.mkdirs()
                unzip(tmpZip, tmpExtract)
                val root = tmpExtract.listFiles()?.firstOrNull { it.isDirectory && File(it, "index.html").exists() }
                    ?: tmpExtract.takeIf { File(it, "index.html").exists() }
                    ?: return@Thread
                if (newDir.exists()) newDir.deleteRecursively()
                copyDirectory(root, newDir)
                if (File(newDir, "index.html").exists()) {
                    prefs.edit().putString("active_snapshot_path", newDir.absolutePath).apply()
                } else {
                    newDir.deleteRecursively()
                }
                tmpExtract.deleteRecursively()
                tmpZip.delete()
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

    private fun unzip(zip: File, dest: File) {
        ZipInputStream(zip.inputStream().buffered()).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                val outFile = File(dest, entry.name)
                val canonicalDest = dest.canonicalPath + File.separator
                val canonicalOut = outFile.canonicalPath
                if (!canonicalOut.startsWith(canonicalDest)) continue
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { output -> zis.copyTo(output) }
                }
                zis.closeEntry()
            }
        }
    }

    private fun copyDirectory(src: File, dest: File) {
        if (src.isDirectory) {
            dest.mkdirs()
            src.listFiles()?.forEach { child -> copyDirectory(child, File(dest, child.name)) }
        } else {
            dest.parentFile?.mkdirs()
            src.inputStream().use { input -> FileOutputStream(dest).use { output -> input.copyTo(output) } }
        }
    }

    private fun pruneOldSnapshotsSafely() {
        try {
            val active = prefs.getString("active_snapshot_path", null)
            filesDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("prism_remote_") && it.absolutePath != active }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(2)
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
