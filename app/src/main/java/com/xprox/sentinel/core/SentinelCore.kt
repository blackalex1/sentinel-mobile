package com.xprox.sentinel.core

import android.content.Context
import android.util.Log
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.xprox.sentinel.config.XrayConfigManager
import com.xprox.sentinel.core.models.*
import kotlinx.serialization.json.Json
import java.util.UUID

object SentinelCore {
    private const val TAG = "SentinelCore"
    private const val LIB_NAME = "sentinel_core"

    interface SentinelCoreLib : Library {
        fun SentinelGetEngineVersion(): Pointer?
        fun SentinelBuildConfig(specJson: String): Pointer?
        fun SentinelParseURI(rawUri: String): Pointer?
        fun SentinelGenerateURI(profileJson: String): Pointer?
        fun SentinelListPresets(): Pointer?
        fun SentinelGetPreset(presetId: String): Pointer?
        fun SentinelGetConfigurationSchema(lang: String): Pointer?
        fun SentinelGetSecuritySchema(lang: String): Pointer?
        fun SentinelGetDefaultSecurityConfig(): Pointer?
        fun SentinelValidateSecurityConfig(configJson: String): Pointer?
        fun SentinelAuditConnection(reqJson: String): Pointer?
        fun SentinelGetPortShieldCatalog(lang: String): Pointer?
        fun SentinelConfigureSecurityPolicy(policyJson: String): Pointer?
        fun SentinelGetSecurityPolicy(): Pointer?
        fun SentinelAndroidWritePcap(filePath: String, rawHex: String, timestampMs: Long): Pointer?
        fun SentinelAndroidSynthesizeAndWritePcap(
            filePath: String, proto: String, srcIP: String, srcPort: Int,
            dstIP: String, dstPort: Int, tcpFlags: Int, seq: Long, ack: Long,
            window: Int, payloadHex: String, timestampMs: Long
        ): Pointer?
        fun SentinelAndroidDissectPacket(rawHex: String): Pointer?
        fun SentinelAndroidBlockApp(pkgName: String): Pointer?
        fun SentinelAndroidUnblockApp(pkgName: String): Pointer?
        fun SentinelAndroidIsAppBlocked(pkgName: String): Pointer?
        fun SentinelAndroidGetBlockedApps(): Pointer?
        fun SentinelAndroidClearThreats(): Pointer?
        fun SentinelBatchPing(targetsJson: String, timeoutMs: Int): Pointer?
        fun SentinelProxyPing(socksPort: Int, authUser: String, authPass: String, targetUrl: String, timeoutMs: Int): Pointer?
        fun SentinelGetPublicIP(socksPort: Int, authUser: String, authPass: String, timeoutMs: Int): Pointer?
        fun SentinelOptimizeRules(rulesJson: String): Pointer?
        fun SentinelAndroidPushLog(logJson: String): Pointer?
        fun SentinelAndroidGetLogs(limit: Int, offset: Int, portFilter: Int, query: String): Pointer?
        fun SentinelAndroidGetLogStats(): Pointer?
        fun SentinelAndroidClearLogs(): Pointer?
        fun SentinelAndroidParseConnectionLog(logLine: String): Pointer?
        fun SentinelFreeString(str: Pointer?)
    }

    @Volatile
    private var activeLib: SentinelCoreLib? = null
    @Volatile
    private var isInitialized = false
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun getOrLoadLib(context: Context? = null): SentinelCoreLib? {
        if (isInitialized && activeLib != null) return activeLib
        synchronized(this) {
            if (isInitialized && activeLib != null) return activeLib

            val candidates = mutableListOf<String>()

            // 1. Priority 1: Check downloaded custom core in app private storage if available
            val effectiveCtx = context ?: appContext
            effectiveCtx?.let { ctx ->
                val customFile = java.io.File(ctx.filesDir, "core/libsentinel_core.so")
                if (customFile.exists() && customFile.length() > 0) {
                    candidates.add(customFile.absolutePath)
                }
            }

            // 2. Priority 2: Standard JNA candidate names
            candidates.add(LIB_NAME)
            candidates.add("sentinel-core")
            candidates.add("libsentinel_core")

            // 3. Priority 3: Dynamically check system property or environment variable
            System.getProperty("jna.library.path")?.split(java.io.File.pathSeparator)?.forEach { dir ->
                val d = java.io.File(dir)
                if (d.exists()) {
                    candidates.add(java.io.File(d, "$LIB_NAME.dll").absolutePath)
                    candidates.add(java.io.File(d, "sentinel-core.dll").absolutePath)
                    candidates.add(java.io.File(d, "lib$LIB_NAME.so").absolutePath)
                    candidates.add(java.io.File(d, "$LIB_NAME.so").absolutePath)
                }
            }
            System.getenv("SENTINEL_CORE_LIB_DIR")?.let { dir ->
                candidates.add(java.io.File(dir, "libsentinel_core.so").absolutePath)
                candidates.add(java.io.File(dir, "sentinel-core.dll").absolutePath)
                candidates.add(java.io.File(dir, "sentinel_core.dll").absolutePath)
            }

            var loaded: SentinelCoreLib? = null
            for (name in candidates) {
                try {
                    loaded = Native.load(name, SentinelCoreLib::class.java)
                    if (loaded != null) {
                        try { Log.i(TAG, "Successfully loaded native Sentinel-Core via: $name") } catch (e: Throwable) {}
                        break
                    }
                } catch (t: Throwable) {
                    // Candidate skipped
                }
            }
            activeLib = loaded
            isInitialized = true
            return loaded
        }
    }

    /**
     * Hot-reloads the Sentinel-Core native engine (e.g. after downloading a new core or reverting).
     */
    fun reload(context: Context? = null) {
        synchronized(this) {
            isInitialized = false
            activeLib = null
            getOrLoadLib(context)
        }
    }

    fun isAvailable(): Boolean = getOrLoadLib() != null

    private fun callNative(block: (SentinelCoreLib) -> Pointer?): String? {
        val lib = getOrLoadLib() ?: return null
        val ptr = try {
            block(lib)
        } catch (t: Throwable) {
            return null
        }
        if (ptr == null) return null
        return try {
            ptr.getString(0, "UTF-8")
        } catch (t: Throwable) {
            null
        } finally {
            try {
                lib.SentinelFreeString(ptr)
            } catch (t: Throwable) {
                // Ignore free error
            }
        }
    }

    /**
     * Parses any supported proxy link into a normalized ServerProfile via Sentinel-Core Go parser.
     */
    fun parseUri(rawUri: String): XrayConfigManager.ServerProfile? {
        val trimmed = rawUri.trim()
        if (trimmed.isEmpty()) return null

        val jsonStr = callNative { it.SentinelParseURI(trimmed) }
        if (jsonStr.isNullOrEmpty()) return null

        return try {
            if (jsonStr.contains("\"error\"") && !jsonStr.contains("\"protocol\"")) {
                Log.d(TAG, "Sentinel-Core URI parse skip: $jsonStr")
                return null
            }
            val coreProfile = SentinelJson.decodeFromString<CoreServerProfile>(jsonStr)
            if (coreProfile.address.isEmpty()) {
                Log.e(TAG, "Sentinel-Core returned empty server address: $jsonStr")
                return null
            }
            coreProfile.toAppProfile()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deserialize parsed URI JSON from Sentinel-Core: $jsonStr", e)
            null
        }
    }

    /**
     * Exports a ServerProfile back to a shareable URI link via Sentinel-Core Go generator.
     */
    fun generateUri(profile: XrayConfigManager.ServerProfile): String {
        val coreProfile = profile.toCoreProfile()
        val profileJson = SentinelJson.encodeToString(CoreServerProfile.serializer(), coreProfile)
        val respJson = callNative { it.SentinelGenerateURI(profileJson) } ?: return ""

        return try {
            val map = SentinelJson.decodeFromString<Map<String, String>>(respJson)
            map["uri"] ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deserialize generated URI JSON: $respJson", e)
            ""
        }
    }

    /**
     * Compiles a complete client configuration for the specified target core (Xray / Sing-box / Hysteria 2).
     */
    fun buildConfig(spec: ConfigSpec): BuildResult {
        val specJson = SentinelJson.encodeToString(ConfigSpec.serializer(), spec)
        val respJson = callNative { it.SentinelBuildConfig(specJson) }
            ?: return BuildResult(error = "Sentinel-Core native library unavailable")

        return try {
            SentinelJson.decodeFromString<BuildResult>(respJson)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode BuildResult from Sentinel-Core: $respJson", e)
            BuildResult(error = e.message ?: "Failed to parse build result")
        }
    }

    /**
     * Lists all available atomic routing presets built into the Sentinel-Core engine.
     */
    fun listPresets(): List<RoutingPreset> {
        val respJson = callNative { it.SentinelListPresets() }
        if (!respJson.isNullOrEmpty()) {
            try {
                val list = SentinelJson.decodeFromString<List<RoutingPreset>>(respJson)
                if (list.isNotEmpty()) {
                    return list.map { p ->
                        if (p.id == "ip_checkers") p.copy(ips = null) else p
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode presets from Sentinel-Core: $respJson", e)
            }
        }
        return emptyList()
    }

    /**
     * Retrieves detailed information about a specific routing preset from the Sentinel-Core engine.
     */
    fun getPreset(presetId: String): RoutingPreset? {
        val respJson = callNative { it.SentinelGetPreset(presetId) }
        if (!respJson.isNullOrEmpty()) {
            try {
                val preset = SentinelJson.decodeFromString<RoutingPreset>(respJson)
                if (preset.id == "ip_checkers") {
                    return preset.copy(ips = null)
                }
                return preset
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode preset $presetId from Sentinel-Core: $respJson", e)
            }
        }
        return null
    }

    /**
     * Retrieves the version string of the loaded native Sentinel-Core engine.
     */
    fun getEngineVersion(): String {
        return callNative { it.SentinelGetEngineVersion() } ?: "dev"
    }

    /**
     * Retrieves the JSON Schema for Security Configuration from Sentinel-Core.
     */
    fun getSecuritySchema(lang: String = "ru"): String? {
        return callNative { it.SentinelGetSecuritySchema(lang) }
    }

    /**
     * Retrieves the default Security Configuration JSON from Sentinel-Core.
     */
    fun getDefaultSecurityConfig(): String? {
        return callNative { it.SentinelGetDefaultSecurityConfig() }
    }

    /**
     * Formats network traffic speed metrics (rx/tx bytes per second) in high performance monospaced style.
     */
    fun formatTrafficSpeed(rxBytesPerSec: Long, txBytesPerSec: Long): String {
        fun formatBytes(bytesPerSec: Long): String {
            return if (bytesPerSec < 1024) {
                "$bytesPerSec B/s"
            } else if (bytesPerSec < 1024 * 1024) {
                String.format(java.util.Locale.US, "%.1f KB/s", bytesPerSec / 1024.0)
            } else {
                String.format(java.util.Locale.US, "%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
            }
        }
        return "↓ ${formatBytes(rxBytesPerSec)}  |  ↑ ${formatBytes(txBytesPerSec)}"
    }

    /**
     * Validates a JSON security configuration.
     */
    fun validateSecurityConfig(configJson: String): Boolean {
        val resp = callNative { it.SentinelValidateSecurityConfig(configJson) } ?: return false
        return resp.contains("\"valid\":true") || resp.contains("\"valid\": true")
    }

    /**
     * Performs a detailed validation of custom domain/IP rules or security configs via Sentinel-Core,
     * returning validation status and diagnostic message.
     */
    fun validateRuleOrConfig(ruleOrConfigJson: String): Pair<Boolean, String> {
        val trimmed = ruleOrConfigJson.trim()
        if (trimmed.isEmpty()) return Pair(false, "Rule cannot be empty")

        if (trimmed.startsWith("{")) {
            val resp = callNative { it.SentinelValidateSecurityConfig(trimmed) }
                ?: return Pair(false, "Sentinel-Core native library unavailable")
            val isValid = resp.contains("\"valid\":true") || resp.contains("\"valid\": true")
            return Pair(isValid, if (isValid) "Valid configuration" else "Invalid security configuration syntax")
        }

        // Basic domain / IP format validation check
        val isDomainOrIp = trimmed.matches(Regex("""^([a-zA-Z0-9_.-]+|([0-9]{1,3}\.){3}[0-9]{1,3}(/[0-9]{1,2})?|([0-9a-fA-F:]+)(/[0-9]{1,3})?|geoip:[a-zA-Z0-9_-]+|geosite:[a-zA-Z0-9_-]+)$"""))
        if (!isDomainOrIp) {
            return Pair(false, "Invalid domain, IP or geosite format")
        }

        val testConfig = """
            {
                "rules": [
                    { "action": "direct", "domains": ["$trimmed"] }
                ]
            }
        """.trimIndent()
        val isValid = validateSecurityConfig(testConfig)
        return Pair(isValid, if (isValid) "Rule syntax valid" else "Invalid rule syntax")
    }

    private val tunConnectionRegex = Regex("""(?i)(?:from\s+)?(tcp|udp):(\[[a-fA-F0-9:]+\]|[^\s:]+):(\d+)\s+accepted\s+(?:tcp|udp):(\[[a-fA-F0-9:]+\]|[^\s:]+):(\d+)""")
    private val legacyAcceptedRegex = Regex("""(?i)connection\s+accepted\s+from\s+([\w.-]+):(\d+)\s+(tcp|udp):([\w.-]+):(\d+)""")

    /**
     * High performance Xray connection log parser driven exclusively by Sentinel-Core native engine.
     */
    fun parseConnectionLog(line: String): ParsedConnectionLog? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || !trimmed.contains("accepted", ignoreCase = true)) return null
        val respJson = callNative { it.SentinelAndroidParseConnectionLog(trimmed) }
        if (!respJson.isNullOrEmpty()) {
            try {
                return SentinelJson.decodeFromString(ParsedConnectionLog.serializer(), respJson)
            } catch (e: Exception) {
                // fall through to JVM regex
            }
        }

        // JVM unit testing support when native test DLL does not export SentinelAndroidParseConnectionLog
        fun isHotspotIp(ip: String): Boolean {
            val clean = ip.trim('[', ']').lowercase()
            return !clean.startsWith("127.") &&
                   !clean.startsWith("10.0.0.") &&
                   !clean.startsWith("fd00:") &&
                   clean != "::1" &&
                   clean != "localhost"
        }

        val match = tunConnectionRegex.find(trimmed)
        if (match != null) {
            val proto = match.groupValues[1].uppercase()
            val srcIp = match.groupValues[2].trim('[', ']')
            val srcPort = match.groupValues[3].toIntOrNull() ?: 0
            val destIp = match.groupValues[4].trim('[', ']')
            val destPort = match.groupValues[5].toIntOrNull() ?: 0
            val isHotspot = isHotspotIp(srcIp)
            return ParsedConnectionLog(
                protocol = proto,
                srcIp = srcIp,
                srcPort = srcPort,
                destIp = destIp,
                destPort = destPort,
                isHotspot = isHotspot,
                sourceType = if (isHotspot) "hotspot" else "local_tun"
            )
        }

        val legacyMatch = legacyAcceptedRegex.find(trimmed)
        if (legacyMatch != null) {
            val srcIp = legacyMatch.groupValues[1].trim('[', ']')
            val srcPort = legacyMatch.groupValues[2].toIntOrNull() ?: 0
            val proto = legacyMatch.groupValues[3].uppercase()
            val destIp = legacyMatch.groupValues[4].trim('[', ']')
            val destPort = legacyMatch.groupValues[5].toIntOrNull() ?: 0
            val isHotspot = isHotspotIp(srcIp)
            return ParsedConnectionLog(
                protocol = proto,
                srcIp = srcIp,
                srcPort = srcPort,
                destIp = destIp,
                destPort = destPort,
                isHotspot = isHotspot,
                sourceType = if (isHotspot) "hotspot" else "local_tun"
            )
        }

        return null
    }

    /**
     * Audits an incoming socket connection or packet header via the high-performance native Go engine.
     */
    fun auditConnection(context: Context? = null, request: AndroidAuditRequest): AndroidAuditVerdict {
        if (context != null && !isInitialized) getOrLoadLib(context)
        return try {
            val reqJson = SentinelJson.encodeToString(AndroidAuditRequest.serializer(), request)
            val respJson = callNative { it.SentinelAuditConnection(reqJson) }
            if (!respJson.isNullOrEmpty()) {
                SentinelJson.decodeFromString(AndroidAuditVerdict.serializer(), respJson)
            } else {
                AndroidAuditVerdict(action = "ALLOW")
            }
        } catch (e: Exception) {
            AndroidAuditVerdict(action = "ALLOW")
        }
    }

    fun configureSecurityPolicy(context: Context? = null, policyJson: String): Boolean {
        if (context != null && !isInitialized) getOrLoadLib(context)
        return try {
            val resp = callNative { it.SentinelConfigureSecurityPolicy(policyJson) }
            resp != null && (resp.contains("\"success\": true") || resp.contains("\"success\":true"))
        } catch (e: Exception) {
            false
        }
    }

    fun getSecurityPolicy(context: Context? = null): String? {
        if (context != null && !isInitialized) getOrLoadLib(context)
        return try {
            callNative { it.SentinelGetSecurityPolicy() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Writes raw packet bytes to a Wireshark-compatible PCAP file via the native engine.
     */
    fun writePcapPacket(context: Context? = null, filePath: String, rawBytes: ByteArray, timestampMs: Long = System.currentTimeMillis()): Boolean {
        if (context != null && !isInitialized) getOrLoadLib(context)
        return try {
            val hexStr = rawBytes.joinToString("") { "%02x".format(it) }
            val resp = callNative { it.SentinelAndroidWritePcap(filePath, hexStr, timestampMs) }
            resp?.contains("\"success\":true") == true || resp?.contains("\"success\": true") == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Synthesizes a valid wire-format TCP/UDP packet and writes it to PCAP via the native engine.
     */
    fun synthesizeAndWritePcap(
        context: Context? = null,
        filePath: String,
        proto: String,
        srcIP: String,
        srcPort: Int,
        dstIP: String,
        dstPort: Int,
        tcpFlags: Int,
        seq: Long,
        ack: Long,
        window: Int,
        payload: ByteArray? = null,
        timestampMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (context != null && !isInitialized) getOrLoadLib(context)
        return try {
            val payloadHex = payload?.joinToString("") { "%02x".format(it) } ?: ""
            val resp = callNative {
                it.SentinelAndroidSynthesizeAndWritePcap(
                    filePath, proto, srcIP, srcPort, dstIP, dstPort,
                    tcpFlags, seq, ack, window, payloadHex, timestampMs
                )
            }
            resp?.contains("\"success\":true") == true || resp?.contains("\"success\": true") == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Dissects a network packet like Wireshark/tshark, extracting headers, flags, and payload previews.
     */
    fun dissectPacket(context: Context? = null, rawBytes: ByteArray): DissectedPacketInfo? {
        if (context != null && !isInitialized) getOrLoadLib(context)
        return try {
            val hexStr = rawBytes.joinToString("") { "%02x".format(it) }
            val respJson = callNative { it.SentinelAndroidDissectPacket(hexStr) }
            if (respJson != null && !respJson.contains("\"error\":")) {
                SentinelJson.decodeFromString(DissectedPacketInfo.serializer(), respJson)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dissect packet via SentinelCore", e)
            null
        }
    }

    fun blockApp(context: Context? = null, packageName: String) {
        if (context != null && !isInitialized) getOrLoadLib(context)
        callNative { it.SentinelAndroidBlockApp(packageName) }
    }

    fun unblockApp(context: Context? = null, packageName: String) {
        if (context != null && !isInitialized) getOrLoadLib(context)
        callNative { it.SentinelAndroidUnblockApp(packageName) }
    }

    fun isAppBlocked(context: Context? = null, packageName: String): Boolean {
        if (context != null && !isInitialized) getOrLoadLib(context)
        val resp = callNative { it.SentinelAndroidIsAppBlocked(packageName) } ?: return false
        return resp.contains("\"blocked\":true") || resp.contains("\"blocked\": true")
    }

    fun getBlockedApps(context: Context? = null): com.xprox.sentinel.core.models.BlockedAppsResult {
        if (context != null && !isInitialized) getOrLoadLib(context)
        val resp = callNative { it.SentinelAndroidGetBlockedApps() } ?: return com.xprox.sentinel.core.models.BlockedAppsResult()
        return try {
            SentinelJson.decodeFromString(com.xprox.sentinel.core.models.BlockedAppsResult.serializer(), resp)
        } catch (e: Exception) {
            com.xprox.sentinel.core.models.BlockedAppsResult()
        }
    }

    fun clearThreats(context: Context? = null) {
        if (context != null && !isInitialized) getOrLoadLib(context)
        callNative { it.SentinelAndroidClearThreats() }
    }

    /**
     * Executes high-performance parallel TCP ping across a batch of targets via Sentinel-Core Go engine.
     */
    fun batchPing(targets: List<PingTarget>, timeoutMs: Int = 2500): List<BatchPingResult> {
        if (targets.isEmpty()) return emptyList()
        val jsonStr = try {
            SentinelJson.encodeToString(kotlinx.serialization.builtins.ListSerializer(com.xprox.sentinel.core.models.PingTarget.serializer()), targets)
        } catch (e: Exception) {
            "[]"
        }
        val respJson = callNative { it.SentinelBatchPing(jsonStr, timeoutMs) }
        if (!respJson.isNullOrEmpty()) {
            try {
                return SentinelJson.decodeFromString<List<BatchPingResult>>(respJson)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode batch ping results: $respJson", e)
            }
        }
        // Fallback: simple response
        return targets.map { BatchPingResult(id = it.id, address = it.address, port = it.port, success = false, error = "Native engine unavailable") }
    }

    /**
     * Measures real HTTP/TLS handshake latency via SOCKS5 proxy using Sentinel-Core.
     */
    fun proxyPing(
        socksPort: Int,
        authUsername: String = "",
        authPassword: String = "",
        targetUrl: String = "http://cp.cloudflare.com/generate_204",
        timeoutMs: Int = 3000
    ): ProxyPingResult {
        val respJson = callNative { it.SentinelProxyPing(socksPort, authUsername, authPassword, targetUrl, timeoutMs) }
        if (!respJson.isNullOrEmpty()) {
            try {
                return SentinelJson.decodeFromString<ProxyPingResult>(respJson)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode proxy ping result: $respJson", e)
            }
        }
        return ProxyPingResult(success = false, error = "Native engine unavailable")
    }

    /**
     * Concurrently probes multiple trusted IP checking services via optional SOCKS5 proxy in Go.
     */
    fun getPublicIP(
        socksPort: Int = 0,
        authUsername: String = "",
        authPassword: String = "",
        timeoutMs: Int = 3500
    ): PublicIPInfo? {
        val respJson = callNative { it.SentinelGetPublicIP(socksPort, authUsername, authPassword, timeoutMs) }
        if (!respJson.isNullOrEmpty()) {
            if (respJson.contains("\"error\":") && !respJson.contains("\"ip\":")) {
                Log.w(TAG, "Public IP lookup error from Sentinel-Core: $respJson")
                return null
            }
            try {
                val res = SentinelJson.decodeFromString<PublicIPInfo>(respJson)
                if (res.ip.isNotEmpty()) {
                    return res
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode public IP info: $respJson", e)
            }
        }
        return null
    }

    /**
     * Pushes an enriched connection log entry into the native in-memory RingBuffer.
     */
    fun pushLog(entry: AndroidLogEntry): Boolean {
        val jsonStr = try {
            SentinelJson.encodeToString(com.xprox.sentinel.core.models.AndroidLogEntry.serializer(), entry)
        } catch (e: Exception) {
            return false
        }
        val resp = callNative { it.SentinelAndroidPushLog(jsonStr) }
        return resp != null && (resp.contains("\"success\": true") || resp.contains("\"success\":true"))
    }

    /**
     * Retrieves filtered, paginated connection log records from native RingBuffer.
     */
    fun getLogs(limit: Int = 100, offset: Int = 0, portFilter: Int = 0, query: String = ""): List<AndroidLogEntry> {
        val resp = callNative { it.SentinelAndroidGetLogs(limit, offset, portFilter, query) }
        if (!resp.isNullOrEmpty()) {
            try {
                return SentinelJson.decodeFromString<List<AndroidLogEntry>>(resp)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode logs from native ring buffer: $resp", e)
            }
        }
        return emptyList()
    }

    /**
     * Retrieves aggregated traffic metrics (Top Apps, Top Ports, Protocol Breakdown) from native RingBuffer.
     */
    fun getLogStats(): AndroidLogStats {
        val resp = callNative { it.SentinelAndroidGetLogStats() }
        if (!resp.isNullOrEmpty()) {
            try {
                return SentinelJson.decodeFromString<AndroidLogStats>(resp)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode log stats: $resp", e)
            }
        }
        return AndroidLogStats()
    }

    /**
     * Clears all log records from the native in-memory RingBuffer.
     */
    fun clearLogs(): Boolean {
        val resp = callNative { it.SentinelAndroidClearLogs() }
        return resp != null && (resp.contains("\"success\": true") || resp.contains("\"success\":true"))
    }
}

/**
 * Extension mapper from CoreServerProfile to app ServerProfile
 */
fun CoreServerProfile.toAppProfile(): XrayConfigManager.ServerProfile {
    val profileId = if (this.id.isNotEmpty()) this.id else UUID.randomUUID().toString()
    val rawProto = this.protocol.trim().uppercase()
    val appType = when (rawProto) {
        "HYSTERIA2", "HY2" -> "HYSTERIA2"
        "VLESS" -> "VLESS"
        "VMESS" -> "VMESS"
        "TROJAN" -> "TROJAN"
        "SHADOWSOCKS", "SS" -> "SHADOWSOCKS"
        "SOCKS", "SOCKS5" -> "SOCKS"
        "DIRECT" -> "DIRECT"
        else -> rawProto
    }

    val sec = when {
        this.security.isNotEmpty() -> this.security.lowercase()
        this.publicKey.isNotEmpty() -> "reality"
        this.sni.isNotEmpty() || this.insecure -> "tls"
        else -> "none"
    }

    return XrayConfigManager.ServerProfile(
        id = profileId,
        name = if (this.name.isNotEmpty()) this.name else "Server $address:$port",
        address = this.address,
        port = this.port,
        type = appType,
        uuid = when {
            this.uuid.isNotEmpty() -> this.uuid
            this.username.isNotEmpty() -> this.username
            else -> this.password
        },
        path = when {
            this.path.isNotEmpty() -> this.path
            this.username.isNotEmpty() && this.password.isNotEmpty() -> this.password
            this.cipher.isNotEmpty() -> this.cipher
            else -> ""
        },
        security = sec,
        sni = this.sni,
        pbk = this.publicKey,
        sid = this.shortId,
        fp = if (this.fingerprint.isNotEmpty()) this.fingerprint else "chrome",
        network = if (this.transport.isNotEmpty()) this.transport else "tcp",
        flow = this.flow,
        encryption = this.encryption,
        spx = this.spiderX,
        host = this.host,
        allowInsecure = this.insecure,
        alpn = this.alpn.joinToString(","),
        pinnedPeerCertSha256 = this.pinnedPeerCertSha256,
        serviceName = this.serviceName,
        fullJsonConfig = this.rawJsonConfig ?: ""
    )
}

/**
 * Extension mapper from app ServerProfile to CoreServerProfile
 */
fun XrayConfigManager.ServerProfile.toCoreProfile(): CoreServerProfile {
    val proto = when (this.type.uppercase()) {
        "HYSTERIA2", "HY2" -> "hysteria2"
        "VLESS" -> "vless"
        "VMESS" -> "vmess"
        "TROJAN" -> "trojan"
        "SHADOWSOCKS", "SS" -> "shadowsocks"
        "SOCKS", "SOCKS5" -> "socks"
        "DIRECT" -> "direct"
        else -> this.type.lowercase()
    }

    val isReality = this.security.equals("reality", ignoreCase = true) || this.pbk.isNotEmpty()
    val isTls = isReality || this.security.equals("tls", ignoreCase = true) || this.sni.isNotEmpty() || this.allowInsecure

    val sec = when {
        isReality -> "reality"
        isTls -> "tls"
        else -> this.security.lowercase()
    }

    val alpnList = if (this.alpn.isNotEmpty()) {
        this.alpn.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    } else emptyList()

    return CoreServerProfile(
        id = this.id,
        name = this.name,
        protocol = proto,
        address = this.address,
        port = this.port,
        transport = if (this.network.isNotEmpty()) this.network.lowercase() else "tcp",
        security = sec,
        uuid = if (proto == "vless" || proto == "vmess") this.uuid else "",
        password = if (proto != "vless" && proto != "vmess") this.uuid else "",
        sni = this.sni,
        alpn = alpnList,
        fingerprint = if (this.fp.isNotEmpty()) this.fp else "chrome",
        insecure = this.allowInsecure,
        publicKey = this.pbk,
        shortId = this.sid,
        spiderX = this.spx,
        flow = this.flow,
        encryption = if (this.encryption.isNotEmpty()) this.encryption else "none",
        path = this.path,
        host = this.host,
        serviceName = if (this.serviceName.isNotEmpty()) this.serviceName else this.path,
        cipher = if (proto == "shadowsocks") this.path else "",
        pinnedPeerCertSha256 = this.pinnedPeerCertSha256,
        rawJsonConfig = if (this.fullJsonConfig.isNotEmpty()) this.fullJsonConfig else null
    )
}
