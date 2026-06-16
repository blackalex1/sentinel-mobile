package com.xprox.sentinel.service

import android.net.TrafficStats
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

object VpnSpeedMonitor {
    fun formatSpeed(bytesPerSec: Long): String {
        return if (bytesPerSec < 1024) {
            "$bytesPerSec B/s"
        } else if (bytesPerSec < 1024 * 1024) {
            String.format(java.util.Locale.US, "%.1f KB/s", bytesPerSec / 1024.0)
        } else {
            String.format(java.util.Locale.US, "%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
        }
    }

    fun start(
        scope: CoroutineScope,
        isRunningFlow: StateFlow<Boolean>,
        onSpeedUpdated: (String) -> Unit
    ): Job {
        return scope.launch(Dispatchers.IO) {
            val myUid = android.os.Process.myUid()
            var lastRxBytes = TrafficStats.getUidRxBytes(myUid)
            var lastTxBytes = TrafficStats.getUidTxBytes(myUid)
            var lastTime = System.currentTimeMillis()

            // Use isActive (coroutine cancellation) instead of isRunningFlow.value so
            // the loop exits immediately when serviceScope is cancelled rather than
            // waiting up to 1500 ms for the next while-condition check.
            while (isActive && isRunningFlow.value) {
                delay(1500)
                if (!isActive || !isRunningFlow.value) break

                val currentRxBytes = TrafficStats.getUidRxBytes(myUid)
                val currentTxBytes = TrafficStats.getUidTxBytes(myUid)
                val currentTime = System.currentTimeMillis()

                val timeDiff = (currentTime - lastTime) / 1000.0
                if (timeDiff > 0) {
                    val rxSpeed = if (currentRxBytes >= 0 && lastRxBytes >= 0 && currentRxBytes >= lastRxBytes) {
                        ((currentRxBytes - lastRxBytes) / timeDiff).toLong()
                    } else {
                        0L
                    }
                    val txSpeed = if (currentTxBytes >= 0 && lastTxBytes >= 0 && currentTxBytes >= lastTxBytes) {
                        ((currentTxBytes - lastTxBytes) / timeDiff).toLong()
                    } else {
                        0L
                    }

                    val speedText = "↓ ${formatSpeed(rxSpeed)}  |  ↑ ${formatSpeed(txSpeed)}"
                    onSpeedUpdated(speedText)
                }

                lastRxBytes = currentRxBytes
                lastTxBytes = currentTxBytes
                lastTime = currentTime
            }
        }
    }
}
