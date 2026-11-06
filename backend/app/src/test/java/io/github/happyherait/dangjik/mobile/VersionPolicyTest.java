package io.github.happyherait.dangjik.mobile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VersionPolicyTest {

    @Test
    void 숫자로_비교한다() {
        assertTrue(VersionPolicy.compare("3.10.0", "3.9.9") > 0);
        assertTrue(VersionPolicy.compare("3.8.0", "3.8.0") == 0);
        assertTrue(VersionPolicy.compare("2.99.99", "3.0.0") < 0);
    }

    @Test
    void 최소_지원_버전보다_낮으면_강제_업데이트() {
        assertTrue(VersionPolicy.decide("3.4.9", "3.8.0", "3.5.0").forceUpdate());
        assertTrue(VersionPolicy.decide("엉터리", "3.8.0", "3.5.0").forceUpdate());
    }

    @Test
    void 최소_지원_이상이고_최신보다_낮으면_선택_업데이트() {
        VersionPolicy.Decision d = VersionPolicy.decide("3.6.0", "3.8.0", "3.5.0");
        assertFalse(d.forceUpdate());
        assertTrue(d.updateAvailable());
        assertFalse(VersionPolicy.decide("3.8.0", "3.8.0", "3.5.0").updateAvailable());
    }
}
