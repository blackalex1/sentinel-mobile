package com.xprox.sentinel.service.downloader

import com.xprox.sentinel.core.SentinelCore
import org.junit.Assert.*
import org.junit.Test

class SentinelCoreDownloaderTest {

    @Test
    fun testAssetMatchingForDifferentAbis() {
        val testRelease = SentinelReleaseInfo(
            tagName = "v0.0.0.4-beta",
            name = "Sentinel-Core v0.0.0.4-beta",
            body = "Beta release with new AST routing",
            isPrerelease = true,
            publishedAt = "2026-08-19T13:08:09Z",
            assets = listOf(
                SentinelReleaseAsset("libsentinel-core-android-arm.so", "https://example.com/arm.so", 7255440L, "armeabi-v7a"),
                SentinelReleaseAsset("libsentinel-core-android-arm64.so", "https://example.com/arm64.so", 7289144L, "arm64-v8a"),
                SentinelReleaseAsset("libsentinel-core-android-x86_64.so", "https://example.com/x86_64.so", 7783000L, "x86_64"),
                SentinelReleaseAsset("sentinel-core-windows-amd64.dll", "https://example.com/win.dll", 7264256L, "unknown")
            )
        )

        val arm64Asset = SentinelCoreDownloader.findMatchingAsset(testRelease, "arm64-v8a")
        assertNotNull("Must match arm64-v8a asset", arm64Asset)
        assertEquals("libsentinel-core-android-arm64.so", arm64Asset!!.name)

        val armv7Asset = SentinelCoreDownloader.findMatchingAsset(testRelease, "armeabi-v7a")
        assertNotNull("Must match armeabi-v7a asset", armv7Asset)
        assertEquals("libsentinel-core-android-arm.so", armv7Asset!!.name)

        val x8664Asset = SentinelCoreDownloader.findMatchingAsset(testRelease, "x86_64")
        assertNotNull("Must match x86_64 asset", x8664Asset)
        assertEquals("libsentinel-core-android-x86_64.so", x8664Asset!!.name)
    }

    @Test
    fun testSentinelCoreReloadDoesNotCrash() {
        assertTrue("Native core must be active initially", SentinelCore.isAvailable())
        SentinelCore.reload()
        assertTrue("Native core must remain available after reload", SentinelCore.isAvailable())
    }
}
