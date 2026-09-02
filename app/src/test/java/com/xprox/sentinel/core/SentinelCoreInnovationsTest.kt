package com.xprox.sentinel.core

import com.xprox.sentinel.core.models.ParsedConnectionLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SentinelCoreInnovationsTest {

    @Test
    fun testNativeCoreAvailability() {
        assertTrue("Native Sentinel-Core library must be loaded and available during unit tests!", SentinelCore.isAvailable())
    }

    @Test
    fun testDynamicPresetsListingAndRetrieval() {
        val presets = SentinelCore.listPresets()
        assertNotNull("SentinelCore.listPresets() must not return null", presets)
        
        // Test fallback or core preset availability
        if (presets.isNotEmpty()) {
            val firstPreset = presets.first()
            assertTrue("Preset ID must not be empty", firstPreset.id.isNotEmpty())
            assertTrue("Preset name must not be empty", firstPreset.name.isNotEmpty())
        }

        // Test fetching specific preset details
        val ruPreset = SentinelCore.getPreset("ru")
        if (ruPreset != null) {
            assertEquals("ru", ruPreset.id)
            assertNotNull("RU preset must contain domains or ips", ruPreset.domains ?: ruPreset.ips)
        }

        val ipCheckersPreset = SentinelCore.getPreset("ip_checkers")
        assertNotNull("ip_checkers preset must be available", ipCheckersPreset)
        assertEquals("ip_checkers", ipCheckersPreset!!.id)
        assertTrue("ip_checkers preset must contain domains", ipCheckersPreset.domains?.isNotEmpty() == true)
        assertTrue("ip_checkers preset should not contain DNS IPs", ipCheckersPreset.ips.isNullOrEmpty())
    }

    @Test
    fun testSecuritySchemaAndDefaultConfig() {
        val schema = SentinelCore.getSecuritySchema("ru")
        // Should return valid string schema or fallback null
        if (schema != null) {
            assertTrue("Security schema must contain JSON or schema keys", schema.contains("rules") || schema.contains("{"))
        }

        val defaultConfig = SentinelCore.getDefaultSecurityConfig()
        if (defaultConfig != null) {
            assertTrue("Default security config must be valid JSON", defaultConfig.startsWith("{") && defaultConfig.endsWith("}"))
        }
    }

    @Test
    fun testRuleOrConfigValidation() {
        // 1. Valid Domain
        val (validDomain, msgDomain) = SentinelCore.validateRuleOrConfig("google.com")
        assertTrue("google.com must be validated as a valid domain rule", validDomain)

        // 2. Valid IP with CIDR
        val (validIp, msgIp) = SentinelCore.validateRuleOrConfig("192.168.1.0/24")
        assertTrue("192.168.1.0/24 must be validated as a valid IP rule", validIp)

        // 3. Valid GeoIP / GeoSite tag
        val (validGeo, msgGeo) = SentinelCore.validateRuleOrConfig("geosite:category-ads-all")
        assertTrue("geosite:category-ads-all must be validated as a valid geosite rule", validGeo)

        // 4. Invalid Rule String
        val (invalidRule, msgInvalid) = SentinelCore.validateRuleOrConfig("invalid domain with spaces!")
        assertFalse("Domain with spaces must fail validation", invalidRule)

        // 5. Empty Rule String
        val (emptyRule, msgEmpty) = SentinelCore.validateRuleOrConfig("")
        assertFalse("Empty rule string must fail validation", emptyRule)
    }

    @Test
    fun testNativeConnectionLogParser() {
        val tunLogLine = "from tcp:10.0.0.2:54321 accepted tcp:198.51.100.131:443"
        val parsed = SentinelCore.parseConnectionLog(tunLogLine)
        
        assertNotNull("SentinelCore.parseConnectionLog must successfully parse valid Xray log line", parsed)
        assertEquals("TCP", parsed!!.protocol)
        assertEquals("10.0.0.2", parsed.srcIp)
        assertEquals(54321, parsed.srcPort)
        assertEquals("198.51.100.131", parsed.destIp)
        assertEquals(443, parsed.destPort)

        // Test non-accepted log skipping
        val nonConnLog = "[Info] xray.com/core/app/proxyman/inbound: connection closed"
        val skipped = SentinelCore.parseConnectionLog(nonConnLog)
        assertTrue("Non-connection log lines must return null", skipped == null)
    }

    @Test
    fun testNativeBatchPing() {
        val targets = listOf(
            com.xprox.sentinel.core.models.PingTarget(id = "target-1", address = "127.0.0.1", port = 80),
            com.xprox.sentinel.core.models.PingTarget(id = "target-2", address = "1.1.1.1", port = 53)
        )
        val results = SentinelCore.batchPing(targets, timeoutMs = 1000)
        assertEquals(2, results.size)
        assertEquals("target-1", results[0].id)
        assertEquals("target-2", results[1].id)
    }

    @Test
    fun testNativeAndroidRingBufferAndStats() {
        SentinelCore.clearLogs()

        val entry1 = com.xprox.sentinel.core.models.AndroidLogEntry(
            packageName = "org.telegram.messenger",
            appName = "Telegram",
            destinationIp = "198.51.100.50",
            destinationPort = 443,
            protocol = "TCP",
            serviceName = "HTTPS",
            action = "direct"
        )
        val entry2 = com.xprox.sentinel.core.models.AndroidLogEntry(
            packageName = "com.google.android.youtube",
            appName = "YouTube",
            destinationIp = "198.51.100.206",
            destinationPort = 80,
            protocol = "TCP",
            serviceName = "HTTP",
            action = "direct"
        )

        SentinelCore.pushLog(entry1)
        SentinelCore.pushLog(entry2)

        val logs = SentinelCore.getLogs(limit = 10)
        assertTrue("RingBuffer must contain at least 2 logs", logs.size >= 2)

        val stats = SentinelCore.getLogStats()
        assertTrue("Stats totalConnections must be at least 2", stats.totalConnections >= 2)

        // Clear and verify
        SentinelCore.clearLogs()
        val clearedLogs = SentinelCore.getLogs(limit = 10)
        assertEquals(0, clearedLogs.size)
    }
}
