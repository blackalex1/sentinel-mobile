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
        fun SentinelAndroidAuditConnection(reqJson: String): Pointer?
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
        fun SentinelFreeString(str: Pointer?)
    }

    @Volatile
    private var activeLib: SentinelCoreLib? = null
    @Volatile
    private var isInitialized = false

    private fun getOrLoadLib(context: Context? = null): SentinelCoreLib? {
        if (isInitialized && activeLib != null) return activeLib
        synchronized(this) {
            if (isInitialized && activeLib != null) return activeLib

            val candidates = mutableListOf<String>()

            // 1. Priority 1: Check downloaded custom core in app private storage if available
            context?.let { ctx ->
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

    private val tunConnectionRegex = Regex("""from\s+(tcp|udp):([\w.-]+):(\d+)\s+accepted\s+(tcp|udp):([\w.-]+):(\d+)""", RegexOption.IGNORE_CASE)
    private val connectionAcceptedRegex = Regex("""connection\s+accepted\s+from\s+([\w.-]+):(\d+)""", RegexOption.IGNORE_CASE)
    private val ipPortRegex = Regex("""(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}):(\d+)""")

    /**
     * High performance Xray connection log parser driven by Sentinel-Core native engine with Regex fallback.
     */
    fun parseConnectionLog(line: String): ParsedConnectionLog? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || !trimmed.contains("accepted", ignoreCase = true)) return null

        return fallbackParseConnectionLog(trimmed)
    }

    private fun fallbackParseConnectionLog(line: String): ParsedConnectionLog? {
        try {
            val tunMatch = tunConnectionRegex.find(line)
            if (tunMatch != null) {
                val proto = tunMatch.groupValues[1].uppercase()
                val srcIp = tunMatch.groupValues[2]
                val srcPort = tunMatch.groupValues[3].toIntOrNull() ?: 0
                val destIp = tunMatch.groupValues[5]
                val destPort = tunMatch.groupValues[6].toIntOrNull() ?: 0
                return ParsedConnectionLog(proto, srcIp, srcPort, destIp, destPort)
            }

            val match = connectionAcceptedRegex.find(line)
            if (match != null) {
                val srcIp = match.groupValues[1]
                val srcPort = match.groupValues[2].toIntOrNull() ?: 0

                val allMatches = ipPortRegex.findAll(line).toList()
                var destIp = "0.0.0.0"
                var destPort = 80

                for (m in allMatches) {
                    val ip = m.groupValues[1]
                    val port = m.groupValues[2].toIntOrNull() ?: 0
                    if (ip != srcIp && port != srcPort) {
                        destIp = ip
                        destPort = port
                        break
                    }
                }

                if (destIp == "0.0.0.0" && allMatches.isNotEmpty()) {
                    val destMatch = Regex("""(tcp|udp):([\w.-]+):(\d+)""", RegexOption.IGNORE_CASE).find(line)
                    if (destMatch != null) {
                        destIp = destMatch.groupValues[2]
                        destPort = destMatch.groupValues[3].toIntOrNull() ?: 0
                    }
                }

                val proto = if (line.contains("udp", ignoreCase = true)) "UDP" else "TCP"
                return ParsedConnectionLog(proto, srcIp, srcPort, destIp, destPort)
            }
        } catch (e: Exception) {
            // Fail safe
        }
        return null
    }

    private val fallbackBlockedApps = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val fallbackAttempts = java.util.concurrent.ConcurrentHashMap<String, MutableList<Long>>()

    /**
     * Audits an incoming socket connection or packet header via the high-performance native Go engine.
     */
    fun auditConnection(context: Context? = null, request: AndroidAuditRequest): AndroidAuditVerdict {
        if (context != null && !isInitialized) getOrLoadLib(context)
        return try {
            val reqJson = SentinelJson.encodeToString(AndroidAuditRequest.serializer(), request)
            val respJson = callNative { it.SentinelAndroidAuditConnection(reqJson) }
            if (respJson != null) {
                SentinelJson.decodeFromString(AndroidAuditVerdict.serializer(), respJson)
            } else {
                fallbackAuditConnection(request)
            }
        } catch (e: Exception) {
            fallbackAuditConnection(request)
        }
    }

    private fun fallbackAuditConnection(req: AndroidAuditRequest): AndroidAuditVerdict {
        val isBlocked = fallbackBlockedApps.contains(req.packageName)
        if (isBlocked) {
            return AndroidAuditVerdict(
                isBlocked = true,
                shouldBlock = true,
                threatDetected = true,
                threatType = "MALWARE_C2_SUSPECT",
                action = "BLOCK",
                riskScore = 100
            )
        }

        if (req.auditPorts != null && !req.auditPorts.contains(req.port)) {
            return AndroidAuditVerdict(action = "ALLOW")
        }

        val isWeb = req.port == 80 || req.port == 443 || req.port == 53
        val now = System.currentTimeMillis()
        val attempts = fallbackAttempts.getOrPut(req.packageName) { java.util.Collections.synchronizedList(mutableListOf()) }
        synchronized(attempts) {
            attempts.removeAll { it < now - 60000L }
            attempts.add(now)

            val threshold = if (req.maxThreshold > 0) req.maxThreshold else 2
            if (attempts.size > threshold && !isWeb) {
                val isSystem = req.packageName == "android" ||
                    req.packageName == "android.system.kernel" ||
                    req.packageName.startsWith("android.system.") ||
                    req.packageName.startsWith("android.uid.") ||
                    req.packageName.startsWith("unknown.uid.")

                if (isSystem) {
                    return AndroidAuditVerdict(
                        isBlocked = false,
                        shouldBlock = false,
                        isSystemFlagged = true,
                        threatDetected = true,
                        threatType = "HIGH_FREQUENCY_PROBE",
                        action = "FLAG_SYSTEM",
                        riskScore = 50,
                        attemptsCount = attempts.size
                    )
                } else {
                    fallbackBlockedApps.add(req.packageName)
                    return AndroidAuditVerdict(
                        isBlocked = true,
                        shouldBlock = true,
                        threatDetected = true,
                        threatType = "HIGH_FREQUENCY_PROBE",
                        action = "BLOCK",
                        riskScore = 100,
                        attemptsCount = attempts.size
                    )
                }
            }
        }

        return AndroidAuditVerdict(
            isBlocked = false,
            shouldBlock = false,
            threatDetected = false,
            action = "ALLOW",
            attemptsCount = attempts.size
        )
    }

    /**
     * Writes raw packet bytes to a Wireshark-compatible PCAP file via the native engine.
     */
    fun writePcapPacket(context: Context? = null, filePath: String, rawBytes: ByteArray, timestampMs: Long = System.currentTimeMillis()): Boolean {
        if (context != null && !isInitialized) getOrLoadLib(context)
        return try {
            val hexStr = rawBytes.joinToString("") { "%02x".format(it) }
            val resp = callNative { it.SentinelAndroidWritePcap(filePath, hexStr, timestampMs) }
            if (resp?.contains("\"success\":true") == true || resp?.contains("\"success\": true") == true) {
                true
            } else {
                fallbackWritePcap(filePath, rawBytes, timestampMs)
            }
        } catch (e: Exception) {
            fallbackWritePcap(filePath, rawBytes, timestampMs)
        }
    }

    private fun fallbackWritePcap(filePath: String, packetBytes: ByteArray, timestampMs: Long): Boolean {
        return try {
            val file = java.io.File(filePath)
            file.parentFile?.mkdirs()
            val exists = file.exists()
            java.io.FileOutputStream(file, true).use { stream ->
                if (!exists) {
                    val globalHeader = byteArrayOf(
                        0xd4.toByte(), 0xc3.toByte(), 0xb2.toByte(), 0xa1.toByte(),
                        0x02.toByte(), 0x00.toByte(),
                        0x04.toByte(), 0x00.toByte(),
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
                        0xff.toByte(), 0xff.toByte(), 0x00.toByte(), 0x00.toByte(),
                        0x65.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()
                    )
                    stream.write(globalHeader)
                }
                val sec = timestampMs / 1000
                val usec = (timestampMs % 1000) * 1000
                val len = packetBytes.size
                val pcapHeader = ByteArray(16)
                pcapHeader[0] = (sec and 0xFF).toByte()
                pcapHeader[1] = ((sec shr 8) and 0xFF).toByte()
                pcapHeader[2] = ((sec shr 16) and 0xFF).toByte()
                pcapHeader[3] = ((sec shr 24) and 0xFF).toByte()
                pcapHeader[4] = (usec and 0xFF).toByte()
                pcapHeader[5] = ((usec shr 8) and 0xFF).toByte()
                pcapHeader[6] = ((usec shr 16) and 0xFF).toByte()
                pcapHeader[7] = ((usec shr 24) and 0xFF).toByte()
                pcapHeader[8] = (len and 0xFF).toByte()
                pcapHeader[9] = ((len shr 8) and 0xFF).toByte()
                pcapHeader[10] = ((len shr 16) and 0xFF).toByte()
                pcapHeader[11] = ((len shr 24) and 0xFF).toByte()
                pcapHeader[12] = (len and 0xFF).toByte()
                pcapHeader[13] = ((len shr 8) and 0xFF).toByte()
                pcapHeader[14] = ((len shr 16) and 0xFF).toByte()
                pcapHeader[15] = ((len shr 24) and 0xFF).toByte()
                stream.write(pcapHeader)
                stream.write(packetBytes)
            }
            true
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
            if (resp?.contains("\"success\":true") == true || resp?.contains("\"success\": true") == true) {
                true
            } else {
                fallbackSynthesizeAndWritePcap(filePath, proto, srcIP, srcPort, dstIP, dstPort, tcpFlags, seq, ack, window, payload, timestampMs)
            }
        } catch (e: Exception) {
            fallbackSynthesizeAndWritePcap(filePath, proto, srcIP, srcPort, dstIP, dstPort, tcpFlags, seq, ack, window, payload, timestampMs)
        }
    }

    private fun fallbackSynthesizeAndWritePcap(
        filePath: String, proto: String, srcIP: String, srcPort: Int,
        dstIP: String, dstPort: Int, tcpFlags: Int, seq: Long, ack: Long,
        window: Int, payload: ByteArray?, timestampMs: Long
    ): Boolean {
        val payloadBytes = payload ?: ByteArray(0)
        val isTcp = proto.equals("TCP", ignoreCase = true)
        val ipProto = if (isTcp) 6 else 17
        val srcBytes = try { java.net.InetAddress.getByName(srcIP).address } catch (e: Exception) { byteArrayOf(10, 0, 0, 2) }
        val dstBytes = try { java.net.InetAddress.getByName(dstIP).address } catch (e: Exception) { byteArrayOf(8, 8, 8, 8) }
        val totalLen = 40 + payloadBytes.size
        val packet = ByteArray(totalLen)
        packet[0] = 0x45.toByte()
        packet[1] = 0x00.toByte()
        packet[2] = ((totalLen shr 8) and 0xFF).toByte()
        packet[3] = (totalLen and 0xFF).toByte()
        packet[4] = 0x12.toByte()
        packet[5] = 0x34.toByte()
        packet[6] = 0x40.toByte()
        packet[8] = 64.toByte()
        packet[9] = ipProto.toByte()
        System.arraycopy(srcBytes, 0, packet, 12, 4)
        System.arraycopy(dstBytes, 0, packet, 16, 4)
        if (isTcp) {
            val sp = if (srcPort > 0) srcPort else 50000
            packet[20] = ((sp shr 8) and 0xFF).toByte()
            packet[21] = (sp and 0xFF).toByte()
            packet[22] = ((dstPort shr 8) and 0xFF).toByte()
            packet[23] = (dstPort and 0xFF).toByte()
            packet[24] = ((seq shr 24) and 0xFF).toByte()
            packet[25] = ((seq shr 16) and 0xFF).toByte()
            packet[26] = ((seq shr 8) and 0xFF).toByte()
            packet[27] = (seq and 0xFF).toByte()
            packet[28] = ((ack shr 24) and 0xFF).toByte()
            packet[29] = ((ack shr 16) and 0xFF).toByte()
            packet[30] = ((ack shr 8) and 0xFF).toByte()
            packet[31] = (ack and 0xFF).toByte()
            packet[32] = 0x50.toByte()
            packet[33] = (tcpFlags and 0xFF).toByte()
            packet[34] = 0xFA.toByte()
            packet[35] = 0xF0.toByte()
            if (payloadBytes.isNotEmpty()) {
                System.arraycopy(payloadBytes, 0, packet, 40, payloadBytes.size)
            }
        }
        return fallbackWritePcap(filePath, packet, timestampMs)
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
        fallbackBlockedApps.add(packageName)
        callNative { it.SentinelAndroidBlockApp(packageName) }
    }

    fun unblockApp(context: Context? = null, packageName: String) {
        if (context != null && !isInitialized) getOrLoadLib(context)
        fallbackBlockedApps.remove(packageName)
        fallbackAttempts.remove(packageName)
        callNative { it.SentinelAndroidUnblockApp(packageName) }
    }

    fun isAppBlocked(context: Context? = null, packageName: String): Boolean {
        if (context != null && !isInitialized) getOrLoadLib(context)
        if (fallbackBlockedApps.contains(packageName)) return true
        val resp = callNative { it.SentinelAndroidIsAppBlocked(packageName) } ?: return false
        return resp.contains("\"blocked\":true") || resp.contains("\"blocked\": true")
    }

    fun clearThreats(context: Context? = null) {
        if (context != null && !isInitialized) getOrLoadLib(context)
        fallbackBlockedApps.clear()
        fallbackAttempts.clear()
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

    private val fallbackRingBuffer = java.util.concurrent.ConcurrentLinkedDeque<AndroidLogEntry>()
    private val fallbackTotalLogged = java.util.concurrent.atomic.AtomicLong(0)

    /**
     * Pushes an enriched connection log entry into the native in-memory RingBuffer.
     */
    fun pushLog(entry: AndroidLogEntry): Boolean {
        fallbackTotalLogged.incrementAndGet()
        fallbackRingBuffer.addFirst(entry)
        while (fallbackRingBuffer.size > 5000) {
            fallbackRingBuffer.removeLast()
        }

        val jsonStr = try {
            SentinelJson.encodeToString(com.xprox.sentinel.core.models.AndroidLogEntry.serializer(), entry)
        } catch (e: Exception) {
            return true
        }
        val resp = callNative { it.SentinelAndroidPushLog(jsonStr) }
        return resp != null && resp.contains("\"success\": true")
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

        // Resilient Fallback for non-JNI JVM runtime
        val qLower = query.trim().lowercase()
        val filtered = fallbackRingBuffer.filter { e ->
            if (portFilter > 0 && e.destinationPort != portFilter) return@filter false
            if (qLower.isNotEmpty()) {
                val match = e.appName.lowercase().contains(qLower) ||
                        e.packageName.lowercase().contains(qLower) ||
                        e.destinationIp.lowercase().contains(qLower) ||
                        (e.serviceName?.lowercase()?.contains(qLower) == true) ||
                        e.protocol.lowercase().contains(qLower)
                if (!match) return@filter false
            }
            true
        }

        if (offset >= filtered.size) return emptyList()
        val end = (offset + limit).coerceAtMost(filtered.size)
        return filtered.subList(offset, end)
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

        // Resilient Fallback
        val appCounts = mutableMapOf<String, com.xprox.sentinel.core.models.AppStat>()
        val portCounts = mutableMapOf<Int, Long>()
        val protoBreakdown = mutableMapOf<String, Long>()
        var threatCount = 0L

        fallbackRingBuffer.forEach { e ->
            val pkg = e.packageName.ifEmpty { "unknown" }
            val cur = appCounts.getOrPut(pkg) { com.xprox.sentinel.core.models.AppStat(pkg, e.appName, 0) }
            appCounts[pkg] = cur.copy(count = cur.count + 1)

            if (e.destinationPort > 0) {
                portCounts[e.destinationPort] = (portCounts[e.destinationPort] ?: 0L) + 1
            }

            val proto = e.protocol.uppercase().ifEmpty { "TCP" }
            protoBreakdown[proto] = (protoBreakdown[proto] ?: 0L) + 1

            if (e.threatType.isNotEmpty() && e.threatType != "NONE") {
                threatCount++
            }
        }

        val topApps = appCounts.values.sortedByDescending { it.count }.take(10)
        val topPorts = portCounts.map { (p, c) ->
            com.xprox.sentinel.core.models.PortStat(port = p, serviceName = "Port $p", count = c)
        }.sortedByDescending { it.count }.take(10)

        return AndroidLogStats(
            totalConnections = fallbackTotalLogged.get(),
            activeAppsCount = appCounts.size,
            threatCount = threatCount,
            protocolBreakdown = protoBreakdown,
            topApps = topApps,
            topPorts = topPorts
        )
    }

    /**
     * Clears all log records from the native in-memory RingBuffer.
     */
    fun clearLogs(): Boolean {
        fallbackRingBuffer.clear()
        fallbackTotalLogged.set(0)
        val resp = callNative { it.SentinelAndroidClearLogs() }
        return resp != null
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
