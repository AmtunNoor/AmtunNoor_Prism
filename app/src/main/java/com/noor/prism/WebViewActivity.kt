package com.noor.prism

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled", "DiscouragedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // DYNAMIC ROUTING: Automatically catches variations like activity_web_view OR activity_webview
        var layoutId = resources.getIdentifier("activity_web_view", "layout", packageName)
        if (layoutId == 0) {
            layoutId = resources.getIdentifier("activity_webview", "layout", packageName)
        }
        if (layoutId == 0) {
            layoutId = resources.getIdentifier("webview_activity", "layout", packageName)
        }
        
        // Fallback to safely inflate the layout view engine
        if (layoutId != 0) {
            setContentView(layoutId)
        } else {
            // Ultimate safety net: creates the WebView programmatically if the file is completely missing
            webView = WebView(this)
            setContentView(webView)
        }

        // Connect the WebView object safely
        try {
            webView = findViewById(resources.getIdentifier("webView", "id", packageName))
        } catch (e: Exception) {
            if (!::webView.isInitialized) {
                webView = WebView(this)
                setContentView(webView)
            }
        }
        
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let { view?.loadUrl(it) }
                return true
            }
        }

        val url = intent.getStringExtra("URL") ?: "https://google.com"
        webView.loadUrl(url)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && ::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
