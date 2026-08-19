package com.xprox.sentinel.config

import com.xprox.sentinel.config.XrayConfigManager.ServerProfile
import com.xprox.sentinel.core.SentinelCore
import com.xprox.sentinel.core.models.*
import com.xprox.sentinel.core.toCoreProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XrayConfigBuilderTest {

    @Test
    fun testVlessConfigCompilationViaSentinelCore() {
        assertTrue("Native Sentinel-Core engine must be loaded!", SentinelCore.isAvailable())

        val profile = ServerProfile(
            name = "VlessReality",
            address = "vless.server.com",
            port = 443,
            type = "VLESS",
            uuid = "uuid-test-123",
            security = "reality",
            sni = "sni.vless.com",
            pbk = "pubkey-123",
            sid = "shortid-123",
            network = "tcp",
            flow = "xtls-rprx-vision"
        )

        val spec = ConfigSpec(
            targetCore = "xray",
            serverNode = profile.toCoreProfile(),
            clientInbound = ClientInboundSpec(
                mode = "mobile_vpn",
                socksPort = 10808,
                authEnabled = true,
                authUsername = "user123",
                authPassword = "tokenXYZ123456789"
            ),
            routing = RoutingSpec(
                rules = listOf(
                    RoutingRule(action = "direct", domains = listOf("geosite:category-ru"), ips = listOf("geoip:ru")),
                    RoutingRule(action = "block", protocols = listOf("bittorrent"))
                )
            )
        )

        val res = SentinelCore.buildConfig(spec)
        assertNotNull("Sentinel-Core buildConfig result cannot be null", res)
        assertEquals("xray", res.targetCore)
        assertTrue("Generated config must not be empty", res.configJson.isNotEmpty())
        assertTrue("Generated config must contain configured SOCKS port", res.configJson.contains("10808"))
        assertTrue("Generated config must contain server address", res.configJson.contains("vless.server.com"))
        assertTrue("Generated config must contain loopback auth token", res.configJson.contains("tokenXYZ123456789"))
    }

    @Test
    fun testHysteria2ConfigCompilationViaSentinelCore() {
        val profile = ServerProfile(
            name = "Hy2Test",
            address = "hy2.server.com",
            port = 443,
            type = "HYSTERIA2",
            uuid = "hy2-password-auth",
            sni = "sni.hy2.com",
            allowInsecure = true
        )

        val spec = ConfigSpec(
            targetCore = "singbox",
            serverNode = profile.toCoreProfile(),
            clientInbound = ClientInboundSpec(
                mode = "mobile_vpn",
                socksPort = 10808
            )
        )

        val res = SentinelCore.buildConfig(spec)
        assertNotNull(res)
        assertEquals("singbox", res.targetCore)
        assertTrue(res.configJson.isNotEmpty())
        assertTrue(res.configJson.contains("hysteria2"))
        assertTrue(res.configJson.contains("hy2.server.com"))
    }

    @Test
    fun testPresetsListViaSentinelCore() {
        val presets = SentinelCore.listPresets()
        assertNotNull(presets)
        assertTrue("Sentinel-Core must provide atomic presets", presets.isNotEmpty())
        val presetIds = presets.map { it.id }
        assertTrue("Presets must include 'ru'", presetIds.contains("ru"))
        assertTrue("Presets must include 'bittorrent'", presetIds.contains("bittorrent"))
        assertTrue("Presets must include 'ads'", presetIds.contains("ads"))
    }

    @Test
    fun testSecureCredentialsGeneration() {
        val creds = XrayConfigManager.generateSecureCredentials()
        assertNotNull(creds)
        assertTrue(creds.port in 1024..65535)
        assertTrue(creds.username.isNotEmpty())
        assertTrue(creds.token.isNotEmpty())

        assertTrue(creds.username.all { it.isLetterOrDigit() })
        assertTrue(creds.token.all { it.isLetterOrDigit() })
        assertTrue(creds.username.length >= 8)
        assertTrue(creds.token.length >= 16)
    }

    @Test
    fun testPortExclusionAndRandomPortGeneration() {
        val testExcludedPort = 36425
        val port = XrayConfigManager.findRandomOpenPort(excludePorts = setOf(testExcludedPort))
        assertTrue("findRandomOpenPort must return a valid port > 0", port > 0)
        assertTrue("findRandomOpenPort must never return an excluded port!", port != testExcludedPort)

        val creds = XrayConfigManager.generateSecureCredentials(excludePorts = setOf(testExcludedPort))
        assertTrue("generateSecureCredentials must produce a non-excluded port", creds.port != testExcludedPort)
    }
}
