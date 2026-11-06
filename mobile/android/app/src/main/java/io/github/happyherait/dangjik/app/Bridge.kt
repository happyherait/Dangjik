package io.github.happyherait.dangjik.app

import android.app.Activity
import android.content.Intent
import android.webkit.JavascriptInterface
import org.json.JSONObject

/**
 * 웹(몰)에서 window.DangjikApp 으로 부르는 기능. 앱 정보 조회와 공유만 연다.
 * MainActivity가 몰 주소 밖의 페이지는 WebView에 띄우지 않으므로 다른 사이트는 이 객체를 볼 수 없다.
 */
class Bridge(private val activity: Activity) {

    @JavascriptInterface
    fun getAppInfo(): String = JSONObject()
        .put("platform", "ANDROID")
        .put("version", BuildConfig.VERSION_NAME)
        // 프로토타입이라 실제 푸시 서버 대신 가짜 토큰을 준다
        .put("pushToken", "demo-android-token")
        .toString()

    @JavascriptInterface
    fun share(text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text.take(500))
        }
        activity.runOnUiThread {
            activity.startActivity(Intent.createChooser(send, "공유하기"))
        }
    }
}
