package io.github.happyherait.dangjik.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

    @Test
    fun comparesNumerically() {
        assertTrue(AppVersion.compare("3.10.0", "3.9.9") > 0)
        assertEquals(0, AppVersion.compare("3.8.0", "3.8.0"))
    }

    @Test
    fun mustUpdateBelowMinimumOrUnknown() {
        assertTrue(AppVersion.mustUpdate("3.4.9", "3.5.0"))
        assertTrue(AppVersion.mustUpdate("dev", "3.5.0"))
        assertFalse(AppVersion.mustUpdate("3.8.0", "3.5.0"))
    }
}
