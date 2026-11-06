package io.github.happyherait.dangjik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.happyherait.dangjik.common.ApiError;
import io.github.happyherait.dangjik.mobile.AppVersionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class AppVersionIT {

    @Autowired
    AppVersionService versions;

    @Test
    void 팀장이_최소_버전을_올리면_예전_앱은_강제_업데이트된다() {
        assertFalse(versions.check("ANDROID", "3.8.0").forceUpdate());

        versions.update("ANDROID", "3.9.0", "3.9.0", "lead.park");

        assertTrue(versions.check("ANDROID", "3.8.0").forceUpdate());
        assertFalse(versions.check("IOS", "3.8.0").forceUpdate());
    }

    @Test
    void 팀장이_아니거나_잘못된_값이면_바꿀_수_없다() {
        assertThrows(ApiError.Conflict.class, () -> versions.update("ANDROID", "3.9.0", "3.9.0", "ops.kim"));
        assertThrows(ApiError.Conflict.class, () -> versions.update("ANDROID", "3.9.0", "4.0.0", "lead.park"));
        assertThrows(ApiError.Conflict.class, () -> versions.update("ANDROID", "3.9", "3.5.0", "lead.park"));
        assertEquals("3.8.0", versions.check("ANDROID", "3.8.0").latestVersion());
    }
}
