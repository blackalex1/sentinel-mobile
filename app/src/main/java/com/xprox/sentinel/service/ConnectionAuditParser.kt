package com.xprox.sentinel.service

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import com.xprox.sentinel.core.SentinelCore
import com.xprox.sentinel.log.LogManager
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * High-performance connection event parser for Xray / Sing-box / Hysteria 2.
 * Delegates log line parsing to Sentinel-Core native engine, uses getConnectionOwnerUid()
 * to identify the initiating package name on Android 10+ (API 29+), and streams
 * them to the central LogManager.
 */
object ConnectionAuditParser {
    private const val TAG = "ConnectionAuditParser"

    fun parseAndLog(context: Context, line: String) {
        try {
            val parsed = SentinelCore.parseConnectionLog(line) ?: return
            resolveAndLogConnection(context, parsed)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Xray connection log: $line", e)
        }
    }

    private fun resolveAndLogConnection(
        context: Context,
        parsed: com.xprox.sentinel.core.models.ParsedConnectionLog
    ) {
        val protocol = if (parsed.protocol.equals("UDP", ignoreCase = true)) 17 else 6
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        var ownerUid = -1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val localAddr = InetAddress.getByName(parsed.srcIp)
                val remoteAddr = InetAddress.getByName(parsed.destIp)
                
                ownerUid = connManager.getConnectionOwnerUid(
                    protocol,
                    InetSocketAddress(localAddr, parsed.srcPort),
                    InetSocketAddress(remoteAddr, parsed.destPort)
                )
            } catch (e: Exception) {
                // Ignore name resolution failure
            }
        }

        val appName: String
        val packageName: String

        if (parsed.isHotspot) {
            appName = "Hotspot Client (${parsed.srcIp})"
            packageName = "hotspot.client"
        } else {
            val appResolver = AppResolver(context)
            val resolved = appResolver.resolveApp(ownerUid)
            appName = resolved.first
            packageName = resolved.second
        }

        // Log sensitive connections using central LogManager
        LogManager.logConnection(
            context = context,
            packageName = packageName,
            appName = appName,
            destinationIp = parsed.destIp,
            port = parsed.destPort,
            protocol = if (protocol == 17) "UDP" else "TCP"
        )
    }
}
