import XCTest
@testable import DangjikApp

final class AppVersionTests: XCTestCase {
    func testComparesNumerically() {
        XCTAssertGreaterThan(AppVersion.compare("3.10.0", "3.9.9"), 0)
        XCTAssertEqual(AppVersion.compare("3.8.0", "3.8.0"), 0)
    }

    func testMustUpdateBelowMinimumOrUnknown() {
        XCTAssertTrue(AppVersion.mustUpdate(current: "3.4.9", minSupported: "3.5.0"))
        XCTAssertTrue(AppVersion.mustUpdate(current: "dev", minSupported: "3.5.0"))
        XCTAssertFalse(AppVersion.mustUpdate(current: "3.8.0", minSupported: "3.5.0"))
    }
}
