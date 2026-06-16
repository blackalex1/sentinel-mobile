package com.xprox.sentinel.service

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class AppResolverTest {

    private lateinit var mockContext: Context
    private lateinit var mockPm: PackageManager

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockPm = mock(PackageManager::class.java)
        `when`(mockContext.packageManager).thenReturn(mockPm)
    }

    @Test
    fun testSystemUidResolution() {
        val resolver = AppResolver(mockContext)

        // UID 0 -> Kernel / Root
        val res0 = resolver.resolveApp(0)
        assertEquals("Kernel / Root", res0.first)
        assertEquals("android.system.kernel", res0.second)

        // UID -5 (invalid) -> Kernel / Root
        val resNegative = resolver.resolveApp(-5)
        assertEquals("Kernel / Root", resNegative.first)
        assertEquals("android.system.kernel", resNegative.second)

        // UID 1000 -> Android System
        val res1000 = resolver.resolveApp(1000)
        assertEquals("Android System", res1000.first)
        assertEquals("android.uid.system", res1000.second)

        // UID 1052 -> Network Daemon (netd)
        val res1052 = resolver.resolveApp(1052)
        assertEquals("Network Daemon (netd)", res1052.first)
        assertEquals("android.uid.netd", res1052.second)

        // Unknown system UID (e.g. 500) -> System Process (500)
        val res500 = resolver.resolveApp(500)
        assertEquals("System Process (500)", res500.first)
        assertEquals("android.system.uid.500", res500.second)
    }

    @Test
    fun testUserAppUidResolution() {
        val resolver = AppResolver(mockContext)
        val uid = 10085

        // Mock packages for UID
        `when`(mockPm.getPackagesForUid(uid)).thenReturn(arrayOf("com.user.application"))

        // Mock application info & label
        val appInfo = ApplicationInfo().apply {
            nonLocalizedLabel = "My User App"
        }
        `when`(mockPm.getApplicationInfo("com.user.application", 0)).thenReturn(appInfo)
        `when`(mockPm.getApplicationLabel(appInfo)).thenReturn("My User App")

        val resolved = resolver.resolveApp(uid)
        assertEquals("My User App", resolved.first)
        assertEquals("com.user.application", resolved.second)
    }

    @Test
    fun testEmptyPackagesResolution() {
        val resolver = AppResolver(mockContext)
        val uid = 10099

        // PackageManager returns null or empty package list
        `when`(mockPm.getPackagesForUid(uid)).thenReturn(null)

        val resolved = resolver.resolveApp(uid)
        assertEquals("Unknown App (10099)", resolved.first)
        assertEquals("unknown.uid.10099", resolved.second)
    }

    @Test
    fun testPackageManagerExceptionFallback() {
        val resolver = AppResolver(mockContext)
        val uid = 10123

        // PackageManager throws an exception
        `when`(mockPm.getPackagesForUid(uid)).thenThrow(RuntimeException("PackageManager has crashed!"))

        val resolved = resolver.resolveApp(uid)
        assertEquals("Unknown App (10123)", resolved.first)
        assertEquals("unknown.uid.10123", resolved.second)
    }
}
