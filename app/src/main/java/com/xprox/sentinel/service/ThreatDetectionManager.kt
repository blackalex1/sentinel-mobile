package com.xprox.sentinel.service

import android.content.Context
import android.content.Intent
import android.util.Log
import com.xprox.sentinel.config.XrayProfilePersistence
import com.xprox.sentinel.core.SentinelCore
import com.xprox.sentinel.core.models.AndroidAuditRequest
import com.xprox.sentinel.log.LogManager
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThreatDetectionManager {
    private const val TAG = "ThreatDetectionManager"

    // Map of package name to trigger timestamp
    private val triggerTimes = ConcurrentHashMap<String, Long>()

    // Bounded map of app/destination to last log timestamp to throttle disk writing
    private val lastLogTimes = ConcurrentHashMap<String, Long>()

    fun getTriggerTime(packageName: String): Long? {
        return triggerTimes[packageName]
    }

    fun isAnyAppCapturingPcap(): Boolean {
        val currentTime = System.currentTimeMillis()
        for ((pkg, triggerTime) in triggerTimes) {
            val isBlockedOrFlagged = blockedApps.contains(pkg) || flaggedSystemApps.contains(pkg)
            if (isBlockedOrFlagged && (currentTime - triggerTime <= 300000L)) {
                return true
            }
        }
        return false
    }

    const val THRESHOLD = 2 // Max 2 requests allowed per minute for non-web ports

    // Connection attempts record map
    private val connectionAttempts = ConcurrentHashMap<String, MutableList<ConnectionRecord>>()

    // In-memory set of actively blocked applications
    private val blockedApps = ConcurrentHashMap.newKeySet<String>()

    // In-memory set of blocked destination IPs/domains for Xray mode
    private val blockedDestinations = ConcurrentHashMap.newKeySet<String>()

    fun getBlockedDestinations(): List<String> {
        return blockedDestinations.toList()
    }

    // In-memory set of blocked ports for Xray mode
    private val blockedPorts = ConcurrentHashMap.newKeySet<Int>()

    fun getBlockedPorts(): List<Int> {
        return blockedPorts.toList()
    }

    private val _blockedAppsFlow = MutableStateFlow<List<String>>(emptyList())
    val blockedAppsFlow: StateFlow<List<String>> = _blockedAppsFlow.asStateFlow()

    // Flagged suspicious system applications that bypassed isolation
    private val flaggedSystemApps = ConcurrentHashMap.newKeySet<String>()

    private val _flaggedSystemAppsFlow = MutableStateFlow<List<String>>(emptyList())
    val flaggedSystemAppsFlow: StateFlow<List<String>> = _flaggedSystemAppsFlow.asStateFlow()

    /**
     * Initializes the threat manager by loading previously blocked apps from persistent store.
     */
    fun init(context: Context) {
        try {
            val saved = XrayProfilePersistence.loadBlockedApps(context)
            blockedApps.clear()
            blockedApps.addAll(saved)
            _blockedAppsFlow.value = blockedApps.toList()

            // Sync into Sentinel-Core native engine
            saved.forEach { pkg ->
                SentinelCore.blockApp(context, pkg)
            }
            Log.i(TAG, "Initialized and loaded ${blockedApps.size} blackholed applications into native core")

            val savedSystem = XrayProfilePersistence.loadFlaggedSystemApps(context)
            flaggedSystemApps.clear()
            flaggedSystemApps.addAll(savedSystem)
            _flaggedSystemAppsFlow.value = flaggedSystemApps.toList()
            Log.i(TAG, "Initialized and loaded ${flaggedSystemApps.size} flagged system applications from persistence")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load blocked/flagged apps during init", e)
        }
    }

    /**
     * Checks if an application is actively blackholed.
     */
    fun isAppBlocked(packageName: String): Boolean {
        return blockedApps.contains(packageName) || SentinelCore.isAppBlocked(null, packageName)
    }

    /**
     * Returns the complete list of blocked packages.
     */
    fun getBlockedAppsList(): List<String> {
        return blockedApps.toList()
    }

    /**
     * Programmatically blackholes an application.
     */
    fun blockApp(context: Context, packageName: String) {
        if (packageName == context.packageName) return // Prevent self-block deadlock

        if (blockedApps.add(packageName)) {
            SentinelCore.blockApp(context, packageName)
            XrayProfilePersistence.saveBlockedApps(context, blockedApps)
            _blockedAppsFlow.value = blockedApps.toList()
            Log.w(TAG, "Application manually blocked: $packageName")
        }
    }

    /**
     * Programmatically unblocks an application, resetting its counters.
     */
    fun unblockApp(context: Context, packageName: String) {
        if (blockedApps.remove(packageName)) {
            SentinelCore.unblockApp(context, packageName)
            XrayProfilePersistence.saveBlockedApps(context, blockedApps)
            blockedDestinations.clear() // Clear Xray destination blocks
            blockedPorts.clear()        // Clear Xray port blocks
            // Remove throttled log times matching this package
            val keysToRemove = lastLogTimes.keys().asSequence().filter { it.startsWith("$packageName:") }
            keysToRemove.forEach { lastLogTimes.remove(it) }
            _blockedAppsFlow.value = blockedApps.toList()
            Log.i(TAG, "Application unblocked and cleared counters: $packageName")

            // Delete dynamic threat forensic files to keep disk clean
            ThreatForensics.deleteThreatReport(context, packageName)
        }
    }

    /**
     * Dismisses (hides) a flagged system application from the UI list, but keeps its logs intact on disk.
     */
    fun dismissFlaggedSystemApp(context: Context, packageName: String) {
        if (flaggedSystemApps.remove(packageName)) {
            XrayProfilePersistence.saveFlaggedSystemApps(context, flaggedSystemApps)
            triggerTimes.remove(packageName)
            _flaggedSystemAppsFlow.value = flaggedSystemApps.toList()
            Log.i(TAG, "Flagged system app warning dismissed from UI: $packageName")
        }
    }

    /**
     * Intercepts connection attempts and audits them via Sentinel-Core native engine.
     * Returns true if the connection belongs to a blocked application or has just triggered a block.
     */
    fun registerConnectionAttempt(
        context: Context,
        packageName: String,
        appName: String,
        destinationIp: String,
        port: Int,
        protocol: String = "TCP",
        ipLength: Int = 0,
        ttl: Int = 0,
        ipFlags: String = "N/A",
        tcpFlags: String = "N/A",
        tcpSeq: Long = 0L,
        tcpAck: Long = 0L,
        tcpWindow: Int = 0,
        rawBytes: ByteArray? = null
    ): Boolean {
        // Prevent self-block deadlock
        if (packageName == context.packageName) {
            return false
        }

        // Active audit ports from user preferences (if empty, audits all non-web ports)
        val activePorts = LogManager.loadActivePorts(context).toList()

        val customBlockedRules = try {
            XrayProfilePersistence.loadCustomBlockRules(context)
        } catch (e: Exception) {
            emptyList<String>()
        }
        val isExplicitBlock = (destinationIp.isNotEmpty() && (blockedDestinations.contains(destinationIp) || customBlockedRules.contains(destinationIp)))

        val req = AndroidAuditRequest(
            packageName = packageName,
            appName = appName,
            destinationIp = destinationIp,
            port = port,
            protocol = protocol,
            ipLength = ipLength,
            ttl = ttl,
            ipFlags = ipFlags,
            tcpFlags = tcpFlags,
            tcpSeq = tcpSeq,
            tcpAck = tcpAck,
            tcpWindow = tcpWindow,
            auditPorts = if (activePorts.isNotEmpty()) activePorts else null,
            maxThreshold = THRESHOLD,
            isExplicitBlock = isExplicitBlock
        )

        val record = ConnectionRecord(
            timestamp = System.currentTimeMillis(),
            destinationIp = destinationIp,
            port = port,
            protocol = protocol,
            ipLength = ipLength,
            ttl = ttl,
            ipFlags = ipFlags,
            tcpFlags = tcpFlags,
            tcpSeq = tcpSeq,
            tcpAck = tcpAck,
            tcpWindow = tcpWindow,
            rawBytes = rawBytes
        )
        val attempts = connectionAttempts.getOrPut(packageName) { java.util.Collections.synchronizedList(mutableListOf()) }
        synchronized(attempts) {
            attempts.removeAll { it.timestamp < System.currentTimeMillis() - 60000L }
            attempts.add(record)
        }

        // Native Sentinel-Core audit call
        val verdict = SentinelCore.auditConnection(context, req)

        // 1. If actively blackholed or destination is blocked, log blocked traffic and drop
        if ((verdict.isBlocked || isExplicitBlock) && !verdict.shouldBlock) {
            val isStandardDnsOrWeb = port == 53 || port == 80 || port == 443 || port == 853 ||
                destinationIp == "8.8.8.8" || destinationIp == "8.8.4.4" ||
                destinationIp == "1.1.1.1" || destinationIp == "1.0.0.1" ||
                destinationIp.startsWith("2001:4860:") || destinationIp.startsWith("2606:4700:") ||
                destinationIp == "127.0.0.1" || destinationIp == "localhost"
            if (!isStandardDnsOrWeb) {
                blockedDestinations.add(destinationIp)
                blockedPorts.add(port)
            }

            val logKey = "$packageName:$destinationIp:$port"
            val lastTime = lastLogTimes[logKey] ?: 0L
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTime >= 1000L) {
                lastLogTimes[logKey] = currentTime
                ThreatForensics.logBlockedTraffic(
                    context, packageName, appName, destinationIp, port,
                    protocol = protocol, ipLength = ipLength, ttl = ttl,
                    ipFlags = ipFlags, tcpFlags = tcpFlags, tcpSeq = tcpSeq,
                    tcpAck = tcpAck, tcpWindow = tcpWindow
                )
            }

            // Append blocked packet to PCAP if within 5-minute capture window
            val triggerTime = triggerTimes[packageName]
            if (triggerTime != null && System.currentTimeMillis() - triggerTime <= 300000L) {
                PacketForensics.writeTcpPayloadToPcap(
                    context = context,
                    packageName = packageName,
                    srcIp = "10.0.0.2",
                    srcPort = 0,
                    dstIp = destinationIp,
                    dstPort = port,
                    seq = tcpSeq,
                    ack = tcpAck,
                    flags = 0x18.toByte(),
                    payload = rawBytes ?: ByteArray(0),
                    timestampMs = System.currentTimeMillis()
                )
            }

            return true
        }

        // 2. Suspicious System App Trigger (Bypasses blackhole to prevent OS freeze)
        if (verdict.isSystemFlagged) {
            Log.w(TAG, "SUSPICIOUS SYSTEM ACTIVITY! System App $appName ($packageName) flagged by native engine. Isolation bypassed.")

            val triggerTime = System.currentTimeMillis()
            triggerTimes[packageName] = triggerTime

            val isNewlyFlagged = flaggedSystemApps.add(packageName)
            if (isNewlyFlagged) {
                XrayProfilePersistence.saveFlaggedSystemApps(context, flaggedSystemApps)
                _flaggedSystemAppsFlow.value = flaggedSystemApps.toList()
            }

            val records = listOf(
                ConnectionRecord(
                    timestamp = triggerTime,
                    destinationIp = destinationIp,
                    port = port,
                    protocol = protocol,
                    ipLength = ipLength,
                    ttl = ttl,
                    ipFlags = ipFlags,
                    tcpFlags = tcpFlags,
                    tcpSeq = tcpSeq,
                    tcpAck = tcpAck,
                    tcpWindow = tcpWindow,
                    rawBytes = rawBytes
                )
            )

            ThreatForensics.generateForensicReport(context, packageName, appName, destinationIp, port, records, isSystemBypassed = true)

            // Write trigger flow to PCAP
            try {
                val pcapFile = File(File(context.filesDir, "threats"), "report_${packageName}.pcap")
                if (isNewlyFlagged) {
                    pcapFile.delete()
                }
                PacketForensics.writeTcpPayloadToPcap(
                    context = context,
                    packageName = packageName,
                    srcIp = "10.0.0.2",
                    srcPort = 0,
                    dstIp = destinationIp,
                    dstPort = port,
                    seq = tcpSeq,
                    ack = tcpAck,
                    flags = 0x02.toByte(),
                    payload = rawBytes ?: ByteArray(0),
                    timestampMs = triggerTime
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write system trigger flow to PCAP", e)
            }

            ThreatNotificationHelper.showSystemSecurityAlertNotification(context, appName, packageName, port)
            return false
        }

        // 3. User App Threat Breach -> Blackhole Quarantine
        if (verdict.shouldBlock || verdict.action == "BLOCK") {
            Log.e(TAG, "THREAT DETECTED by Sentinel-Core! App $appName ($packageName) triggered action: ${verdict.action}")

            val triggerTime = System.currentTimeMillis()
            triggerTimes[packageName] = triggerTime

            blockedApps.add(packageName)
            val isStandardDnsOrWeb = port == 53 || port == 80 || port == 443 || port == 853 ||
                destinationIp == "8.8.8.8" || destinationIp == "8.8.4.4" ||
                destinationIp == "1.1.1.1" || destinationIp == "1.0.0.1" ||
                destinationIp.startsWith("2001:4860:") || destinationIp.startsWith("2606:4700:") ||
                destinationIp == "127.0.0.1" || destinationIp == "localhost"
            if (!isStandardDnsOrWeb) {
                blockedDestinations.add(destinationIp)
                blockedPorts.add(port)
            }
            XrayProfilePersistence.saveBlockedApps(context, blockedApps)
            _blockedAppsFlow.value = blockedApps.toList()

            val records = listOf(
                ConnectionRecord(
                    timestamp = triggerTime,
                    destinationIp = destinationIp,
                    port = port,
                    protocol = protocol,
                    ipLength = ipLength,
                    ttl = ttl,
                    ipFlags = ipFlags,
                    tcpFlags = tcpFlags,
                    tcpSeq = tcpSeq,
                    tcpAck = tcpAck,
                    tcpWindow = tcpWindow,
                    rawBytes = rawBytes
                )
            )

            // Generate forensic reports and threat analysis files
            ThreatForensics.generateForensicReport(context, packageName, appName, destinationIp, port, records, isSystemBypassed = false)

            ThreatForensics.logBlockedTraffic(
                context, packageName, appName, destinationIp, port, isTrigger = true,
                protocol = protocol, ipLength = ipLength, ttl = ttl,
                ipFlags = ipFlags, tcpFlags = tcpFlags, tcpSeq = tcpSeq,
                tcpAck = tcpAck, tcpWindow = tcpWindow
            )

            // Write trigger flow to PCAP
            try {
                File(File(context.filesDir, "threats"), "report_${packageName}.pcap").delete()
                PacketForensics.writeTcpPayloadToPcap(
                    context = context,
                    packageName = packageName,
                    srcIp = "10.0.0.2",
                    srcPort = 0,
                    dstIp = destinationIp,
                    dstPort = port,
                    seq = tcpSeq,
                    ack = tcpAck,
                    flags = 0x02.toByte(),
                    payload = rawBytes ?: ByteArray(0),
                    timestampMs = triggerTime
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write trigger flow to PCAP", e)
            }

            ThreatNotificationHelper.showSecurityAlertNotification(context, appName, packageName)
            return true
        }

        return false
    }

    /**
     * Resets all in-memory threat states, blackholes, and native Sentinel-Core engine state.
     */
    fun clearThreats(context: Context? = null) {
        blockedApps.clear()
        flaggedSystemApps.clear()
        blockedDestinations.clear()
        blockedPorts.clear()
        triggerTimes.clear()
        lastLogTimes.clear()
        connectionAttempts.clear()
        _blockedAppsFlow.value = emptyList()
        _flaggedSystemAppsFlow.value = emptyList()
        if (context != null) {
            XrayProfilePersistence.saveBlockedApps(context, emptySet())
            XrayProfilePersistence.saveFlaggedSystemApps(context, emptySet())
        }
        SentinelCore.clearThreats()
    }

    /**
     * Reads all logged blocked traffic for a given package name.
     */
    fun readThreatLogs(context: Context, packageName: String): List<String> {
        return ThreatForensics.readThreatLogs(context, packageName)
    }

    /**
     * Returns the human-readable text forensic report path if it exists.
     */
    fun getForensicReportFile(context: Context, packageName: String): File? {
        return ThreatForensics.getForensicReportFile(context, packageName)
    }

    /**
     * Returns the binary PCAP report path if it exists.
     */
    fun getPcapReportFile(context: Context, packageName: String): File? {
        return ThreatForensics.getPcapReportFile(context, packageName)
    }
}
