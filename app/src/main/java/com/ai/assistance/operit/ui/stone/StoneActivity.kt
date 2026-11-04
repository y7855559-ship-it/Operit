package com.ai.assistance.operit.ui.stone

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.ai.assistance.operit.core.tools.AIToolHandler
import com.ai.assistance.operit.data.model.AITool
import com.ai.assistance.operit.data.model.ToolParameter
import java.io.File

class StoneActivity : Activity() {

    private var webView: WebView? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val REQ_FILE = 1001
    private val REQ_MIC = 1002

    // 外部页面（可直接用文件管理器替换，改界面不用重新编译）
    private val extPage = File("/sdcard/Download/DSH-Go/stone-v2/Stone_VelvetPaper_6_20_Fix29_Frontend.html")
    private val extPage2 = File("/sdcard/Download/DSH-Go/stone-v2/Stone_v2.html")

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            try { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_MIC) } catch (_: Exception) {}
        }
        try {
            if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")))
            }
        } catch (_: Exception) {}

        val wv = WebView(this)
        webView = wv
        wv.setBackgroundColor(0xFF0A0908.toInt())
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = true
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        wv.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(webView: WebView?, callback: ValueCallback<Array<Uri>>?, params: FileChooserParams?): Boolean {
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

        val target = when {
            extPage.exists() -> extPage
            extPage2.exists() -> extPage2
            else -> null
        }
        if (target != null) {
            wv.loadUrl("file://" + target.absolutePath)
            Toast.makeText(this, "Stone：外部页面已加载", Toast.LENGTH_SHORT).show()
        } else {
            wv.loadUrl("file:///android_asset/stone/index.html")
        }
        wv.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                view?.loadUrl("file:///android_asset/stone/index.html")
            }
        }

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
                        WebChromeClient.FileChooserParams.parseResult(resultCode, data) else null
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

    private fun esc(v: String): String =
        v.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")

    inner class StoneBridge {
        @JavascriptInterface fun ping(): String = "pong"
        @JavascriptInterface fun reload() { runOnUiThread { webView?.reload() } }
        @JavascriptInterface fun openOperit() {
            runOnUiThread {
                try {
                    val cls = Class.forName("com.ai.assistance.operit.ui.main.MainActivity")
                    startActivity(Intent(this@StoneActivity, cls))
                } catch (e: Exception) {
                    Toast.makeText(this@StoneActivity, "打不开 Operit：${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        /** 列出 Operit 全部工具名 */
        @JavascriptInterface fun listTools(): String {
            return try {
                val h = AIToolHandler.getInstance(this@StoneActivity)
                val names = h.getAllToolNames()
                names.joinToString(prefix = "[", postfix = "]") { "\"" + esc(it) + "\"" }
            } catch (e: Exception) { "[]" }
        }

        /** 调用 Operit 工具：name + 参数数组 JSON [{"name":"path","value":"/sdcard"}] */
        @JavascriptInterface fun callTool(name: String, paramsJson: String): String {
            return try {
                val h = AIToolHandler.getInstance(this@StoneActivity)
                val arr = org.json.JSONArray(paramsJson.ifBlank { "[]" })
                val params = ArrayList<ToolParameter>()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    params.add(ToolParameter(o.optString("name"), o.optString("value")))
                }
                val res = h.executeTool(AITool(name = name, parameters = params))
                "{\"success\":" + res.success +
                    ",\"error\":\"" + esc(res.error ?: "") +
                    "\",\"result\":\"" + esc(res.result.toString()) + "\"}"
            } catch (e: Exception) {
                "{\"success\":false,\"error\":\"" + esc(e.message ?: "call failed") +
                    "\",\"result\":\"\"}"
            }
        }
    }
}
