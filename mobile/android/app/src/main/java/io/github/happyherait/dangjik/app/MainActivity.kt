package io.github.happyherait.dangjik.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject

/** 하루마켓 몰을 띄우는 하이브리드 앱 셸 */
class MainActivity : Activity() {

    private lateinit var web: WebView
    private val mallHost: String by lazy { Uri.parse(BuildConfig.MALL_URL).host ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        // 몰은 이 표시로 앱 안에서 열렸는지 안다
        web.settings.userAgentString = web.settings.userAgentString + " DangjikApp/${BuildConfig.VERSION_NAME} (Android)"
        web.addJavascriptInterface(Bridge(this), "DangjikApp")
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.host == mallHost) return false
                // 몰 밖의 주소는 앱 안에서 열지 않고 브라우저로 넘긴다
                startActivity(Intent(Intent.ACTION_VIEW, uri))
                return true
            }
        }

        if (savedInstanceState == null) {
            web.loadUrl(BuildConfig.MALL_URL)
        } else {
            web.restoreState(savedInstanceState)
        }
        checkVersion()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && web.canGoBack()) {
            web.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    /** 서버에 버전을 묻고, 최소 지원 버전보다 낮으면 닫을 수 없는 안내를 띄운다 */
    private fun checkVersion() {
        Thread {
            val result = runCatching {
                val version = URLEncoder.encode(BuildConfig.VERSION_NAME, "UTF-8")
                val url = URL("${BuildConfig.API_URL}/api/app/version-check?platform=ANDROID&version=$version")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            }.getOrNull() ?: return@Thread

            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                if (result.optBoolean("forceUpdate")) {
                    AlertDialog.Builder(this)
                        .setTitle("업데이트가 필요합니다")
                        .setMessage(result.optString("message"))
                        .setCancelable(false)
                        .setPositiveButton("닫기") { _, _ -> finish() }
                        .show()
                } else if (result.optBoolean("updateAvailable")) {
                    Toast.makeText(this, result.optString("message"), Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}
