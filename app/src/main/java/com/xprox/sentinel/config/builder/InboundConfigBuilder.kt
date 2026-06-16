package com.xprox.sentinel.config.builder

import android.content.Context
import com.xprox.sentinel.config.XrayConfigManager.LocalProxyCredentials
import com.xprox.sentinel.config.XrayProfilePersistence

object InboundConfigBuilder {
    fun buildInboundsJson(
        context: Context,
        creds: LocalProxyCredentials,
        lanAuthEnabled: Boolean,
        lanCreds: LocalProxyCredentials?,
        tetheringIps: List<String>,
        lanHttpPort: Int,
        lanSocksPort: Int
    ): String {
        val isLanSharingEnabled = XrayProfilePersistence.loadLanSharing(context)
        val lanHttpEnabled = XrayProfilePersistence.loadLanSharingHttp(context)
        val lanSocksEnabled = XrayProfilePersistence.loadLanSharingSocks(context)

        return buildString {
            append("[\n")
            append("            {\n")
            append("              \"tag\": \"socks-in\",\n")
            append("              \"port\": ${creds.port},\n")
            append("              \"listen\": \"127.0.0.1\",\n")
            append("              \"protocol\": \"socks\",\n")
            append("              \"settings\": {\n")
            append("                \"auth\": \"password\",\n")
            append("                \"accounts\": [\n")
            append("                  {\n")
            append("                    \"user\": \"${creds.username}\",\n")
            append("                    \"pass\": \"${creds.token}\"\n")
            append("                  }\n")
            append("                ],\n")
            append("                \"udp\": false\n")
            append("              }\n")
            append("            },\n")
            append("            {\n")
            append("              \"tag\": \"tun-in\",\n")
            append("              \"protocol\": \"tun\",\n")
            append("              \"settings\": {\n")
            append("                \"name\": \"tun0\",\n")
            append("                \"mtu\": 1500,\n")
            append("                \"gateway\": [\"10.0.0.1/24\", \"fd00::1/64\"]\n")
            append("              },\n")
            append("              \"sniffing\": {\n")
            append("                \"enabled\": true,\n")
            append("                \"destOverride\": [\"http\", \"tls\", \"quic\"]\n")
            append("              }\n")
            append("            }")

            if (isLanSharingEnabled && (lanHttpEnabled || lanSocksEnabled)) {
                val ipsToBind = tetheringIps.filter { it.isNotEmpty() }
                val lanInbounds = mutableListOf<String>()
                val bindIps = if (ipsToBind.isNotEmpty()) ipsToBind else listOf("0.0.0.0")
                
                for (ip in bindIps) {
                    if (lanHttpEnabled) {
                        val httpAuthJson = if (lanAuthEnabled && lanCreds != null) {
                            ",\n                \"accounts\": [\n                  {\n                    \"user\": \"${lanCreds.username}\",\n                    \"pass\": \"${lanCreds.token}\"\n                  }\n                ]"
                        } else ""
                        
                        lanInbounds.add("""
                        {
                          "tag": "lan-http-in",
                          "port": ${lanHttpPort},
                          "listen": "$ip",
                          "protocol": "http",
                          "settings": {
                            "allowTransparent": false$httpAuthJson
                          }
                        }
                        """.trimIndent())
                    }
                    
                    if (lanSocksEnabled) {
                        val socksAuthJson = if (lanAuthEnabled && lanCreds != null) {
                            "\"auth\": \"password\",\n    \"accounts\": [\n      {\n        \"user\": \"${lanCreds.username}\",\n        \"pass\": \"${lanCreds.token}\"\n      }\n    ],"
                        } else {
                            "\"auth\": \"noauth\","
                        }
                        
                        lanInbounds.add("""
                        {
                          "tag": "lan-socks-in",
                          "port": ${lanSocksPort},
                          "listen": "$ip",
                          "protocol": "socks",
                          "settings": {
                            $socksAuthJson
                            "udp": true
                          }
                        }
                        """.trimIndent())
                    }
                }
                
                if (lanInbounds.isNotEmpty()) {
                    append(",\n")
                    append(lanInbounds.joinToString(separator = ",\n") { it.prependIndent("            ") })
                }
            }
            append("\n          ]")
        }
    }

    fun buildInboundsList(
        context: Context
    ): String {
        val isLanSharingEnabled = XrayProfilePersistence.loadLanSharing(context)
        val lanHttpEnabled = XrayProfilePersistence.loadLanSharingHttp(context)
        val lanSocksEnabled = XrayProfilePersistence.loadLanSharingSocks(context)

        return buildString {
            append("[\"tun-in\", \"socks-in\"")
            if (isLanSharingEnabled) {
                if (lanHttpEnabled) append(", \"lan-http-in\"")
                if (lanSocksEnabled) append(", \"lan-socks-in\"")
            }
            append("]")
        }
    }
}
