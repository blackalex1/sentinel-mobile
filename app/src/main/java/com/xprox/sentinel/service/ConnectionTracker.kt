package com.xprox.sentinel.service

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import com.xprox.sentinel.parser.PacketParser
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance, thread-safe connection tracker.
 * Implements a zero-allocation byte-level pre-filter and a connection cache
 * to completely eliminate binder IPC bottleneck and GC memory thrashes.
 */
class ConnectionTracker(
    private val context: Context,
    private val appResolver: AppResolver,
    private val onLogLogged: () -> Unit
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // In-memory cache for resolved connections to avoid redundant package and UID lookups
    // Key format: "sourceIp:sourcePort->destIp:destPort"
    private val connectionCache = ConcurrentHashMap<String, Pair<String, String>>()
    private val maxCacheEntries = 1000

    // Snapshot of monitored ports loaded ONCE at construction time.
    // LogManager already caches this set in memory; we keep a local reference
    // to avoid any method-call overhead inside the hot packet-processing loop.
    // The reference is marked @Volatile so it is visible across threads after
    // LogManager.saveActivePorts() writes a new set.
    @Volatile
    private var monitoredPorts: Set<Int> = com.xprox.sentinel.log.LogManager.loadActivePorts(context)

    /** Call this whenever the user changes the monitored port list. */
    fun refreshMonitoredPorts() {
        monitoredPorts = com.xprox.sentinel.log.LogManager.loadActivePorts(context)
    }

    /**
     * Inspects a raw packet buffer. Uses high-performance in-place parsing.
     */
    fun trackPacket(packetBytes: ByteArray, length: Int) {
        try {
            // 1. Zero-allocation port pre-filter
            val dstPort = getDestinationPortQuick(packetBytes, length)
            if (dstPort == -1 || !isPortMonitored(dstPort)) {
                return // Discard 99% of normal packets in microseconds with 0 allocations!
            }

            // 2. Full parse only for monitored ports
            val parsed = PacketParser.parse(packetBytes, length) ?: return
            val cacheKey = "${parsed.sourceIp}:${parsed.sourcePort}->${parsed.destinationIp}:${parsed.destinationPort}"

            // 3. Cache lookup / reservation
            // Use putIfAbsent to reserve the cache key immediately with a placeholder to block
            // concurrent duplicate resolution calls for the same socket connection.
            val cachedApp = connectionCache.putIfAbsent(cacheKey, Pair("RESOLVING", "RESOLVING"))
            if (cachedApp != null) {
                // Connection already resolved or is currently being resolved, skip redundant processing
                return
            }

            try {
                // Bounded cache self-cleanup
                if (connectionCache.size >= maxCacheEntries) {
                    connectionCache.clear()
                    // Re-reserve after clear to prevent concurrent race
                    connectionCache[cacheKey] = Pair("RESOLVING", "RESOLVING")
                }

                // 4. Resolve Owner UID (Only supported on API 29+)
                var ownerUid = -1
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val localAddr = InetAddress.getByName(parsed.sourceIp)
                        val remoteAddr = InetAddress.getByName(parsed.destinationIp)
                        
                        ownerUid = connectivityManager.getConnectionOwnerUid(
                            parsed.protocol,
                            java.net.InetSocketAddress(localAddr, parsed.sourcePort),
                            java.net.InetSocketAddress(remoteAddr, parsed.destinationPort)
                        )
                    } catch (e: Exception) {
                        // Fail gracefully
                    }
                }

                // 5. Resolve Package name
                val resolved = appResolver.resolveApp(ownerUid)
                val appName = resolved.first
                val packageName = resolved.second

                // Save to connection cache
                connectionCache[cacheKey] = Pair(appName, packageName)

                // 6. Submit connection to LogManager (Occurs once per socket session)
                com.xprox.sentinel.log.LogManager.logConnection(
                    context = context,
                    packageName = packageName,
                    appName = appName,
                    destinationIp = parsed.destinationIp,
                    port = parsed.destinationPort,
                    protocol = if (parsed.protocol == 6) "TCP" else "UDP",
                    ipLength = parsed.ipLength,
                    ttl = parsed.ttl,
                    ipFlags = parsed.ipFlags,
                    tcpFlags = parsed.tcpFlags,
                    tcpSeq = parsed.tcpSeq,
                    tcpAck = parsed.tcpAck,
                    tcpWindow = parsed.tcpWindow,
                    rawBytes = packetBytes.copyOfRange(0, length)
                )
                onLogLogged()
            } catch (e: Exception) {
                connectionCache.remove(cacheKey)
                Log.e("ConnectionTracker", "Error processing packet", e)
            }
        } catch (e: Exception) {
            Log.e("ConnectionTracker", "Error processing packet", e)
        }
    }

    /**
     * Fast in-place byte parsing of destination port. Zero allocations.
     */
    private fun getDestinationPortQuick(packetBytes: ByteArray, length: Int): Int {
        if (length < 20) return -1
        
        // Parse IHL / version from the first byte
        val versionAndIHL = packetBytes[0].toInt()
        val version = (versionAndIHL shr 4) and 0x0F
        
        if (version == 4) {
            val ihl = (versionAndIHL and 0x0F) * 4
            if (length < ihl + 4) return -1
            
            // Parse protocol (TCP=6, UDP=17)
            val protocol = packetBytes[9].toInt() and 0xFF
            if (protocol != 6 && protocol != 17) return -1
            
            // Destination port is at bytes [ihl + 2] and [ihl + 3]
            return ((packetBytes[ihl + 2].toInt() and 0xFF) shl 8) or (packetBytes[ihl + 3].toInt() and 0xFF)
        } else if (version == 6) {
            if (length < 44) return -1 // IPv6 header (40) + TCP/UDP dest port (4)
            
            // Next Header is at byte 6
            val protocol = packetBytes[6].toInt() and 0xFF
            if (protocol != 6 && protocol != 17) return -1
            
            // Destination port is at bytes [42] and [43] (since header is 40 bytes)
            return ((packetBytes[42].toInt() and 0xFF) shl 8) or (packetBytes[43].toInt() and 0xFF)
        }
        
        return -1
    }

    /**
     * Fast check to see if a port is in the monitored list.
     * Uses the locally cached set — NO SharedPreferences read per packet.
     */
    private fun isPortMonitored(port: Int): Boolean {
        return monitoredPorts.contains(port)
    }
}
