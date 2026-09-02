package com.xprox.sentinel.parser

import com.xprox.sentinel.core.SentinelCore
import java.nio.ByteBuffer

/**
 * Thread-safe utility to parse raw IP and TCP/UDP packets from a TUN interface.
 * Delegates binary packet parsing to Sentinel-Core native engine (SentinelCore.dissectPacket)
 * with a fallback ByteBuffer parser.
 */
object PacketParser {

    class ParsedPacket(
        val protocol: Int, // 6 = TCP, 17 = UDP, etc.
        val sourceIp: String,
        val destinationIp: String,
        val sourcePort: Int,
        val destinationPort: Int,
        // Forensic metrics:
        val ttl: Int,
        val ipLength: Int,
        val ipFlags: String,
        val tcpFlags: String,
        val tcpSeq: Long,
        val tcpAck: Long,
        val tcpWindow: Int
    )

    fun parse(packetBytes: ByteArray, length: Int): ParsedPacket? {
        if (length < 20) return null

        try {
            val slice = if (length == packetBytes.size) packetBytes else packetBytes.copyOfRange(0, length)
            val dissected = SentinelCore.dissectPacket(null, slice)
            if (dissected != null && dissected.sourceIp.isNotEmpty() && dissected.destinationIp.isNotEmpty()) {
                val isTcp = dissected.protocol.equals("TCP", ignoreCase = true)
                val isUdp = dissected.protocol.equals("UDP", ignoreCase = true)
                if (!isTcp && !isUdp) return null
                if (dissected.destinationPort <= 0) return null

                val protoInt = if (isUdp) 17 else 6
                val flags = formatTcpFlags(dissected.tcpFlags, isUdp)
                val srcIp = formatIp(dissected.sourceIp)
                val dstIp = formatIp(dissected.destinationIp)

                return ParsedPacket(
                    protocol = protoInt,
                    sourceIp = srcIp,
                    destinationIp = dstIp,
                    sourcePort = dissected.sourcePort,
                    destinationPort = dissected.destinationPort,
                    ttl = dissected.ttl,
                    ipLength = dissected.totalLength,
                    ipFlags = dissected.ipFlags.replace("|", ", "),
                    tcpFlags = flags,
                    tcpSeq = dissected.tcpSeq,
                    tcpAck = dissected.tcpAck,
                    tcpWindow = dissected.tcpWindow
                )
            }
        } catch (e: Exception) {
            // Fallback to local bytebuffer parser below
        }

        return fallbackParse(packetBytes, length)
    }

    private fun formatIp(ip: String): String {
        return try {
            val addr = java.net.InetAddress.getByName(ip)
            if (addr is java.net.Inet6Address) {
                val bytes = addr.address
                val sb = StringBuilder(39)
                for (i in 0 until 8) {
                    val part = ((bytes[i * 2].toInt() and 0xFF) shl 8) or (bytes[i * 2 + 1].toInt() and 0xFF)
                    sb.append(part.toString(16))
                    if (i < 7) sb.append(':')
                }
                sb.toString()
            } else {
                addr.hostAddress ?: ip
            }
        } catch (e: Exception) {
            ip
        }
    }

    private fun formatTcpFlags(flags: String, isUdp: Boolean): String {
        if (isUdp) return "N/A (UDP)"
        if (flags.isEmpty() || flags == "None") return "None"
        val parts = flags.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return "None"
        return parts.sorted().joinToString(", ")
    }

    private fun fallbackParse(packetBytes: ByteArray, length: Int): ParsedPacket? {
        if (length < 20) return null

        val buffer = ByteBuffer.wrap(packetBytes, 0, length)
        
        val versionAndIHL = buffer.get(0).toInt()
        val version = (versionAndIHL shr 4) and 0x0F
        
        if (version == 4) {
            val ihl = (versionAndIHL and 0x0F) * 4
            if (length < ihl) return null

            val protocol = buffer.get(9).toInt() and 0xFF
            val ttl = buffer.get(8).toInt() and 0xFF
            val ipLength = buffer.getShort(2).toInt() and 0xFFFF
            
            val flagsAndOffset = buffer.getShort(6).toInt() and 0xFFFF
            val df = (flagsAndOffset and 0x4000) != 0
            val mf = (flagsAndOffset and 0x2000) != 0
            val ipFlags = when {
                df && mf -> "DF, MF"
                df -> "DF"
                mf -> "MF"
                else -> "None"
            }

            val sourceIp = "${buffer.get(12).toInt() and 0xFF}.${buffer.get(13).toInt() and 0xFF}.${buffer.get(14).toInt() and 0xFF}.${buffer.get(15).toInt() and 0xFF}"
            val destinationIp = "${buffer.get(16).toInt() and 0xFF}.${buffer.get(17).toInt() and 0xFF}.${buffer.get(18).toInt() and 0xFF}.${buffer.get(19).toInt() and 0xFF}"

            var srcPort = 0
            var dstPort = 0
            var tcpFlags = ""
            var tcpSeq = 0L
            var tcpAck = 0L
            var tcpWindow = 0

            if (protocol == 6 && length >= ihl + 20) {
                buffer.position(ihl)
                srcPort = buffer.short.toInt() and 0xFFFF
                dstPort = buffer.short.toInt() and 0xFFFF
                tcpSeq = buffer.int.toLong() and 0xFFFFFFFFL
                tcpAck = buffer.int.toLong() and 0xFFFFFFFFL
                
                val flagsByte = buffer.get(ihl + 13).toInt() and 0xFF
                tcpFlags = buildTcpFlagsString(flagsByte)
                
                tcpWindow = buffer.getShort(ihl + 14).toInt() and 0xFFFF
            } else if (protocol == 17 && length >= ihl + 8) {
                buffer.position(ihl)
                srcPort = buffer.short.toInt() and 0xFFFF
                dstPort = buffer.short.toInt() and 0xFFFF
                tcpFlags = "N/A (UDP)"
            } else {
                return null
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
            if (length < 40) return null

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
            if (protocol == 6 && length >= ihl + 20) {
                buffer.position(ihl)
                srcPort = buffer.short.toInt() and 0xFFFF
                dstPort = buffer.short.toInt() and 0xFFFF
                tcpSeq = buffer.int.toLong() and 0xFFFFFFFFL
                tcpAck = buffer.int.toLong() and 0xFFFFFFFFL
                
                val flagsByte = buffer.get(ihl + 13).toInt() and 0xFF
                tcpFlags = buildTcpFlagsString(flagsByte)
                
                tcpWindow = buffer.getShort(ihl + 14).toInt() and 0xFFFF
            } else if (protocol == 17 && length >= ihl + 8) {
                buffer.position(ihl)
                srcPort = buffer.short.toInt() and 0xFFFF
                dstPort = buffer.short.toInt() and 0xFFFF
                tcpFlags = "N/A (UDP)"
            } else {
                return null
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
            return null
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
            sb.setLength(sb.length - 2)
            sb.toString()
        } else {
            "None"
        }
    }
}
