package com.xprox.sentinel.service

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import com.xprox.sentinel.log.LogManager
import java.net.InetAddress
import java.net.InetSocketAddress

/**
2026-05-31: High-performance connection event parser for Xray.
Parses standard inbound connection logs from Xray stdout, uses getConnectionOwnerUid()
to identify the initiating package name on Android 10+ (API 29+), and streams
them to the central LogManager.
 */
object ConnectionAuditParser {
    private const val TAG = "ConnectionAuditParser"

    // Matches standard Xray connection log patterns
    // Example: "accepted tcp:10.0.0.2:54321 accepted tcp:8.8.8.8:443"
    // Or: "[Info] xray.com/core/app/proxyman/inbound: connection accepted from 10.0.0.2:54321"
    private val tunConnectionRegex = Regex("""from\s+(tcp|udp):([\w.-]+):(\d+)\s+accepted\s+(tcp|udp):([\w.-]+):(\d+)""")
    private val connectionAcceptedRegex = Regex("""connection\s+accepted\s+from\s+([\w.-]+):(\d+)""")
    private val ipPortRegex = Regex("""(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}):(\d+)""")

    fun parseAndLog(context: Context, line: String) {
        try {
            // Check if log contains connection events
            if (!line.contains("accepted", ignoreCase = true)) {
                return
            }

            // 1. Try to match the standard native Xray TUN/SOCKS format:
            // "from tcp:10.0.0.2:38836 accepted tcp:91.219.148.131:443"
            val tunMatch = tunConnectionRegex.find(line)
            if (tunMatch != null) {
                val protocolStr = tunMatch.groupValues[1]
                val clientIp = tunMatch.groupValues[2]
                val clientPort = tunMatch.groupValues[3].toInt()
                val destIp = tunMatch.groupValues[5]
                val destPort = tunMatch.groupValues[6].toInt()
                
                val protocol = if (protocolStr.equals("udp", ignoreCase = true)) 17 else 6
                resolveAndLogConnection(context, protocol, clientIp, clientPort, destIp, destPort)
                return
            }

            // 2. Fall back to old format: "connection accepted from IP:port"
            val match = connectionAcceptedRegex.find(line)
            if (match != null) {
                val clientIp = match.groupValues[1]
                val clientPort = match.groupValues[2].toInt()

                // Extract any other destination IP and port from the same log line if present
                val allMatches = ipPortRegex.findAll(line).toList()
                var destIp = "0.0.0.0"
                var destPort = 80

                for (m in allMatches) {
                    val ip = m.groupValues[1]
                    val port = m.groupValues[2].toInt()
                    if (ip != clientIp && port != clientPort) {
                        destIp = ip
                        destPort = port
                        break
                    }
                }

                // If no other IP found, use a placeholder or fallback
                if (destIp == "0.0.0.0" && allMatches.isNotEmpty()) {
                    // Try to scan for general hostname/IP destinations (e.g. tcp:8.8.8.8:443)
                    val destMatch = Regex("""(tcp|udp):([\w.-]+):(\d+)""").find(line)
                    if (destMatch != null) {
                        destIp = destMatch.groupValues[2]
                        destPort = destMatch.groupValues[3].toInt()
                    }
                }

                val protocol = if (line.contains("udp", ignoreCase = true)) 17 else 6

                resolveAndLogConnection(context, protocol, clientIp, clientPort, destIp, destPort)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Xray connection log: $line", e)
        }
    }

    private fun resolveAndLogConnection(
        context: Context,
        protocol: Int,
        srcIp: String,
        srcPort: Int,
        destIp: String,
        destPort: Int
    ) {
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        var ownerUid = -1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val localAddr = InetAddress.getByName(srcIp)
                val remoteAddr = InetAddress.getByName(destIp)
                
                ownerUid = connManager.getConnectionOwnerUid(
                    protocol,
                    InetSocketAddress(localAddr, srcPort),
                    InetSocketAddress(remoteAddr, destPort)
                )
            } catch (e: Exception) {
                // Ignore name resolution failure
            }
        }

        val isHotspot = !srcIp.startsWith("127.") && 
                        !srcIp.startsWith("10.0.0.") && 
                        srcIp != "::1" && 
                        srcIp != "localhost"

        val appName: String
        val packageName: String

        if (isHotspot) {
            appName = "Hotspot Client ($srcIp)"
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
            destinationIp = destIp,
            port = destPort,
            protocol = if (protocol == 17) "UDP" else "TCP"
        )
    }
}
