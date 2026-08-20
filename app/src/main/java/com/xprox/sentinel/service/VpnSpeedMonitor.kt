package com.xprox.sentinel.service

import android.net.TrafficStats
import com.xprox.sentinel.core.SentinelCore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

/**
 * High performance VPN traffic speed monitor.
 * Delegates speed formatting to SentinelCore native engine.
 */
object VpnSpeedMonitor {
    fun formatSpeed(bytesPerSec: Long): String {
        return SentinelCore.formatTrafficSpeed(bytesPerSec, 0L).substringAfter("↓ ").substringBefore("  |")
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

                    val speedText = SentinelCore.formatTrafficSpeed(rxSpeed, txSpeed)
                    onSpeedUpdated(speedText)
                }

                lastRxBytes = currentRxBytes
                lastTxBytes = currentTxBytes
                lastTime = currentTime
            }
        }
    }
}
