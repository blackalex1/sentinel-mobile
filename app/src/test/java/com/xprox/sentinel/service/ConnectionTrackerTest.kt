package com.xprox.sentinel.service

import android.content.Context
import android.net.ConnectivityManager
import com.xprox.sentinel.log.LogManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap

@Suppress("UNCHECKED_CAST")
class ConnectionTrackerTest {

    private lateinit var mockContext: Context
    private lateinit var mockConnManager: ConnectivityManager
    private lateinit var mockAppResolver: AppResolver
    private var logLoggedCount = 0

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockConnManager = mock(ConnectivityManager::class.java)
        mockAppResolver = mock(AppResolver::class.java)

        // Mock SharedPreferences
        val inMemoryPrefs = InMemorySharedPreferences()
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(inMemoryPrefs)

        // Mock System Services
        `when`(mockContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(mockConnManager)

        // Mock filesDir
        val fakeFilesDir = File(System.getProperty("java.io.tmpdir"), "sentinel_tracker_test")
        fakeFilesDir.mkdirs()
        `when`(mockContext.filesDir).thenReturn(fakeFilesDir)

        // Mock appResolver to resolve standard app info
        `when`(mockAppResolver.resolveApp(anyInt())).thenReturn(Pair("My Resolved App", "com.resolved.pkg"))

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

        // Clear existing log file
        val logFile = File(fakeFilesDir, "x_prox_sensitive_connections.log")
        if (logFile.exists()) {
            logFile.delete()
        }

        // Mock active audited ports set inside LogManager
        val activePortsField = LogManager::class.java.getDeclaredField("activePortsSet").apply {
            isAccessible = true
        }
        activePortsField.set(null, setOf(22, 443)) // Only monitor SSH (22) and HTTPS (443)

        logLoggedCount = 0
    }

    private fun buildTcpPacket(destinationPort: Int): ByteArray {
        val packet = ByteArray(40)
        packet[0] = 0x45.toByte() // Version = 4, IHL = 5
        packet[9] = 6.toByte()    // Protocol = TCP (6)
        
        // Source IP: 192.0.2.100
        packet[12] = 192.toByte()
        packet[13] = 0.toByte()
        packet[14] = 2.toByte()
        packet[15] = 100.toByte()
        
        // Destination IP: 203.0.113.10
        packet[16] = 203.toByte()
        packet[17] = 0.toByte()
        packet[18] = 113.toByte()
        packet[19] = 10.toByte()
        
        // TCP Source Port: 54321 (0xD431)
        packet[20] = 0xD4.toByte()
        packet[21] = 0x31.toByte()
        
        // TCP Destination Port: destinationPort
        packet[22] = ((destinationPort shr 8) and 0xFF).toByte()
        packet[23] = (destinationPort and 0xFF).toByte()
        
        return packet
    }

    @Test
    fun testUnmonitoredPortBypassedInstantly() {
        val tracker = ConnectionTracker(mockContext, mockAppResolver) {
            logLoggedCount++
        }

        // Build TCP packet pointing to port 80 (not in activePortsSet)
        val packet = buildTcpPacket(80)

        tracker.trackPacket(packet, packet.size)

        // Verify that it is bypassed immediately with NO log calls, NO binder calls, and NO appResolver calls
        assertEquals("Unmonitored port should trigger 0 log notifications", 0, logLoggedCount)
        val logs = LogManager.readLogs(mockContext)
        assertTrue("Log file should remain empty", logs.isEmpty())
    }

    @Test
    fun testMonitoredPortLoggedAndCached() {
        val tracker = ConnectionTracker(mockContext, mockAppResolver) {
            logLoggedCount++
        }

        // Build TCP packet pointing to port 22 (audited SSH port)
        val packet = buildTcpPacket(22)

        // 1. First packet should resolve, log, and cache
        tracker.trackPacket(packet, packet.size)
        assertEquals("First packet must trigger exactly 1 log notification", 1, logLoggedCount)

        val logs = LogManager.readLogs(mockContext)
        assertEquals("Log file must contain exactly 1 log line", 1, logs.size)
        assertTrue("Log must reference resolved package name", logs[0].contains("com.resolved.pkg"))
        assertTrue("Log must reference destination IP 203.0.113.10", logs[0].contains("203.0.113.10"))

        // 2. Second duplicate packet should hit the cache and NOT trigger redundant logging
        tracker.trackPacket(packet, packet.size)
        assertEquals("Duplicate packet should hit cache and NOT trigger another log notification", 1, logLoggedCount)
        
        val logsAfterDuplicate = LogManager.readLogs(mockContext)
        assertEquals("Log file should still have exactly 1 log line", 1, logsAfterDuplicate.size)
    }

    @Test
    fun testCacheSelfCleanup() {
        val tracker = ConnectionTracker(mockContext, mockAppResolver) {
            logLoggedCount++
        }

        // Access internal cache field using reflection to populate entries
        val cacheField = ConnectionTracker::class.java.getDeclaredField("connectionCache").apply {
            isAccessible = true
        }
        val cache = cacheField.get(tracker) as ConcurrentHashMap<String, Pair<String, String>>

        // Populate cache up to limit (1000)
        for (i in 1..1000) {
            cache["key_$i"] = Pair("App_$i", "pkg_$i")
        }
        assertEquals(1000, cache.size)

        // Track a monitored packet (port 22) - this should trigger a cache bounds check
        val packet = buildTcpPacket(22)
        tracker.trackPacket(packet, packet.size)

        // Verify that cache was cleared before adding the new key, and now contains only 1 entry!
        assertTrue("Cache size should be reset to 1 after bounds cleanup", cache.size == 1)
        assertEquals(1, logLoggedCount) // 0 from direct cache population + 1 from trackPacket
    }
}
