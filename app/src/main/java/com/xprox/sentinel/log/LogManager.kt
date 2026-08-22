package com.xprox.sentinel.log

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles creation and secure logging of network connection requests.
 * Monitors sensitive target ports and records records to 'x_prox_sensitive_connections.log'.
 */
object LogManager {
    private const val TAG = "LogManager"
    private const val LOG_FILE_NAME = "x_prox_sensitive_connections.log"

    private val logExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "sentinel-log-writer").apply {
            priority = Thread.MIN_PRIORITY
        }
    }
    private const val PREFS_NAME = "x_prox_sensitive_ports_prefs"
    private const val KEY_ACTIVE_PORTS = "active_sensitive_ports"

    private val DEFAULT_ACTIVE_PORTS = emptySet<Int>()
    private var activePortsSet: Set<Int> = emptySet()

    private const val KEY_SHIELD_MODE = "security_shield_mode"
    private const val KEY_BLOCK_THRESHOLD = "security_block_threshold"
    private const val KEY_AUTO_PCAP = "security_auto_pcap"

    fun loadShieldMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SHIELD_MODE, "threshold_block") ?: "threshold_block"
    }

    fun saveShieldMode(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SHIELD_MODE, mode).apply()
        syncPolicyWithNativeCore(context)
    }

    fun loadBlockThreshold(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_BLOCK_THRESHOLD, 3)
    }

    fun saveBlockThreshold(context: Context, threshold: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_BLOCK_THRESHOLD, threshold).apply()
        syncPolicyWithNativeCore(context)
    }

    fun loadAutoPcap(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_PCAP, true)
    }

    fun saveAutoPcap(context: Context, auto: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_PCAP, auto).apply()
        syncPolicyWithNativeCore(context)
    }

    fun syncPolicyWithNativeCore(context: Context) {
        try {
            val mode = loadShieldMode(context)
            val blockThreshold = loadBlockThreshold(context)
            val autoPcap = loadAutoPcap(context)
            val activePorts = loadActivePorts(context).toList()
            val pcapDir = File(context.filesDir, "threat_pcaps").apply { mkdirs() }.absolutePath

            val json = org.json.JSONObject().apply {
                put("mode", mode)
                put("block_threshold", blockThreshold)
                put("pcap_threshold", 3)
                put("port_scan_threshold", 5)
                put("window_seconds", 30)
                put("auto_pcap_capture", autoPcap)
                put("pcap_directory", pcapDir)
                put("protected_ports", org.json.JSONArray(activePorts))
            }

            com.xprox.sentinel.core.SentinelCore.configureSecurityPolicy(context, json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync policy with native core", e)
        }
    }

    fun loadCustomPorts(context: Context): Map<Int, String> {
        customPortsMap?.let { return it }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_CUSTOM_PORTS, null)
        if (jsonStr.isNullOrEmpty()) {
            customPortsMap = emptyMap()
            return emptyMap()
        }
        val map = mutableMapOf<Int, String>()
        try {
            val json = org.json.JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val port = key.toIntOrNull()
                if (port != null) {
                    map[port] = json.getString(key)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse custom ports", e)
        }
        customPortsMap = map
        return map
    }

    fun addCustomPort(context: Context, port: Int, serviceName: String) {
        val current = loadCustomPorts(context).toMutableMap()
        current[port] = serviceName
        customPortsMap = current
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = org.json.JSONObject()
        current.forEach { (p, name) -> json.put(p.toString(), name) }
        prefs.edit().putString(KEY_CUSTOM_PORTS, json.toString()).apply()
        
        // Also automatically add it to the active monitored ports!
        val active = loadActivePorts(context).toMutableSet()
        active.add(port)
        saveActivePorts(context, active)
    }

    fun deleteCustomPort(context: Context, port: Int) {
        val current = loadCustomPorts(context).toMutableMap()
        current.remove(port)
        customPortsMap = current
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = org.json.JSONObject()
        current.forEach { (p, name) -> json.put(p.toString(), name) }
        prefs.edit().putString(KEY_CUSTOM_PORTS, json.toString()).apply()
        
        // Also remove it from the active monitored ports
        val active = loadActivePorts(context).toMutableSet()
        active.remove(port)
        saveActivePorts(context, active)
    }

    // Set of known sensitive ports
    val ALL_AVAILABLE_SENSITIVE_PORTS = mapOf(
        21 to "FTP",
        22 to "SSH",
        23 to "Telnet",
        25 to "SMTP",
        53 to "DNS Bypass Attempt",
        80 to "HTTP (Unencrypted Web)",
        110 to "POP3",
        143 to "IMAP",
        443 to "HTTPS",
        445 to "SMB (Windows Share)",
        853 to "DNS over TLS (DoT)",
        3389 to "RDP (Remote Desktop)",
        3306 to "MySQL",
        6379 to "Redis",
        27017 to "MongoDB"
    )

    fun loadActivePorts(context: Context): Set<Int> {
        if (activePortsSet.isNotEmpty()) {
            return activePortsSet
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getStringSet(KEY_ACTIVE_PORTS, null)
        activePortsSet = if (saved != null) {
            saved.mapNotNull { it.toIntOrNull() }.toSet()
        } else {
            DEFAULT_ACTIVE_PORTS
        }
        return activePortsSet
    }

    fun saveActivePorts(context: Context, ports: Set<Int>) {
        activePortsSet = ports
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_ACTIVE_PORTS, ports.map { it.toString() }.toSet()).apply()
    }

    data class LogEntry(
        val timestamp: String,
        val appName: String,
        val packageName: String,
        val destination: String,
        val port: Int,
        val service: String,
        val isSensitive: Boolean
    )

    private val _logFlow = MutableSharedFlow<LogEntry>(replay = 100)
    val logFlow: SharedFlow<LogEntry> = _logFlow.asSharedFlow()

    /**
     * Inspects a connection and logs details if it matches a monitored port.
     */
    fun logConnection(
        context: Context,
        packageName: String,
        appName: String,
        destinationIp: String,
        port: Int,
        // Optional packet parameters:
        protocol: String = "TCP",
        ipLength: Int = 0,
        ttl: Int = 0,
        ipFlags: String = "N/A",
        tcpFlags: String = "N/A",
        tcpSeq: Long = 0L,
        tcpAck: Long = 0L,
        tcpWindow: Int = 0,
        rawBytes: ByteArray? = null
    ) {
        val isThreatBlocked = com.xprox.sentinel.service.ThreatDetectionManager.registerConnectionAttempt(
            context = context,
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
            rawBytes = rawBytes
        )

        val activePorts = loadActivePorts(context)
        val isSensitive = activePorts.contains(port)
        val service = ALL_AVAILABLE_SENSITIVE_PORTS[port] ?: "Unknown Service"
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())

        val logEntry = LogEntry(
            timestamp = timestamp,
            appName = appName,
            packageName = packageName,
            destination = "$destinationIp:$port",
            port = port,
            service = if (isThreatBlocked) "BLOCKED / BLACKHOLED" else service,
            isSensitive = isSensitive || isThreatBlocked
        )

        // Broadcast to dynamic UI flow
        _logFlow.tryEmit(logEntry)

        // Push to native Go zero-allocation RingBuffer
        try {
            com.xprox.sentinel.core.SentinelCore.pushLog(
                com.xprox.sentinel.core.models.AndroidLogEntry(
                    timestamp = System.currentTimeMillis(),
                    packageName = packageName,
                    appName = appName,
                    destinationIp = destinationIp,
                    destinationPort = port,
                    protocol = protocol,
                    serviceName = if (isThreatBlocked) "BLOCKED" else service,
                    action = if (isThreatBlocked) "block" else "direct",
                    threatType = if (isThreatBlocked) "THREAT" else "NONE"
                )
            )
        } catch (e: Throwable) {
            // Ignore native push error
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saveAll = prefs.getBoolean("save_all_logs_to_disk", false)

        // If saveAll is active, OR if it's a sensitive port and NOT blocked, OR if it's a blocked traffic attempt, write it to our dedicated physical log file!
        if (saveAll || (isSensitive && !isThreatBlocked) || isThreatBlocked) {
            writeLogToFile(context, logEntry)
        }
    }

    private fun writeLogToFile(context: Context, entry: LogEntry) {
        val isUnitTest = try {
            !(System.getProperty("java.vm.name") ?: "").contains("Dalvik", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
        val appContext = context.applicationContext ?: context
        if (isUnitTest) {
            writeLogToFileInternal(appContext, entry)
        } else {
            logExecutor.execute {
                writeLogToFileInternal(appContext, entry)
            }
        }
    }

    @Synchronized
    private fun writeLogToFileInternal(context: Context, entry: LogEntry) {
        try {
            val directory = context.filesDir ?: return
            val logFile = File(directory, LOG_FILE_NAME)
            
            // Format log: [TIMESTAMP] [ALERT: SSH] [Telegram (com.telegram.org)] -> [192.168.1.10:22]
            val logLine = "[${entry.timestamp}] [ALERT: ${entry.service} (Port ${entry.port})] App: ${entry.appName} (${entry.packageName}) -> Dest: ${entry.destination}\n"
            
            FileOutputStream(logFile, true).use { stream ->
                stream.write(logLine.toByteArray(Charsets.UTF_8))
            }
            Log.d(TAG, "Sensitive Connection Logged: $logLine")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write sensitive connection to log file", e)
        }
    }

    data class SessionInfo(
        val index: Int,
        val name: String,
        val exists: Boolean,
        val sizeBytes: Long,
        val logCount: Int
    )

    /**
     * Retrieve all saved sensitive connections from physical log file of a specific session.
     * sessionIndex = 0 is active log
     * sessionIndex in 1..5 is historical log
     */
    fun readLogs(context: Context, sessionIndex: Int = 0): List<String> {
        return try {
            val directory = context.filesDir
            val filename = if (sessionIndex == 0) LOG_FILE_NAME else "$LOG_FILE_NAME.$sessionIndex"
            val logFile = File(directory, filename)
            if (logFile.exists()) {
                logFile.readLines(Charsets.UTF_8)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read logs for session $sessionIndex", e)
            emptyList()
        }
    }

    /**
     * Get statistics and metadata for the active and last 5 rotated sessions on disk
     */
    fun getSessionHistory(context: Context): List<SessionInfo> {
        val directory = context.filesDir
        val list = mutableListOf<SessionInfo>()
        
        // Active session
        val activeFile = File(directory, LOG_FILE_NAME)
        val activeLines = try {
            if (activeFile.exists()) activeFile.readLines(Charsets.UTF_8).size else 0
        } catch (e: Exception) {
            0
        }
        list.add(
            SessionInfo(
                index = 0,
                name = "Активная сессия",
                exists = activeFile.exists(),
                sizeBytes = if (activeFile.exists()) activeFile.length() else 0L,
                logCount = activeLines
            )
        )
        
        // Historical sessions
        for (i in 1..5) {
            val histFile = File(directory, "$LOG_FILE_NAME.$i")
            val histLines = try {
                if (histFile.exists()) histFile.readLines(Charsets.UTF_8).size else 0
            } catch (e: Exception) {
                0
            }
            list.add(
                SessionInfo(
                    index = i,
                    name = "Предыдущая сессия $i",
                    exists = histFile.exists(),
                    sizeBytes = if (histFile.exists()) histFile.length() else 0L,
                    logCount = histLines
                )
            )
        }
        return list
    }

    /**
     * Clears all log history including the 5 historical sessions
     */
    @Synchronized
    fun clearLogs(context: Context): Boolean {
        return try {
            val directory = context.filesDir
            val activeLog = File(directory, LOG_FILE_NAME)
            if (activeLog.exists()) {
                activeLog.delete()
            }
            for (i in 1..5) {
                val histFile = File(directory, "$LOG_FILE_NAME.$i")
                if (histFile.exists()) {
                    histFile.delete()
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs", e)
            false
        }
    }

    /**
     * Rotates session logs. Shifts previous sessions (1->2->3->4->5) and wipes
     * the active connection log so each session starts fresh.
     */
    @Synchronized
    fun rotateLogs(context: Context) {
        LogRotator.rotateLogs(context)
    }

    /**
     * Get path of the physical log file
     */
    fun getLogFilePath(context: Context): String {
        val directory = context.filesDir
        return File(directory, LOG_FILE_NAME).absolutePath
    }
}
