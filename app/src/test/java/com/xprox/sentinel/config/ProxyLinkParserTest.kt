package com.xprox.sentinel.config

import com.xprox.sentinel.config.parser.ProxyLinkParser
import com.xprox.sentinel.core.SentinelCore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyLinkParserTest {

    @Test
    fun testCoreEngineIsActive() {
        assertTrue("Native Sentinel-Core engine must be loaded and available during tests!", SentinelCore.isAvailable())
    }

    @Test
    fun testVlessParsing() {
        val link = "vless://uuid-123@domain.com:443?security=tls&sni=sni.com#TestVless"
        val profile = ProxyLinkParser.parse(link)
        assertNotNull("Sentinel-Core must successfully parse VLESS URI", profile)
        assertEquals("TestVless", profile!!.name)
        assertEquals("domain.com", profile.address)
        assertEquals(443, profile.port)
        assertEquals("uuid-123", profile.uuid)
        assertEquals("tls", profile.security)
        assertEquals("VLESS", profile.type)

        val exported = ProxyLinkParser.export(profile)
        assertTrue(exported.contains("vless://"))
        assertTrue(exported.contains("domain.com:443"))
    }

    @Test
    fun testVmessParsing() {
        val link = "vmess://eyJhZGQiOiJzZXJ2ZXIuY29tIiwicG9ydCI6NDQzLCJpZCI6InV1aWQtMTIzIiwibmV0Ijoid3MiLCJ0bHMiOiJ0bHMiLCJwYXRoIjoiL3dzIiwicHMiOiJUZXN0Vm1lc3MifQ=="
        val profile = ProxyLinkParser.parse(link)
        assertNotNull("Sentinel-Core must successfully parse VMess URI", profile)
        assertEquals("TestVmess", profile!!.name)
        assertEquals("server.com", profile.address)
        assertEquals(443, profile.port)
        assertEquals("uuid-123", profile.uuid)
        assertEquals("ws", profile.network)
        assertEquals("tls", profile.security)
        assertEquals("VMESS", profile.type)

        val exported = ProxyLinkParser.export(profile)
        assertTrue(exported.startsWith("vmess://"))
    }

    @Test
    fun testTrojanParsing() {
        val link = "trojan://pass-123@trojan-server.com:443?security=tls&sni=sni.com#TestTrojan"
        val profile = ProxyLinkParser.parse(link)
        assertNotNull("Sentinel-Core must successfully parse Trojan URI", profile)
        assertEquals("TestTrojan", profile!!.name)
        assertEquals("trojan-server.com", profile.address)
        assertEquals(443, profile.port)
        assertEquals("pass-123", profile.uuid)
        assertEquals("tls", profile.security)
        assertEquals("TROJAN", profile.type)

        val exported = ProxyLinkParser.export(profile)
        assertTrue(exported.contains("trojan://"))
        assertTrue(exported.contains("trojan-server.com:443"))
    }

    @Test
    fun testShadowsocksParsing() {
        val link = "ss://Y2hhY2hhMjAtaWV0Zi1wb2x5MTMwNTpkdW1teS1wYXNzd29yZA@127.0.0.1:8388?type=tcp#browser"
        val profile = ProxyLinkParser.parse(link)
        assertNotNull("Sentinel-Core must successfully parse Shadowsocks URI", profile)
        assertEquals("browser", profile!!.name)
        assertEquals("127.0.0.1", profile.address)
        assertEquals(8388, profile.port)
        assertEquals("dummy-password", profile.uuid)
        assertEquals("SHADOWSOCKS", profile.type)

        val exported = ProxyLinkParser.export(profile)
        assertTrue(exported.startsWith("ss://"))
        assertTrue(exported.contains("127.0.0.1:8388"))
    }

    @Test
    fun testHysteria2Parsing() {
        val link = "hysteria2://auth-token-123@hy2-server.com:443?sni=sni.com&insecure=1&pinSHA256=636cebbab1918299403de72b037ab2745338f4c4cc8aba6fa06ed2f5740b1711#TestHy2"
        val profile = ProxyLinkParser.parse(link)
        assertNotNull("Sentinel-Core must successfully parse Hysteria 2 URI", profile)
        assertEquals("TestHy2", profile!!.name)
        assertEquals("hy2-server.com", profile.address)
        assertEquals(443, profile.port)
        assertEquals("auth-token-123", profile.uuid)
        assertEquals(true, profile.allowInsecure)
        assertEquals("636cebbab1918299403de72b037ab2745338f4c4cc8aba6fa06ed2f5740b1711", profile.pinnedPeerCertSha256)
        assertEquals("HYSTERIA2", profile.type)

        val exported = ProxyLinkParser.export(profile)
        assertTrue(exported.contains("hy2://") || exported.contains("hysteria2://"))
        assertTrue(exported.contains("auth-token-123@hy2-server.com:443"))
    }

    @Test
    fun testSocks5Parsing() {
        val link = "socks5://user:pass@socks-server.com:1080#TestSocks"
        val profile = ProxyLinkParser.parse(link)
        assertNotNull("Sentinel-Core must successfully parse SOCKS5 URI", profile)
        assertEquals("TestSocks", profile!!.name)
        assertEquals("socks-server.com", profile.address)
        assertEquals(1080, profile.port)
        assertEquals("user", profile.uuid)
        assertEquals("SOCKS", profile.type)

        val exported = ProxyLinkParser.export(profile)
        assertTrue(exported.contains("socks5://") || exported.contains("socks://"))
        assertTrue(exported.contains("socks-server.com:1080"))
    }

    @Test
    fun testIPv6VlessParsing() {
        val link = "vless://00000000-0000-0000-0000-000000000042@[2001:db8:1000::1]:443?security=reality&sni=node.example.test#TestIPv6Vless"
        val profile = ProxyLinkParser.parse(link)
        assertNotNull("Sentinel-Core must successfully parse IPv6 VLESS URI", profile)
        assertEquals("TestIPv6Vless", profile!!.name)
        assertEquals("2001:db8:1000::1", profile.address)
        assertEquals(443, profile.port)
        assertEquals("00000000-0000-0000-0000-000000000042", profile.uuid)

        val exported = ProxyLinkParser.export(profile)
        assertTrue("Exported URI must retain bracketed IPv6", exported.contains("@[2001:db8:1000::1]:443"))
    }

    @Test
    fun testIPv6Hysteria2Parsing() {
        val link = "hysteria2://mock_pass@[2001:db8:cafe::42]:8443?sni=node.example.test#TestIPv6Hy2"
        val profile = ProxyLinkParser.parse(link)
        assertNotNull("Sentinel-Core must successfully parse IPv6 Hysteria2 URI", profile)
        assertEquals("TestIPv6Hy2", profile!!.name)
        assertEquals("2001:db8:cafe::42", profile.address)
        assertEquals(8443, profile.port)

        val exported = ProxyLinkParser.export(profile)
        assertTrue("Exported Hysteria 2 must retain bracketed IPv6", exported.contains("@[2001:db8:cafe::42]:8443"))
    }

    @Test
    fun testIPv6TrojanParsing() {
        val link = "trojan://mock_pass@[2001:db8:beef::99]:443?security=tls&sni=node.example.test#TestIPv6Trojan"
        val profile = ProxyLinkParser.parse(link)
        assertNotNull("Sentinel-Core must successfully parse IPv6 Trojan URI", profile)
        assertEquals("TestIPv6Trojan", profile!!.name)
        assertEquals("2001:db8:beef::99", profile.address)
        assertEquals(443, profile.port)

        val exported = ProxyLinkParser.export(profile)
        assertTrue("Exported Trojan must retain bracketed IPv6", exported.contains("@[2001:db8:beef::99]:443"))
    }

    @Test
    fun testIPv6ShadowsocksParsing() {
        val link = "ss://YWVzLTI1Ni1nY206cGFzc3dvcmQ=@[fd00::1]:8388#TestIPv6SS"
        val profile = ProxyLinkParser.parse(link)
        assertNotNull("Sentinel-Core must successfully parse IPv6 Shadowsocks URI", profile)
        assertEquals("TestIPv6SS", profile!!.name)
        assertEquals("fd00::1", profile.address)
        assertEquals(8388, profile.port)

        val exported = ProxyLinkParser.export(profile)
        assertTrue("Exported Shadowsocks must retain bracketed IPv6", exported.contains("@[fd00::1]:8388"))
    }

    @Test
    fun testIPv6WireGuardParsing() {
        val link = "wireguard://aGVsbG9fd2lyZWd1YXJkX3ByaXZhdGVfa2V5XzEyMw==@[2001:db8:3000::8]:51820?publickey=pubkey&ip=fd00::2/64#TestIPv6WG"
        val profile = ProxyLinkParser.parse(link)
        assertNotNull("Sentinel-Core must successfully parse IPv6 WireGuard URI", profile)
        assertEquals("TestIPv6WG", profile!!.name)
        assertEquals("2001:db8:3000::8", profile.address)
        assertEquals(51820, profile.port)

        val exported = ProxyLinkParser.export(profile)
        assertTrue("Exported WireGuard must retain bracketed IPv6", exported.contains("@[2001:db8:3000::8]:51820"))
    }

    @Test
    fun testIPv6UnbracketedNormalization() {
        val link = "vless://00000000-0000-0000-0000-000000000042@2001:db8:1000::1:443?security=reality&sni=node.example.test#TestUnbracketed"
        val profile = ProxyLinkParser.parse(link)
        assertNotNull("Sentinel-Core must successfully normalize unbracketed IPv6 URI", profile)
        assertEquals("2001:db8:1000::1", profile!!.address)
        assertEquals(443, profile.port)

        val exported = ProxyLinkParser.export(profile)
        assertTrue("Exported URI must be formatted with RFC 3986 brackets", exported.contains("@[2001:db8:1000::1]:443"))
    }
}
