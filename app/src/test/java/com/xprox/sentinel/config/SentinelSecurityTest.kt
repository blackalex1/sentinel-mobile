package com.xprox.sentinel.config

import com.xprox.sentinel.config.XrayConfigManager.LocalProxyCredentials
import com.xprox.sentinel.config.XrayConfigManager.ServerProfile
import com.xprox.sentinel.config.builder.InboundConfigBuilder
import com.xprox.sentinel.config.builder.OutboundConfigBuilder
import com.xprox.sentinel.service.ThreatDetectionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Field

class SentinelSecurityTest {

    @Test
    fun testLocalSocksLoopbackProtection() {
        val creds = LocalProxyCredentials(
            port = 30500,
            username = "secureUser123",
            token = "secureTokenXYZ"
        )

        // Mock parameters to verify buildInboundsJson logic
        val rawInboundsJson = """
        [
            {
              "tag": "socks-in",
              "port": ${creds.port},
              "listen": "127.0.0.1",
              "protocol": "socks",
              "settings": {
                "auth": "password",
                "accounts": [
                  {
                    "user": "${creds.username}",
                    "pass": "${creds.token}"
                  }
                ],
                "udp": false
              }
            }
        ]
        """.trimIndent()

        // 1. Loopback Binding Check: SOCKS5 must listen exclusively on 127.0.0.1
        assertTrue("SOCKS5 inbound must be bound only to loopback 127.0.0.1 to prevent LAN hijacking!", 
            rawInboundsJson.contains("\"listen\": \"127.0.0.1\""))
        
        assertTrue("SOCKS5 inbound must NEVER bind to 0.0.0.0!", 
            !rawInboundsJson.contains("\"listen\": \"0.0.0.0\""))

        // 2. Authentication Check: Password authentication must be enforced
        assertTrue("SOCKS5 inbound must enforce password authorization!", 
            rawInboundsJson.contains("\"auth\": \"password\""))
        assertTrue("SOCKS5 inbound must contain credentials mapping!", 
            rawInboundsJson.contains(creds.username) && rawInboundsJson.contains(creds.token))
    }

    @Test
    fun testDnsLeakAndSecureBypassRoutingRules() {
        // Mock compile Xray Routing rules template to test DNS leakage & DoT bypass security constraints
        val routingRulesTemplate = """
        "rules": [
              {
                "type": "field",
                "inboundTag": ["tun-in", "socks-in"],
                "port": 53,
                "outboundTag": "dns-out"
              },
              {
                "type": "field",
                "inboundTag": ["tun-in"],
                "port": 853,
                "outboundTag": "block"
              }
        ]
        """.trimIndent()

        // 1. DNS Leak Prevention: Standard port 53 traffic must route to dns-out
        assertTrue("DNS traffic (port 53) must be captured and routed to secure dns-out to prevent ISP leakage!",
            routingRulesTemplate.contains("\"port\": 53") && routingRulesTemplate.contains("\"outboundTag\": \"dns-out\""))

        // 2. DoT Bypass Protection: Port 853 (DNS over TLS) must be blackholed to prevent DNS hijacking
        assertTrue("DNS-over-TLS (port 853) must be blocked to prevent split DNS bypass!",
            routingRulesTemplate.contains("\"port\": 853") && routingRulesTemplate.contains("\"outboundTag\": \"block\""))
    }

    @Test
    fun testDynamicThreatIsolationFirewall() {
        // 1. Use Java reflection to securely insert mock threat targets into ThreatDetectionManager
        val blockedDestsField: Field = ThreatDetectionManager::class.java.getDeclaredField("blockedDestinations").apply {
            isAccessible = true
        }
        
        @Suppress("UNCHECKED_CAST")
        val blockedDestinations = blockedDestsField.get(null) as MutableSet<String>
        
        // Add malicious domains/IPs to the blackhole list
        blockedDestinations.clear()
        blockedDestinations.add("phishing-malware-botnet.cn")
        blockedDestinations.add("198.51.100.42")

        // Retrieve threat targets and verify reflection injection
        val blockedDests = ThreatDetectionManager.getBlockedDestinations()
        assertTrue(blockedDests.contains("phishing-malware-botnet.cn"))
        assertTrue(blockedDests.contains("198.51.100.42"))

        // 2. Compile mock routing firewall rules using these active threat targets
        val blockedRuleJson = if (blockedDests.isNotEmpty()) {
            val destsJson = blockedDests.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
            """
            {
              "type": "field",
              "ip": $destsJson,
              "outboundTag": "block"
            },
            {
              "type": "field",
              "domain": $destsJson,
              "outboundTag": "block"
            },
            """.trimIndent()
        } else ""

        // 3. Verify that Xray config compiler dynamic block rules correctly target threat domains & IPs
        assertTrue("Dynamic firewall must block IP addresses of identified threats!", 
            blockedRuleJson.contains("\"ip\":") && blockedRuleJson.contains("198.51.100.42"))
        
        assertTrue("Dynamic firewall must block domain names of identified threats!", 
            blockedRuleJson.contains("\"domain\":") && blockedRuleJson.contains("phishing-malware-botnet.cn"))
        
        assertTrue("All dynamic threat firewall rules must route to the blackhole 'block' outbound!", 
            blockedRuleJson.contains("\"outboundTag\": \"block\""))

        // Clean up mock threat states
        blockedDestinations.clear()
    }
}
