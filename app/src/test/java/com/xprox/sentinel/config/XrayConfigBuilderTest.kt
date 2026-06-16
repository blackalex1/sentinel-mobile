package com.xprox.sentinel.config

import com.xprox.sentinel.config.XrayConfigManager.ServerProfile
import com.xprox.sentinel.config.builder.OutboundConfigBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XrayConfigBuilderTest {

    @Test
    fun testVlessOutboundCompilation() {
        val profile = ServerProfile(
            name = "VlessTCP",
            address = "vless.server.com",
            port = 443,
            type = "VLESS",
            uuid = "uuid-test-123",
            security = "tls",
            sni = "sni.vless.com",
            network = "tcp"
        )

        val settingsJson = OutboundConfigBuilder.buildSettingsJson(profile)
        val streamSettingsJson = OutboundConfigBuilder.buildStreamSettingsJson(profile)

        // Verify settings
        assertTrue(settingsJson.contains("vnext"))
        assertTrue(settingsJson.contains("vless.server.com"))
        assertTrue(settingsJson.contains("uuid-test-123"))

        // Verify streamSettings
        assertTrue(streamSettingsJson.contains("\"network\": \"tcp\""))
        assertTrue(streamSettingsJson.contains("\"security\": \"tls\""))
        assertTrue(streamSettingsJson.contains("\"serverName\": \"sni.vless.com\""))
    }

    @Test
    fun testVMessOutboundCompilation() {
        val profile = ServerProfile(
            name = "VmessWS",
            address = "vmess.server.com",
            port = 443,
            type = "VMESS",
            uuid = "uuid-test-123",
            security = "none",
            network = "ws",
            path = "/vmess-ws",
            host = "ws-host.com"
        )

        val settingsJson = OutboundConfigBuilder.buildSettingsJson(profile)
        val streamSettingsJson = OutboundConfigBuilder.buildStreamSettingsJson(profile)

        // Verify settings
        assertTrue(settingsJson.contains("vnext"))
        assertTrue(settingsJson.contains("vmess.server.com"))

        // Verify streamSettings
        assertTrue(streamSettingsJson.contains("\"network\": \"ws\""))
        assertTrue(streamSettingsJson.contains("\"path\": \"/vmess-ws\""))
        assertTrue(streamSettingsJson.contains("\"Host\": \"ws-host.com\""))
    }

    @Test
    fun testTrojanOutboundCompilation() {
        val profile = ServerProfile(
            name = "TrojanTest",
            address = "trojan.server.com",
            port = 443,
            type = "TROJAN",
            uuid = "password-123",
            security = "tls"
        )

        val settingsJson = OutboundConfigBuilder.buildSettingsJson(profile)
        assertTrue(settingsJson.contains("servers"))
        assertTrue(settingsJson.contains("password-123"))
        assertTrue(settingsJson.contains("trojan.server.com"))
    }

    @Test
    fun testShadowsocksOutboundCompilation() {
        val profile = ServerProfile(
            name = "SSTest",
            address = "ss.server.com",
            port = 1234,
            type = "SHADOWSOCKS",
            uuid = "ss-password-123",
            path = "chacha20-ietf-poly1305" // Cipher stored in path
        )

        val settingsJson = OutboundConfigBuilder.buildSettingsJson(profile)
        assertTrue(settingsJson.contains("servers"))
        assertTrue(settingsJson.contains("ss.server.com"))
        assertTrue(settingsJson.contains("ss-password-123"))
        assertTrue(settingsJson.contains("\"method\": \"chacha20-ietf-poly1305\""))
    }

    @Test
    fun testHysteria2OutboundCompilation() {
        val profile = ServerProfile(
            name = "Hy2Test",
            address = "hy2.server.com",
            port = 443,
            type = "HYSTERIA2",
            uuid = "hy2-password-auth",
            sni = "sni.hy2.com",
            allowInsecure = true,
            pinnedPeerCertSha256 = "636cebbab1918299403de72b037ab2745338f4c4cc8aba6fa06ed2f5740b1711"
        )

        val settingsJson = OutboundConfigBuilder.buildSettingsJson(profile)
        val streamSettingsJson = OutboundConfigBuilder.buildStreamSettingsJson(profile)

        // Verify version 2 settings
        assertTrue(settingsJson.contains("\"version\": 2"))
        assertTrue(settingsJson.contains("hy2.server.com"))

        // Verify hysteria settings
        assertTrue(streamSettingsJson.contains("\"network\": \"hysteria\""))
        assertTrue(streamSettingsJson.contains("\"security\": \"tls\""))
        assertTrue(streamSettingsJson.contains("\"serverName\": \"sni.hy2.com\""))
        assertTrue(streamSettingsJson.contains("\"version\": 2"))
        assertTrue(streamSettingsJson.contains("\"auth\": \"hy2-password-auth\""))
        assertTrue(streamSettingsJson.contains("\"alpn\": [\"h3\"]"))
        assertTrue(streamSettingsJson.contains("\"allowInsecure\": true"))
        assertTrue(streamSettingsJson.contains("\"pinnedPeerCertSha256\": \"636cebbab1918299403de72b037ab2745338f4c4cc8aba6fa06ed2f5740b1711\""))
    }

    @Test
    fun testSocksOutboundCompilation() {
        val profile = ServerProfile(
            name = "SocksTest",
            address = "socks.server.com",
            port = 1080,
            type = "SOCKS",
            uuid = "socks-user",
            path = "socks-pass"
        )

        val settingsJson = OutboundConfigBuilder.buildSettingsJson(profile)
        assertTrue(settingsJson.contains("socks.server.com"))
        assertTrue(settingsJson.contains("1080"))
        assertTrue(settingsJson.contains("socks-user"))
        assertTrue(settingsJson.contains("socks-pass"))
    }

    @Test
    fun testSecureCredentialsGeneration() {
        val creds = XrayConfigManager.generateSecureCredentials()
        assertNotNull(creds)
        assertTrue(creds.port in 1024..65535)
        assertTrue(creds.username.isNotEmpty())
        assertTrue(creds.token.isNotEmpty())
        
        // Ensure credentials are alphanumeric and of strong length
        assertTrue(creds.username.all { it.isLetterOrDigit() })
        assertTrue(creds.token.all { it.isLetterOrDigit() })
        assertTrue(creds.username.length >= 8)
        assertTrue(creds.token.length >= 16)
    }
}
