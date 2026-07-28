package com.xprox.sentinel.config

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
}
