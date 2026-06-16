package com.xprox.sentinel.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class PacketParserTest {

    @Test
    fun testValidTcpPacket() {
        val packet = ByteArray(40)
        
        // IPv4 Header
        packet[0] = 0x45.toByte() // Version = 4, IHL = 5 (20 bytes)
        packet[9] = 6.toByte()    // Protocol = TCP (6)
        
        // Source IP: 192.0.2.50 (TEST-NET-1 RFC 5737)
        packet[12] = 192.toByte()
        packet[13] = 0.toByte()
        packet[14] = 2.toByte()
        packet[15] = 50.toByte()
        
        // Destination IP: 8.8.8.8
        packet[16] = 8.toByte()
        packet[17] = 8.toByte()
        packet[18] = 8.toByte()
        packet[19] = 8.toByte()
        
        // TCP Header (Starts at index 20)
        // Source Port: 49152 (0xC000)
        packet[20] = 0xC0.toByte()
        packet[21] = 0x00.toByte()
        
        // Destination Port: 443 (0x01BB)
        packet[22] = 0x01.toByte()
        packet[23] = 0xBB.toByte()

        val parsed = PacketParser.parse(packet, packet.size)
        assertNotNull("Should successfully parse valid TCP packet", parsed)
        assertEquals(6, parsed!!.protocol)
        assertEquals("192.0.2.50", parsed.sourceIp)
        assertEquals("8.8.8.8", parsed.destinationIp)
        assertEquals(49152, parsed.sourcePort)
        assertEquals(443, parsed.destinationPort)
    }

    @Test
    fun testValidUdpPacket() {
        val packet = ByteArray(28)
        
        // IPv4 Header
        packet[0] = 0x45.toByte() // Version = 4, IHL = 5 (20 bytes)
        packet[9] = 17.toByte()   // Protocol = UDP (17)
        
        // Source IP: 10.0.0.5
        packet[12] = 10.toByte()
        packet[13] = 0.toByte()
        packet[14] = 0.toByte()
        packet[15] = 5.toByte()
        
        // Destination IP: 1.1.1.1
        packet[16] = 1.toByte()
        packet[17] = 1.toByte()
        packet[18] = 1.toByte()
        packet[19] = 1.toByte()
        
        // UDP Header (Starts at index 20)
        // Source Port: 54321 (0xD431)
        packet[20] = 0xD4.toByte()
        packet[21] = 0x31.toByte()
        
        // Destination Port: 53 (0x0035)
        packet[22] = 0x00.toByte()
        packet[23] = 0x35.toByte()

        val parsed = PacketParser.parse(packet, packet.size)
        assertNotNull("Should successfully parse valid UDP packet", parsed)
        assertEquals(17, parsed!!.protocol)
        assertEquals("10.0.0.5", parsed.sourceIp)
        assertEquals("1.1.1.1", parsed.destinationIp)
        assertEquals(54321, parsed.sourcePort)
        assertEquals(53, parsed.destinationPort)
    }

    @Test
    fun testPacketTooShort() {
        val packet = ByteArray(19) // Minimum is 20
        val parsed = PacketParser.parse(packet, packet.size)
        assertNull("Should return null for packets shorter than 20 bytes", parsed)
    }

    @Test
    fun testUnsupportedIpVersion() {
        val packet = ByteArray(40)
        packet[0] = 0x55.toByte() // Version = 5 (Unsupported)
        val parsed = PacketParser.parse(packet, packet.size)
        assertNull("Should return null for unsupported IP versions", parsed)
    }

    @Test
    fun testValidIPv6TcpPacket() {
        val packet = ByteArray(60) // IPv6 header (40) + TCP header (20)
        
        // IPv6 Header
        packet[0] = 0x60.toByte() // Version = 6
        packet[4] = 0x00.toByte() // Payload Length = 20 bytes (0x0014)
        packet[5] = 0x14.toByte()
        packet[6] = 6.toByte()    // Next Header = TCP (6)
        packet[7] = 64.toByte()   // Hop Limit = 64
        
        // Source IPv6: 2001:db8::1
        packet[8] = 0x20.toByte(); packet[9] = 0x01.toByte()
        packet[10] = 0x0d.toByte(); packet[11] = 0xb8.toByte()
        packet[23] = 0x01.toByte()
        
        // Destination IPv6: 2001:db8::2
        packet[24] = 0x20.toByte(); packet[25] = 0x01.toByte()
        packet[26] = 0x0d.toByte(); packet[27] = 0xb8.toByte()
        packet[39] = 0x02.toByte()
        
        // TCP Header (Starts at index 40)
        // Source Port: 49152 (0xC000)
        packet[40] = 0xC0.toByte()
        packet[41] = 0x00.toByte()
        // Destination Port: 443 (0x01BB)
        packet[42] = 0x01.toByte()
        packet[43] = 0xBB.toByte()
        
        // Sequence number: 1000 -> 0x000003E8
        packet[46] = 0x03.toByte()
        packet[47] = 0xE8.toByte()
        
        // Acknowledgment number: 2000 -> 0x000007D0
        packet[50] = 0x07.toByte()
        packet[51] = 0xD0.toByte()
        
        // TCP Flags: SYN-ACK (0x12) at TCP offset 13 (index 53)
        packet[53] = 0x12.toByte()
        
        // Window size: 1024 (0x0400) at TCP offset 14 (index 54-55)
        packet[54] = 0x04.toByte()
        packet[55] = 0x00.toByte()

        val parsed = PacketParser.parse(packet, packet.size)
        assertNotNull("Should successfully parse valid IPv6 TCP packet", parsed)
        assertEquals(6, parsed!!.protocol)
        assertEquals("2001:db8:0:0:0:0:0:1", parsed.sourceIp)
        assertEquals("2001:db8:0:0:0:0:0:2", parsed.destinationIp)
        assertEquals(49152, parsed.sourcePort)
        assertEquals(443, parsed.destinationPort)
        assertEquals(64, parsed.ttl)
        assertEquals(60, parsed.ipLength)
        assertEquals("ACK, SYN", parsed.tcpFlags)
        assertEquals(1000L, parsed.tcpSeq)
        assertEquals(2000L, parsed.tcpAck)
        assertEquals(1024, parsed.tcpWindow)
    }

    @Test
    fun testValidIPv6UdpPacket() {
        val packet = ByteArray(48) // IPv6 header (40) + UDP header (8)
        
        // IPv6 Header
        packet[0] = 0x60.toByte() // Version = 6
        packet[4] = 0x00.toByte() // Payload Length = 8 bytes (0x0008)
        packet[5] = 0x08.toByte()
        packet[6] = 17.toByte()   // Next Header = UDP (17)
        packet[7] = 128.toByte()  // Hop Limit = 128
        
        // Source IPv6: 2001:db8::1
        packet[8] = 0x20.toByte(); packet[9] = 0x01.toByte()
        packet[10] = 0x0d.toByte(); packet[11] = 0xb8.toByte()
        packet[23] = 0x01.toByte()
        
        // Destination IPv6: 2001:db8::2
        packet[24] = 0x20.toByte(); packet[25] = 0x01.toByte()
        packet[26] = 0x0d.toByte(); packet[27] = 0xb8.toByte()
        packet[39] = 0x02.toByte()
        
        // UDP Header (Starts at index 40)
        // Source Port: 54321 (0xD431)
        packet[40] = 0xD4.toByte()
        packet[41] = 0x31.toByte()
        // Destination Port: 53 (0x0035)
        packet[42] = 0x00.toByte()
        packet[43] = 0x35.toByte()

        val parsed = PacketParser.parse(packet, packet.size)
        assertNotNull("Should successfully parse valid IPv6 UDP packet", parsed)
        assertEquals(17, parsed!!.protocol)
        assertEquals("2001:db8:0:0:0:0:0:1", parsed.sourceIp)
        assertEquals("2001:db8:0:0:0:0:0:2", parsed.destinationIp)
        assertEquals(54321, parsed.sourcePort)
        assertEquals(53, parsed.destinationPort)
        assertEquals(128, parsed.ttl)
        assertEquals(48, parsed.ipLength)
        assertEquals("N/A (UDP)", parsed.tcpFlags)
    }

    @Test
    fun testIhlLargerThanPacketLength() {
        val packet = ByteArray(22)
        packet[0] = 0x46.toByte() // Version = 4, IHL = 6 (24 bytes)
        val parsed = PacketParser.parse(packet, packet.size)
        assertNull("Should return null if IHL specifies more bytes than available", parsed)
    }

    @Test
    fun testUnsupportedProtocol() {
        val packet = ByteArray(40)
        packet[0] = 0x45.toByte() // Version = 4, IHL = 5
        packet[9] = 1.toByte()    // Protocol = ICMP (1) (Unsupported)
        
        val parsed = PacketParser.parse(packet, packet.size)
        assertNull("Should return null for unsupported protocols like ICMP", parsed)
    }

    @Test
    fun testTruncatedHeader() {
        val packet = ByteArray(22)
        packet[0] = 0x45.toByte() // Version = 4, IHL = 5
        packet[9] = 6.toByte()    // Protocol = TCP (6)
        
        // Length 22 is not enough to read 4 bytes of TCP ports (needs at least ihl + 4 = 24 bytes)
        val parsed = PacketParser.parse(packet, packet.size)
        assertNull("Should return null for truncated TCP headers", parsed)
    }
}
