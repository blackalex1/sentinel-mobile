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
        val tunLogLine = "from tcp:10.0.0.2:54321 accepted tcp:91.219.148.131:443"
        val parsed = SentinelCore.parseConnectionLog(tunLogLine)
        
        assertNotNull("SentinelCore.parseConnectionLog must successfully parse valid Xray log line", parsed)
        assertEquals("TCP", parsed!!.protocol)
        assertEquals("10.0.0.2", parsed.srcIp)
        assertEquals(54321, parsed.srcPort)
        assertEquals("91.219.148.131", parsed.destIp)
        assertEquals(443, parsed.destPort)

        // Test non-accepted log skipping
        val nonConnLog = "[Info] xray.com/core/app/proxyman/inbound: connection closed"
        val skipped = SentinelCore.parseConnectionLog(nonConnLog)
        assertTrue("Non-connection log lines must return null", skipped == null)
    }
}
