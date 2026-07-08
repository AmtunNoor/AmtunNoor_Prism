package com.noor.prism

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
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
    private val prismUrl = "https://amtunnoor.github.io/Quran/index.html?app=prism"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )

        webView = WebView(this)
        webView.setBackgroundColor(Color.rgb(15, 23, 42))
        setContentView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        configureWebView()
        if (savedInstanceState == null) webView.loadUrl(prismUrl)
        else webView.restoreState(savedInstanceState)
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
                injectPrismAppShellFixes(view)
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

    private fun injectPrismAppShellFixes(view: WebView) {
        val js = """
            (function(){
              document.documentElement.classList.add('prism-android-app');
              document.body && document.body.classList.add('prism-android-app');
              if(navigator.serviceWorker){
                navigator.serviceWorker.getRegistrations().then(function(regs){
                  regs.forEach(function(r){ try{ r.update(); }catch(e){} });
                }).catch(function(){});
              }
              if(window.__prismHealTopbar) window.__prismHealTopbar();
              setTimeout(function(){ if(window.__prismHealTopbar) window.__prismHealTopbar(); }, 400);
              setTimeout(function(){ if(window.__prismHealTopbar) window.__prismHealTopbar(); }, 1200);
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
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
        webView.onResume()
        webView.evaluateJavascript("if(window.__prismHealTopbar) window.__prismHealTopbar();", null)
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
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
