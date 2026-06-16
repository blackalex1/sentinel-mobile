package com.xprox.sentinel.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Method

class TetheringScannerTest {

    private fun invokeIsPrivateIp(ip: String): Boolean {
        val method = TetheringScanner::class.java.getDeclaredMethod("isPrivateIp", String::class.java).apply {
            isAccessible = true
        }
        return method.invoke(TetheringScanner, ip) as Boolean
    }

    @Test
    fun testIsPrivateIpStandardRanges() {
        // Class C private range 192.168.0.0/16
        assertTrue(invokeIsPrivateIp("192.168.1.1"))
        assertTrue(invokeIsPrivateIp("192.168.254.254"))

        // Class A private range 10.0.0.0/8 (excluding VPN range 10.0.0.x)
        assertTrue(invokeIsPrivateIp("10.5.5.5"))
        assertTrue(invokeIsPrivateIp("10.254.254.254"))

        // Class B private range 172.16.0.0/12
        assertTrue(invokeIsPrivateIp("172.16.0.1"))
        assertTrue(invokeIsPrivateIp("172.31.255.254"))

        // CGNAT range 100.64.0.0/10
        assertTrue(invokeIsPrivateIp("100.64.0.1"))
        assertTrue(invokeIsPrivateIp("100.127.255.254"))
    }

    @Test
    fun testIsPrivateIpExcludedAndPublicRanges() {
        // Excluded loopback range 127.0.0.0/8
        assertFalse(invokeIsPrivateIp("127.0.0.1"))
        assertFalse(invokeIsPrivateIp("127.255.255.255"))

        // Excluded VPN range 10.0.0.0/24
        assertFalse(invokeIsPrivateIp("10.0.0.1"))
        assertFalse(invokeIsPrivateIp("10.0.0.50"))
        assertFalse(invokeIsPrivateIp("10.0.0.254"))

        // Public WAN ranges
        assertFalse(invokeIsPrivateIp("8.8.8.8"))
        assertFalse(invokeIsPrivateIp("203.0.113.8"))
        assertFalse(invokeIsPrivateIp("185.152.12.3"))
    }

    @Test
    fun testIsPrivateIpInvalidFormats() {
        // Invalid IP patterns or characters
        assertFalse(invokeIsPrivateIp("abc"))
        assertFalse(invokeIsPrivateIp("192.168.1"))
        assertFalse(invokeIsPrivateIp("192.168.1.1.1"))
        assertFalse(invokeIsPrivateIp("192.168.1.abc"))
    }

    @Test
    fun testGetActiveTetheringIpsExecutesSuccessfully() {
        // Verify that calling getActiveTetheringIps completes without throwing exceptions on local JVM
        val ips = TetheringScanner.getActiveTetheringIps()
        // The JVM environment may return an empty list or local adapter IPs; just verify it compiles and runs fine
        assertTrue("Should return a valid List object", ips is List<String>)
    }
}
