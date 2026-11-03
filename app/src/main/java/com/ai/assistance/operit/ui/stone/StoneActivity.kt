package com.ai.assistance.operit.ui.stone

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class StoneActivity : Activity() {

    private var webView: WebView? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val REQ_FILE = 1001
    private val REQ_MIC = 1002

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            try { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_MIC) } catch (_: Exception) {}
        }

        val wv = WebView(this)
        webView = wv
        wv.setBackgroundColor(0xFF0A0908.toInt())
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            // 语音/TTS 直连各家 API 需要（页面是 file:// 源）
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = true
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        wv.webViewClient = WebViewClient()
        wv.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                val intent = try { params?.createIntent() } catch (_: Exception) { null }
                    ?: Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
                return try {
                    startActivityForResult(Intent.createChooser(intent, "选择文件"), REQ_FILE); true
                } catch (_: Exception) { filePathCallback = null; false }
            }
            override fun onPermissionRequest(request: PermissionRequest?) {
                try { request?.grant(request.resources) } catch (_: Exception) {}
            }
        }
        wv.addJavascriptInterface(StoneBridge(), "StoneNative")
        wv.loadUrl("file:///android_asset/stone/index.html")

        setContentView(wv, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQ_FILE) {
            val cb = filePathCallback; filePathCallback = null
            if (cb != null) {
                cb.onReceiveValue(
                    if (resultCode == RESULT_OK && data != null)
                        WebChromeClient.FileChooserParams.parseResult(resultCode, data)
                    else null
                )
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val wv = webView
        if (wv != null && wv.canGoBack()) wv.goBack()
        else { @Suppress("DEPRECATION") super.onBackPressed() }
    }

    override fun onDestroy() {
        webView?.let { wv ->
            wv.loadUrl("about:blank")
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.destroy()
        }
        webView = null
        super.onDestroy()
    }

    inner class StoneBridge {
        @JavascriptInterface fun ping(): String = "pong"
    }
}
