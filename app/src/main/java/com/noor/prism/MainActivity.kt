package com.noor.prism

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var splashOverlay: View
    private val handler = Handler(Looper.getMainLooper())
    private var splashHidden = false

    private val prismUrl = "https://amtunnoor.github.io/Quran/index.html?app=prism-runtime"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setImmersiveFlags()

        val root = FrameLayout(this)
        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(9, 11, 34))
            alpha = 0f
        }
        root.addView(
            webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        splashOverlay = createNativeSplash()
        root.addView(
            splashOverlay,
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
            webView.restoreState(savedInstanceState)
        }

        handler.postDelayed({ hideSplashSafely() }, 12000)
    }

    private fun createNativeSplash(): View {
        val background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.rgb(8, 12, 40),
                Color.rgb(30, 25, 80),
                Color.rgb(7, 35, 72)
            )
        )

        val container = FrameLayout(this).apply { this.background = background }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        val icon = ImageView(this).apply {
            setImageResource(R.drawable.app_icon)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        content.addView(icon, LinearLayout.LayoutParams(dp(120), dp(120)))

        val title = TextView(this).apply {
            text = "Noor's Prism"
            setTextColor(Color.WHITE)
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val titleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(18) }
        content.addView(title, titleParams)

        val subtitle = TextView(this).apply {
            text = "Preparing adventures…"
            setTextColor(Color.argb(220, 230, 235, 255))
            textSize = 16f
            gravity = Gravity.CENTER
        }
        val subtitleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) }
        content.addView(subtitle, subtitleParams)

        container.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        return container
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

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
            cacheMode = if (isOnline()) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK

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

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                injectRuntimeExperience()
                scheduleNonDestructiveTopbarSelfHeal()
                requestServiceWorkerUpdateWithoutReload()
                waitForLandingThenReveal()
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

    private fun waitForLandingThenReveal() {
        val checks = longArrayOf(120, 300, 600, 1000, 1600, 2400, 3600, 5200, 7600)
        checks.forEach { delay ->
            handler.postDelayed({
                if (splashHidden || !::webView.isInitialized) return@postDelayed
                webView.evaluateJavascript(
                    "(function(){var g=document.getElementById('grid');return !!(document.body&&g&&g.children&&g.children.length>0);})()"
                ) { result ->
                    if (result == "true") hideSplashSafely()
                }
            }, delay)
        }
    }

    private fun hideSplashSafely() {
        if (splashHidden || !::webView.isInitialized) return
        splashHidden = true
        webView.animate().alpha(1f).setDuration(220).start()
        splashOverlay.animate()
            .alpha(0f)
            .setDuration(260)
            .withEndAction { splashOverlay.visibility = View.GONE }
            .start()
    }

    private fun injectRuntimeExperience() {
        if (!::webView.isInitialized) return

        val deviceClass = when {
            isTelevision() -> "prism-app-tv"
            resources.configuration.smallestScreenWidthDp >= 600 -> "prism-app-tablet"
            else -> "prism-app-phone"
        }

        val script = """
            (function(){
              try {
                var root=document.documentElement, body=document.body;
                if(!body) return false;
                root.classList.add('prism-android-app');
                body.classList.add('prism-android-app','$deviceClass');

                var old=document.getElementById('prismAndroidRuntimeStyle');
                if(old) old.remove();
                var style=document.createElement('style');
                style.id='prismAndroidRuntimeStyle';
                style.textContent=`
                  body.prism-android-app.landing-mode #grid.grid{
                    scrollbar-width:none!important;
                    -ms-overflow-style:none!important;
                    overscroll-behavior-x:contain!important;
                    scroll-behavior:smooth!important;
                  }
                  body.prism-android-app.landing-mode #grid.grid::-webkit-scrollbar{display:none!important;}
                  body.prism-android-app.landing-mode #grid.grid .card{scroll-snap-align:center!important;}

                  body.prism-app-phone.landing-mode #grid.grid{
                    display:grid!important;
                    grid-template-columns:none!important;
                    grid-template-rows:1fr!important;
                    grid-auto-flow:column!important;
                    grid-auto-columns:min(72vw,340px)!important;
                    overflow-x:auto!important;
                    overflow-y:hidden!important;
                    scroll-snap-type:x mandatory!important;
                    justify-content:start!important;
                    align-content:center!important;
                    gap:18px!important;
                    padding-left:14vw!important;
                    padding-right:14vw!important;
                  }
                  body.prism-app-tablet.landing-mode #grid.grid{
                    display:grid!important;
                    grid-template-columns:none!important;
                    grid-template-rows:repeat(2,minmax(190px,1fr))!important;
                    grid-auto-flow:column!important;
                    grid-auto-columns:min(36vw,360px)!important;
                    overflow-x:auto!important;
                    overflow-y:hidden!important;
                    scroll-snap-type:x mandatory!important;
                    justify-content:start!important;
                    gap:18px!important;
                    padding-left:7vw!important;
                    padding-right:7vw!important;
                  }
                  body.prism-app-tv.landing-mode #grid.grid{
                    display:grid!important;
                    grid-template-columns:none!important;
                    grid-template-rows:repeat(2,minmax(210px,1fr))!important;
                    grid-auto-flow:column!important;
                    grid-auto-columns:clamp(240px,22vw,360px)!important;
                    overflow-x:auto!important;
                    overflow-y:hidden!important;
                    scroll-snap-type:x mandatory!important;
                    justify-content:start!important;
                    align-content:center!important;
                    gap:20px!important;
                    padding-left:4vw!important;
                    padding-right:4vw!important;
                  }
                `;
                document.head.appendChild(style);

                if(!window.__prismAndroidLandingObserver){
                  window.__prismAndroidLandingObserver=new MutationObserver(function(){
                    if(!document.body) return;
                    document.body.classList.add('prism-android-app','$deviceClass');
                  });
                  window.__prismAndroidLandingObserver.observe(document.body,{attributes:true,attributeFilter:['class']});
                }
                return true;
              } catch(e){ return false; }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    private fun isTelevision(): Boolean {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    private fun scheduleNonDestructiveTopbarSelfHeal() {
        val delaysMs = longArrayOf(250, 700, 1400, 2500, 4000, 6500, 9000)
        delaysMs.forEach { delay -> handler.postDelayed({ injectTopbarSelfHeal() }, delay) }
    }

    private fun injectTopbarSelfHeal() {
        if (!::webView.isInitialized) return
        val script = """
            (function () {
              try {
                var body = document.body;
                if (!body || body.classList.contains('landing-mode')) return true;
                try { if (typeof window.__prismEnsureTopbar === 'function') window.__prismEnsureTopbar(); } catch (_) {}
                try { if (typeof window.__prismHealTopbar === 'function') window.__prismHealTopbar(); } catch (_) {}
                try { window.dispatchEvent(new CustomEvent('prism:ensure-topbar')); } catch (_) {}
                var skip=['plugin-letters','plugin-angels','plugin-pillars','plugin-months','plugin-numbers'];
                for(var i=0;i<skip.length;i++){ if(body.classList.contains(skip[i])) return true; }
                var topbar=document.getElementById('topbar')||document.querySelector('.topbar');
                if(!topbar) return false;
                var style=window.getComputedStyle(topbar), rect=topbar.getBoundingClientRect();
                var hidden=style.display==='none'||style.visibility==='hidden'||parseFloat(style.opacity||'1')===0||rect.height<8;
                if(hidden){
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
              navigator.serviceWorker.getRegistrations().then(function(regs){
                regs.forEach(function(reg){try{reg.update();}catch(_){}});
              }).catch(function(){});
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
        } catch (_: Throwable) { true }
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
            injectRuntimeExperience()
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
            if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else moveTaskToBack(true)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
