package com.xprox.sentinel.parser

import com.xprox.sentinel.core.SentinelCore

/**
 * Thread-safe utility to parse raw IP and TCP/UDP packets from a TUN interface.
 * Delegates binary packet parsing exclusively to Sentinel-Core native engine (SentinelCore.dissectPacket).
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
            return null
        }

        return null
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
}
