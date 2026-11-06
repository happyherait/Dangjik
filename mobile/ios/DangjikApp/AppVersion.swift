import Foundation

/// 서버의 VersionPolicy와 같은 규칙
enum AppVersion {
    static func isValid(_ v: String) -> Bool {
        v.range(of: #"^\d{1,3}\.\d{1,3}\.\d{1,3}$"#, options: .regularExpression) != nil
    }

    static func compare(_ a: String, _ b: String) -> Int {
        let x = a.split(separator: ".").compactMap { Int($0) }
        let y = b.split(separator: ".").compactMap { Int($0) }
        for i in 0..<3 where x[i] != y[i] {
            return x[i] < y[i] ? -1 : 1
        }
        return 0
    }

    static func mustUpdate(current: String, minSupported: String) -> Bool {
        !isValid(current) || compare(current, minSupported) < 0
    }
}
