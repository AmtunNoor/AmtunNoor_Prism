package com.noor.prism

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
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
import android.webkit.JavascriptInterface
import android.webkit.ServiceWorkerClient
import android.webkit.ServiceWorkerController
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
    private lateinit var homeFallback: TextView
    private lateinit var snapshotManager: PrismSnapshotManager

    private val mainHandler = Handler(Looper.getMainLooper())
    private val stopAudioService = Runnable { if (!audioPlaying) stopPlaybackService() }
    private val firstLaunchFallback = Runnable {
        if (!firstContentShown && isOnline()) loadPrism(showLoader = true)
    }

    @Volatile private var audioPlaying = false
    private var firstContentShown = false
    private var mainFrameFailed = false
    private var syncStarted = false
    private var pageLoadStarted = false
    private var introShownAt = 0L

    private val deviceKind: String get() = if (isTelevision()) "tv" else "phone"
    private val basePrismUrl: String get() = snapshotManager.activeIndexUrl(deviceKind)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        snapshotManager = PrismSnapshotManager(applicationContext)
        enableImmersiveMode()
        buildUi()
        configureWebView()
        configureBackNavigation()

        if (savedInstanceState != null &&
            webView.restoreState(savedInstanceState) != null &&
            snapshotManager.hasUsableSnapshot()
        ) {
            firstContentShown = true
            pageLoadStarted = true
            webView.visibility = View.VISIBLE
            showLoading(false)
            startBackgroundSync()
        } else {
            startRuntime()
        }
    }

    private fun startRuntime() {
        mainHandler.removeCallbacks(firstLaunchFallback)
        showError(false)
        homeFallback.visibility = View.GONE

        if (snapshotManager.hasUsableSnapshot()) {
            loadPrism(showLoader = true)
            startBackgroundSync()
            return
        }

        if (!isOnline()) {
            showLoading(false)
            showError(true)
            return
        }

        showLoading(true)
        introShownAt = System.currentTimeMillis()
        pageLoadStarted = false
        syncStarted = true

        snapshotManager.startSync(
            onReady = {
                runOnUiThread { loadAfterMinimumIntro() }
            },
            onProgress = { },
            onComplete = { result ->
                runOnUiThread {
                    syncStarted = false
                    if (!pageLoadStarted && (result.hasUsableSnapshot || isOnline())) {
                        loadAfterMinimumIntro()
                    } else if (!result.success && !pageLoadStarted && !isOnline()) {
                        showLoading(false)
                        showError(true)
                    }
                }
            }
        )

        // Never hold a child on the intro while the full offline copy is still downloading.
        mainHandler.postDelayed(firstLaunchFallback, MAX_FIRST_LAUNCH_WAIT_MS)
    }

    private fun loadAfterMinimumIntro() {
        if (pageLoadStarted) return
        val elapsed = System.currentTimeMillis() - introShownAt
        val remaining = (MIN_INTRO_MS - elapsed).coerceAtLeast(0L)
        mainHandler.postDelayed({ loadPrism(showLoader = true) }, remaining)
    }

    private fun startBackgroundSync() {
        if (syncStarted || !isOnline()) return
        syncStarted = true
        snapshotManager.startSync(
            onReady = { },
            onProgress = { },
            onComplete = { runOnUiThread { syncStarted = false } }
        )
    }

    private fun buildUi() {
        root = FrameLayout(this).apply { setBackgroundColor(PRISM_BACKGROUND) }
        webView = WebView(this).apply {
            setBackgroundColor(PRISM_BACKGROUND)
            isFocusable = true
            isFocusableInTouchMode = true
            overScrollMode = View.OVER_SCROLL_NEVER
            visibility = View.INVISIBLE
        }
        loadingOverlay = PrismLoadingView(this)
        errorOverlay = createErrorOverlay()
        homeFallback = createHomeFallback()

        root.addView(webView, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(loadingOverlay, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(errorOverlay, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(homeFallback, FrameLayout.LayoutParams(dp(54), dp(54), Gravity.TOP or Gravity.START).apply {
            topMargin = dp(14)
            marginStart = dp(14)
        })
        setContentView(root)
    }

    private fun createHomeFallback(): TextView = TextView(this).apply {
        text = "⌂"
        textSize = 28f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        isFocusable = true
        contentDescription = "Prism Home"
        visibility = View.GONE
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.argb(205, 15, 24, 58))
            setStroke(dp(1), Color.argb(210, 118, 220, 255))
        }
        elevation = dp(8).toFloat()
        setOnClickListener { returnToPrismHome() }
    }

    private fun createErrorOverlay(): View {
        errorMessage = TextView(this).apply {
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(dp(28), dp(20), dp(28), dp(20))
        }
        val retry = Button(this).apply {
            text = "Try again"
            textSize = 18f
            isAllCaps = false
            isFocusable = true
            setOnClickListener { startRuntime() }
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            addView(errorMessage, LinearLayout.LayoutParams(MATCH, WRAP))
            addView(retry, LinearLayout.LayoutParams(dp(190), dp(58)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(18)
            })
        }
        return FrameLayout(this).apply {
            setBackgroundColor(PRISM_BACKGROUND)
            visibility = View.GONE
            addView(panel, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.CENTER))
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.addJavascriptInterface(RuntimeBridge(), NATIVE_BRIDGE)
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
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
            loadWithOverviewMode = false
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
                val controller = ServiceWorkerController.getInstance()
                controller.serviceWorkerWebSettings.apply {
                    allowContentAccess = false
                    allowFileAccess = false
                    blockNetworkLoads = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                }
                controller.setServiceWorkerClient(object : ServiceWorkerClient() {
                    override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                        if (request.method != "GET") return null
                        return snapshotManager.responseFor(request.url.toString(), request.requestHeaders["Range"])
                    }
                })
            }
        }

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val target = request.url
                if (target.scheme != "https") return true
                return target.host?.lowercase() !in ALLOWED_HOSTS
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (request.method != "GET") return null
                return snapshotManager.responseFor(request.url.toString(), request.requestHeaders["Range"])
            }

            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                mainFrameFailed = false
                showError(false)
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (mainFrameFailed) return
                installRuntimeObservers()
                firstContentShown = true
                webView.visibility = View.VISIBLE
                showLoading(false)
                showError(false)
                view.requestFocus()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (!request.isForMainFrame) return
                mainFrameFailed = true
                if (snapshotManager.hasUsableSnapshot() && request.url.host == PAGES_HOST) {
                    view.postDelayed({ view.reload() }, 350L)
                    return
                }
                webView.visibility = View.INVISIBLE
                showLoading(false)
                showError(true)
            }
        }
    }

    private fun loadPrism(showLoader: Boolean) {
        if (pageLoadStarted && !mainFrameFailed) return
        pageLoadStarted = true
        mainHandler.removeCallbacks(firstLaunchFallback)
        mainFrameFailed = false
        showError(false)
        homeFallback.visibility = View.GONE
        if (showLoader && !firstContentShown) showLoading(true)
        webView.loadUrl(basePrismUrl)
    }

    private fun installRuntimeObservers() {
        val script = """
            (function () {
              if (!window.__noorPrismNativeAudioObserver) {
                window.__noorPrismNativeAudioObserver = true;
                var lastAudio = null;
                function reportAudio() {
                  try {
                    var playing = Array.prototype.some.call(document.querySelectorAll('audio,video'), function (m) {
                      return !m.paused && !m.ended && m.readyState >= 2;
                    });
                    if (playing !== lastAudio) { lastAudio = playing; window.PrismNative.setAudioPlaying(playing); }
                  } catch (_) {}
                }
                ['play','playing','pause','ended','emptied'].forEach(function(e){ document.addEventListener(e, reportAudio, true); });
                setInterval(reportAudio, 1000);
                reportAudio();
              }

              if (!window.__noorPrismNavigationGuard) {
                window.__noorPrismNavigationGuard = true;
                function guardNavigation() {
                  try {
                    var params = new URLSearchParams(location.search);
                    var plugin = (params.get('plugin') || '').toLowerCase();
                    var body = document.body;
                    var protectedModule = ['quran','names','dua','salah'].indexOf(plugin) >= 0 ||
                      location.host === 'busymommh.github.io' ||
                      (body && (body.classList.contains('plugin-names') || body.classList.contains('plugin-quran')));
                    var landing = body && body.classList.contains('landing-mode');
                    if (landing) protectedModule = false;
                    var top = document.getElementById('topbar');
                    if (protectedModule && top) {
                      top.classList.add('prism-topbar-ready');
                      top.style.setProperty('display','flex','important');
                      top.style.setProperty('visibility','visible','important');
                      top.style.setProperty('opacity','1','important');
                      var home = document.getElementById('btnBack');
                      if (home) home.style.setProperty('display','inline-block','important');
                    }
                    var visible = !!(top && top.getClientRects().length &&
                      getComputedStyle(top).visibility !== 'hidden' && getComputedStyle(top).display !== 'none');
                    window.PrismNative.setHomeFallbackVisible(protectedModule && !visible);
                  } catch (_) { window.PrismNative.setHomeFallbackVisible(true); }
                }
                new MutationObserver(guardNavigation).observe(document.documentElement, {
                  subtree:true, childList:true, attributes:true, attributeFilter:['class','style']
                });
                window.addEventListener('pageshow', guardNavigation);
                window.addEventListener('popstate', guardNavigation);
                window.addEventListener('hashchange', guardNavigation);
                document.addEventListener('visibilitychange', guardNavigation);
                setInterval(guardNavigation, 750);
                guardNavigation();
              }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    private fun configureBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (errorOverlay.visibility == View.VISIBLE) {
                    if (snapshotManager.hasUsableSnapshot() || isOnline()) {
                        pageLoadStarted = false
                        loadPrism(false)
                    } else {
                        moveTaskToBack(true)
                    }
                    return
                }
                attemptWebHomeOrBack()
            }
        })
    }

    private fun attemptWebHomeOrBack() {
        val script = """
            (function () {
              try {
                var home = document.getElementById('btnBack');
                if (home && home.offsetParent !== null) { home.click(); return true; }
                if (typeof goHome === 'function') { goHome(); return true; }
              } catch (_) {}
              return false;
            })();
        """.trimIndent()
        webView.evaluateJavascript(script) { result ->
            if (result == "true") return@evaluateJavascript
            if (webView.canGoBack()) webView.goBack() else returnToPrismHome()
        }
    }

    private fun returnToPrismHome() {
        homeFallback.visibility = View.GONE
        pageLoadStarted = false
        webView.loadUrl(basePrismUrl)
        pageLoadStarted = true
    }

    private inner class RuntimeBridge {
        @JavascriptInterface
        fun setAudioPlaying(playing: Boolean) {
            runOnUiThread {
                audioPlaying = playing
                mainHandler.removeCallbacks(stopAudioService)
                if (playing) startPlaybackService() else mainHandler.postDelayed(stopAudioService, AUDIO_STOP_GRACE_MS)
            }
        }

        @JavascriptInterface
        fun setHomeFallbackVisible(visible: Boolean) {
            runOnUiThread {
                homeFallback.visibility = if (visible && webView.visibility == View.VISIBLE) View.VISIBLE else View.GONE
            }
        }
    }

    private fun startPlaybackService() {
        val intent = Intent(this, PlaybackKeepAliveService::class.java)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
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
            errorMessage.text = when {
                snapshotManager.hasUsableSnapshot() -> "Prism could not open its saved copy.\nPlease try again."
                isOnline() -> "Prism could not start.\nPlease try again."
                else -> "Connect to the internet once so Prism can prepare its offline copy."
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
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                manager.activeNetworkInfo?.isConnected == true
            }
        }.getOrDefault(false)
    }

    private fun isTelevision(): Boolean {
        @Suppress("DEPRECATION")
        return packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
    }

    private fun enableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
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
        if (firstContentShown) installRuntimeObservers()
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
        snapshotManager.close()
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
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_MENU) return true
        return super.dispatchKeyEvent(event)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val PAGES_HOST = "amtunnoor.github.io"
        private val ALLOWED_HOSTS = setOf(PAGES_HOST, "busymommh.github.io")
        private const val NATIVE_BRIDGE = "PrismNative"
        private const val AUDIO_STOP_GRACE_MS = 1800L
        private const val MIN_INTRO_MS = 1800L
        private const val MAX_FIRST_LAUNCH_WAIT_MS = 6500L
        private const val MATCH = FrameLayout.LayoutParams.MATCH_PARENT
        private const val WRAP = FrameLayout.LayoutParams.WRAP_CONTENT
        private val PRISM_BACKGROUND = Color.rgb(4, 8, 28)
    }
}
