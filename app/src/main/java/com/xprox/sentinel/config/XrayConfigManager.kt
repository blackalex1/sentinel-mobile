package com.xprox.sentinel.config

import android.content.Context
import android.util.Log
import com.xprox.sentinel.core.SentinelCore
import com.xprox.sentinel.core.models.*
import com.xprox.sentinel.core.toCoreProfile
import java.io.File
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

/**
 * Handles creation and manipulation of proxy server profiles and client configurations.
 * Delegates parsing, outbounds, inbounds, DNS, and preset routing compilation to the
 * high-performance Sentinel-Core Go engine.
 */
object XrayConfigManager {
    private const val TAG = "XrayConfigManager"
    private const val SECURE_CONFIG_NAME = "secure_xray_config.json"

    data class ServerProfile(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val address: String,
        val port: Int,
        val type: String = "VLESS", // VLESS, VMess, Shadowsocks, Trojan, Hysteria2, Socks, Direct
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
        val serviceName: String = "",
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
     * Compiles the secure client config using the Sentinel-Core Go engine.
     * All routing presets (RU, torrents, ads, etc.) and security policies are compiled
     * directly by Sentinel-Core as the single source of truth.
     */
    fun compileSecureConfig(
        context: Context,
        profile: ServerProfile,
        creds: LocalProxyCredentials,
        allowedApps: List<String> = emptyList(),
        blockedApps: List<String> = emptyList(),
        geoipRules: List<String> = emptyList(),
        geositeRules: List<String> = emptyList(),
        lanAuthEnabled: Boolean = false,
        lanCreds: LocalProxyCredentials? = null,
        tetheringIps: List<String> = emptyList(),
        lanHttpPort: Int = 10809,
        lanSocksPort: Int = 10808,
        captureProxyPort: Int = 0
    ): File {
        val configFile = File(context.filesDir, SECURE_CONFIG_NAME)

        // 1. Resolve target core
        val isHysteria = profile.type.equals("HYSTERIA2", ignoreCase = true) || profile.type.equals("HY2", ignoreCase = true)
        val targetCore = if (isHysteria) "singbox" else "xray"

        val isLanSharingEnabled = XrayProfilePersistence.loadLanSharing(context)
        val isLanHttp = XrayProfilePersistence.loadLanSharingHttp(context)
        val isLanSocks = XrayProfilePersistence.loadLanSharingSocks(context)

        // 2. Prepare Inbound configuration
        val clientInbound = ClientInboundSpec(
            mode = "mobile_vpn",
            socksPort = creds.port,
            authEnabled = true,
            authUsername = creds.username,
            authPassword = creds.token,
            lanSharingEnabled = isLanSharingEnabled,
            lanHttpPort = if (isLanHttp) lanHttpPort else 0,
            lanSocksPort = if (isLanSocks) lanSocksPort else 0,
            lanAuthEnabled = lanAuthEnabled,
            lanUsername = lanCreds?.username,
            lanPassword = lanCreds?.token
        )

        val serverInbounds: List<ServerInboundSpec>? = null

        // 4. Assemble Routing rules and dynamic presets
        val routingRules = mutableListOf<RoutingRule>()

        // 4a. Dynamic threat isolation (blocked destinations and blocked ports)
        val blockedDests = com.xprox.sentinel.service.ThreatDetectionManager.getBlockedDestinations().toList()
        if (blockedDests.isNotEmpty()) {
            val ips = blockedDests.filter { it.matches(Regex("^[0-9.]+$")) || it.contains(":") }
            val domains = blockedDests.filter { !it.matches(Regex("^[0-9.]+$")) && !it.contains(":") }
            if (ips.isNotEmpty()) {
                routingRules.add(RoutingRule(action = "block", ips = ips))
            }
            if (domains.isNotEmpty()) {
                routingRules.add(RoutingRule(action = "block", domains = domains))
            }
        }

        val blockedPorts = com.xprox.sentinel.service.ThreatDetectionManager.getBlockedPorts().toList()
        if (blockedPorts.isNotEmpty()) {
            routingRules.add(RoutingRule(action = "block", ports = blockedPorts.map { it.toString() }))
        }

        // 4b. Blocked application user IDs (UIDs)
        val pm = context.packageManager
        val allBlockedApps = (blockedApps + com.xprox.sentinel.service.ThreatDetectionManager.getBlockedAppsList()).distinct()
        val blockedUids = mutableListOf<String>()
        for (pkg in allBlockedApps) {
            try {
                if (pkg.isNotEmpty() && !pkg.startsWith("android.system.") && !pkg.startsWith("android.uid.")) {
                    val uid = pm.getPackageUid(pkg, 0)
                    blockedUids.add(uid.toString())
                }
            } catch (e: Exception) {}
        }
        if (blockedUids.isNotEmpty()) {
            routingRules.add(RoutingRule(action = "block", packageUids = blockedUids))
        }

        // 4c. User Custom Overrides (Direct, Proxy, Block)
        val customDirect = XrayProfilePersistence.loadCustomDirectRules(context)
        val customProxy = XrayProfilePersistence.loadCustomProxyRules(context)
        val customBlock = XrayProfilePersistence.loadCustomBlockRules(context)

        fun splitDomainsAndIps(rules: List<String>): Pair<List<String>, List<String>> {
            val domains = mutableListOf<String>()
            val ips = mutableListOf<String>()
            for (r in rules) {
                val tr = r.trim()
                if (tr.isEmpty()) continue
                if (tr.startsWith("geoip:") || tr.contains("/") || tr.matches(Regex("^[0-9.]+$")) || tr.matches(Regex("^[0-9a-f:]+$"))) {
                    ips.add(tr)
                } else {
                    domains.add(tr)
                }
            }
            return Pair(domains, ips)
        }

        if (customBlock.isNotEmpty()) {
            val (d, i) = splitDomainsAndIps(customBlock)
            if (d.isNotEmpty()) routingRules.add(RoutingRule(action = "block", domains = d))
            if (i.isNotEmpty()) routingRules.add(RoutingRule(action = "block", ips = i))
        }
        if (customDirect.isNotEmpty()) {
            val (d, i) = splitDomainsAndIps(customDirect)
            if (d.isNotEmpty()) routingRules.add(RoutingRule(action = "direct", domains = d))
            if (i.isNotEmpty()) routingRules.add(RoutingRule(action = "direct", ips = i))
        }
        if (customProxy.isNotEmpty()) {
            val (d, i) = splitDomainsAndIps(customProxy)
            if (d.isNotEmpty()) routingRules.add(RoutingRule(action = "proxy", domains = d))
            if (i.isNotEmpty()) routingRules.add(RoutingRule(action = "proxy", ips = i))
        }

        // 4d. Presets from Sentinel-Core Builtin Single Source of Truth
        val quickPrefs = context.getSharedPreferences("sentinel_quick_actions_prefs", Context.MODE_PRIVATE)
        val isBypassRu = XrayProfilePersistence.loadBypassRuSites(context)
        val isBypassTorrents = XrayProfilePersistence.loadBypassTorrents(context)
        val isBypassLan = XrayProfilePersistence.loadBypassLan(context)
        val isBlockQuic = XrayProfilePersistence.loadBlockQuic(context)

        // Dynamically fetch and compile rules for all atomic presets from Sentinel-Core Engine (Single Source of Truth)
        val corePresets = SentinelCore.listPresets()

        for (p in corePresets) {
            val presetId = p.id
            val isEnabled = when (presetId) {
                "ru" -> isBypassRu
                "bittorrent" -> isBypassTorrents
                "lan" -> isBypassLan
                "quic" -> isBlockQuic
                "ip_checkers" -> quickPrefs.getBoolean("enabled_$presetId", true)
                else -> quickPrefs.getBoolean("enabled_$presetId", false)
            }

            if (isEnabled) {
                val actionKey = if (presetId == "bittorrent") "action_bt" else "action_$presetId"
                val savedAction = quickPrefs.getString(actionKey, p.defaultTarget)?.lowercase() ?: p.defaultTarget.lowercase()
                val target = when (savedAction) {
                    "blocked", "block" -> "block"
                    "vpn", "proxy" -> "proxy"
                    else -> "direct"
                }

                val fullPreset = SentinelCore.getPreset(presetId) ?: p
                if (!fullPreset.domains.isNullOrEmpty()) {
                    routingRules.add(
                        RoutingRule(
                            action = target,
                            domains = fullPreset.domains,
                            protocols = fullPreset.protocols,
                            ports = fullPreset.ports
                        )
                    )
                }
                if (!fullPreset.ips.isNullOrEmpty()) {
                    routingRules.add(
                        RoutingRule(
                            action = target,
                            ips = fullPreset.ips,
                            protocols = fullPreset.protocols,
                            ports = fullPreset.ports
                        )
                    )
                }
                if (fullPreset.domains.isNullOrEmpty() && fullPreset.ips.isNullOrEmpty() && (!fullPreset.protocols.isNullOrEmpty() || !fullPreset.ports.isNullOrEmpty())) {
                    routingRules.add(
                        RoutingRule(
                            action = target,
                            protocols = fullPreset.protocols,
                            ports = fullPreset.ports
                        )
                    )
                }
            }
        }

        val dnsServers = XrayProfilePersistence.loadDnsServers(context)
        val dnsSpec = DNSSpec(
            servers = dnsServers.ifEmpty { listOf("https://dns.google/dns-query", "8.8.8.8") },
            finalServer = dnsServers.firstOrNull() ?: "8.8.8.8",
            strategy = "prefer_ipv4"
        )

        val routingSpec = RoutingSpec(
            defaultAction = if (profile.type.equals("DIRECT", ignoreCase = true)) "direct" else "proxy",
            rules = routingRules,
            autoDetectInterface = true,
            overrideDns = true
        )

        val spec = ConfigSpec(
            targetCore = targetCore,
            serverNode = profile.toCoreProfile(),
            clientInbound = clientInbound,
            serverInbounds = serverInbounds,
            routing = routingSpec,
            dns = dnsSpec,
            logLevel = "info",
            rawJsonConfig = if (profile.fullJsonConfig.isNotEmpty()) profile.fullJsonConfig else null
        )

        // 5. Build configuration via Sentinel-Core Go Engine (Single Source of Truth)
        val buildResult = SentinelCore.buildConfig(spec)
        if (buildResult.configJson.isNotEmpty()) {
            configFile.writeText(buildResult.configJson, Charsets.UTF_8)
            Log.i(TAG, "Successfully compiled client configuration via Sentinel-Core (${buildResult.targetCore})")
            return configFile
        }

        throw IllegalStateException("Sentinel-Core failed to compile configuration: ${buildResult.error}")
    }
}
