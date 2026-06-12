package com.noor.prism

import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // FIXED: Inflates the physical XML resource layout file to inherit system network permissions
        setContentView(R.layout.activity_web_view)
        
        // FIXED: Explicitly binds the view instance by its resource layout ID identifier
        webView = findViewById(R.id.tvWebView)

        // FORCE RENDER WEB COMPLIANCE SETTINGS
        webView.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let { view?.loadUrl(it) }
                return true
            }
        }

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.databaseEnabled = true
        webSettings.mediaPlaybackRequiresUserGesture = false

        // Fetch URL route bundle variables passed forward out of MainActivity click streams
        val url = intent.getStringExtra("URL")
        if (!url.isNullOrEmpty()) {
            webView.loadUrl(url)
        } else {
            finish()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
