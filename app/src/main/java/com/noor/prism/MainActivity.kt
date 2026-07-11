package com.noor.prism

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
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
import android.view.Window
import android.view.WindowManager
import android.webkit.ServiceWorkerController
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var root: FrameLayout
    private lateinit var splash: FrameLayout
    private val handler = Handler(Looper.getMainLooper())
    private val isTv: Boolean by lazy {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    private val prismUrl: String
        get() {
            val device = if (isTv) "tv" else "phone"
            return "https://amtunnoor.github.io/Quran/index.html?runtime=android&device=$device"
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setImmersiveFlags()

        root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(7, 9, 28))
        }
        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(7, 9, 28))
            alpha = 0f
        }
        root.addView(
            webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        splash = buildSplash()
        root.addView(
            splash,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)
        configureWebView()

        if (savedInstanceState == null) {
            webView.loadUrl(prismUrl)
        } else {
            val restored = webView.restoreState(savedInstanceState)
            if (restored == null) webView.loadUrl(prismUrl)
        }

        // Never leave the child stuck on the splash if the network is slow.
        handler.postDelayed({ revealWebView() }, 15000)
    }

    private fun buildSplash(): FrameLayout {
        val container = FrameLayout(this)
        container.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.rgb(8, 12, 45),
                Color.rgb(34, 22, 86),
                Color.rgb(7, 45, 79)
            )
        )

        val stars = TextView(this).apply {
            text = "✦     ✧        ✦    ✧       ✦\n      ✧    ✦          ✧\n✧          ✦     ✧          ✦"
            textSize = if (isTv) 30f else 22f
            setTextColor(Color.argb(175, 210, 238, 255))
            gravity = Gravity.CENTER
            alpha = 0.78f
        }
        container.addView(
            stars,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val content = FrameLayout(this)
        val contentParams = FrameLayout.LayoutParams(
            if (isTv) dp(560) else dp(420),
            if (isTv) dp(380) else dp(320),
            Gravity.CENTER
        )
        container.addView(content, contentParams)

        val icon = ImageView(this).apply {
            setImageResource(com.noor.prism.R.drawable.app_icon)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            elevation = dp(14).toFloat()
        }
        val iconSize = if (isTv) dp(190) else dp(150)
        content.addView(
            icon,
            FrameLayout.LayoutParams(iconSize, iconSize, Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        )
        icon.animate()
            .scaleX(1.06f).scaleY(1.06f)
            .setDuration(1100)
            .withEndAction {
                icon.animate().scaleX(1f).scaleY(1f).setDuration(900).start()
            }.start()

        val title = TextView(this).apply {
            text = "Noor’s Prism"
            textSize = if (isTv) 38f else 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        content.addView(
            title,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(70),
                Gravity.CENTER
            ).apply { topMargin = if (isTv) dp(82) else dp(60) }
        )

        val subtitle = TextView(this).apply {
            text = "✨ Preparing your adventure… ✨"
            textSize = if (isTv) 21f else 17f
            setTextColor(Color.rgb(211, 235, 255))
            gravity = Gravity.CENTER
        }
        content.addView(
            subtitle,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(60),
                Gravity.BOTTOM
            ).apply { bottomMargin = if (isTv) dp(30) else dp(20) }
        )

        return container
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
                // Some TV WebView builds expose fewer service-worker settings.
            }
        }

        WebView.setWebContentsDebuggingEnabled(false)
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
                installAndroidOnlyPresentation()
                scheduleNonDestructiveTopbarSelfHeal()
                waitForLandingCardsThenReveal()
                requestServiceWorkerUpdateWithoutReload()
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                // Preserve any cached page; never replace Prism with a native fallback menu.
                handler.postDelayed({ revealWebView() }, 1200)
            }
        }
    }

    /**
     * Adds presentation-only CSS to the landing page inside the Android app.
     * It does not alter the DOM, card data, click handlers, plugins, module pages,
     * audio, coordinates, effects, Learn, 5x or Hifz.
     */
    private fun installAndroidOnlyPresentation() {
        val deviceClass = if (isTv) "prism-runtime-tv" else "prism-runtime-phone"
        val script = """
            (function () {
              try {
                document.documentElement.classList.add('prism-runtime-android', '$deviceClass');
                if (document.body) document.body.classList.add('prism-runtime-android', '$deviceClass');
                if (document.getElementById('prism-runtime-style')) return true;
                var style = document.createElement('style');
                style.id = 'prism-runtime-style';
                style.textContent = `
                  body.landing-mode.prism-runtime-android .grid {
                    display: grid !important;
                    grid-auto-flow: column !important;
                    grid-template-columns: none !important;
                    overflow-x: auto !important;
                    overflow-y: hidden !important;
                    overscroll-behavior-x: contain !important;
                    scroll-snap-type: x mandatory !important;
                    scroll-behavior: smooth !important;
                    align-content: center !important;
                    justify-content: start !important;
                    width: 100% !important;
                    max-width: none !important;
                    scrollbar-width: none !important;
                    padding-inline: clamp(18px,4vw,70px) !important;
                  }
                  body.landing-mode.prism-runtime-android .grid::-webkit-scrollbar { display:none !important; }
                  body.landing-mode.prism-runtime-android .card {
                    scroll-snap-align: center !important;
                    contain: layout paint !important;
                  }
                  body.landing-mode.prism-runtime-phone .grid {
                    grid-template-rows: repeat(2, minmax(0,1fr)) !important;
                    grid-auto-columns: minmax(210px, 30vw) !important;
                    height: calc(100vh - 150px) !important;
                    gap: 14px !important;
                  }
                  body.landing-mode.prism-runtime-tv .grid {
                    grid-template-rows: repeat(2, minmax(0,1fr)) !important;
                    grid-auto-columns: minmax(250px, 22vw) !important;
                    height: calc(100vh - 175px) !important;
                    gap: clamp(14px,1.5vw,26px) !important;
                  }
                  body.landing-mode.prism-runtime-tv .card.remote-focus {
                    transform: scale(1.045) !important;
                    filter: brightness(1.08) drop-shadow(0 0 16px rgba(140,220,255,.72)) !important;
                  }
                `;
                document.head.appendChild(style);
                return true;
              } catch (_) { return false; }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    private fun waitForLandingCardsThenReveal(attempt: Int = 0) {
        if (!::webView.isInitialized) return
        val script = """
            (function () {
              try {
                var cards = document.querySelectorAll('body.landing-mode .grid .card');
                if (!cards || cards.length === 0) return false;
                var ready = 0;
                for (var i=0; i<cards.length; i++) {
                  var img = cards[i].querySelector('img');
                  if (!img || (img.complete && img.naturalWidth > 0)) ready++;
                }
                return ready >= Math.min(cards.length, 4);
              } catch (_) { return false; }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script) { result ->
            val ready = result == "true"
            if (ready || attempt >= 30) {
                revealWebView()
            } else {
                handler.postDelayed({ waitForLandingCardsThenReveal(attempt + 1) }, 250)
            }
        }
    }

    private fun revealWebView() {
        if (!::webView.isInitialized || !::splash.isInitialized) return
        if (webView.alpha < 1f) {
            webView.animate().alpha(1f).setDuration(400).start()
        }
        if (splash.visibility == View.VISIBLE) {
            splash.animate()
                .alpha(0f)
                .setDuration(450)
                .withEndAction {
                    splash.visibility = View.GONE
                    splash.alpha = 1f
                }
                .start()
        }
    }

    private fun scheduleNonDestructiveTopbarSelfHeal() {
        val delays = longArrayOf(250, 700, 1400, 2500, 4000, 6500, 9000)
        delays.forEach { delay -> handler.postDelayed({ injectTopbarSelfHeal() }, delay) }
    }

    private fun injectTopbarSelfHeal() {
        if (!::webView.isInitialized) return
        val script = """
            (function () {
              try {
                try { if (typeof window.__prismEnsureTopbar === 'function') window.__prismEnsureTopbar(); } catch (_) {}
                try { if (typeof window.__prismHealTopbar === 'function') window.__prismHealTopbar(); } catch (_) {}
                try { window.dispatchEvent(new CustomEvent('prism:ensure-topbar')); } catch (_) {}
                var body = document.body;
                if (!body || body.classList.contains('landing-mode')) return true;
                var skip = ['plugin-letters','plugin-angels','plugin-pillars','plugin-months','plugin-numbers'];
                for (var i=0; i<skip.length; i++) if (body.classList.contains(skip[i])) return true;
                var topbar = document.getElementById('topbar') || document.querySelector('.topbar');
                if (!topbar) return false;
                var cs = getComputedStyle(topbar), r = topbar.getBoundingClientRect();
                var hidden = cs.display === 'none' || cs.visibility === 'hidden' ||
                             parseFloat(cs.opacity || '1') === 0 || r.height < 8;
                if (hidden) {
                  topbar.style.setProperty('display','flex','important');
                  topbar.style.setProperty('visibility','visible','important');
                  topbar.style.setProperty('opacity','1','important');
                  topbar.style.setProperty('pointer-events','auto','important');
                  topbar.style.setProperty('z-index','999999','important');
                  body.classList.add('prism-android-topbar-healed','show-topbar');
                }
                return true;
              } catch (_) { return false; }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    private fun requestServiceWorkerUpdateWithoutReload() {
        val script = """
            (function () {
              if (!('serviceWorker' in navigator)) return;
              navigator.serviceWorker.getRegistrations().then(function (regs) {
                regs.forEach(function (reg) { try { reg.update(); } catch (_) {} });
              }).catch(function () {});
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::webView.isInitialized) webView.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        setImmersiveFlags()
        if (::webView.isInitialized) {
            webView.onResume()
            webView.resumeTimers()
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
