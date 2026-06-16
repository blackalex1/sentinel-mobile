package com.xprox.sentinel.service

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

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

    /**
     * Helper to synthesize a valid IPv4 TCP/UDP raw packet byte array on-the-fly
     * when real packet bytes are not accessible (e.g. under Native Xray mode).
     * This ensures the PCAP dump is valid and fully readable in Wireshark.
     */
    fun synthesizePacket(
        protocol: String,
        destinationIp: String,
        port: Int
    ): ByteArray {
        try {
            val isTcp = protocol.equals("TCP", ignoreCase = true)
            val ipProto = if (isTcp) 6 else 17
            
            // Parse destination IP to bytes
            val destBytes = try {
                java.net.InetAddress.getByName(destinationIp).address
            } catch (e: Exception) {
                byteArrayOf(8, 8, 8, 8) // Fallback to safe DNS IP
            }
            if (destBytes.size != 4) {
                return synthesizePacket(protocol, "8.8.8.8", port)
            }
            
            val srcBytes = byteArrayOf(10, 0, 0, 2) // Standard tunnel IP
            val srcPort = 49152 + (Math.random() * 16383).toInt() // Random ephemeral port
            
            val ipHeaderLen = 20
            val transportHeaderLen = if (isTcp) 20 else 8
            val totalLen = ipHeaderLen + transportHeaderLen
            
            val packet = ByteArray(totalLen)
            
            // --- IPv4 Header (20 bytes) ---
            packet[0] = 0x45.toByte() // Version 4, IHL 5 (20 bytes)
            packet[1] = 0x00.toByte() // DSCP/ECN
            // Total Length
            packet[2] = ((totalLen shr 8) and 0xFF).toByte()
            packet[3] = (totalLen and 0xFF).toByte()
            // Identification
            packet[4] = 0x12.toByte()
            packet[5] = 0x34.toByte()
            // Flags (Don't Fragment) & Fragment Offset
            packet[6] = 0x40.toByte()
            packet[7] = 0x00.toByte()
            // TTL
            packet[8] = 64.toByte()
            // Protocol
            packet[9] = ipProto.toByte()
            // Checksum (Leaving as 0, Wireshark works fine)
            packet[10] = 0x00.toByte()
            packet[11] = 0x00.toByte()
            // Source IP
            System.arraycopy(srcBytes, 0, packet, 12, 4)
            // Destination IP
            System.arraycopy(destBytes, 0, packet, 16, 4)
            
            // --- Transport Header (TCP/UDP) ---
            if (isTcp) {
                // Source Port
                packet[20] = ((srcPort shr 8) and 0xFF).toByte()
                packet[21] = (srcPort and 0xFF).toByte()
                // Destination Port
                packet[22] = ((port shr 8) and 0xFF).toByte()
                packet[23] = (port and 0xFF).toByte()
                // Sequence Number (0)
                // Acknowledgment Number (0)
                // Data Offset (5 = 20 bytes TCP header)
                packet[32] = 0x50.toByte()
                // Flags: SYN (0x02)
                packet[33] = 0x02.toByte()
                // Window Size (64240)
                packet[34] = 0xFA.toByte()
                packet[35] = 0xF0.toByte()
            } else {
                // Source Port
                packet[20] = ((srcPort shr 8) and 0xFF).toByte()
                packet[21] = (srcPort and 0xFF).toByte()
                // Destination Port
                packet[22] = ((port shr 8) and 0xFF).toByte()
                packet[23] = (port and 0xFF).toByte()
                // Length (UDP header + payload = 8 bytes)
                packet[24] = ((transportHeaderLen shr 8) and 0xFF).toByte()
                packet[25] = (transportHeaderLen and 0xFF).toByte()
            }
            
            return packet
        } catch (e: Exception) {
            return ByteArray(40) // Fallback
        }
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
        try {
            val srcBytes = try {
                java.net.InetAddress.getByName(srcIp).address
            } catch (e: Exception) {
                byteArrayOf(10, 0, 0, 2)
            }
            val destBytes = try {
                java.net.InetAddress.getByName(dstIp).address
            } catch (e: Exception) {
                byteArrayOf(8, 8, 8, 8)
            }
            if (srcBytes.size != 4 || destBytes.size != 4) return

            val totalLen = 40 + payloadLength
            val packet = ByteArray(totalLen)

            // --- IPv4 Header (20 bytes) ---
            packet[0] = 0x45.toByte() // Version 4, IHL 5 (20 bytes)
            packet[1] = 0x00.toByte() // TOS
            packet[2] = ((totalLen shr 8) and 0xFF).toByte()
            packet[3] = (totalLen and 0xFF).toByte()
            packet[4] = 0x12.toByte()
            packet[5] = 0x34.toByte()
            packet[6] = 0x40.toByte() // Flags: Don't Fragment
            packet[7] = 0x00.toByte()
            packet[8] = 64.toByte()   // TTL
            packet[9] = 6.toByte()    // Protocol (TCP = 6)
            packet[10] = 0.toByte()
            packet[11] = 0.toByte()
            System.arraycopy(srcBytes, 0, packet, 12, 4)
            System.arraycopy(destBytes, 0, packet, 16, 4)

            // Compute IP Checksum
            var ipSum = 0
            for (i in 0 until 10) {
                val word = ((packet[i * 2].toInt() and 0xFF) shl 8) or (packet[i * 2 + 1].toInt() and 0xFF)
                ipSum += word
            }
            while (ipSum shr 16 != 0) {
                ipSum = (ipSum and 0xFFFF) + (ipSum shr 16)
            }
            val ipChecksum = (ipSum.inv() and 0xFFFF)
            packet[10] = ((ipChecksum shr 8) and 0xFF).toByte()
            packet[11] = (ipChecksum and 0xFF).toByte()

            // --- TCP Header (20 bytes) ---
            packet[20] = ((srcPort shr 8) and 0xFF).toByte()
            packet[21] = (srcPort and 0xFF).toByte()
            packet[22] = ((dstPort shr 8) and 0xFF).toByte()
            packet[23] = (dstPort and 0xFF).toByte()
            // Seq
            packet[24] = ((seq shr 24) and 0xFF).toByte()
            packet[25] = ((seq shr 16) and 0xFF).toByte()
            packet[26] = ((seq shr 8) and 0xFF).toByte()
            packet[27] = (seq and 0xFF).toByte()
            // Ack
            packet[28] = ((ack shr 24) and 0xFF).toByte()
            packet[29] = ((ack shr 16) and 0xFF).toByte()
            packet[30] = ((ack shr 8) and 0xFF).toByte()
            packet[31] = (ack and 0xFF).toByte()
            packet[32] = 0x50.toByte() // Data Offset 5 (20 bytes)
            packet[33] = flags
            packet[34] = 0xFA.toByte() // Win Size
            packet[35] = 0xF0.toByte()
            packet[36] = 0.toByte()
            packet[37] = 0.toByte()
            packet[38] = 0.toByte()
            packet[39] = 0.toByte()

            // Copy TCP Payload
            if (payloadLength > 0) {
                System.arraycopy(payload, payloadOffset, packet, 40, payloadLength)
            }

            // Compute TCP Checksum
            val tcpLength = 20 + payloadLength
            var pseudoSum = 0
            // Src IP
            pseudoSum += ((srcBytes[0].toInt() and 0xFF) shl 8) or (srcBytes[1].toInt() and 0xFF)
            pseudoSum += ((srcBytes[2].toInt() and 0xFF) shl 8) or (srcBytes[3].toInt() and 0xFF)
            // Dst IP
            pseudoSum += ((destBytes[0].toInt() and 0xFF) shl 8) or (destBytes[1].toInt() and 0xFF)
            pseudoSum += ((destBytes[2].toInt() and 0xFF) shl 8) or (destBytes[3].toInt() and 0xFF)
            // Protocol + TCP Length
            pseudoSum += 6
            pseudoSum += tcpLength

            // TCP Header (words 10 to 19)
            for (i in 10 until 20) {
                val word = ((packet[i * 2].toInt() and 0xFF) shl 8) or (packet[i * 2 + 1].toInt() and 0xFF)
                pseudoSum += word
            }
            // Payload
            var p = 0
            while (p < payloadLength - 1) {
                val word = ((payload[payloadOffset + p].toInt() and 0xFF) shl 8) or (payload[payloadOffset + p + 1].toInt() and 0xFF)
                pseudoSum += word
                p += 2
            }
            if (payloadLength % 2 != 0) {
                val word = ((payload[payloadOffset + payloadLength - 1].toInt() and 0xFF) shl 8)
                pseudoSum += word
            }

            while (pseudoSum shr 16 != 0) {
                pseudoSum = (pseudoSum and 0xFFFF) + (pseudoSum shr 16)
            }
            val tcpChecksum = (pseudoSum.inv() and 0xFFFF)
            packet[36] = ((tcpChecksum shr 8) and 0xFF).toByte()
            packet[37] = (tcpChecksum and 0xFF).toByte()

            writePacketToPcap(context, packageName, packet, timestampMs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write TCP payload packet to PCAP", e)
        }
    }

    /**
     * Helper to write raw network packets in standard little-endian PCAP binary format.
     */
    @Synchronized
    fun writePacketToPcap(
        context: Context,
        packageName: String,
        packetBytes: ByteArray,
        timestampMs: Long
    ) {
        try {
            val directory = File(context.filesDir, "threats")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val pcapFile = File(directory, "report_${packageName}.pcap")
            val exists = pcapFile.exists()
            
            FileOutputStream(pcapFile, true).use { stream ->
                if (!exists) {
                    // Write Global Header (24 bytes, little-endian)
                    val globalHeader = byteArrayOf(
                        0xd4.toByte(), 0xc3.toByte(), 0xb2.toByte(), 0xa1.toByte(), // Magic
                        0x02.toByte(), 0x00.toByte(),                             // Major version 2
                        0x04.toByte(), 0x00.toByte(),                             // Minor version 4
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // GMT correction 0
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // Accuracy 0
                        0xff.toByte(), 0xff.toByte(), 0x00.toByte(), 0x00.toByte(), // SnapLen 65535
                        0x65.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()  // LinkType 101 (Raw IP)
                    )
                    stream.write(globalHeader)
                }
                
                // Write Packet Header (16 bytes, little-endian)
                val sec = timestampMs / 1000
                val usec = (timestampMs % 1000) * 1000
                val len = packetBytes.size
                
                val pcapHeader = ByteArray(16)
                // Seconds
                pcapHeader[0] = (sec and 0xFF).toByte()
                pcapHeader[1] = ((sec shr 8) and 0xFF).toByte()
                pcapHeader[2] = ((sec shr 16) and 0xFF).toByte()
                pcapHeader[3] = ((sec shr 24) and 0xFF).toByte()
                // Microseconds
                pcapHeader[4] = (usec and 0xFF).toByte()
                pcapHeader[5] = ((usec shr 8) and 0xFF).toByte()
                pcapHeader[6] = ((usec shr 16) and 0xFF).toByte()
                pcapHeader[7] = ((usec shr 24) and 0xFF).toByte()
                // Saved Length
                pcapHeader[8] = (len and 0xFF).toByte()
                pcapHeader[9] = ((len shr 8) and 0xFF).toByte()
                pcapHeader[10] = ((len shr 16) and 0xFF).toByte()
                pcapHeader[11] = ((len shr 24) and 0xFF).toByte()
                // Original Length
                pcapHeader[12] = (len and 0xFF).toByte()
                pcapHeader[13] = ((len shr 8) and 0xFF).toByte()
                pcapHeader[14] = ((len shr 16) and 0xFF).toByte()
                pcapHeader[15] = ((len shr 24) and 0xFF).toByte()
                
                stream.write(pcapHeader)
                stream.write(packetBytes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write packet to PCAP file", e)
        }
    }
}

