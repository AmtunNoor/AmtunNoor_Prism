package com.noor.prism

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.ServiceWorkerController
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class MainActivity : AppCompatActivity() {

    private lateinit var root: FrameLayout
    private lateinit var webView: WebView
    private lateinit var splash: FrameLayout
    private val handler = Handler(Looper.getMainLooper())
    private val inFlight = ConcurrentHashMap<String, Any>()
    @Volatile private var audioPlaying = false
    @Volatile private var pageReady = false

    private val prismUrl: String by lazy {
        val device = if (isTelevision()) "tv" else "phone"
        "https://amtunnoor.github.io/Quran/index.html?runtime=android&device=$device"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        handler.postDelayed({
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        }, 1400L)

        enableImmersiveMode()
        requestNotificationPermissionIfNeeded()

        root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(4, 8, 28)) }
        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(4, 8, 28))
            alpha = 0f
            isFocusable = true
            isFocusableInTouchMode = true
        }
        splash = buildSplash()

        root.addView(webView, FrameLayout.LayoutParams(-1, -1))
        root.addView(splash, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        configureWebView()

        if (savedInstanceState == null) {
            webView.loadUrl(prismUrl)
        } else {
            webView.restoreState(savedInstanceState)
            handler.postDelayed({ revealWebView() }, 350L)
        }
    }

    private fun buildSplash(): FrameLayout {
        val frame = FrameLayout(this)
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(24, 12, 73), Color.rgb(4, 43, 88))
        )
        frame.background = gradient

        val icon = ImageView(this).apply {
            setImageResource(com.noor.prism.R.drawable.app_icon_foreground)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            alpha = 0f
            scaleX = 0.82f
            scaleY = 0.82f
            animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(650).start()
        }
        frame.addView(icon, FrameLayout.LayoutParams(dp(230), dp(230), Gravity.CENTER).apply {
            bottomMargin = dp(80)
        })

        val stars = TextView(this).apply {
            text = "✦   ✧   ✦   ✧   ✦"
            textSize = 28f
            setTextColor(Color.rgb(202, 218, 255))
            gravity = Gravity.CENTER
            alpha = 0.25f
            animate().alpha(0.9f).setDuration(900).withEndAction {
                animate().alpha(0.35f).setDuration(900).start()
            }.start()
        }
        frame.addView(stars, FrameLayout.LayoutParams(-1, dp(70), Gravity.CENTER).apply {
            topMargin = dp(220)
        })

        val title = TextView(this).apply {
            text = "Noor's Prism"
            textSize = 33f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        frame.addView(title, FrameLayout.LayoutParams(-1, dp(70), Gravity.CENTER).apply {
            topMargin = dp(335)
        })

        val subtitle = TextView(this).apply {
            text = "✨ Preparing your adventure... ✨"
            textSize = 20f
            setTextColor(Color.rgb(210, 220, 255))
            gravity = Gravity.CENTER
        }
        frame.addView(subtitle, FrameLayout.LayoutParams(-1, dp(60), Gravity.CENTER).apply {
            topMargin = dp(425)
        })

        return frame
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.addJavascriptInterface(RuntimeBridge(), "PrismNative")
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadsImagesAutomatically = true
            blockNetworkImage = false
            allowFileAccess = true
            allowContentAccess = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = if (isOnline()) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK
            userAgentString = "$userAgentString NoorPrismAndroid/2.2"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                forceDark = WebSettings.FORCE_DARK_OFF
            }
            if (Build.VERSION.SDK_INT >= 33) {
                isAlgorithmicDarkeningAllowed = false
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                ServiceWorkerController.getInstance().serviceWorkerWebSettings.apply {
                    allowContentAccess = true
                    allowFileAccess = true
                    blockNetworkLoads = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                }
            } catch (_: Throwable) {
            }
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

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return interceptCacheableAsset(request)
                    ?: super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                installRuntimeObserver()
                scheduleTopbarSelfHeal()
                requestServiceWorkerUpdateWithoutReload()
                handler.postDelayed({ checkAndReveal() }, 250L)
                handler.postDelayed({ checkAndReveal() }, 800L)
                handler.postDelayed({ checkAndReveal() }, 1600L)
                handler.postDelayed({ if (!pageReady) revealWebView() }, 5500L)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
            }
        }
    }

    private fun installRuntimeObserver() {
        val script = """
            (function () {
              if (window.__prismAndroidObserverInstalled) return true;
              window.__prismAndroidObserverInstalled = true;

              function reportAudio() {
                try {
                  var playing = Array.from(document.querySelectorAll('audio')).some(function(a) {
                    return !a.paused && !a.ended && a.readyState > 1;
                  });
                  PrismNative.setAudioPlaying(playing);
                } catch (_) {}
              }

              document.addEventListener('play', reportAudio, true);
              document.addEventListener('pause', reportAudio, true);
              document.addEventListener('ended', reportAudio, true);
              setInterval(reportAudio, 1200);

              function heal() {
                try {
                  if (typeof window.__prismEnsureTopbar === 'function') window.__prismEnsureTopbar();
                  if (typeof window.__prismHealTopbar === 'function') window.__prismHealTopbar();
                  window.dispatchEvent(new CustomEvent('prism:ensure-topbar'));
                } catch (_) {}
              }
              heal();
              var observer = new MutationObserver(function() { heal(); });
              observer.observe(document.documentElement, {childList:true, subtree:true, attributes:true, attributeFilter:['class','style']});
              setInterval(heal, 2500);
              return true;
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    private fun scheduleTopbarSelfHeal() {
        longArrayOf(150, 500, 1000, 1800, 3000, 5000, 8000).forEach { delay ->
            handler.postDelayed({ injectTopbarSelfHeal() }, delay)
        }
    }

    private fun injectTopbarSelfHeal() {
        if (!::webView.isInitialized) return
        val script = """
            (function () {
              try {
                var body = document.body;
                if (!body || body.classList.contains('landing-mode')) return true;
                var topbar = document.getElementById('topbar') || document.querySelector('.topbar');
                if (!topbar) {
                  try { if (typeof window.__prismEnsureTopbar === 'function') window.__prismEnsureTopbar(); } catch (_) {}
                  return false;
                }
                var style = getComputedStyle(topbar), rect = topbar.getBoundingClientRect();
                if (style.display === 'none' || style.visibility === 'hidden' || parseFloat(style.opacity || '1') === 0 || rect.height < 8) {
                  topbar.style.setProperty('display','flex','important');
                  topbar.style.setProperty('visibility','visible','important');
                  topbar.style.setProperty('opacity','1','important');
                  topbar.style.setProperty('pointer-events','auto','important');
                  topbar.style.setProperty('z-index','999999','important');
                }
                return true;
              } catch (_) { return false; }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    private fun checkAndReveal() {
        if (!::webView.isInitialized || pageReady) return
        val script = """
            (function () {
              try {
                var body = document.body;
                if (!body) return false;
                var cards = document.querySelectorAll('[data-plugin], .menu-card, .surah-card, .card, .tile');
                var meaningful = (body.innerText || '').trim().length > 80;
                return meaningful && (cards.length > 0 || !body.classList.contains('landing-mode'));
              } catch (_) { return false; }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script) { result ->
            if (result == "true") revealWebView()
        }
    }

    private fun revealWebView() {
        if (pageReady) return
        pageReady = true
        webView.animate().alpha(1f).setDuration(300).start()
        splash.animate().alpha(0f).setDuration(420).withEndAction {
            splash.visibility = View.GONE
        }.start()
        webView.requestFocus()
    }

    private fun requestServiceWorkerUpdateWithoutReload() {
        webView.evaluateJavascript(
            """
            (function(){
              if (!('serviceWorker' in navigator)) return;
              navigator.serviceWorker.getRegistrations().then(function(regs){
                regs.forEach(function(reg){ try { reg.update(); } catch (_) {} });
              }).catch(function(){});
            })();
            """.trimIndent(),
            null
        )
    }

    private fun interceptCacheableAsset(request: WebResourceRequest): WebResourceResponse? {
        if (!request.method.equals("GET", ignoreCase = true)) return null
        val uri = request.url
        if (!uri.host.equals("amtunnoor.github.io", ignoreCase = true)) return null
        val path = uri.path ?: return null
        val extension = path.substringAfterLast('.', "").lowercase(Locale.US)
        val cacheable = extension in setOf("mp3", "webp", "png", "jpg", "jpeg", "gif", "svg")
        if (!cacheable) return null

        return try {
            val url = uri.toString()
            val file = cachedFileFor(url, extension)
            if (!file.exists() || file.length() == 0L) {
                synchronized(inFlight.computeIfAbsent(url) { Any() }) {
                    if (!file.exists() || file.length() == 0L) downloadFully(url, file)
                }
                inFlight.remove(url)
            }
            if (!file.exists() || file.length() == 0L) return null
            buildCachedResponse(file, extension, request.requestHeaders["Range"])
        } catch (_: Throwable) {
            null
        }
    }

    private fun cachedFileFor(url: String, extension: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
        val name = digest.joinToString("") { "%02x".format(it) }
        val dir = File(cacheDir, "prism-assets").apply { mkdirs() }
        return File(dir, "$name.$extension")
    }

    private fun downloadFully(urlString: String, destination: File) {
        val temp = File(destination.parentFile, destination.name + ".part")
        if (temp.exists()) temp.delete()
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 60000
            instanceFollowRedirects = true
            useCaches = true
            setRequestProperty("User-Agent", "NoorPrismAndroid/2.2")
        }
        try {
            connection.connect()
            if (connection.responseCode !in 200..299) return
            connection.inputStream.use { input ->
                FileOutputStream(temp).use { output -> input.copyTo(output, 64 * 1024) }
            }
            val expected = connection.contentLengthLong
            if (temp.length() > 0L && (expected <= 0L || temp.length() == expected)) {
                if (destination.exists()) destination.delete()
                temp.renameTo(destination)
            }
        } finally {
            connection.disconnect()
            if (temp.exists() && !destination.exists()) temp.delete()
        }
    }

    private fun buildCachedResponse(file: File, extension: String, rangeHeader: String?): WebResourceResponse {
        val mime = when (extension) {
            "mp3" -> "audio/mpeg"
            "webp" -> "image/webp"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            else -> "application/octet-stream"
        }

        if (rangeHeader.isNullOrBlank() || !rangeHeader.startsWith("bytes=")) {
            return WebResourceResponse(mime, null, FileInputStream(file)).apply {
                responseHeaders = mapOf(
                    "Accept-Ranges" to "bytes",
                    "Content-Length" to file.length().toString(),
                    "Cache-Control" to "public, max-age=31536000"
                )
            }
        }

        val total = file.length()
        val range = rangeHeader.removePrefix("bytes=").substringBefore(',')
        val start = range.substringBefore('-').toLongOrNull() ?: 0L
        val end = range.substringAfter('-', "").toLongOrNull()?.coerceAtMost(total - 1) ?: (total - 1)
        if (start >= total || end < start) {
            return WebResourceResponse(mime, null, 416, "Range Not Satisfiable", mapOf("Content-Range" to "bytes */$total"), ByteArrayInputStream(ByteArray(0)))
        }
        val length = end - start + 1
        val input = RangedFileInputStream(file, start, length)
        return WebResourceResponse(
            mime,
            null,
            206,
            "Partial Content",
            mapOf(
                "Accept-Ranges" to "bytes",
                "Content-Range" to "bytes $start-$end/$total",
                "Content-Length" to length.toString(),
                "Cache-Control" to "public, max-age=31536000"
            ),
            input
        )
    }

    private inner class RuntimeBridge {
        @JavascriptInterface
        fun setAudioPlaying(playing: Boolean) {
            audioPlaying = playing
            runOnUiThread {
                if (playing) startPlaybackService() else stopPlaybackService()
            }
        }
    }

    private fun startPlaybackService() {
        val intent = Intent(this, PlaybackKeepAliveService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        } catch (_: Throwable) {
        }
        try {
            WebView.enableSlowWholeDocumentDraw()
            webView.resumeTimers()
            webView.onResume()
        } catch (_: Throwable) {
        }
    }

    private fun stopPlaybackService() {
        try { stopService(Intent(this, PlaybackKeepAliveService::class.java)) } catch (_: Throwable) {}
    }

    private fun isOnline(): Boolean {
        return try {
            val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Throwable) {
            true
        }
    }

    private fun isTelevision(): Boolean = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

    private fun enableImmersiveMode() {
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            window.insetsController?.systemBarsBehavior =
                android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2202)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        enableImmersiveMode()
        if (::webView.isInitialized) {
            webView.onResume()
            webView.resumeTimers()
            scheduleTopbarSelfHeal()
        }
    }

    override fun onPause() {
        if (::webView.isInitialized && !audioPlaying) {
            webView.onPause()
        } else if (::webView.isInitialized) {
            webView.onResume()
            webView.resumeTimers()
            startPlaybackService()
        }
        super.onPause()
    }

    override fun onStop() {
        if (audioPlaying && ::webView.isInitialized) {
            webView.onResume()
            webView.resumeTimers()
            startPlaybackService()
        }
        super.onStop()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (!audioPlaying) stopPlaybackService()
        if (::webView.isInitialized) {
            try { webView.removeJavascriptInterface("PrismNative") } catch (_: Throwable) {}
            try { webView.destroy() } catch (_: Throwable) {}
        }
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else moveTaskToBack(true)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

private class RangedFileInputStream(file: File, start: Long, private var remaining: Long) : InputStream() {
    private val input = FileInputStream(file).apply { channel.position(start) }

    override fun read(): Int {
        if (remaining <= 0) return -1
        val value = input.read()
        if (value >= 0) remaining--
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (remaining <= 0) return -1
        val toRead = minOf(length.toLong(), remaining).toInt()
        val read = input.read(buffer, offset, toRead)
        if (read > 0) remaining -= read.toLong()
        return read
    }

    override fun close() = input.close()
}
