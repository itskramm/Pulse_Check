package com.example.pulsecheck

import android.os.Bundle
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class Location : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)
        val container = findViewById<FrameLayout>(R.id.map_container)
        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            loadUrl("file:///android_asset/map.html")
        }
        container.removeAllViews()
        container.addView(webView)
        NavHelper.setup(this, NavHelper.TAB_LOCATION)
    }
}
