package com.xprox.sentinel.service

import android.util.Log
import java.net.Inet4Address
import java.net.NetworkInterface

object TetheringScanner {
    private const val TAG = "TetheringScanner"

    private fun isPrivateIp(ipAddress: String): Boolean {
        return try {
            val parts = ipAddress.split(".")
            if (parts.size != 4 || parts.any { it.toIntOrNull() == null }) return false
            val p0 = parts[0].toInt()
            val p1 = parts[1].toInt()
            
            // Exclude loopback 127.0.0.0/8
            if (p0 == 127) return false
            // Exclude VPN internal range 10.0.0.0/24
            if (p0 == 10 && p1 == 0) return false
            
            // 10.0.0.0/8
            if (p0 == 10) return true
            // 172.16.0.0/12
            if (p0 == 172 && p1 in 16..31) return true
            // 192.168.0.0/16
            if (p0 == 192 && p1 == 168) return true
            // 100.64.0.0/10 (CGNAT)
            if (p0 == 100 && p1 in 64..127) return true
            
            false
        } catch (e: Exception) {
            false
        }
    }

    fun getActiveTetheringIps(): List<String> {
        val ips = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val name = iface.name.lowercase()
                
                // Hotspot, USB tethering, and other standard tethering interfaces
                if (name.contains("ap") || 
                    name.contains("wlan1") || 
                    name.contains("wlan2") || 
                    name.contains("rndis") || 
                    name.contains("tether") || 
                    name.contains("swlan") ||
                    name.contains("bridge")
                ) {
                    val addresses = iface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val ip = addr.hostAddress
                            if (ip != null && isPrivateIp(ip)) {
                                ips.add(ip)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to scan network interfaces", e)
        }
        
        // Fallback: search all interfaces except loopback and VPN, strictly private IPs
        if (ips.isEmpty()) {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val iface = interfaces.nextElement()
                    if (iface.isLoopback || iface.name.lowercase().contains("tun")) continue
                    val addresses = iface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val ip = addr.hostAddress
                            // Exclude primary Wi-Fi interface if possible (wlan0), ensure private IP
                            if (iface.name.lowercase() != "wlan0" && ip != null && isPrivateIp(ip)) {
                                ips.add(ip)
                            }
                        }
                    }
                }
            } catch (e: Exception) {}
        }
        return ips
    }
}
