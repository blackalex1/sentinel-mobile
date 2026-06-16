package com.xprox.sentinel.service

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File

class PacketForensicsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var testDir: File

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        testDir = tempFolder.newFolder("filesDir")
        `when`(mockContext.filesDir).thenReturn(testDir)
    }

    @Test
    fun testSynthesizePacket() {
        val protocol = "TCP"
        val destIp = "8.8.8.8"
        val port = 80
        val packet = PacketForensics.synthesizePacket(protocol, destIp, port)

        // Standard IP header + TCP header is 40 bytes
        assertEquals(40, packet.size)

        // Version 4, IHL 5
        assertEquals(0x45.toByte(), packet[0])

        // Protocol (TCP = 6)
        assertEquals(6.toByte(), packet[9])

        // Destination IP matches
        assertEquals(8.toByte(), packet[16])
        assertEquals(8.toByte(), packet[17])
        assertEquals(8.toByte(), packet[18])
        assertEquals(8.toByte(), packet[19])

        // Destination Port matches
        val destPortHigh = packet[22].toInt() and 0xFF
        val destPortLow = packet[23].toInt() and 0xFF
        val actualPort = (destPortHigh shl 8) or destPortLow
        assertEquals(port, actualPort)

        // TCP Flags SYN (0x02)
        assertEquals(0x02.toByte(), packet[33])
    }

    @Test
    fun testWriteTcpPayloadToPcap() {
        val packageName = "com.test.app"
        val srcIp = "10.0.0.2"
        val srcPort = 50000
        val dstIp = "1.2.3.4"
        val dstPort = 80
        val seq = 1000L
        val ack = 2000L
        val flags = 0x18.toByte() // PSH-ACK
        val payload = "Hello World".toByteArray(Charsets.UTF_8)
        val timestampMs = 1700000000000L

        PacketForensics.writeTcpPayloadToPcap(
            mockContext,
            packageName,
            srcIp,
            srcPort,
            dstIp,
            dstPort,
            seq,
            ack,
            flags,
            payload,
            timestampMs
        )

        // Check if threats directory and pcap file are created
        val threatsDir = File(testDir, "threats")
        assertTrue(threatsDir.exists())
        val pcapFile = File(threatsDir, "report_$packageName.pcap")
        assertTrue(pcapFile.exists())

        // File size calculation:
        // PCAP Global Header: 24 bytes
        // Packet Header: 16 bytes
        // Packet Content (IP + TCP + payload): 20 + 20 + 11 = 51 bytes
        // Total expected size = 24 + 16 + 51 = 91 bytes
        assertEquals(91L, pcapFile.length())

        // Verify the binary structure of the PCAP file
        val bytes = pcapFile.readBytes()
        println("Generated PCAP bytes (hex): " + bytes.joinToString(" ") { String.format("%02x", it) })
        
        // PCAP Magic number: 0xd4c3b2a1 (little endian)
        assertEquals(0xd4.toByte(), bytes[0])
        assertEquals(0xc3.toByte(), bytes[1])
        assertEquals(0xb2.toByte(), bytes[2])
        assertEquals(0xa1.toByte(), bytes[3])

        // LinkType: 101 (Raw IP, little-endian = 0x65, 0x00, 0x00, 0x00)
        assertEquals(0x65.toByte(), bytes[20])
        assertEquals(0x00.toByte(), bytes[21])
        assertEquals(0x00.toByte(), bytes[22])
        assertEquals(0x00.toByte(), bytes[23])

        // Packet Header:
        // Seconds: timestampMs / 1000 = 1700000000 -> 0x6553f100 (little-endian: 0x00, 0xf1, 0x53, 0x65)
        assertEquals(0x00.toByte(), bytes[24])
        assertEquals(0xf1.toByte(), bytes[25])
        assertEquals(0x53.toByte(), bytes[26])
        assertEquals(0x65.toByte(), bytes[27])

        // Packet Length: 51 -> 0x33 (little endian: 0x33, 0x00, 0x00, 0x00)
        assertEquals(0x33.toByte(), bytes[32])
        assertEquals(0x00.toByte(), bytes[33])
        assertEquals(0x00.toByte(), bytes[34])
        assertEquals(0x00.toByte(), bytes[35])

        // Payload inside the TCP packet starts at global_hdr(24) + pkt_hdr(16) + ip_hdr(20) + tcp_hdr(20) = 80
        val payloadStart = 80
        val extractedPayload = bytes.copyOfRange(payloadStart, payloadStart + payload.size)
        assertEquals("Hello World", String(extractedPayload, Charsets.UTF_8))

        // Copy to workspace for verification script (graceful fallback)
        try {
            val buildDir = System.getProperty("user.dir")
            val destFile = File(buildDir, "app/build/tmp/test_packet_forensics.pcap")
            destFile.parentFile?.mkdirs()
            pcapFile.copyTo(destFile, overwrite = true)
        } catch (e: Exception) {}
    }
}
