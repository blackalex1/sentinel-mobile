package com.xprox.sentinel.parser

import java.nio.ByteBuffer

/**
 * Thread-safe utility to parse raw IP and TCP/UDP packets from a TUN interface.
 * Implemented with strict boundaries to ensure zero buffer overflow vulnerabilities.
 */
object PacketParser {

    class ParsedPacket(
        val protocol: Int, // 6 = TCP, 17 = UDP, etc.
        val sourceIp: String,
        val destinationIp: String,
        val sourcePort: Int,
        val destinationPort: Int,
        // New low-level forensic metrics:
        val ttl: Int,
        val ipLength: Int,
        val ipFlags: String,
        val tcpFlags: String,
        val tcpSeq: Long,
        val tcpAck: Long,
        val tcpWindow: Int
    )

    fun parse(packetBytes: ByteArray, length: Int): ParsedPacket? {
        if (length < 20) return null // Minimum IP header length is 20 bytes (IPv4)

        val buffer = ByteBuffer.wrap(packetBytes, 0, length)
        
        // Parse IP Header Version
        val versionAndIHL = buffer.get(0).toInt()
        val version = (versionAndIHL shr 4) and 0x0F
        
        if (version == 4) {
            val ihl = (versionAndIHL and 0x0F) * 4
            if (length < ihl) return null // Packet is smaller than specified IP header

            val protocol = buffer.get(9).toInt() and 0xFF
            val ttl = buffer.get(8).toInt() and 0xFF
            val ipLength = buffer.getShort(2).toInt() and 0xFFFF
            
            // Extract IP Flags (DF, MF) at offset 6 (Bytes 6-7)
            val flagsAndOffset = buffer.getShort(6).toInt() and 0xFFFF
            val df = (flagsAndOffset and 0x4000) != 0
            val mf = (flagsAndOffset and 0x2000) != 0
            val ipFlags = when {
                df && mf -> "DF, MF"
                df -> "DF"
                mf -> "MF"
                else -> "None"
            }

            // Source IP Offset (Bytes 12-15) - Parse directly without allocating ByteArray(4)
            val sourceIp = "${buffer.get(12).toInt() and 0xFF}.${buffer.get(13).toInt() and 0xFF}.${buffer.get(14).toInt() and 0xFF}.${buffer.get(15).toInt() and 0xFF}"

            // Destination IP Offset (Bytes 16-19) - Parse directly without allocating ByteArray(4)
            val destinationIp = "${buffer.get(16).toInt() and 0xFF}.${buffer.get(17).toInt() and 0xFF}.${buffer.get(18).toInt() and 0xFF}.${buffer.get(19).toInt() and 0xFF}"

            var srcPort = 0
            var dstPort = 0
            var tcpFlags = ""
            var tcpSeq = 0L
            var tcpAck = 0L
            var tcpWindow = 0

            // Parse TCP/UDP Header
            if (protocol == 6 && length >= ihl + 20) { // TCP Protocol needs at least 20 bytes of TCP header
                buffer.position(ihl)
                srcPort = buffer.short.toInt() and 0xFFFF
                dstPort = buffer.short.toInt() and 0xFFFF
                tcpSeq = buffer.int.toLong() and 0xFFFFFFFFL
                tcpAck = buffer.int.toLong() and 0xFFFFFFFFL
                
                // TCP Flags are at offset 13 from TCP start (ihl + 13)
                val flagsByte = buffer.get(ihl + 13).toInt() and 0xFF
                tcpFlags = buildTcpFlagsString(flagsByte)
                
                // Window Size is at offset 14 (ihl + 14)
                tcpWindow = buffer.getShort(ihl + 14).toInt() and 0xFFFF
            } else if (protocol == 17 && length >= ihl + 8) { // UDP Protocol
                buffer.position(ihl)
                srcPort = buffer.short.toInt() and 0xFFFF
                dstPort = buffer.short.toInt() and 0xFFFF
                tcpFlags = "N/A (UDP)"
            } else {
                return null // Not TCP or UDP, or packet truncated
            }

            return ParsedPacket(
                protocol = protocol,
                sourceIp = sourceIp,
                destinationIp = destinationIp,
                sourcePort = srcPort,
                destinationPort = dstPort,
                ttl = ttl,
                ipLength = ipLength,
                ipFlags = ipFlags,
                tcpFlags = tcpFlags,
                tcpSeq = tcpSeq,
                tcpAck = tcpAck,
                tcpWindow = tcpWindow
            )
        } else if (version == 6) {
            if (length < 40) return null // Minimum IPv6 header is 40 bytes

            val payloadLength = buffer.getShort(4).toInt() and 0xFFFF
            val protocol = buffer.get(6).toInt() and 0xFF
            val ttl = buffer.get(7).toInt() and 0xFF
            val ipLength = payloadLength + 40
            val ipFlags = "None"

            val sourceIp = getIPv6AddressString(buffer, 8)
            val destinationIp = getIPv6AddressString(buffer, 24)

            var srcPort = 0
            var dstPort = 0
            var tcpFlags = ""
            var tcpSeq = 0L
            var tcpAck = 0L
            var tcpWindow = 0

            val ihl = 40
            // Parse TCP/UDP Header
            if (protocol == 6 && length >= ihl + 20) { // TCP Protocol needs at least 20 bytes of TCP header
                buffer.position(ihl)
                srcPort = buffer.short.toInt() and 0xFFFF
                dstPort = buffer.short.toInt() and 0xFFFF
                tcpSeq = buffer.int.toLong() and 0xFFFFFFFFL
                tcpAck = buffer.int.toLong() and 0xFFFFFFFFL
                
                // TCP Flags are at offset 13 from TCP start (ihl + 13)
                val flagsByte = buffer.get(ihl + 13).toInt() and 0xFF
                tcpFlags = buildTcpFlagsString(flagsByte)
                
                // Window Size is at offset 14 (ihl + 14)
                tcpWindow = buffer.getShort(ihl + 14).toInt() and 0xFFFF
            } else if (protocol == 17 && length >= ihl + 8) { // UDP Protocol
                buffer.position(ihl)
                srcPort = buffer.short.toInt() and 0xFFFF
                dstPort = buffer.short.toInt() and 0xFFFF
                tcpFlags = "N/A (UDP)"
            } else {
                return null // Not TCP or UDP, or packet truncated
            }

            return ParsedPacket(
                protocol = protocol,
                sourceIp = sourceIp,
                destinationIp = destinationIp,
                sourcePort = srcPort,
                destinationPort = dstPort,
                ttl = ttl,
                ipLength = ipLength,
                ipFlags = ipFlags,
                tcpFlags = tcpFlags,
                tcpSeq = tcpSeq,
                tcpAck = tcpAck,
                tcpWindow = tcpWindow
            )
        } else {
            return null // Unsupported IP version
        }
    }

    private fun getIPv6AddressString(buffer: ByteBuffer, offset: Int): String {
        val sb = java.lang.StringBuilder(39)
        for (i in 0 until 8) {
            val part = buffer.getShort(offset + i * 2).toInt() and 0xFFFF
            sb.append(part.toString(16))
            if (i < 7) sb.append(':')
        }
        return sb.toString()
    }

    private fun buildTcpFlagsString(flagsByte: Int): String {
        val sb = StringBuilder()
        if ((flagsByte and 0x80) != 0) sb.append("CWR, ")
        if ((flagsByte and 0x40) != 0) sb.append("ECE, ")
        if ((flagsByte and 0x20) != 0) sb.append("URG, ")
        if ((flagsByte and 0x10) != 0) sb.append("ACK, ")
        if ((flagsByte and 0x08) != 0) sb.append("PSH, ")
        if ((flagsByte and 0x04) != 0) sb.append("RST, ")
        if ((flagsByte and 0x02) != 0) sb.append("SYN, ")
        if ((flagsByte and 0x01) != 0) sb.append("FIN, ")
        return if (sb.isNotEmpty()) {
            sb.setLength(sb.length - 2) // remove trailing comma and space
            sb.toString()
        } else {
            "None"
        }
    }
}
