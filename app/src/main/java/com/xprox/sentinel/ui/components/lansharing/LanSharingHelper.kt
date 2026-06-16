package com.xprox.sentinel.ui.components.lansharing

import java.net.Inet4Address
import java.net.NetworkInterface

object LanSharingHelper {
    fun isHotspotInterfaceActive(): Boolean {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val name = iface.name.lowercase()
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
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {}
        return false
    }
}
