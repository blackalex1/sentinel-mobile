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
        val username: String = "",
        val password: String = "",
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

    fun getLoopbackAddress(): String {
        return try {
            java.net.InetAddress.getLoopbackAddress().hostAddress ?: "127.0.0.1"
        } catch (e: Exception) {
            "127.0.0.1"
        }
    }

    /**
     * Resolves dynamic DNS servers from user preferences or directly from the active network interface.
     */
    fun resolveDynamicDnsServers(context: Context): List<String> {
        val userDns = XrayProfilePersistence.loadDnsServers(context)
        if (userDns.isNotEmpty()) return userDns

        return resolveActiveNetworkDns(context)
    }

    /**
     * Queries DNS server addresses directly from the active network interface via LinkProperties.
     */
    fun resolveActiveNetworkDns(context: Context): List<String> {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val activeNet = cm?.activeNetwork
            if (activeNet != null) {
                val lp = cm.getLinkProperties(activeNet)
                val servers = lp?.dnsServers?.mapNotNull { it.hostAddress }?.filter { it.isNotBlank() }
                if (!servers.isNullOrEmpty()) {
                    return servers
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Dynamic DNS query from active network failed: ${e.message}")
        }
        return emptyList()
    }

    /**
     * Checks whether a given string is a valid numeric IPv4 or IPv6 address literal
     * without performing any blocking network lookups.
     */
    fun isNumericIpAddress(address: String): Boolean {
        if (address.isBlank() || address.startsWith("http://", ignoreCase = true) || address.startsWith("https://", ignoreCase = true)) {
            return false
        }
        val clean = address.trim()
        val ipv4Parts = clean.split('.')
        if (ipv4Parts.size == 4) {
            return ipv4Parts.all { part ->
                val num = part.toIntOrNull()
                num != null && num in 0..255 && (part == "0" || !part.startsWith("0"))
            }
        }
        if (clean.contains(':')) {
            return try {
                val addr = java.net.InetAddress.getByName(clean)
                addr is java.net.Inet6Address && (addr.hostAddress == clean || clean.startsWith("["))
            } catch (_: Exception) {
                false
            }
        }
        return false
    }

    /**
     * Finds a random open port for our local inbound socket proxy.
     */
    fun findRandomOpenPort(excludePorts: Set<Int> = emptySet()): Int {
        for (attempt in 1..20) {
            try {
                val socket = java.net.ServerSocket()
                socket.reuseAddress = true
                socket.bind(java.net.InetSocketAddress(getLoopbackAddress(), 0))
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

    fun getActiveConfigFile(context: Context): File = File(context.filesDir, SECURE_CONFIG_NAME)

    /**
     * Reads the active secure config JSON from disk if it exists.
     */
    fun readActiveConfigJson(context: Context): String? {
        val file = getActiveConfigFile(context)
        return if (file.exists() && file.length() > 0) {
            try {
                file.readText(Charsets.UTF_8)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    /**
     * Builds the unified ConfigSpec for Sentinel-Core Go engine.
     * All routing presets (RU, torrents, ads, etc.) and security policies are compiled
     * directly by Sentinel-Core as the single source of truth.
     */
    fun buildConfigSpec(
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
        lanHttpPort: Int = 0,
        lanSocksPort: Int = 0,
        captureProxyPort: Int = 0,
        chainProfiles: List<ServerProfile>? = null,
        entryHopCreds: LocalProxyCredentials? = null
    ): ConfigSpec {
        // 1. Resolve target core and multi-hop structure
        val isChain = !chainProfiles.isNullOrEmpty() && chainProfiles.size >= 2
        val effectiveChain = if (isChain && entryHopCreds != null) {
            val entryHopProfile = ServerProfile(
                id = "chain_entry_gateway",
                name = "entry-gateway",
                type = "SOCKS",
                address = getLoopbackAddress(),
                port = entryHopCreds.port,
                username = entryHopCreds.username,
                password = entryHopCreds.token
            )
            listOf(entryHopProfile) + chainProfiles.drop(1)
        } else chainProfiles

        val activeTargetProfile = if (isChain) effectiveChain!!.last() else profile
        val targetCore = "xray"

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

        val dnsServers = resolveDynamicDnsServers(context)
        val dnsSpec = DNSSpec(
            servers = dnsServers.ifEmpty { listOf("https://1.1.1.1/dns-query") },
            finalServer = dnsServers.firstOrNull() ?: "1.1.1.1",
            strategy = "prefer_ipv4"
        )

        val routingSpec = RoutingSpec(
            defaultAction = if (profile.type.equals("DIRECT", ignoreCase = true)) "direct" else "proxy",
            rules = routingRules,
            autoDetectInterface = true,
            overrideDns = true
        )

        return ConfigSpec(
            targetCore = targetCore,
            serverNode = activeTargetProfile.toCoreProfile(),
            chainNodes = if (isChain) effectiveChain!!.map { it.toCoreProfile() } else null,
            clientInbound = clientInbound,
            serverInbounds = serverInbounds,
            routing = routingSpec,
            dns = dnsSpec,
            logLevel = "info",
            rawJsonConfig = if (!isChain && profile.fullJsonConfig.isNotEmpty()) profile.fullJsonConfig else null
        )
    }

    /**
     * Compiles the secure client config using the Sentinel-Core Go engine.
     * All routing presets (RU, torrents, ads, etc.) and security policies are compiled
     * directly by Sentinel-Core as the single source of truth.
     */
    fun getEntryConfigFile(context: Context): File {
        return File(context.filesDir, "entry_xray_config.json")
    }

    /**
     * Compiles the Entry Gateway configuration for multi-instance chaining (Hop 1).
     * Supports complex subscription configurations with burst observatory and leastLoad balancers.
     * Exposes a local authenticated SOCKS5 proxy on 127.0.0.1:[entryCreds.port].
     */
    fun compileEntryGatewayConfig(
        context: Context,
        profile: ServerProfile,
        entryCreds: LocalProxyCredentials
    ): File {
        val configFile = getEntryConfigFile(context)
        val clientInbound = ClientInboundSpec(
            mode = "system_proxy",
            socksPort = entryCreds.port,
            listenAddress = getLoopbackAddress(),
            authEnabled = true,
            authUsername = entryCreds.username,
            authPassword = entryCreds.token
        )
        val spec = ConfigSpec(
            targetCore = "xray",
            serverNode = if (profile.fullJsonConfig.isEmpty()) profile.toCoreProfile() else null,
            clientInbound = clientInbound,
            logLevel = "warning",
            rawJsonConfig = if (profile.fullJsonConfig.isNotEmpty()) profile.fullJsonConfig else null
        )
        val buildResult = SentinelCore.buildConfig(spec)
        if (buildResult.configJson.isNotEmpty()) {
            configFile.writeText(buildResult.configJson, Charsets.UTF_8)
            Log.i(TAG, "Successfully compiled entry gateway config via Sentinel-Core (${buildResult.targetCore})")
            return configFile
        }
        throw IllegalStateException("Sentinel-Core failed to compile entry gateway config: ${buildResult.error}")
    }

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
        lanHttpPort: Int = 0,
        lanSocksPort: Int = 0,
        captureProxyPort: Int = 0,
        chainProfiles: List<ServerProfile>? = null,
        entryHopCreds: LocalProxyCredentials? = null
    ): File {
        val configFile = getActiveConfigFile(context)
        val spec = buildConfigSpec(
            context = context,
            profile = profile,
            creds = creds,
            allowedApps = allowedApps,
            blockedApps = blockedApps,
            geoipRules = geoipRules,
            geositeRules = geositeRules,
            lanAuthEnabled = lanAuthEnabled,
            lanCreds = lanCreds,
            tetheringIps = tetheringIps,
            lanHttpPort = lanHttpPort,
            lanSocksPort = lanSocksPort,
            captureProxyPort = captureProxyPort,
            chainProfiles = chainProfiles,
            entryHopCreds = entryHopCreds
        )

        // Build configuration via Sentinel-Core Go Engine (Single Source of Truth)
        val buildResult = SentinelCore.buildConfig(spec)
        if (buildResult.configJson.isNotEmpty()) {
            configFile.writeText(buildResult.configJson, Charsets.UTF_8)
            Log.i(TAG, "Successfully compiled client configuration via Sentinel-Core (${buildResult.targetCore})")
            return configFile
        }

        throw IllegalStateException("Sentinel-Core failed to compile configuration: ${buildResult.error}")
    }

    /**
     * Compiles a preview config JSON for the active or specified profile using Sentinel-Core
     * without starting the VPN process. Supports multi-hop chained proxy setups.
     */
    fun buildPreviewConfigJson(
        context: Context,
        profile: ServerProfile? = null,
        chainProfiles: List<ServerProfile>? = null,
        entryHopCreds: LocalProxyCredentials? = null
    ): Pair<String, String?> {
        return try {
            val effectiveChain = chainProfiles ?: if (profile == null && XrayProfilePersistence.loadChainEnabled(context)) {
                XrayProfilePersistence.resolveChainProfiles(context).takeIf { it.size >= 2 }
            } else null

            val targetProfile = profile
                ?: effectiveChain?.lastOrNull()
                ?: XrayProfilePersistence.resolveActiveProfile(context)
                ?: ServerProfile(name = "Direct", address = getLoopbackAddress(), port = findRandomOpenPort(), type = "DIRECT")

            val savedPort = XrayProfilePersistence.loadLocalProxyPort(context)
            val creds = if (savedPort > 0) {
                LocalProxyCredentials(
                    port = savedPort,
                    username = XrayProfilePersistence.loadLocalProxyUsername(context).ifEmpty { generateSecureCredentials().username },
                    token = XrayProfilePersistence.loadLocalProxyPassword(context).ifEmpty { generateSecureCredentials().token }
                )
            } else {
                generateSecureCredentials()
            }

            val effectiveEntryCreds = entryHopCreds ?: if (effectiveChain != null) {
                generateSecureCredentials(excludePorts = setOf(creds.port))
            } else null

            val isLanSharingEnabled = XrayProfilePersistence.loadLanSharing(context)
            val isLanHttp = XrayProfilePersistence.loadLanSharingHttp(context)
            val isLanSocks = XrayProfilePersistence.loadLanSharingSocks(context)
            val isLanAuth = XrayProfilePersistence.loadLanSharingAuth(context)
            val lanCreds = if (isLanAuth) {
                val lanUser = XrayProfilePersistence.loadLanSharingUsername(context)
                val lanToken = XrayProfilePersistence.loadLanSharingPassword(context)
                if (lanUser.isNotEmpty() && lanToken.isNotEmpty()) {
                    LocalProxyCredentials(port = 0, username = lanUser, token = lanToken)
                } else {
                    val gen = generateSecureCredentials()
                    LocalProxyCredentials(port = 0, username = gen.username, token = gen.token)
                }
            } else null

            val allowedApps = XrayProfilePersistence.loadAllowedApps(context)
            val blockedApps = XrayProfilePersistence.loadBlockedApps(context).toList()
            val geoipRules = XrayProfilePersistence.loadGeoIpRules(context)
            val geositeRules = XrayProfilePersistence.loadGeoSiteRules(context)
            val lanHttpPort = if (isLanHttp) XrayProfilePersistence.loadLanSharingHttpPort(context) else 0
            val lanSocksPort = if (isLanSocks) XrayProfilePersistence.loadLanSharingSocksPort(context) else 0

            val spec = buildConfigSpec(
                context = context,
                profile = targetProfile,
                creds = creds,
                allowedApps = allowedApps,
                blockedApps = blockedApps,
                geoipRules = geoipRules,
                geositeRules = geositeRules,
                lanAuthEnabled = isLanAuth,
                lanCreds = lanCreds,
                lanHttpPort = lanHttpPort,
                lanSocksPort = lanSocksPort,
                chainProfiles = effectiveChain,
                entryHopCreds = effectiveEntryCreds
            )

            val buildResult = SentinelCore.buildConfig(spec)
            if (buildResult.configJson.isNotEmpty()) {
                Pair(buildResult.configJson, null)
            } else {
                Pair("", buildResult.error ?: "Sentinel-Core compilation error")
            }
        } catch (e: Exception) {
            Pair("", e.message ?: "Failed to generate preview")
        }
    }

    /**
     * Compiles a preview Entry Gateway config JSON (Hop 1) for the entry profile using Sentinel-Core
     * without modifying the live file on disk.
     */
    fun buildPreviewEntryGatewayJson(
        context: Context,
        profile: ServerProfile? = null,
        entryCreds: LocalProxyCredentials? = null
    ): Pair<String, String?> {
        return try {
            val targetProfile = profile
                ?: XrayProfilePersistence.resolveChainProfiles(context).firstOrNull()
                ?: XrayProfilePersistence.resolveActiveProfile(context)
                ?: ServerProfile(name = "Direct", address = getLoopbackAddress(), port = findRandomOpenPort(), type = "DIRECT")

            val effectiveCreds = entryCreds ?: generateSecureCredentials()

            val clientInbound = ClientInboundSpec(
                mode = "system_proxy",
                socksPort = effectiveCreds.port,
                listenAddress = getLoopbackAddress(),
                authEnabled = true,
                authUsername = effectiveCreds.username,
                authPassword = effectiveCreds.token
            )
            val spec = ConfigSpec(
                targetCore = "xray",
                serverNode = if (targetProfile.fullJsonConfig.isEmpty()) targetProfile.toCoreProfile() else null,
                clientInbound = clientInbound,
                logLevel = "warning",
                rawJsonConfig = if (targetProfile.fullJsonConfig.isNotEmpty()) targetProfile.fullJsonConfig else null
            )
            val buildResult = SentinelCore.buildConfig(spec)
            if (buildResult.configJson.isNotEmpty()) {
                Pair(buildResult.configJson, null)
            } else {
                Pair("", buildResult.error ?: "Sentinel-Core entry gateway preview error")
            }
        } catch (e: Exception) {
            Pair("", e.message ?: "Failed to generate entry gateway preview")
        }
    }
}
