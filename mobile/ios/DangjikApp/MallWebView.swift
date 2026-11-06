import SwiftUI
import UIKit
import WebKit

/// 하루마켓 몰을 띄우는 WKWebView. Android와 같은 window.DangjikApp 이름으로 기능을 연다.
struct MallWebView: UIViewRepresentable {
    let url: URL

    func makeCoordinator() -> Coordinator { Coordinator(mallHost: url.host ?? "") }

    func makeUIView(context: Context) -> WKWebView {
        let controller = WKUserContentController()
        controller.add(context.coordinator, name: "dangjik")
        // Android의 addJavascriptInterface와 같은 모양으로 맞춘다
        let shim = """
        window.DangjikApp = {
          getAppInfo: function () {
            return JSON.stringify({ platform: 'IOS', version: '\(AppConfig.version)', pushToken: 'demo-ios-token' });
          },
          share: function (text) { window.webkit.messageHandlers.dangjik.postMessage({ type: 'share', text: String(text) }); }
        };
        """
        controller.addUserScript(WKUserScript(source: shim, injectionTime: .atDocumentStart, forMainFrameOnly: true))

        let config = WKWebViewConfiguration()
        config.userContentController = controller
        config.applicationNameForUserAgent = "DangjikApp/\(AppConfig.version) (iOS)"

        let web = WKWebView(frame: .zero, configuration: config)
        web.navigationDelegate = context.coordinator
        web.allowsBackForwardNavigationGestures = true
        web.load(URLRequest(url: url))
        return web
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {}

    final class Coordinator: NSObject, WKScriptMessageHandler, WKNavigationDelegate {
        let mallHost: String

        init(mallHost: String) { self.mallHost = mallHost }

        func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
            // 몰 페이지에서 온 메시지만 받는다
            guard message.frameInfo.isMainFrame, message.frameInfo.request.url?.host == mallHost,
                  let body = message.body as? [String: Any], body["type"] as? String == "share",
                  let text = body["text"] as? String else { return }
            let sheet = UIActivityViewController(activityItems: [String(text.prefix(500))], applicationActivities: nil)
            UIApplication.shared.connectedScenes
                .compactMap { ($0 as? UIWindowScene)?.keyWindow?.rootViewController }
                .first?
                .present(sheet, animated: true)
        }

        func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction,
                     decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
            guard let url = navigationAction.request.url, let host = url.host, host != mallHost else {
                decisionHandler(.allow)
                return
            }
            // 몰 밖의 주소는 앱 안에서 열지 않고 Safari로 넘긴다
            UIApplication.shared.open(url)
            decisionHandler(.cancel)
        }
    }
}
