package com.xprox.sentinel.service.vpn

import android.net.TrafficStats
import java.text.DecimalFormat

object VpnTrafficCounter {

    data class TrafficSnapshot(
        val rxBytes: Long,
        val txBytes: Long,
        val timestampMs: Long
    )

    data class SpeedMetrics(
        val rxSpeedBytesPerSec: Long,
        val txSpeedBytesPerSec: Long,
        val totalRxBytes: Long,
        val totalTxBytes: Long,
        val downloadSpeedFormatted: String,
        val uploadSpeedFormatted: String,
        val downloadTotalFormatted: String,
        val uploadTotalFormatted: String
    )

    private var initialRxBytes: Long = 0
    private var initialTxBytes: Long = 0
    private var lastRxBytes: Long = 0
    private var lastTxBytes: Long = 0
    private var lastTimestampMs: Long = 0

    fun resetSession(uid: Int = android.os.Process.myUid()) {
        val rx = if (uid != -1) TrafficStats.getUidRxBytes(uid) else TrafficStats.getTotalRxBytes()
        val tx = if (uid != -1) TrafficStats.getUidTxBytes(uid) else TrafficStats.getTotalTxBytes()
        initialRxBytes = if (rx != TrafficStats.UNSUPPORTED.toLong()) rx else 0
        initialTxBytes = if (tx != TrafficStats.UNSUPPORTED.toLong()) tx else 0
        lastRxBytes = initialRxBytes
        lastTxBytes = initialTxBytes
        lastTimestampMs = System.currentTimeMillis()
    }

    fun calculateSpeed(uid: Int = android.os.Process.myUid()): SpeedMetrics {
        val now = System.currentTimeMillis()
        val currentRx = if (uid != -1) TrafficStats.getUidRxBytes(uid) else TrafficStats.getTotalRxBytes()
        val currentTx = if (uid != -1) TrafficStats.getUidTxBytes(uid) else TrafficStats.getTotalTxBytes()

        val actualRx = if (currentRx != TrafficStats.UNSUPPORTED.toLong()) currentRx else 0
        val actualTx = if (currentTx != TrafficStats.UNSUPPORTED.toLong()) currentTx else 0

        val timeDiffSec = ((now - lastTimestampMs) / 1000.0).coerceAtLeast(0.1)

        val rxDiff = (actualRx - lastRxBytes).coerceAtLeast(0)
        val txDiff = (actualTx - lastTxBytes).coerceAtLeast(0)

        val rxSpeed = (rxDiff / timeDiffSec).toLong()
        val txSpeed = (txDiff / timeDiffSec).toLong()

        val totalRx = (actualRx - initialRxBytes).coerceAtLeast(0)
        val totalTx = (actualTx - initialTxBytes).coerceAtLeast(0)

        lastRxBytes = actualRx
        lastTxBytes = actualTx
        lastTimestampMs = now

        return SpeedMetrics(
            rxSpeedBytesPerSec = rxSpeed,
            txSpeedBytesPerSec = txSpeed,
            totalRxBytes = totalRx,
            totalTxBytes = totalTx,
            downloadSpeedFormatted = formatSpeed(rxSpeed),
            uploadSpeedFormatted = formatSpeed(txSpeed),
            downloadTotalFormatted = formatBytes(totalRx),
            uploadTotalFormatted = formatBytes(totalTx)
        )
    }

    fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 * 1024 -> "${DecimalFormat("#.##").format(bytesPerSec / (1024.0 * 1024.0 * 1024.0))} GB/s"
            bytesPerSec >= 1024 * 1024 -> "${DecimalFormat("#.##").format(bytesPerSec / (1024.0 * 1024.0))} MB/s"
            bytesPerSec >= 1024 -> "${DecimalFormat("#.#").format(bytesPerSec / 1024.0)} KB/s"
            else -> "$bytesPerSec B/s"
        }
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> "${DecimalFormat("#.##").format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
            bytes >= 1024 * 1024 -> "${DecimalFormat("#.##").format(bytes / (1024.0 * 1024.0))} MB"
            bytes >= 1024 -> "${DecimalFormat("#.#").format(bytes / 1024.0)} KB"
            else -> "$bytes B"
        }
    }
}
