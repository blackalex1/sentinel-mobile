package com.xprox.sentinel.service

import android.content.Context
import android.net.ConnectivityManager
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import com.xprox.sentinel.log.LogManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.first

@Suppress("UNCHECKED_CAST")
class ConnectionAuditParserTest {

    private lateinit var mockContext: Context
    private lateinit var mockPm: PackageManager
    private lateinit var mockConnManager: ConnectivityManager
    private val logEntries = mutableListOf<LogManager.LogEntry>()

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockPm = mock(PackageManager::class.java)
        mockConnManager = mock(ConnectivityManager::class.java)

        // Mock SharedPreferences
        val inMemoryPrefs = InMemorySharedPreferences()
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(inMemoryPrefs)

        // Mock Context services
        `when`(mockContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(mockConnManager)
        `when`(mockContext.packageManager).thenReturn(mockPm)

        // Mock filesDir
        val fakeFilesDir = File(System.getProperty("java.io.tmpdir"), "sentinel_audit_test")
        fakeFilesDir.mkdirs()
        `when`(mockContext.filesDir).thenReturn(fakeFilesDir)

        // Mock PackageManager info
        `when`(mockPm.getPackagesForUid(anyInt())).thenReturn(arrayOf("com.test.resolved.app"))
        val appInfo = ApplicationInfo().apply {
            nonLocalizedLabel = "Resolved Test App"
        }
        `when`(mockPm.getApplicationInfo(anyString(), anyInt())).thenReturn(appInfo)
        `when`(mockPm.getApplicationLabel(appInfo)).thenReturn("Resolved Test App")

        // Reset ThreatDetectionManager maps
        val connectionAttemptsField = ThreatDetectionManager::class.java.getDeclaredField("connectionAttempts").apply {
            isAccessible = true
        }
        val connectionAttempts = connectionAttemptsField.get(null) as ConcurrentHashMap<*, *>
        connectionAttempts.clear()

        val blockedAppsField = ThreatDetectionManager::class.java.getDeclaredField("blockedApps").apply {
            isAccessible = true
        }
        val blockedApps = blockedAppsField.get(null) as MutableSet<String>
        blockedApps.clear()

        // Clear existing log file if any
        val logFile = File(fakeFilesDir, "x_prox_sensitive_connections.log")
        if (logFile.exists()) {
            logFile.delete()
        }

        // Mock active ports
        val activePortsField = LogManager::class.java.getDeclaredField("activePortsSet").apply {
            isAccessible = true
        }
        activePortsField.set(null, setOf(22, 443, 80))

        logEntries.clear()
    }

    @Test
    fun testStandardTunLogParsing() {
        // Standard Xray TUN log line
        val logLine = "from tcp:127.0.0.1:54321 accepted tcp:203.0.113.8:443"

        // Mock getConnectionOwnerUid to return a normal user app UID (>= 10000)
        `when`(mockConnManager.getConnectionOwnerUid(
            anyInt(), any(), any()
        )).thenReturn(10050)

        // Parse log line
        ConnectionAuditParser.parseAndLog(mockContext, logLine)

        // Verify that the connection is resolved as Kernel/Root on JVM since SDK_INT = 0
        val logs = LogManager.readLogs(mockContext)
        assertTrue("Log file should contain logged connection", logs.isNotEmpty())
        assertTrue("Log entry should reference Kernel / Root", logs[0].contains("Kernel / Root"))
        assertTrue("Log entry should reference android.system.kernel", logs[0].contains("android.system.kernel"))
        assertTrue("Log entry should contain destination IP 203.0.113.8", logs[0].contains("203.0.113.8"))
        assertTrue("Log entry should contain port 443", logs[0].contains("443"))
    }

    @Test
    fun testLegacyLogFormatParsing() {
        // Legacy fallback format
        val logLine = "connection accepted from 10.0.0.5:1234 tcp:203.0.113.10:80"

        // Mock getConnectionOwnerUid to return standard system UI UID (1000)
        `when`(mockConnManager.getConnectionOwnerUid(
            anyInt(), any(), any()
        )).thenReturn(1000)

        ConnectionAuditParser.parseAndLog(mockContext, logLine)

        val logs = LogManager.readLogs(mockContext)
        assertTrue("Log file should contain logged connection", logs.isNotEmpty())
        assertTrue("System UID should map to android.system.kernel on JVM", logs[0].contains("android.system.kernel"))
        assertTrue("Log entry should contain destination 203.0.113.10:80", logs[0].contains("203.0.113.10:80"))
    }

    @Test
    fun testHotspotClientDetection() {
        // Source IP is from non-local range (a hotspot client)
        val logLine = "from tcp:198.51.100.25:54321 accepted tcp:203.0.113.20:22"

        ConnectionAuditParser.parseAndLog(mockContext, logLine)

        val logs = LogManager.readLogs(mockContext)
        assertTrue("Log file should contain logged connection", logs.isNotEmpty())
        assertTrue("Should be logged as Hotspot Client", logs[0].contains("Hotspot Client (198.51.100.25)"))
        assertTrue("Package name should be hotspot.client", logs[0].contains("hotspot.client"))
    }

    @Test
    fun testNonAcceptedLogsSkipped() {
        val logLine = "[Info] xray.com/core/app/proxyman/inbound: connection closed"
        ConnectionAuditParser.parseAndLog(mockContext, logLine)

        val logs = LogManager.readLogs(mockContext)
        assertTrue("Log file should remain empty for non-accepted logs", logs.isEmpty())
    }
}
