package com.xprox.sentinel.service

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.xprox.sentinel.log.LogManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap

@Suppress("UNCHECKED_CAST")
class ThreatDetectionManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockPm: PackageManager

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockPm = mock(PackageManager::class.java)

        // Mock PackageName & ApplicationContext
        `when`(mockContext.packageName).thenReturn("com.xprox.sentinel")
        `when`(mockContext.applicationContext).thenReturn(mockContext)

        // Mock SharedPreferences
        val inMemoryPrefs = InMemorySharedPreferences()
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(inMemoryPrefs)

        // Mock PackageManager
        `when`(mockContext.packageManager).thenReturn(mockPm)
        val packageInfo = PackageInfo().apply {
            versionName = "1.0"
            firstInstallTime = System.currentTimeMillis()
        }
        val appInfo = ApplicationInfo().apply {
            sourceDir = "/fake/apk/path"
        }
        packageInfo.applicationInfo = appInfo
        `when`(mockPm.getPackageInfo(anyString(), anyInt())).thenReturn(packageInfo)

        // Mock filesDir for forensic logs
        val fakeFilesDir = File(System.getProperty("java.io.tmpdir"), "sentinel_test")
        fakeFilesDir.mkdirs()
        `when`(mockContext.filesDir).thenReturn(fakeFilesDir)

        // Mock Active Monitored Ports Set using reflection to bypass SharedPreferences
        val activePortsField = LogManager::class.java.getDeclaredField("activePortsSet").apply {
            isAccessible = true
        }
        activePortsField.set(null, setOf(22, 443))

        // Reset ThreatDetectionManager maps using reflection
        val connectionAttemptsField = ThreatDetectionManager::class.java.getDeclaredField("connectionAttempts").apply {
            isAccessible = true
        }
        val connectionAttempts = connectionAttemptsField.get(null) as ConcurrentHashMap<*, *>
        connectionAttempts.clear()

        val blockedAppsField = ThreatDetectionManager::class.java.getDeclaredField("blockedApps").apply {
            isAccessible = true
        }
        val blockedApps = blockedAppsField.get(null) as MutableSet<String>
        blockedApps.clear()

        val flaggedSystemAppsField = ThreatDetectionManager::class.java.getDeclaredField("flaggedSystemApps").apply {
            isAccessible = true
        }
        val flaggedSystemApps = flaggedSystemAppsField.get(null) as MutableSet<String>
        flaggedSystemApps.clear()
    }

    @Test
    fun testWarningAccumulationAndBlackholeTransition() {
        val appPackage = "com.suspicious.malware"
        val ip = "203.0.113.8"
        val port = 22 // Audited port

        // Attempt 1: accumulated warning (should NOT block)
        var isBlocked = ThreatDetectionManager.registerConnectionAttempt(
            mockContext, appPackage, "Malicious App", ip, port
        )
        assertFalse("First attempt should not trigger blackhole", isBlocked)
        assertFalse(ThreatDetectionManager.isAppBlocked(appPackage))

        // Attempt 2: accumulated warning (should NOT block)
        isBlocked = ThreatDetectionManager.registerConnectionAttempt(
            mockContext, appPackage, "Malicious App", ip, port
        )
        assertFalse("Second attempt should not trigger blackhole", isBlocked)
        assertFalse(ThreatDetectionManager.isAppBlocked(appPackage))

        // Attempt 3: LIMIT EXCEEDED (Threshold = 2, so 3rd attempt triggers block!)
        isBlocked = ThreatDetectionManager.registerConnectionAttempt(
            mockContext, appPackage, "Malicious App", ip, port
        )
        
        // Assert that the app has indeed been transitioned into the blackhole list successfully!
        assertTrue("Third attempt must trigger instant blackhole!", isBlocked)
        assertTrue(ThreatDetectionManager.isAppBlocked(appPackage))
        assertTrue(ThreatDetectionManager.getBlockedAppsList().contains(appPackage))
    }

    @Test
    fun testNonAuditedPortBypass() {
        val appPackage = "com.normal.app"
        val ip = "8.8.8.8"
        val port = 80 // Non-audited port

        // Send 5 attempts: should NEVER block because port 80 is not in activePortsSet
        for (i in 1..5) {
            val isBlocked = ThreatDetectionManager.registerConnectionAttempt(
                mockContext, appPackage, "Normal App", ip, port
            )
            assertFalse("Non-audited ports must be bypassed and never block!", isBlocked)
        }
        assertFalse(ThreatDetectionManager.isAppBlocked(appPackage))
    }

    @Test
    fun testSystemAppIsolationBypass() {
        val systemPackage = "android.system.kernel"
        val ip = "10.0.0.1"
        val port = 22 // Audited port

        // Send 3 attempts: Limit is exceeded, but it should NOT block (returns false) because it is a system app
        var isBlocked = false
        for (i in 1..3) {
            isBlocked = ThreatDetectionManager.registerConnectionAttempt(
                mockContext, systemPackage, "System Kernel", ip, port
            )
        }

        assertFalse("System apps must bypass block to prevent device lockouts!", isBlocked)
        assertFalse(ThreatDetectionManager.isAppBlocked(systemPackage))
        assertTrue("Suspicious system app must be flagged in UI!", 
            ThreatDetectionManager.flaggedSystemAppsFlow.value.contains(systemPackage))
    }
}

@Suppress("UNCHECKED_CAST")
class InMemorySharedPreferences : SharedPreferences {
    private val map = java.util.concurrent.ConcurrentHashMap<String, Any>()

    override fun getAll(): Map<String, *> = map
    override fun getString(key: String, defValue: String?): String? = map[key] as? String ?: defValue
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = map[key] as? Set<String> ?: defValues
    override fun getInt(key: String, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String): Boolean = map.containsKey(key)

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    inner class Editor : SharedPreferences.Editor {
        private val tempMap = HashMap<String, Any?>()
        private var clear = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            tempMap[key] = value
            return this
        }
        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
            tempMap[key] = values
            return this
        }
        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            tempMap[key] = value
            return this
        }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            tempMap[key] = value
            return this
        }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            tempMap[key] = value
            return this
        }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            tempMap[key] = value
            return this
        }
        override fun remove(key: String): SharedPreferences.Editor {
            tempMap[key] = null
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            clear = true
            return this
        }
        override fun commit(): Boolean {
            apply()
            return true
        }
        override fun apply() {
            if (clear) {
                map.clear()
            }
            tempMap.forEach { (key, value) ->
                if (value == null) {
                    map.remove(key)
                } else {
                    map[key] = value
                }
            }
        }
    }
}
