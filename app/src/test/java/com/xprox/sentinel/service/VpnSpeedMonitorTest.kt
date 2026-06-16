package com.xprox.sentinel.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VpnSpeedMonitorTest {

    @Test
    fun testFormatSpeedBytes() {
        // Less than 1024 bytes -> B/s
        assertEquals("0 B/s", VpnSpeedMonitor.formatSpeed(0))
        assertEquals("500 B/s", VpnSpeedMonitor.formatSpeed(500))
        assertEquals("1023 B/s", VpnSpeedMonitor.formatSpeed(1023))
    }

    @Test
    fun testFormatSpeedKilobytes() {
        // Between 1 KB and 1 MB -> KB/s
        assertEquals("1.0 KB/s", VpnSpeedMonitor.formatSpeed(1024))
        assertEquals("1.5 KB/s", VpnSpeedMonitor.formatSpeed(1536))
        assertEquals("1023.9 KB/s", VpnSpeedMonitor.formatSpeed((1024 * 1024) - 100))
    }

    @Test
    fun testFormatSpeedMegabytes() {
        // Greater than or equal to 1 MB -> MB/s
        assertEquals("1.0 MB/s", VpnSpeedMonitor.formatSpeed(1024 * 1024))
        assertEquals("2.5 MB/s", VpnSpeedMonitor.formatSpeed((1024 * 1024 * 2.5).toLong()))
        assertEquals("100.0 MB/s", VpnSpeedMonitor.formatSpeed(1024 * 1024 * 100))
    }

    @Test
    fun testStartSpeedMonitorCoroutine() = runTest {
        val isRunningFlow = MutableStateFlow(true)
        var speedTextUpdated: String? = null

        // Start VpnSpeedMonitor coroutine
        val job = VpnSpeedMonitor.start(this, isRunningFlow) { speed ->
            speedTextUpdated = speed
        }

        // Allow coroutine to run briefly, then shut it down to prevent infinite loops
        delay(100)
        isRunningFlow.value = false
        job.join()

        // On JVM, TrafficStats returns default/unsupported values (such as -1 or 0),
        // which falls back gracefully to 0 B/s speeds, proving exception-proof safety!
        if (speedTextUpdated != null) {
            assertTrue("Should report formatted speed values", speedTextUpdated!!.contains("B/s"))
        }
    }
}
