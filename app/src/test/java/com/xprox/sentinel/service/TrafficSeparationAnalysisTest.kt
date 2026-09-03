package com.xprox.sentinel.service

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import com.xprox.sentinel.config.XrayConfigManager
import com.xprox.sentinel.config.XrayProfilePersistence
import com.xprox.sentinel.core.SentinelCore
import com.xprox.sentinel.core.models.AndroidLogEntry
import com.xprox.sentinel.log.LogManager
import com.xprox.sentinel.parser.PacketParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File

@Suppress("UNCHECKED_CAST")
class TrafficSeparationAnalysisTest {

    private lateinit var mockContext: Context
    private lateinit var mockPm: PackageManager
    private lateinit var mockConnManager: ConnectivityManager
    private lateinit var fakeFilesDir: File

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockPm = mock(PackageManager::class.java)
        mockConnManager = mock(ConnectivityManager::class.java)

        // Mock SharedPreferences
        val inMemoryPrefs = InMemorySharedPreferences()
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(inMemoryPrefs)
        `when`(mockContext.packageName).thenReturn("com.xprox.sentinel")
        `when`(mockContext.applicationContext).thenReturn(mockContext)

        // Mock Context services
        `when`(mockContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(mockConnManager)
        `when`(mockContext.packageManager).thenReturn(mockPm)

        // Mock filesDir
        fakeFilesDir = File(System.getProperty("java.io.tmpdir"), "sentinel_traffic_sep_test")
        fakeFilesDir.mkdirs()
        `when`(mockContext.filesDir).thenReturn(fakeFilesDir)

        // Mock PackageManager info
        `when`(mockPm.getPackagesForUid(anyInt())).thenReturn(arrayOf("com.android.chrome"))
        val appInfo = ApplicationInfo().apply {
            nonLocalizedLabel = "Google Chrome"
            packageName = "com.android.chrome"
        }
        `when`(mockPm.getApplicationInfo(anyString(), anyInt())).thenReturn(appInfo)
        `when`(mockPm.getApplicationLabel(appInfo)).thenReturn("Google Chrome")

        // Clean up test files
        LogManager.clearLogs(mockContext)
        File(fakeFilesDir, "secure_xray_config.json").delete()

        // Reset ThreatDetectionManager, persistence cache, and native core threats
        ThreatDetectionManager.clearThreats(mockContext)
        SentinelCore.clearLogs()
        XrayProfilePersistence.resetCacheForTesting()

        // Configure monitored ports for test
        LogManager.saveActivePorts(mockContext, setOf(22, 443, 80, 53, 445))
    }

    private fun buildRawIPv4Packet(srcIp: String, dstIp: String, srcPort: Int, dstPort: Int, isUdp: Boolean): ByteArray {
        val headerLen = 20
        val payloadLen = 16
        val totalLen = headerLen + (if (isUdp) 8 else 20) + payloadLen
        val packet = ByteArray(totalLen)

        // IPv4 Header
        packet[0] = 0x45.toByte() // Version 4, IHL 5
        packet[8] = 64.toByte()   // TTL = 64
        packet[9] = (if (isUdp) 17 else 6).toByte() // Protocol

        // Source IP
        val srcParts = srcIp.split(".").map { it.toInt() }
        packet[12] = srcParts[0].toByte()
        packet[13] = srcParts[1].toByte()
        packet[14] = srcParts[2].toByte()
        packet[15] = srcParts[3].toByte()

        // Destination IP
        val dstParts = dstIp.split(".").map { it.toInt() }
        packet[16] = dstParts[0].toByte()
        packet[17] = dstParts[1].toByte()
        packet[18] = dstParts[2].toByte()
        packet[19] = dstParts[3].toByte()

        // Transport Header
        packet[20] = ((srcPort shr 8) and 0xFF).toByte()
        packet[21] = (srcPort and 0xFF).toByte()
        packet[22] = ((dstPort shr 8) and 0xFF).toByte()
        packet[23] = (dstPort and 0xFF).toByte()

        if (!isUdp) {
            // TCP Data Offset (5 = 20 bytes)
            packet[32] = 0x50.toByte()
            // TCP SYN Flag
            packet[33] = 0x02.toByte()
        }

        return packet
    }

    @Test
    fun testTrafficAnalysis_LocalDeviceVsHotspotLogSeparation() {
        // 1. Ingest log line for local device traffic (loopback/TUN source)
        val localLogLine = "from tcp:127.0.0.1:54321 accepted tcp:198.51.100.46:443"
        ConnectionAuditParser.parseAndLog(mockContext, localLogLine)

        // 2. Ingest log line for tethered Hotspot client traffic (Wi-Fi hotspot subnet)
        val hotspotLogLine = "from tcp:192.168.43.105:51234 accepted tcp:1.1.1.1:53"
        ConnectionAuditParser.parseAndLog(mockContext, hotspotLogLine)

        // 3. Ingest log line for local IPv6 TUN traffic (fd00::2 - local phone interface)
        val localIpv6LogLine = "from udp:[fd00::2]:56536 accepted udp:[2001:4860:4860::8888]:443"
        ConnectionAuditParser.parseAndLog(mockContext, localIpv6LogLine)

        val logs = LogManager.readLogs(mockContext)
        assertEquals("All 3 connections must be recorded. Actual logs: $logs", 3, logs.size)

        // Verify Local Traffic Attribution
        val localLog = logs.find { it.contains("198.51.100.46:443") }
        assertNotNull("Local device log entry must exist", localLog)
        assertTrue("Local entry must identify local system kernel/app", 
            localLog!!.contains("android.system.kernel") || localLog.contains("Kernel / Root"))
        assertFalse("Local entry must NOT be marked as hotspot client", localLog.contains("hotspot.client"))

        // Verify Local IPv6 Traffic Attribution
        val localIpv6Log = logs.find { it.contains("2001:4860:4860::8888:443") }
        assertNotNull("Local IPv6 device log entry must exist", localIpv6Log)
        assertFalse("Local IPv6 TUN entry must NOT be marked as hotspot client", localIpv6Log!!.contains("hotspot.client"))

        // Verify Hotspot Client Traffic Attribution
        val hotspotLog = logs.find { it.contains("1.1.1.1:53") }
        assertNotNull("Hotspot client log entry must exist", hotspotLog)
        assertTrue("Hotspot entry must be explicitly labeled as Hotspot Client", 
            hotspotLog!!.contains("Hotspot Client (192.168.43.105)"))
        assertTrue("Hotspot package name must be hotspot.client", 
            hotspotLog.contains("hotspot.client"))
    }

    @Test
    fun testTrafficAnalysis_WireFormatPacketDissectionSeparation() {
        // 1. Dissect Local Device Packet (10.0.0.2 -> 198.51.100.34:80 TCP)
        val localPacket = buildRawIPv4Packet("10.0.0.2", "198.51.100.34", 45000, 80, isUdp = false)
        val localParsed = PacketParser.parse(localPacket, localPacket.size)

        assertNotNull("Local packet must be dissected by Sentinel-Core", localParsed)
        assertEquals("10.0.0.2", localParsed!!.sourceIp)
        assertEquals("198.51.100.34", localParsed.destinationIp)
        assertEquals(80, localParsed.destinationPort)
        assertEquals(6, localParsed.protocol)
        assertEquals("SYN", localParsed.tcpFlags)

        // 2. Dissect Tethered Hotspot Client Packet (192.168.43.55 -> 8.8.8.8:53 UDP)
        val hotspotPacket = buildRawIPv4Packet("192.168.43.55", "8.8.8.8", 53000, 53, isUdp = true)
        val hotspotParsed = PacketParser.parse(hotspotPacket, hotspotPacket.size)

        assertNotNull("Hotspot client packet must be dissected by Sentinel-Core", hotspotParsed)
        assertEquals("192.168.43.55", hotspotParsed!!.sourceIp)
        assertEquals("8.8.8.8", hotspotParsed.destinationIp)
        assertEquals(53, hotspotParsed.destinationPort)
        assertEquals(17, hotspotParsed.protocol)
        assertEquals("N/A (UDP)", hotspotParsed.tcpFlags)
    }

    @Test
    fun testTrafficAnalysis_QuarantineIsolationSeparation() {
        val hotspotIp = "192.168.43.77"
        val localAppPackage = "com.legit.userapp"

        // Step 1: Hotspot client probes sensitive SSH port 22 three times (exceeding threshold)
        for (i in 1..3) {
            ThreatDetectionManager.registerConnectionAttempt(
                mockContext,
                packageName = "hotspot.client",
                appName = "Hotspot Client ($hotspotIp)",
                destinationIp = "198.51.100.22",
                port = 22
            )
        }

        // Assert that the hotspot client is quarantined in Zero-Trust registry
        assertTrue("Hotspot client must be blackholed after limit breach", 
            ThreatDetectionManager.isAppBlocked("hotspot.client"))

        // Step 2: Legitimate local phone application connects to web port 443
        val localIsBlocked = ThreatDetectionManager.registerConnectionAttempt(
            mockContext,
            packageName = localAppPackage,
            appName = "Legit User App",
            destinationIp = "198.51.100.46",
            port = 443
        )

        // Assert that the local phone app is completely unaffected by the hotspot client's quarantine!
        assertFalse("Local phone application must NOT be blocked when hotspot client is quarantined", localIsBlocked)
        assertFalse("Local application must not be in blocked apps list", 
            ThreatDetectionManager.isAppBlocked(localAppPackage))
    }

    @Test
    fun testTrafficAnalysis_NativeRingBufferSeparation() {
        val now = System.currentTimeMillis()

        // Push local app log into native Go ring buffer
        SentinelCore.pushLog(
            AndroidLogEntry(
                timestamp = now,
                packageName = "com.android.chrome",
                appName = "Google Chrome",
                destinationIp = "198.51.100.46",
                destinationPort = 443,
                protocol = "TCP",
                serviceName = "HTTPS",
                action = "direct",
                threatType = "NONE"
            )
        )

        // Push hotspot client log into native Go ring buffer
        SentinelCore.pushLog(
            AndroidLogEntry(
                timestamp = now + 10,
                packageName = "hotspot.client",
                appName = "Hotspot Client (192.168.43.88)",
                destinationIp = "8.8.8.8",
                destinationPort = 53,
                protocol = "UDP",
                serviceName = "DNS",
                action = "direct",
                threatType = "NONE"
            )
        )

        val retrievedLogs = SentinelCore.getLogs(limit = 10)
        assertEquals("Ring buffer must contain both log entries", 2, retrievedLogs.size)

        val localEntry = retrievedLogs.find { it.packageName == "com.android.chrome" }
        assertNotNull("Local app entry must exist in native ring buffer", localEntry)
        assertEquals("Google Chrome", localEntry!!.appName)
        assertEquals(443, localEntry.destinationPort)

        val hotspotEntry = retrievedLogs.find { it.packageName == "hotspot.client" }
        assertNotNull("Hotspot entry must exist in native ring buffer", hotspotEntry)
        assertEquals("Hotspot Client (192.168.43.88)", hotspotEntry!!.appName)
        assertEquals(53, hotspotEntry.destinationPort)

        // Validate stats aggregation
        val stats = SentinelCore.getLogStats()
        assertTrue("Total connections must be at least 2", stats.totalConnections >= 2)
        val appPackages = stats.topApps.map { it.packageName }
        assertTrue("App stats must contain com.android.chrome", appPackages.contains("com.android.chrome"))
        assertTrue("App stats must contain hotspot.client", appPackages.contains("hotspot.client"))
    }

    @Test
    fun testTrafficAnalysis_HotspotAndLocalInboundsConfigCompilation() {
        val profile = XrayConfigManager.ServerProfile(
            id = "test-prof-1",
            name = "Test VLESS Server",
            address = "gateway.example.com",
            port = 443,
            uuid = "00000000-0000-0000-0000-000000000001",
            type = "VLESS",
            flow = "xtls-rprx-vision",
            security = "reality",
            sni = "gateway.example.com",
            pbk = "mock-public-key-abcdef",
            sid = "4acb4c423d47ae19",
            fp = "chrome"
        )

        val creds = XrayConfigManager.LocalProxyCredentials(
            port = 10808,
            username = "localuser",
            token = "localpass"
        )

        val lanCreds = XrayConfigManager.LocalProxyCredentials(
            port = 10809,
            username = "lanuser",
            token = "lanpass"
        )

        // Enable LAN/Hotspot sharing in mock preferences
        XrayProfilePersistence.saveLanSharing(mockContext, true)
        XrayProfilePersistence.saveLanSharingHttp(mockContext, true)
        XrayProfilePersistence.saveLanSharingSocks(mockContext, true)

        val configFile = XrayConfigManager.compileSecureConfig(
            context = mockContext,
            profile = profile,
            creds = creds,
            lanAuthEnabled = true,
            lanCreds = lanCreds,
            lanHttpPort = 10809,
            lanSocksPort = 10808
        )

        assertTrue("Generated config file must exist", configFile.exists())
        val configContent = configFile.readText(Charsets.UTF_8)
        assertTrue("Config must contain inbounds section", configContent.contains("\"inbounds\""))

        // Verify distinct inbounds for Local Device vs Tethered Hotspot Clients
        assertTrue("Must have local socks-in inbound", configContent.contains("\"socks-in\""))
        assertTrue("Must have local tun-in inbound", configContent.contains("\"tun-in\""))
        assertTrue("Must have dedicated lan-http-in inbound for Hotspot", configContent.contains("\"lan-http-in\""))
        assertTrue("Must have dedicated lan-socks-in inbound for Hotspot", configContent.contains("\"lan-socks-in\""))

        // Check listen address and credentials separation
        assertTrue("Local socks must listen on 127.0.0.1", configContent.contains("127.0.0.1"))
        assertTrue("LAN HTTP must listen on 0.0.0.0", configContent.contains("0.0.0.0"))
        assertTrue("LAN HTTP port 10809 must be configured", configContent.contains("10809"))
        assertTrue("LAN SOCKS port 10808 must be configured", configContent.contains("10808"))
        assertTrue("LAN credentials username must be configured", configContent.contains("lanuser"))
        assertTrue("LAN credentials password must be configured", configContent.contains("lanpass"))
    }
}
