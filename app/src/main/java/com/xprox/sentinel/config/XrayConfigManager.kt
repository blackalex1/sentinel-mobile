package com.xprox.sentinel.config

import android.content.Context
import java.util.Base64
import android.util.Log
import java.io.File
import java.security.SecureRandom
import java.util.UUID
import com.xprox.sentinel.config.builder.InboundConfigBuilder
import com.xprox.sentinel.config.builder.OutboundConfigBuilder

/**
 * Handles creation and manipulation of Xray config models.
 * Implements security compilation templates that defend against local SOCKS5 hijacking
 * using random dynamic ports and highly-secure random authorization tokens.
 */
object XrayConfigManager {
    private const val TAG = "XrayConfigManager"
    private const val SECURE_CONFIG_NAME = "secure_xray_config.json"
    data class ServerProfile(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val address: String,
        val port: Int,
        val type: String = "VLESS", // VLESS, VMess, Shadowsocks, Trojan
        val uuid: String = "",
        val path: String = "",
        val security: String = "none",
        val sni: String = "",
        val pbk: String = "",
        val sid: String = "",
        val fp: String = "chrome",
        val network: String = "tcp",
        val flow: String = "",
        val encryption: String = "none",
        val spx: String = "",
        val host: String = "",
        val allowInsecure: Boolean = false,
        val alpn: String = "",
        val headerType: String = "",
        val pinnedPeerCertSha256: String = "",
        val groupId: String? = null,
        val fullJsonConfig: String = ""
    )

    data class LocalProxyCredentials(
        val port: Int,
        val username: String,
        val token: String
    )

    /**
     * Finds a random open port for our local inbound socket proxy.
     */
    fun findRandomOpenPort(): Int {
        return try {
            val socket = java.net.ServerSocket(0)
            val port = socket.localPort
            socket.close()
            port
        } catch (e: Exception) {
            // Fallback to high ephemeral range
            (30000..65000).random()
        }
    }

    /**
     * Generates extremely secure unique local proxy credentials for this session.
     */
    fun generateSecureCredentials(): LocalProxyCredentials {
        val random = SecureRandom()
        val usernameBytes = ByteArray(12)
        val tokenBytes = ByteArray(24)
        
        random.nextBytes(usernameBytes)
        random.nextBytes(tokenBytes)

        val username = Base64.getEncoder().withoutPadding().encodeToString(usernameBytes)
            .filter { it.isLetterOrDigit() }
        val token = Base64.getEncoder().withoutPadding().encodeToString(tokenBytes)
            .filter { it.isLetterOrDigit() }

        return LocalProxyCredentials(
            port = findRandomOpenPort(),
            username = username,
            token = token
        )
    }

    /**
     * Compiles the secure Xray config, embedding loopback binding, custom geosite/geoip rules,
     * and secure local SOCKS5 authentication with our random credentials.
     */
    fun compileSecureConfig(
        context: Context,
        profile: ServerProfile,
        creds: LocalProxyCredentials,
        allowedApps: List<String>,
        blockedApps: List<String>,
        geoipRules: List<String>, // e.g. ["geoip:private", "geoip:ru"]
        geositeRules: List<String>, // e.g. ["geosite:google", "geosite:category-ads-all"]
        lanAuthEnabled: Boolean = false,
        lanCreds: LocalProxyCredentials? = null,
        tetheringIps: List<String> = emptyList(),
        lanHttpPort: Int = 10809,
        lanSocksPort: Int = 10808,
        captureProxyPort: Int = 0
    ): File {
        val configFile = File(context.filesDir, SECURE_CONFIG_NAME)

        // If the profile contains a full raw JSON configuration (from VoxGate/Inki),
        // we run it directly but inject our secure SOCKS5 loopback inbounds for leak protection.
        if (!profile.fullJsonConfig.isNullOrEmpty()) {
            try {
                val jsonObject = org.json.JSONObject(profile.fullJsonConfig)
                val inboundsListJson = InboundConfigBuilder.buildInboundsJson(
                    context, creds, lanAuthEnabled, lanCreds, tetheringIps, lanHttpPort, lanSocksPort
                )
                val secureInboundsArray = org.json.JSONArray(inboundsListJson)
                jsonObject.put("inbounds", secureInboundsArray)
                
                configFile.writeText(jsonObject.toString(), Charsets.UTF_8)
                Log.d(TAG, "Secure JSON Xray config written dynamically with secure inbounds")
                return configFile
            } catch (e: Exception) {
                Log.e(TAG, "Failed to inject secure inbounds into raw JSON config, falling back to compile template", e)
            }
        }
        val inboundsJson = InboundConfigBuilder.buildInboundsJson(
            context, creds, lanAuthEnabled, lanCreds, tetheringIps, lanHttpPort, lanSocksPort
        )
        val inboundsList = InboundConfigBuilder.buildInboundsList(context)
        val dnsServers = XrayProfilePersistence.loadDnsServers(context)
        val dnsServersJson = dnsServers.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }

        // Compile dynamic blackhole rules for blocked applications
        val blockedDests = com.xprox.sentinel.service.ThreatDetectionManager.getBlockedDestinations()
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
            """.trimIndent().prependIndent("              ")
        } else ""

        // Compile dynamic port block rules for breached ports as a comma-separated string (Xray routing expects port to be a string or number, not an array)
        val blockedPortsList = com.xprox.sentinel.service.ThreatDetectionManager.getBlockedPorts()
        val blockedPortsRuleJson = if (blockedPortsList.isNotEmpty()) {
            val portsJson = blockedPortsList.joinToString(separator = ",") { "$it" }
            """
            {
              "type": "field",
              "port": "$portsJson",
              "outboundTag": "block"
            },
            """.trimIndent().prependIndent("              ")
        } else ""

        // Compile dynamic capture proxy outbounds and rules for all active capturing/isolated packages
        val captureRules = java.lang.StringBuilder()
        val captureOutbounds = java.lang.StringBuilder()
        val pm = context.packageManager
        val currentTime = System.currentTimeMillis()

        // Resolve system user IDs (UIDs) of all blocked applications for Zero-Trust kernel-level socket blocking
        val allBlockedApps = (blockedApps + com.xprox.sentinel.service.ThreatDetectionManager.getBlockedAppsList()).distinct()
        val blockedUids = mutableListOf<String>()
        for (pkg in allBlockedApps) {
            try {
                if (pkg.isNotEmpty() && 
                    pkg != "android.system.kernel" && 
                    pkg != "hotspot.client" &&
                    !pkg.startsWith("android.system.") && 
                    !pkg.startsWith("android.uid.") && 
                    !pkg.startsWith("unknown.uid.")
                ) {
                    val triggerTime = com.xprox.sentinel.service.ThreatDetectionManager.getTriggerTime(pkg)
                    val isCapturing = captureProxyPort > 0 && triggerTime != null && (currentTime - triggerTime <= 300000L)
                    
                    if (isCapturing) {
                        val uid = pm.getPackageUid(pkg, 0)
                        val tag = "capture-outbound-$pkg"
                        
                        // Add routing rule
                        captureRules.append("""
                        {
                          "type": "field",
                          "user": ["$uid"],
                          "outboundTag": "$tag"
                        },
                        """.trimIndent().prependIndent("              ")).append("\n")
                        
                        // Add SOCKS outbound
                        captureOutbounds.append("""
                        ,
                        {
                          "tag": "$tag",
                          "protocol": "socks",
                          "settings": {
                            "servers": [
                              {
                                "address": "127.0.0.1",
                                "port": $captureProxyPort,
                                "users": [
                                  {
                                    "user": "$pkg",
                                    "pass": "socks_token"
                                  }
                                ]
                              }
                            ]
                          }
                        }
                        """.trimIndent().prependIndent("            "))
                    } else {
                        val uid = pm.getPackageUid(pkg, 0)
                        blockedUids.add(uid.toString())
                    }
                }
            } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                // Ignore virtual packages or uninstalled apps gracefully
                Log.d(TAG, "Blocked app not installed or virtual package: $pkg")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error getting UID for blocked app $pkg", e)
            }
        }

        val blockedUidsRuleJson = if (blockedUids.isNotEmpty()) {
            val uidsJson = blockedUids.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
            """
            {
              "type": "field",
              "user": $uidsJson,
              "outboundTag": "block"
            },
            """.trimIndent().prependIndent("              ")
        } else ""

        val isDirectMode = profile.type.uppercase() == "DIRECT"
        val streamSettingsJson = if (isDirectMode) "{}" else OutboundConfigBuilder.buildStreamSettingsJson(profile)
        val settingsJson = if (isDirectMode) "{}" else OutboundConfigBuilder.buildSettingsJson(profile)
        val outboundProtocol = when {
            isDirectMode -> "freedom"
            profile.type.uppercase() == "HYSTERIA2" -> "hysteria"
            else -> profile.type.lowercase()
        }

        // Compile the template
        val json = """
        {
          "log": {
            "loglevel": "info"
          },
          "dns": {
            "servers": $dnsServersJson
          },
          "inbounds": $inboundsJson,
          "outbounds": [
            {
              "tag": "proxy",
              "protocol": "$outboundProtocol",
              "settings": $settingsJson,
              "streamSettings": $streamSettingsJson
            },
            {
              "tag": "direct",
              "protocol": "freedom",
              "settings": {}
            },
            {
              "tag": "block",
              "protocol": "blackhole",
              "settings": {
                "response": {
                  "type": "http"
                }
              }
            },
            {
              "tag": "dns-out",
              "protocol": "dns",
              "settings": {}
            }$captureOutbounds
          ],
          "routing": {
            "domainStrategy": "IPIfNonMatch",
            "rules": [
$captureRules$blockedRuleJson
$blockedPortsRuleJson
$blockedUidsRuleJson
              {
                "type": "field",
                "inboundTag": $inboundsList,
                "port": 53,
                "outboundTag": "dns-out"
              },
              {
                "type": "field",
                "port": 443,
                "network": "udp",
                "outboundTag": "block"
              },
              {
                "type": "field",
                "inboundTag": ["tun-in"],
                "port": 853,
                "outboundTag": "block"
              },
              {
                "type": "field",
                "ip": ${geoipRules.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
                "outboundTag": "direct"
              },
              {
                "type": "field",
                "domain": ${geositeRules.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
                "outboundTag": "proxy"
              }
            ]
          }
        }
        """.trimIndent()

        configFile.writeText(json, Charsets.UTF_8)
        Log.d(TAG, "Secure Xray config compiled to ${configFile.absolutePath} on Port ${creds.port} with secure credentials")
        return configFile
    }
}
