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

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val handler = Handler(Looper.getMainLooper())

    // Stable URL: no timestamp/cache-buster on every launch.
    // This allows WebView + the existing service worker to reuse cached UI/assets.
    private val prismUrl = "https://amtunnoor.github.io/Quran/index.html?app=prism-live"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setImmersiveFlags()

        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(9, 11, 34))
        }
        setContentView(
            webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        configureWebView()

        if (savedInstanceState == null) {
            webView.loadUrl(prismUrl)
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    private fun setImmersiveFlags() {
        @Suppress("DEPRECATION")
        run {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
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
            cacheMode = if (isOnline()) {
                WebSettings.LOAD_DEFAULT
            } else {
                WebSettings.LOAD_CACHE_ELSE_NETWORK
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
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
                // Older/custom WebView implementations may not expose all settings.
            }
        }

        WebView.setWebContentsDebuggingEnabled(false)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                view.loadUrl(request.url.toString())
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                scheduleNonDestructiveTopbarSelfHeal()
                requestServiceWorkerUpdateWithoutReload()
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                // Keep any available cached page visible. Never replace Prism with a native
                // dashboard or a partial local snapshot.
            }
        }
    }

    /**
     * Repairs only the fixed Prism menu/topbar.
     * It never reloads the page, restarts audio, resets effects, or navigates away.
     */
    private fun scheduleNonDestructiveTopbarSelfHeal() {
        val delaysMs = longArrayOf(250, 700, 1400, 2500, 4000, 6500, 9000)
        delaysMs.forEach { delay ->
            handler.postDelayed({ injectTopbarSelfHeal() }, delay)
        }
    }

    private fun injectTopbarSelfHeal() {
        if (!::webView.isInitialized) return

        val script = """
            (function () {
              try {
                document.documentElement.classList.add('prism-android-app');
                if (document.body) document.body.classList.add('prism-android-app');

                // Use the web app's own repair hooks first, when available.
                try { if (typeof window.__prismEnsureTopbar === 'function') window.__prismEnsureTopbar(); } catch (_) {}
                try { if (typeof window.__prismHealTopbar === 'function') window.__prismHealTopbar(); } catch (_) {}
                try { window.dispatchEvent(new CustomEvent('prism:ensure-topbar')); } catch (_) {}

                var body = document.body;
                if (!body || body.classList.contains('landing-mode')) return true;

                // Visual modules intentionally using floating controls should not receive
                // the fixed full menu bar.
                var skip = ['plugin-letters','plugin-angels','plugin-pillars','plugin-months','plugin-numbers'];
                for (var i = 0; i < skip.length; i++) {
                  if (body.classList.contains(skip[i])) return true;
                }

                var topbar = document.getElementById('topbar') || document.querySelector('.topbar');
                if (!topbar) return false;

                var style = window.getComputedStyle(topbar);
                var rect = topbar.getBoundingClientRect();
                var hidden = style.display === 'none' ||
                             style.visibility === 'hidden' ||
                             parseFloat(style.opacity || '1') === 0 ||
                             rect.height < 8;

                if (hidden) {
                  topbar.style.setProperty('display', 'flex', 'important');
                  topbar.style.setProperty('visibility', 'visible', 'important');
                  topbar.style.setProperty('opacity', '1', 'important');
                  topbar.style.setProperty('pointer-events', 'auto', 'important');
                  topbar.style.setProperty('z-index', '999999', 'important');
                  body.classList.add('prism-android-topbar-healed', 'show-topbar');
                }
                return true;
              } catch (_) {
                return false;
              }
            })();
        """.trimIndent()

        webView.evaluateJavascript(script, null)
    }

    /** Updates the service worker registration in the background without reloading Prism. */
    private fun requestServiceWorkerUpdateWithoutReload() {
        val script = """
            (function () {
              if (!('serviceWorker' in navigator)) return;
              navigator.serviceWorker.getRegistrations()
                .then(function (regs) {
                  regs.forEach(function (reg) {
                    try { reg.update(); } catch (_) {}
                  });
                })
                .catch(function () {});
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        setImmersiveFlags()
        if (::webView.isInitialized) {
            webView.onResume()
            scheduleNonDestructiveTopbarSelfHeal()
        }
    }

    override fun onPause() {
        if (::webView.isInitialized) webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (::webView.isInitialized) {
            try { webView.destroy() } catch (_: Throwable) {}
        }
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (::webView.isInitialized && webView.canGoBack()) {
                webView.goBack()
            } else {
                moveTaskToBack(true)
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
