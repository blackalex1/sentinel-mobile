package com.xprox.sentinel.service

import android.content.Context
import android.util.Log
import com.xprox.sentinel.core.SentinelCore
import java.io.File

data class ConnectionRecord(
    val timestamp: Long,
    val destinationIp: String,
    val port: Int,
    val protocol: String = "TCP",
    val ipLength: Int = 0,
    val ttl: Int = 0,
    val ipFlags: String = "N/A",
    val tcpFlags: String = "N/A",
    val tcpSeq: Long = 0L,
    val tcpAck: Long = 0L,
    val tcpWindow: Int = 0,
    val rawBytes: ByteArray? = null
)

object PacketForensics {
    private const val TAG = "PacketForensics"

    private val executor = java.util.concurrent.Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "sentinel-pcap-writer").apply {
            priority = Thread.MIN_PRIORITY
        }
    }

    private fun getPcapFile(context: Context, packageName: String): File {
        val directory = File(context.filesDir, "threats")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return File(directory, "report_${packageName}.pcap")
    }

    /**
     * Helper to synthesize a valid IPv4 TCP/UDP raw packet byte array on-the-fly.
     */
    fun synthesizePacket(
        protocol: String,
        destinationIp: String,
        port: Int
    ): ByteArray {
        val isTcp = protocol.equals("TCP", ignoreCase = true)
        val ipProto = if (isTcp) 6 else 17
        val destBytes = try {
            java.net.InetAddress.getByName(destinationIp).address
        } catch (e: Exception) {
            byteArrayOf(8, 8, 8, 8)
        }
        val safeDest = if (destBytes.size == 4) destBytes else byteArrayOf(8, 8, 8, 8)
        val srcBytes = byteArrayOf(10, 0, 0, 2)
        val totalLen = 40
        val packet = ByteArray(totalLen)
        packet[0] = 0x45.toByte()
        packet[1] = 0x00.toByte()
        packet[2] = ((totalLen shr 8) and 0xFF).toByte()
        packet[3] = (totalLen and 0xFF).toByte()
        packet[4] = 0x12.toByte()
        packet[5] = 0x34.toByte()
        packet[6] = 0x40.toByte()
        packet[8] = 64.toByte()
        packet[9] = ipProto.toByte()
        System.arraycopy(srcBytes, 0, packet, 12, 4)
        System.arraycopy(safeDest, 0, packet, 16, 4)
        if (isTcp) {
            packet[20] = (0xC0).toByte()
            packet[21] = (0x00).toByte()
            packet[22] = ((port shr 8) and 0xFF).toByte()
            packet[23] = (port and 0xFF).toByte()
            packet[32] = 0x50.toByte()
            packet[33] = 0x02.toByte()
            packet[34] = 0xFA.toByte()
            packet[35] = 0xF0.toByte()
        }
        return packet
    }

    /**
     * Synthesizes and writes a TCP payload flow packet directly into the PCAP dump using Sentinel-Core native engine.
     */
    fun writeTcpPayloadToPcap(
        context: Context,
        packageName: String,
        srcIp: String,
        srcPort: Int,
        dstIp: String,
        dstPort: Int,
        seq: Long,
        ack: Long,
        flags: Byte,
        payload: ByteArray,
        timestampMs: Long
    ) {
        writeTcpPayloadToPcap(
            context = context,
            packageName = packageName,
            srcIp = srcIp,
            srcPort = srcPort,
            dstIp = dstIp,
            dstPort = dstPort,
            seq = seq,
            ack = ack,
            flags = flags,
            payload = payload,
            payloadOffset = 0,
            payloadLength = payload.size,
            timestampMs = timestampMs
        )
    }

    fun writeTcpPayloadToPcap(
        context: Context,
        packageName: String,
        srcIp: String,
        srcPort: Int,
        dstIp: String,
        dstPort: Int,
        seq: Long,
        ack: Long,
        flags: Byte,
        payload: ByteArray,
        payloadOffset: Int,
        payloadLength: Int,
        timestampMs: Long
    ) {
        val payloadSlice = if (payloadOffset == 0 && payloadLength == payload.size) {
            payload
        } else {
            payload.copyOfRange(payloadOffset, payloadOffset + payloadLength)
        }

        val pcapPath = getPcapFile(context, packageName).absolutePath
        val isUnitTest = try {
            !(System.getProperty("java.vm.name") ?: "").contains("Dalvik", ignoreCase = true)
        } catch (e: Exception) {
            false
        }

        if (isUnitTest) {
            SentinelCore.synthesizeAndWritePcap(
                context = context,
                filePath = pcapPath,
                proto = "TCP",
                srcIP = srcIp,
                srcPort = srcPort,
                dstIP = dstIp,
                dstPort = dstPort,
                tcpFlags = flags.toInt() and 0xFF,
                seq = seq,
                ack = ack,
                window = 64240,
                payload = payloadSlice,
                timestampMs = timestampMs
            )
        } else {
            executor.execute {
                SentinelCore.synthesizeAndWritePcap(
                    context = context,
                    filePath = pcapPath,
                    proto = "TCP",
                    srcIP = srcIp,
                    srcPort = srcPort,
                    dstIP = dstIp,
                    dstPort = dstPort,
                    tcpFlags = flags.toInt() and 0xFF,
                    seq = seq,
                    ack = ack,
                    window = 64240,
                    payload = payloadSlice,
                    timestampMs = timestampMs
                )
            }
        }
    }

    /**
     * Appends raw network packets to a standard PCAP file via Sentinel-Core native engine.
     */
    fun writePacketToPcap(
        context: Context,
        packageName: String,
        packetBytes: ByteArray,
        timestampMs: Long
    ) {
        val pcapPath = getPcapFile(context, packageName).absolutePath
        val isUnitTest = try {
            !(System.getProperty("java.vm.name") ?: "").contains("Dalvik", ignoreCase = true)
        } catch (e: Exception) {
            false
        }

        if (isUnitTest) {
            SentinelCore.writePcapPacket(context, pcapPath, packetBytes, timestampMs)
        } else {
            executor.execute {
                SentinelCore.writePcapPacket(context, pcapPath, packetBytes, timestampMs)
            }
        }
    }
}
