import SwiftUI

struct VersionCheck: Decodable {
    let forceUpdate: Bool
    let updateAvailable: Bool
    let message: String
}

@main
struct DangjikApp: App {
    @State private var force: VersionCheck?

    var body: some Scene {
        WindowGroup {
            MallWebView(url: AppConfig.mallURL)
                .ignoresSafeArea(edges: .bottom)
                .task { force = await Self.checkVersion() }
                .alert("업데이트가 필요합니다", isPresented: .constant(force?.forceUpdate == true)) {
                    Button("닫기") { exit(0) }
                } message: {
                    Text(force?.message ?? "")
                }
        }
    }

    /// 서버에 버전을 묻는다. 실패하면 앱을 막지 않는다.
    static func checkVersion() async -> VersionCheck? {
        var parts = URLComponents(url: AppConfig.apiURL.appendingPathComponent("api/app/version-check"),
                                  resolvingAgainstBaseURL: false)
        parts?.queryItems = [URLQueryItem(name: "platform", value: "IOS"),
                             URLQueryItem(name: "version", value: AppConfig.version)]
        guard let url = parts?.url,
              let (data, _) = try? await URLSession.shared.data(from: url) else { return nil }
        return try? JSONDecoder().decode(VersionCheck.self, from: data)
    }
}
