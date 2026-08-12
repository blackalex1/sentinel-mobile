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
    fun findRandomOpenPort(excludePorts: Set<Int> = emptySet()): Int {
        for (attempt in 1..20) {
            try {
                val socket = java.net.ServerSocket()
                socket.reuseAddress = true
                socket.bind(java.net.InetSocketAddress("127.0.0.1", 0))
                val port = socket.localPort
                socket.close()
                if (port > 0 && !excludePorts.contains(port)) {
                    return port
                }
            } catch (e: Exception) {
                // Ignore and retry
            }
        }
        // Fallback to high ephemeral range excluding any forbidden ports
        var fallback = (30000..65000).random()
        while (excludePorts.contains(fallback)) {
            fallback = (30000..65000).random()
        }
        return fallback
    }

    /**
     * Generates extremely secure unique local proxy credentials for this session.
     */
    fun generateSecureCredentials(excludePorts: Set<Int> = emptySet()): LocalProxyCredentials {
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
            port = findRandomOpenPort(excludePorts),
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

        // Smart Routing Configurations
        val isBypassRu = XrayProfilePersistence.loadBypassRuSites(context)
        val isBypassTorrents = XrayProfilePersistence.loadBypassTorrents(context)
        val isBlockQuic = XrayProfilePersistence.loadBlockQuic(context)
        val isBypassLan = XrayProfilePersistence.loadBypassLan(context)

        val customDirect = XrayProfilePersistence.loadCustomDirectRules(context)
        val customProxy = XrayProfilePersistence.loadCustomProxyRules(context)
        val customBlock = XrayProfilePersistence.loadCustomBlockRules(context)

        fun isIpRule(rule: String): Boolean {
            val t = rule.trim().lowercase()
            return t.startsWith("geoip:") || t.contains("/") || t.matches(Regex("^[0-9.]+$")) || t.matches(Regex("^[0-9a-f:]+$"))
        }

        val ruDomainsList = listOf(
            "geosite:category-gov-ru",
            "geosite:category-bank-ru",
            "geosite:category-ecommerce-ru",
            "geosite:category-media-ru",
            "geosite:category-retail-ru",
            "geosite:yandex",
            "geosite:vk",
            "geosite:category-ru",
            "telega.me", "2gis.com", "2gis.ru",
            "47news.ru", "alfabank.ru", "auth-nsdi.ru", "auto.ru", "avito.ru", "avito.st", "cdn-vk.ru",
            "cikrf.ru", "dzen.ru", "gazeta.ru", "gosuslugi.ru", "gov.ru", "government.ru", "gu-st.ru",
            "izbirkom.ru", "kinopoisk.ru", "kp.ru", "kremlin.ru", "lemanapro.ru", "lenta.ru", "lmru.tech",
            "mail.ru", "max.ru", "mradx.net", "ok.ru", "okcdn.ru", "oneme.ru", "ozon.ru", "ozone.ru",
            "pochta.ru", "rambler.ru", "rbc.ru", "res-nsdi.ru", "rutube.ru", "rutubelist.ru", "rzd.ru",
            "t2.ru", "taximaxim.ru", "tutu.ru", "userapi.com", "vk-portal.net", "vk.com", "vk.ru",
            "vtb.ru", "wb.ru", "wildberries.ru", "ya.ru", "yandex.com", "yandex.net", "yandex.ru",
            "yastatic.net", "mos.ru", "tbank.ru", "cdn-tinkoff.ru", "tinkoff.ru", "nalog.ru"
        )
        val lanIpList = listOf(
            "geoip:private",
            "0.0.0.0/8", "10.0.0.0/8", "100.64.0.0/10", "127.0.0.0/8", "169.254.0.0/16",
            "172.16.0.0/12", "192.0.0.0/24", "192.0.2.0/24", "192.88.99.0/24", "192.168.0.0/16",
            "198.18.0.0/15", "198.51.100.0/24", "203.0.113.0/24", "224.0.0.0/3", "fc00::/7", "fe80::/10"
        )
        val torrentTrackers = listOf(
            "geosite:rutracker",
            "domain:bittorrent.com", "domain:utorrent.com", "domain:transmissionbt.com", "domain:vuze.com",
            "domain:opentrackr.org", "domain:openbittorrent.com", "domain:publicbt.com", "domain:rarbg.com",
            "domain:rutracker.org", "domain:rutor.is", "domain:opentor.org", "domain:nyaa.si"
        )

        // Dynamic Quick Actions Preferences from SmartRoutingPanel
        val quickPrefs = context.getSharedPreferences("sentinel_quick_actions_prefs", Context.MODE_PRIVATE)
        val enabledIpService = quickPrefs.getBoolean("enabled_ip_service", true)
        val actionIpService = quickPrefs.getString("action_ip_service", "DIRECT") ?: "DIRECT"
        val enabledAds = quickPrefs.getBoolean("enabled_ads", false)
        val actionAds = quickPrefs.getString("action_ads", "BLOCKED") ?: "BLOCKED"
        val enabledCn = quickPrefs.getBoolean("enabled_cn", false)
        val actionCn = quickPrefs.getString("action_cn", "BLOCKED") ?: "BLOCKED"
        val enabledUs = quickPrefs.getBoolean("enabled_us", false)
        val actionUs = quickPrefs.getString("action_us", "BLOCKED") ?: "BLOCKED"

        val ipCheckerDomains = listOf(
            "domain:ipify.org", "domain:api.ipify.org", "domain:checkip.amazonaws.com", "domain:ifconfig.me", "domain:ifconfig.co", "domain:ifconfig.io",
            "domain:telega.me", "domain:ipinfo.io", "domain:2ip.ru", "domain:2ip.io", "domain:2ip.ua", "domain:2ip.me",
            "domain:myip.ru", "domain:myip.com", "domain:icanhazip.com", "domain:wtfismyip.com", "domain:ip.sb",
            "domain:ipapi.co", "domain:ip-api.com", "domain:ipapi.com", "domain:db-ip.com", "domain:whoer.net",
            "domain:ipwhois.io", "domain:ipwho.is", "domain:ipaddress.my", "domain:ipaddress.com", "domain:check-host.net",
            "domain:browserleaks.com", "domain:ip2location.com", "domain:ip2location.io", "domain:showmyip.com",
            "domain:whatsmyip.org", "domain:whatismyip.com", "domain:whatsmyipaddress.com", "domain:whatismyipaddress.com",
            "domain:dnsleaktest.com", "domain:ipleak.net", "domain:ip.me", "domain:ip.cn", "domain:ip138.com",
            "domain:ident.me", "domain:curlmyip.org", "domain:eth0.me", "domain:myexternalip.com", "domain:ip.nf",
            "domain:trackip.net", "domain:checkip.dyndns.org",
            "keyword:ipify", "keyword:2ip", "keyword:ipwhois", "keyword:icanhazip", "keyword:ifconfig", "keyword:checkip", "keyword:browserleaks", "keyword:whoer", "keyword:ipleak"
        )
        val ipCheckerIps = listOf("1.1.1.1/32", "1.0.0.1/32")

        // Combine Direct Domains & IPs
        val directDomains = mutableListOf<String>()
        val directIps = mutableListOf<String>()
        if (isBypassRu) {
            ruDomainsList.forEach { if (isIpRule(it)) directIps.add(it) else directDomains.add(it) }
        }
        if (isBypassTorrents) {
            torrentTrackers.forEach { if (isIpRule(it)) directIps.add(it) else directDomains.add(it) }
        }
        if (isBypassLan) {
            lanIpList.forEach { if (isIpRule(it)) directIps.add(it) else directDomains.add(it) }
        }
        
        for (rule in customDirect) {
            if (isIpRule(rule)) directIps.add(rule) else directDomains.add(rule)
        }

        // Combine Proxy Domains & IPs
        val proxyDomains = mutableListOf<String>()
        val proxyIps = mutableListOf<String>()
        for (rule in customProxy) {
            if (isIpRule(rule)) proxyIps.add(rule) else proxyDomains.add(rule)
        }

        // Combine Block Domains & IPs
        val blockDomains = mutableListOf<String>()
        val blockIps = mutableListOf<String>()
        for (rule in customBlock) {
            if (isIpRule(rule)) blockIps.add(rule) else blockDomains.add(rule)
        }

        // Apply Quick IP Checkers Rule
        if (enabledIpService) {
            when (actionIpService.uppercase()) {
                "BLOCKED" -> {
                    blockDomains.addAll(ipCheckerDomains)
                    blockIps.addAll(ipCheckerIps)
                }
                "VPN", "PROXY" -> {
                    proxyDomains.addAll(ipCheckerDomains)
                    proxyIps.addAll(ipCheckerIps)
                }
                "DIRECT" -> {
                    directDomains.addAll(ipCheckerDomains)
                    directIps.addAll(ipCheckerIps)
                }
            }
        }

        // Apply Quick Ads Rule
        if (enabledAds) {
            val adsDomains = listOf("geosite:category-ads-all")
            when (actionAds.uppercase()) {
                "DIRECT" -> directDomains.addAll(adsDomains)
                "VPN" -> proxyDomains.addAll(adsDomains)
                else -> blockDomains.addAll(adsDomains)
            }
        }

        // Apply Quick China Rule
        if (enabledCn) {
            val cnDomains = listOf("geosite:cn")
            val cnIps = listOf("geoip:cn")
            when (actionCn.uppercase()) {
                "DIRECT" -> { directDomains.addAll(cnDomains); directIps.addAll(cnIps) }
                "VPN" -> { proxyDomains.addAll(cnDomains); proxyIps.addAll(cnIps) }
                else -> { blockDomains.addAll(cnDomains); blockIps.addAll(cnIps) }
            }
        }

        // Apply Quick US Rule
        if (enabledUs) {
            val usDomains = listOf("geosite:us")
            val usIps = listOf("geoip:us")
            when (actionUs.uppercase()) {
                "DIRECT" -> { directDomains.addAll(usDomains); directIps.addAll(usIps) }
                "VPN" -> { proxyDomains.addAll(usDomains); proxyIps.addAll(usIps) }
                else -> { blockDomains.addAll(usDomains); blockIps.addAll(usIps) }
            }
        }


        val torrentProtocolRule = if (isBypassTorrents) """
            {
              "type": "field",
              "protocol": ["bittorrent"],
              "outboundTag": "direct"
            },
        """.trimIndent().prependIndent("              ") else ""

        val quicBlockRule = if (isBlockQuic) """
            {
              "type": "field",
              "port": 443,
              "network": "udp",
              "outboundTag": "block"
            },
        """.trimIndent().prependIndent("              ") else ""

        val smartDirectDomainsRule = if (directDomains.isNotEmpty()) """
            {
              "type": "field",
              "domain": ${directDomains.distinct().joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
              "outboundTag": "direct"
            },
        """.trimIndent().prependIndent("              ") else ""

        val smartDirectIpsRule = if (directIps.isNotEmpty()) """
            {
              "type": "field",
              "ip": ${directIps.distinct().joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
              "outboundTag": "direct"
            },
        """.trimIndent().prependIndent("              ") else ""

        val smartProxyDomainsRule = if (proxyDomains.isNotEmpty()) """
            {
              "type": "field",
              "domain": ${proxyDomains.distinct().joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
              "outboundTag": "proxy"
            },
        """.trimIndent().prependIndent("              ") else ""

        val smartProxyIpsRule = if (proxyIps.isNotEmpty()) """
            {
              "type": "field",
              "ip": ${proxyIps.distinct().joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
              "outboundTag": "proxy"
            },
        """.trimIndent().prependIndent("              ") else ""

        val smartBlockDomainsRule = if (blockDomains.isNotEmpty()) """
            {
              "type": "field",
              "domain": ${blockDomains.distinct().joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
              "outboundTag": "block"
            },
        """.trimIndent().prependIndent("              ") else ""

        val smartBlockIpsRule = if (blockIps.isNotEmpty()) """
            {
              "type": "field",
              "ip": ${blockIps.distinct().joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
              "outboundTag": "block"
            },
        """.trimIndent().prependIndent("              ") else ""

        val geoipRuleJson = if (geoipRules.isNotEmpty()) {
            """
            {
              "type": "field",
              "ip": ${geoipRules.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
              "outboundTag": "direct"
            },
            """.trimIndent().prependIndent("              ")
        } else ""

        val geositeRuleJson = if (geositeRules.isNotEmpty()) {
            """
            {
              "type": "field",
              "domain": ${geositeRules.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
              "outboundTag": "proxy"
            },
            """.trimIndent().prependIndent("              ")
        } else ""

        val prefs = context.getSharedPreferences("x_prox_sensitive_ports_prefs", Context.MODE_PRIVATE)
        val logLevel = prefs.getString("xray_log_level", "info") ?: "info"

        // Compile the template
        val json = """
        {
          "log": {
            "loglevel": "$logLevel"
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
              {
                "type": "field",
                "inboundTag": $inboundsList,
                "port": 53,
                "outboundTag": "dns-out"
              },
$captureRules$blockedRuleJson
$blockedPortsRuleJson
$blockedUidsRuleJson
              {
                "type": "field",
                "inboundTag": ["tun-in"],
                "port": 853,
                "outboundTag": "block"
              },
$torrentProtocolRule$quicBlockRule$smartDirectDomainsRule$smartDirectIpsRule$smartProxyDomainsRule$smartProxyIpsRule$smartBlockDomainsRule$smartBlockIpsRule$geoipRuleJson$geositeRuleJson
              {
                "type": "field",
                "network": "tcp,udp",
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
