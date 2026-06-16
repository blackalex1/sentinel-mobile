package com.xprox.sentinel.service

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Helper to resolve App Package names and Friendly Names from connection UIDs.
 */
class AppResolver(private val context: Context) {
    private val pm: PackageManager = context.packageManager

    private val SYSTEM_UIDS = mapOf(
        0 to Pair("Kernel / Root", "android.system.kernel"),
        1000 to Pair("Android System", "android.uid.system"),
        1001 to Pair("Telephony / Radio", "android.uid.phone"),
        1013 to Pair("Media Server", "android.uid.media"),
        1020 to Pair("GPS Daemon", "android.uid.gps"),
        1027 to Pair("NFC Service", "android.uid.nfc"),
        1051 to Pair("DNS Resolver", "android.uid.dnsresolver"),
        1052 to Pair("Network Daemon (netd)", "android.uid.netd"),
        1073 to Pair("WebView Zygote", "android.uid.webview_zygote")
    )

    companion object {
        // Cache package resolution across all instances of AppResolver
        private val resolvedCache = java.util.concurrent.ConcurrentHashMap<Int, Pair<String, String>>()
    }

    /**
     * Resolves the app name and package name for a given system User ID (UID).
     */
    fun resolveApp(uid: Int): Pair<String, String> {
        if (uid <= 0) return Pair("Kernel / Root", "android.system.kernel")

        val cached = resolvedCache[uid]
        if (cached != null) {
            return cached
        }

        // 1. Check known system UIDs map (< 10000)
        if (uid < 10000) {
            val systemApp = SYSTEM_UIDS[uid]
            val resolved = if (systemApp != null) {
                systemApp
            } else {
                Pair("System Process ($uid)", "android.system.uid.$uid")
            }
            resolvedCache[uid] = resolved
            return resolved
        }

        // 2. Resolve installed application package
        val resolved = try {
            val packages = pm.getPackagesForUid(uid)
            if (!packages.isNullOrEmpty()) {
                val packageName = packages[0]
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val appName = pm.getApplicationLabel(appInfo).toString()
                Pair(appName, packageName)
            } else {
                Pair("Unknown App ($uid)", "unknown.uid.$uid")
            }
        } catch (e: Exception) {
            Pair("Unknown App ($uid)", "unknown.uid.$uid")
        }
        
        resolvedCache[uid] = resolved
        return resolved
    }
}
