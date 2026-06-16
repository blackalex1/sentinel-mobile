package com.xprox.sentinel.service

import android.content.Context
import android.content.Intent
import android.util.Log
import com.xprox.sentinel.config.XrayProfilePersistence
import com.xprox.sentinel.log.LogManager
import java.io.File
import java.util.Collections
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

    private const val WINDOW_MS = 60000L // 1-minute sliding window
    const val THRESHOLD = 2      // Max 2 requests allowed per minute

    // Active package connection timestamps for sliding window
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
            Log.i(TAG, "Initialized and loaded ${blockedApps.size} blackholed applications from persistence")

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
        return blockedApps.contains(packageName)
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
            XrayProfilePersistence.saveBlockedApps(context, blockedApps)
            _blockedAppsFlow.value = blockedApps.toList()
            Log.w(TAG, "Application manually blocked: $packageName")
            
            // Trigger VPN/Xray configuration update
            triggerVpnRebuild(context)
        }
    }

    /**
     * Programmatically unblocks an application, resetting its counters.
     */
    fun unblockApp(context: Context, packageName: String) {
        if (blockedApps.remove(packageName)) {
            XrayProfilePersistence.saveBlockedApps(context, blockedApps)
            connectionAttempts.remove(packageName)
            blockedDestinations.clear() // Clear Xray destination blocks
            blockedPorts.clear()        // Clear Xray port blocks
            // Remove throttled log times matching this package
            val keysToRemove = lastLogTimes.keys().asSequence().filter { it.startsWith("$packageName:") }
            keysToRemove.forEach { lastLogTimes.remove(it) }
            _blockedAppsFlow.value = blockedApps.toList()
            Log.i(TAG, "Application unblocked and cleared counters: $packageName")
            
            // Delete dynamic threat forensic files to keep disk clean
            ThreatForensics.deleteThreatReport(context, packageName)

            // Trigger VPN/Xray configuration update
            triggerVpnRebuild(context)
        }
    }

    /**
     * Dismisses (hides) a flagged system application from the UI list, but keeps its logs intact on disk.
     */
    fun dismissFlaggedSystemApp(context: Context, packageName: String) {
        if (flaggedSystemApps.remove(packageName)) {
            XrayProfilePersistence.saveFlaggedSystemApps(context, flaggedSystemApps)
            triggerTimes.remove(packageName)
            connectionAttempts.remove(packageName)
            _flaggedSystemAppsFlow.value = flaggedSystemApps.toList()
            Log.i(TAG, "Flagged system app warning dismissed from UI: $packageName")
        }
    }

    /**
     * Intercepts connection attempts and registers them.
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

        val resolvedBytes = rawBytes ?: PacketForensics.synthesizePacket(protocol, destinationIp, port)

        // If flagged system app, write to pcap and return early if within the active 5-minute capture window
        // This prevents threshold checks and notification spam during the capture
        if (flaggedSystemApps.contains(packageName)) {
            val triggerTime = triggerTimes[packageName]
            if (triggerTime != null && System.currentTimeMillis() - triggerTime <= 300000L) {
                PacketForensics.writePacketToPcap(context, packageName, resolvedBytes, System.currentTimeMillis())
                return false
            }
        }

        // 1. If already blackholed, write to isolated log, write to pcap if in window, and drop immediately (FOR ALL PORTS!)
        if (isAppBlocked(packageName)) {
            blockedDestinations.add(destinationIp)
            blockedPorts.add(port)
            
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
                PacketForensics.writePacketToPcap(context, packageName, resolvedBytes, System.currentTimeMillis())
            }
            
            return true
        }

        // 2. Dynamic Audit Ports check: Only monitor ports explicitly enabled in user settings
        val activePorts = LogManager.loadActivePorts(context)
        if (!activePorts.contains(port)) {
            return false
        }

        // 3. Sliding Window Cooldown Calculation (1 minute)
        val currentTime = System.currentTimeMillis()
        val attempts = connectionAttempts.getOrPut(packageName) { Collections.synchronizedList(mutableListOf()) }

        synchronized(attempts) {
            // Remove attempts older than 1 minute
            attempts.removeAll { it.timestamp < currentTime - WINDOW_MS }

            // Register current connection attempt
            attempts.add(
                ConnectionRecord(
                    currentTime, destinationIp, port,
                    protocol = protocol, ipLength = ipLength, ttl = ttl,
                    ipFlags = ipFlags, tcpFlags = tcpFlags, tcpSeq = tcpSeq,
                    tcpAck = tcpAck, tcpWindow = tcpWindow,
                    rawBytes = resolvedBytes
                )
            )

            // 4. Check Threat Threshold
            if (attempts.size > THRESHOLD) {
                // Determine if this is a critical system/kernel package
                val isSystemPackage = packageName == "android.system.kernel" || 
                                     packageName.startsWith("android.system.") || 
                                     packageName.startsWith("android.uid.") || 
                                     packageName == "android" ||
                                     packageName.startsWith("unknown.uid.")

                if (isSystemPackage) {
                    Log.w(TAG, "SUSPICIOUS SYSTEM ACTIVITY! System App $appName ($packageName) made ${attempts.size} requests to port $port within 1 minute. Isolation bypassed to prevent system lockout.")
                    
                    val triggerTime = System.currentTimeMillis()
                    triggerTimes[packageName] = triggerTime

                    // Add to flagged system apps for UI display
                    val isNewlyFlagged = flaggedSystemApps.add(packageName)
                    if (isNewlyFlagged) {
                        XrayProfilePersistence.saveFlaggedSystemApps(context, flaggedSystemApps)
                        _flaggedSystemAppsFlow.value = flaggedSystemApps.toList()
                    }

                    // Generate special forensic report for system package
                    ThreatForensics.generateForensicReport(context, packageName, appName, destinationIp, port, attempts.toList(), isSystemBypassed = true)
                    
                    // Write all buffered pre-trigger packet bytes and trigger packet bytes to PCAP file!
                    try {
                        val pcapFile = File(File(context.filesDir, "threats"), "report_${packageName}.pcap")
                        if (isNewlyFlagged) {
                            pcapFile.delete()
                        }
                        attempts.forEach { record ->
                            val bytes = record.rawBytes ?: PacketForensics.synthesizePacket(record.protocol, record.destinationIp, record.port)
                            PacketForensics.writePacketToPcap(context, packageName, bytes, record.timestamp)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to write system trigger flow to PCAP", e)
                    }

                    // Post system-alert notification warning about system app compromise
                    ThreatNotificationHelper.showSystemSecurityAlertNotification(context, appName, packageName, port)
                    
                    return false // Do NOT block, allow connection
                } else {
                    Log.e(TAG, "THREAT DETECTED! App $appName ($packageName) made ${attempts.size} requests to port $port within 1 minute.")
                    
                    // Store the trigger time first
                    val triggerTime = System.currentTimeMillis()
                    triggerTimes[packageName] = triggerTime

                    // Trigger instant blackhole
                    blockedApps.add(packageName)
                    blockedDestinations.add(destinationIp)
                    blockedPorts.add(port)
                    XrayProfilePersistence.saveBlockedApps(context, blockedApps)
                    _blockedAppsFlow.value = blockedApps.toList()

                    // Generate forensic reports and threat analysis files passing all pre-trigger attempts
                    ThreatForensics.generateForensicReport(context, packageName, appName, destinationIp, port, attempts.toList(), isSystemBypassed = false)

                    // Write the trigger log entry in the isolated threat log
                    ThreatForensics.logBlockedTraffic(
                        context, packageName, appName, destinationIp, port, isTrigger = true,
                        protocol = protocol, ipLength = ipLength, ttl = ttl,
                        ipFlags = ipFlags, tcpFlags = tcpFlags, tcpSeq = tcpSeq,
                        tcpAck = tcpAck, tcpWindow = tcpWindow
                    )

                    // Write all buffered pre-trigger packet bytes and trigger packet bytes to PCAP file!
                    try {
                        File(File(context.filesDir, "threats"), "report_${packageName}.pcap").delete()
                        attempts.forEach { record ->
                            val bytes = record.rawBytes ?: PacketForensics.synthesizePacket(record.protocol, record.destinationIp, record.port)
                            PacketForensics.writePacketToPcap(context, packageName, bytes, record.timestamp)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to write trigger flow to PCAP", e)
                    }

                    // Post high-priority security alert system notification
                    ThreatNotificationHelper.showSecurityAlertNotification(context, appName, packageName)

                    // Re-compile VPN routing / Xray config to apply blacklist immediately
                    triggerVpnRebuild(context)

                    return true
                }
            }
        }

        return false
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

    /**
     * Restarts/reloads the VPN configuration dynamically.
     */
    private fun triggerVpnRebuild(context: Context) {
        try {
            val isVpnActive = VpnManagerService.isRunningFlow.value
            if (isVpnActive) {
                Log.i(TAG, "Rebuilding VPN and Xray configuration to apply new blackhole changes dynamically")
                val intent = Intent(context, VpnManagerService::class.java).apply {
                    action = VpnManagerService.ACTION_RELOAD_CONFIG
                }
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rebuild VPN interface dynamically", e)
        }
    }
}
