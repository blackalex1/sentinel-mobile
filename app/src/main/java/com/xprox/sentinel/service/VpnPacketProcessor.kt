package com.xprox.sentinel.service

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.xprox.sentinel.parser.PacketParser
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles raw reading from the TUN Interface in a dedicated background thread,
 * inspects package ownership for Zero Trust threat blackholing, logs blocked traffic,
 * drops blocked packets, and loopbacks allowed packets to prevent connection hang.
 */
class VpnPacketProcessor(
    private val context: Context,
    private val vpnInterface: ParcelFileDescriptor,
    private val tracker: ConnectionTracker
) : Runnable {
    private val TAG = "VpnPacketProcessor"
    
    @Volatile
    private var isRunning = true

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val appResolver = AppResolver(context)

    private data class ResolvedConnection(val appName: String, val packageName: String)
    private val connectionCache = ConcurrentHashMap<String, ResolvedConnection>()

    override fun run() {
        Log.i(TAG, "VPN packet processing thread successfully started")
        
        val inputStream = FileInputStream(vpnInterface.fileDescriptor)
        val outputStream = FileOutputStream(vpnInterface.fileDescriptor)
        val buffer = ByteArray(32767)

        try {
            while (isRunning) {
                val length = inputStream.read(buffer)
                if (length > 0) {
                    // 0. Leakage protection Kill Switch check
                    if (com.xprox.sentinel.config.XrayProfilePersistence.loadKillSwitch(context)) {
                        // Drop the packet immediately to prevent unencrypted leaks
                        continue
                    }

                    var shouldDrop = false
                    
                    // 1. Zero Trust total app blackhole check & dynamic forensics logging
                    try {
                        val parsed = PacketParser.parse(buffer, length)
                        if (parsed != null) {
                            val cacheKey = "${parsed.sourceIp}:${parsed.sourcePort}->${parsed.destinationIp}:${parsed.destinationPort}:${parsed.protocol}"
                            
                            var resolved = connectionCache[cacheKey]
                            if (resolved == null) {
                                var ownerUid = -1
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    try {
                                        val localAddr = InetAddress.getByName(parsed.sourceIp)
                                        val remoteAddr = InetAddress.getByName(parsed.destinationIp)
                                        
                                        ownerUid = connectivityManager.getConnectionOwnerUid(
                                            parsed.protocol,
                                            InetSocketAddress(localAddr, parsed.sourcePort),
                                            InetSocketAddress(remoteAddr, parsed.destinationPort)
                                        )
                                    } catch (e: Exception) {}
                                }
                                
                                val appInfo = appResolver.resolveApp(ownerUid)
                                resolved = ResolvedConnection(appInfo.first, appInfo.second)
                                
                                // Bounded cache self-cleanup
                                if (connectionCache.size >= 1000) {
                                    connectionCache.clear()
                                }
                                connectionCache[cacheKey] = resolved
                            }
                            
                            val packageName = resolved.packageName
                            val appName = resolved.appName

                            if (ThreatDetectionManager.isAppBlocked(packageName)) {
                                shouldDrop = true
                                
                                // Dynamically log the blocked connection attempt!
                                // This records it in threat_[package].log, report_[package].txt and emits it to compose UI
                                ThreatDetectionManager.registerConnectionAttempt(
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
                                    rawBytes = buffer.copyOfRange(0, length)
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // Fail safe
                    }

                    if (shouldDrop) {
                        // Blackhole: simply drop the packet by skipping outputStream.write!
                        continue
                    }

                    // Send to tracker to inspect and log sensitive port access
                    tracker.trackPacket(buffer, length)
                    
                    // Route/Forward packet back to local network loop
                    outputStream.write(buffer, 0, length)
                }
            }
        } catch (e: Exception) {
            if (isRunning) {
                Log.e(TAG, "Exception in VpnPacketProcessor loop", e)
            }
        } finally {
            try {
                inputStream.close()
                outputStream.close()
            } catch (e: Exception) {
                // Ignore closing exceptions
            }
            Log.i(TAG, "VPN packet processing thread stopped")
        }
    }

    fun stop() {
        isRunning = false
    }
}
