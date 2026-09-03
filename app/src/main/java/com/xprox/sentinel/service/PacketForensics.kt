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
