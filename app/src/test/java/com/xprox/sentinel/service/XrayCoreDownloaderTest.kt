package com.xprox.sentinel.service

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class XrayCoreDownloaderTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private val prefMap = mutableMapOf<String, Any>()

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)

        prefMap.clear()

        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)

        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            val value = invocation.getArgument<Boolean>(1)
            prefMap[key] = value
            mockEditor
        }
        `when`(mockEditor.putString(anyString(), any())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            val value = invocation.getArgument<String?>(1)
            if (value != null) prefMap[key] = value
            mockEditor
        }

        `when`(mockPrefs.getBoolean(anyString(), anyBoolean())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            val defaultVal = invocation.getArgument<Boolean>(1)
            (prefMap[key] as? Boolean) ?: defaultVal
        }
        `when`(mockPrefs.getString(anyString(), any())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            val defaultVal = invocation.getArgument<String?>(1)
            (prefMap[key] as? String) ?: defaultVal
        }
    }

    @Test
    fun testAllowPrereleasePreference() {
        // Default should be false
        assertFalse(XrayCoreDownloader.getAllowPrerelease(mockContext))

        // Set to true
        XrayCoreDownloader.setAllowPrerelease(mockContext, true)
        assertTrue(XrayCoreDownloader.getAllowPrerelease(mockContext))

        // Set to false
        XrayCoreDownloader.setAllowPrerelease(mockContext, false)
        assertFalse(XrayCoreDownloader.getAllowPrerelease(mockContext))
    }

    @Test
    fun testDownloadUrlFormat() {
        val urlStable = XrayCoreDownloader.getDownloadUrl("v26.3.27")
        assertTrue(urlStable.contains("v26.3.27"))
        assertTrue(urlStable.contains("Xray-android-"))

        val urlPrerelease = XrayCoreDownloader.getDownloadUrl("v26.3.27-pre1")
        assertTrue(urlPrerelease.contains("v26.3.27-pre1"))
        assertTrue(urlPrerelease.endsWith(".zip"))
    }

    @Test
    fun testXrayReleaseInfoDataClass() {
        val info = XrayReleaseInfo("v26.3.27-pre1", true, "2026-07-28T12:00:00Z")
        assertEquals("v26.3.27-pre1", info.tagName)
        assertTrue(info.isPrerelease)
        assertEquals("2026-07-28T12:00:00Z", info.publishedAt)
    }
}
