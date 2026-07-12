package com.noor.prism

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.webkit.JavascriptInterface
import android.webkit.ServiceWorkerController
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : AppCompatActivity() {

    private lateinit var root: FrameLayout
    private lateinit var webView: WebView
    private lateinit var loadingOverlay: View
    private lateinit var errorOverlay: View
    private lateinit var errorMessage: TextView

    private val mainHandler = Handler(Looper.getMainLooper())
    private val stopAudioService = Runnable {
        if (!audioPlaying) stopPlaybackService()
    }

    @Volatile private var audioPlaying = false
    private var firstContentShown = false
    private var mainFrameFailed = false

    private val basePrismUrl: String
        get() {
            val device = if (isTelevision()) "tv" else "phone"
            return "https://amtunnoor.github.io/Quran/index.html?runtime=android&device=$device"
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableImmersiveMode()
        buildUi()
        configureWebView()
        configureBackNavigation()

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            loadPrism(forceNetworkValidation = true)
        } else {
            showLoading(false)
            firstContentShown = true
        }
    }

    private fun buildUi() {
        root = FrameLayout(this).apply {
            setBackgroundColor(PRISM_BACKGROUND)
        }

        webView = WebView(this).apply {
            setBackgroundColor(PRISM_BACKGROUND)
            isFocusable = true
            isFocusableInTouchMode = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        loadingOverlay = createLoadingOverlay()
        errorOverlay = createErrorOverlay()

        root.addView(webView, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(loadingOverlay, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(errorOverlay, FrameLayout.LayoutParams(MATCH, MATCH))
        setContentView(root)
    }

    private fun createLoadingOverlay(): View {
        return TextView(this).apply {
            setBackgroundColor(PRISM_BACKGROUND)
            text = "✦\n\nPreparing Noor's Prism…"
            textSize = 22f
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.WHITE)
            isFocusable = false
        }
    }

    private fun createErrorOverlay(): View {
        errorMessage = TextView(this).apply {
            textSize = 19f
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(dp(28), dp(20), dp(28), dp(20))
        }

        val retry = Button(this).apply {
            text = "Try again"
            textSize = 18f
            isAllCaps = false
            isFocusable = true
            setOnClickListener { loadPrism(forceNetworkValidation = true) }
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            addView(errorMessage, LinearLayout.LayoutParams(MATCH, WRAP))
            addView(retry, LinearLayout.LayoutParams(dp(190), dp(58)).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                topMargin = dp(18)
            })
        }

        return FrameLayout(this).apply {
            setBackgroundColor(PRISM_BACKGROUND)
            visibility = View.GONE
            addView(panel, FrameLayout.LayoutParams(MATCH, WRAP, android.view.Gravity.CENTER))
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.addJavascriptInterface(RuntimeBridge(), NATIVE_BRIDGE)

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadsImagesAutomatically = true
            blockNetworkImage = false
            allowFileAccess = false
            allowContentAccess = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = "$userAgentString NoorPrismAndroid/${BuildConfig.VERSION_NAME}"
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) safeBrowsingEnabled = true
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) forceDark = WebSettings.FORCE_DARK_OFF
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) isAlgorithmicDarkeningAllowed = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching {
                ServiceWorkerController.getInstance().serviceWorkerWebSettings.apply {
                    allowContentAccess = false
                    allowFileAccess = false
                    blockNetworkLoads = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                }
            }
        }

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val target = request.url
                return if (target.scheme == "https" && target.host.equals(ALLOWED_HOST, ignoreCase = true)) {
                    false
                } else {
                    true
                }
            }

            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                mainFrameFailed = false
                if (!firstContentShown) showLoading(true)
                showError(false)
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (mainFrameFailed) return
                installAudioObserver()
                requestServiceWorkerRefresh()
                firstContentShown = true
                showLoading(false)
                showError(false)
                view.requestFocus()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (!request.isForMainFrame) return
                mainFrameFailed = true
                showLoading(false)
                showError(true)
            }
        }
    }

    private fun loadPrism(forceNetworkValidation: Boolean) {
        mainFrameFailed = false
        showError(false)
        showLoading(!firstContentShown)

        webView.settings.cacheMode = if (isOnline()) {
            WebSettings.LOAD_DEFAULT
        } else {
            WebSettings.LOAD_CACHE_ELSE_NETWORK
        }

        val headers = if (forceNetworkValidation && isOnline()) {
            mapOf("Cache-Control" to "no-cache")
        } else {
            emptyMap()
        }
        webView.loadUrl(basePrismUrl, headers)
    }

    private fun installAudioObserver() {
        val script = """
            (function () {
              if (window.__noorPrismNativeAudioObserver) return;
              window.__noorPrismNativeAudioObserver = true;
              var last = null;
              function report() {
                try {
                  var playing = Array.prototype.some.call(document.querySelectorAll('audio,video'), function (m) {
                    return !m.paused && !m.ended && m.readyState >= 2;
                  });
                  if (playing !== last) {
                    last = playing;
                    window.PrismNative.setAudioPlaying(playing);
                  }
                } catch (_) {}
              }
              document.addEventListener('play', report, true);
              document.addEventListener('playing', report, true);
              document.addEventListener('pause', report, true);
              document.addEventListener('ended', report, true);
              document.addEventListener('emptied', report, true);
              setInterval(report, 1000);
              report();
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    private fun requestServiceWorkerRefresh() {
        if (!isOnline()) return
        webView.evaluateJavascript(
            """
            (function () {
              if (!('serviceWorker' in navigator)) return;
              navigator.serviceWorker.getRegistrations().then(function (registrations) {
                registrations.forEach(function (registration) { registration.update().catch(function () {}); });
              }).catch(function () {});
            })();
            """.trimIndent(),
            null
        )
    }

    private fun configureBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    errorOverlay.visibility == View.VISIBLE && webView.canGoBack() -> {
                        showError(false)
                        webView.goBack()
                    }
                    webView.canGoBack() -> webView.goBack()
                    else -> moveTaskToBack(true)
                }
            }
        })
    }

    private inner class RuntimeBridge {
        @JavascriptInterface
        fun setAudioPlaying(playing: Boolean) {
            runOnUiThread {
                audioPlaying = playing
                mainHandler.removeCallbacks(stopAudioService)
                if (playing) {
                    startPlaybackService()
                } else {
                    mainHandler.postDelayed(stopAudioService, AUDIO_STOP_GRACE_MS)
                }
            }
        }
    }

    private fun startPlaybackService() {
        val intent = Intent(this, PlaybackKeepAliveService::class.java)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)
        }
    }

    private fun stopPlaybackService() {
        runCatching { stopService(Intent(this, PlaybackKeepAliveService::class.java)) }
    }

    private fun showLoading(show: Boolean) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(show: Boolean) {
        if (show) {
            errorMessage.text = if (isOnline()) {
                "Noor's Prism could not start.\nPlease try again."
            } else {
                "You're offline and Prism has not finished caching yet.\nConnect once, then it will reopen from cache."
            }
        }
        errorOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun isOnline(): Boolean {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = manager.activeNetwork ?: return false
                val capabilities = manager.getNetworkCapabilities(network) ?: return false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } else {
                @Suppress("DEPRECATION")
                manager.activeNetworkInfo?.isConnected == true
            }
        }.getOrDefault(false)
    }

    private fun isTelevision(): Boolean =
        packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)

    private fun enableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveMode()
    }

    override fun onResume() {
        super.onResume()
        enableImmersiveMode()
        webView.onResume()
        webView.resumeTimers()
    }

    override fun onPause() {
        if (!audioPlaying) webView.onPause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        if (!audioPlaying) stopPlaybackService()
        runCatching { webView.removeJavascriptInterface(NATIVE_BRIDGE) }
        runCatching {
            webView.stopLoading()
            webView.webChromeClient = null
            webView.webViewClient = WebViewClient()
            webView.destroy()
        }
        super.onDestroy()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_MENU) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val ALLOWED_HOST = "amtunnoor.github.io"
        private const val NATIVE_BRIDGE = "PrismNative"
        private const val AUDIO_STOP_GRACE_MS = 1800L
        private const val MATCH = FrameLayout.LayoutParams.MATCH_PARENT
        private const val WRAP = FrameLayout.LayoutParams.WRAP_CONTENT
        private val PRISM_BACKGROUND = Color.rgb(4, 8, 28)
    }
}
