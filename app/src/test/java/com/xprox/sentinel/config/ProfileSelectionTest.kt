package com.xprox.sentinel.config

import com.xprox.sentinel.core.toCoreProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ProfileSelectionTest {

    @Test
    fun testResolveActiveProfilePrioritizesProxyOverDirectWhenActiveIdNull() {
        val directProfile = XrayConfigManager.ServerProfile(
            id = "direct-id-1",
            name = "Анализ трафика",
            address = "",
            port = 0,
            type = "DIRECT",
            uuid = "",
            security = "none"
        )

        val subscriptionVlessProfile = XrayConfigManager.ServerProfile(
            id = "sub-vless-id-2",
            name = "Subscription Server 1",
            address = "proxy.example.com",
            port = 443,
            type = "VLESS",
            uuid = "uuid-12345",
            security = "reality"
        )

        val profiles = listOf(directProfile, subscriptionVlessProfile)

        // Clear active ID to simulate freshly started session or unmapped ID
        val resolved = XrayProfilePersistence.resolveActiveProfileFromList(profiles, activeId = null)
        assertNotNull(resolved)
        // Must prioritize subscription VLESS profile instead of falling back to DIRECT
        assertEquals("sub-vless-id-2", resolved?.id)
        assertEquals("Subscription Server 1", resolved?.name)
    }

    @Test
    fun testResolveActiveProfileRespectsValidActiveId() {
        val directProfile = XrayConfigManager.ServerProfile(
            id = "direct-id-1",
            name = "Анализ трафика",
            address = "",
            port = 0,
            type = "DIRECT",
            uuid = "",
            security = "none"
        )

        val subscriptionVlessProfile = XrayConfigManager.ServerProfile(
            id = "sub-vless-id-2",
            name = "Subscription Server 1",
            address = "proxy.example.com",
            port = 443,
            type = "VLESS",
            uuid = "uuid-12345",
            security = "reality"
        )

        val profiles = listOf(directProfile, subscriptionVlessProfile)

        val resolved = XrayProfilePersistence.resolveActiveProfileFromList(profiles, activeId = "sub-vless-id-2")
        assertNotNull(resolved)
        assertEquals("sub-vless-id-2", resolved?.id)
    }

    @Test
    fun testDirectTrafficAnalysisConfigCompilation() {
        val directProfile = XrayConfigManager.ServerProfile(
            id = "direct-profile-id",
            name = "Анализ трафика",
            address = "",
            port = 0,
            type = "DIRECT",
            uuid = "",
            security = "none"
        )

        val spec = com.xprox.sentinel.core.models.ConfigSpec(
            targetCore = "xray",
            serverNode = directProfile.toCoreProfile(),
            clientInbound = com.xprox.sentinel.core.models.ClientInboundSpec(
                mode = "mobile_vpn",
                socksPort = 10808,
                httpPort = 10809
            ),
            routing = com.xprox.sentinel.core.models.RoutingSpec(
                defaultAction = "direct",
                rules = listOf(
                    com.xprox.sentinel.core.models.RoutingRule(action = "block", domains = listOf("geosite:category-ads-all")),
                    com.xprox.sentinel.core.models.RoutingRule(action = "direct", ips = listOf("geoip:private"))
                )
            )
        )

        val res = com.xprox.sentinel.core.SentinelCore.buildConfig(spec)
        assertNotNull("Build result must not be null", res)
        org.junit.Assert.assertTrue("Config JSON must be generated", res.configJson.isNotEmpty())
        org.junit.Assert.assertTrue(
            "Xray config must contain freedom or direct outbound",
            res.configJson.contains("\"freedom\"") || res.configJson.contains("\"direct\"")
        )
    }
}
