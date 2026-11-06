import Foundation

enum AppConfig {
    static var version: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "0.0.0"
    }
    static var mallURL: URL {
        URL(string: Bundle.main.object(forInfoDictionaryKey: "DangjikMallURL") as? String ?? "http://localhost:3005")!
    }
    static var apiURL: URL {
        URL(string: Bundle.main.object(forInfoDictionaryKey: "DangjikApiURL") as? String ?? "http://localhost:18082")!
    }
}
