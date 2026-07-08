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
    private val prismBaseUrl = "https://amtunnoor.github.io/Quran/index.html"

    private fun prismLaunchUrl(): String {
        // Fresh shell every launch. Assets/audio remain cacheable through normal WebView + service worker caching.
        return "$prismBaseUrl?app=prism&apk=${BuildConfig.VERSION_CODE}&shell=${System.currentTimeMillis()}"
    }

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

        clearWebViewCacheOncePerApkVersion()
        configureWebView()
        if (savedInstanceState == null) webView.loadUrl(prismLaunchUrl())
        else webView.restoreState(savedInstanceState)
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

    private fun clearWebViewCacheOncePerApkVersion() {
        val prefs = getSharedPreferences("prism_app_state", MODE_PRIVATE)
        val lastCacheVersion = prefs.getInt("last_cache_version", -1)
        if (lastCacheVersion != BuildConfig.VERSION_CODE) {
            try {
                webView.stopLoading()
                webView.clearCache(true)
                webView.clearHistory()
            } catch (_: Throwable) {}
            prefs.edit().putInt("last_cache_version", BuildConfig.VERSION_CODE).apply()
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
        s.javaScriptCanOpenWindowsAutomatically = false
        s.setSupportZoom(false)
        s.builtInZoomControls = false
        s.displayZoomControls = false
        s.loadWithOverviewMode = true
        s.useWideViewPort = true
        s.cacheMode = if (isOnline()) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK
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
                sw.cacheMode = WebSettings.LOAD_DEFAULT
            } catch (_: Throwable) {}
        }

        WebView.setWebContentsDebuggingEnabled(false)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                return true
            }
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

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                // Keep last cached page visible where possible. No native dashboard fallback.
            }
        }
    }

    private fun runAppShellSelfHealChecks() {
        val delays = longArrayOf(250, 800, 1600, 3000, 5000, 8000)
        delays.forEach { delay ->
            handler.postDelayed({ injectPrismAppShellFixes(webView) }, delay)
        }
    }

    private fun injectPrismAppShellFixes(view: WebView) {
        val js = """
            (function(){
              function anyAudioPlaying(){
                try{
                  return Array.prototype.some.call(document.querySelectorAll('audio'), function(a){
                    return a && !a.paused && !a.ended && a.currentTime > 0;
                  });
                }catch(e){ return false; }
              }

              function shouldForceTopbar(){
                try{
                  var body = document.body;
                  if(!body) return false;
                  var cls = body.classList;
                  if(cls.contains('landing-mode')) return false;
                  // These visual-only/floating-control modules intentionally do not need the full fixed menu.
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

              document.documentElement.classList.add('prism-android-app');
              if(document.body) document.body.classList.add('prism-android-app');

              // Update service worker in the background, but never interrupt currently playing audio/effects.
              if(navigator.serviceWorker){
                navigator.serviceWorker.getRegistrations().then(function(regs){
                  regs.forEach(function(r){ try{ r.update(); }catch(e){} });
                }).catch(function(){});
              }

              forceTopbarVisible();

              if(!window.__prismAndroidTopbarWatchdog){
                window.__prismAndroidTopbarWatchdog = setInterval(function(){
                  // JS self-heal only. Do NOT reload while audio is playing.
                  forceTopbarVisible();
                  anyAudioPlaying();
                }, 1500);
                setTimeout(function(){
                  try{ clearInterval(window.__prismAndroidTopbarWatchdog); window.__prismAndroidTopbarWatchdog = null; }catch(e){}
                }, 15000);
              }
            })();
        """.trimIndent()
        view.evaluateJavascript(js, null)
    }

    private fun isOnline(): Boolean {
        return try {
            val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Throwable) { true }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        setImmersiveFlags()
        webView.onResume()
        runAppShellSelfHealChecks()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try { webView.destroy() } catch (_: Throwable) {}
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                moveTaskToBack(true)
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
