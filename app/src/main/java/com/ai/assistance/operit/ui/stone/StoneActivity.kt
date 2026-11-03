package com.ai.assistance.operit.ui.stone

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
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

/**
 * Stone —— 以本地页面形式内置在 Operit 里的私人空间。
 * 页面资源放在 assets/stone/index.html，全部离线可用。
 */
class StoneActivity : Activity() {

    private var webView: WebView? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val REQ_FILE = 1001
    private val REQ_PERM = 1002

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val wv = WebView(this)
        webView = wv
        wv.setBackgroundColor(0xFF0A0908.toInt())
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        wv.webViewClient = WebViewClient()
        wv.webChromeClient = object : WebChromeClient() {
            // 关键：让网页里的 <input type="file"> 能真正弹出选择器
            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                val intent = try {
                    params?.createIntent()
                } catch (e: Exception) {
                    null
                } ?: Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
                return try {
                    startActivityForResult(Intent.createChooser(intent, "选择文件"), REQ_FILE)
                    true
                } catch (e: Exception) {
                    filePathCallback = null
                    false
                }
            }

            // 录音 / 摄像头权限（语音输入要用）
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }
        wv.addJavascriptInterface(StoneBridge(), "StoneNative")
        wv.loadUrl("file:///android_asset/stone/index.html")

        setContentView(
            wv,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQ_FILE) {
            val cb = filePathCallback
            filePathCallback = null
            if (cb != null) {
                val uris = if (resultCode == RESULT_OK && data != null) {
                    WebChromeClient.FileChooserParams.parseResult(resultCode, data)
                } else null
                cb.onReceiveValue(uris)
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val wv = webView
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
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
        @JavascriptInterface
        fun ping(): String = "pong"
    }
}
