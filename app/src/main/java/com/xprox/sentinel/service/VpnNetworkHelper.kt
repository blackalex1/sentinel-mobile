package com.xprox.sentinel.service

import android.util.Log
import com.xprox.sentinel.config.XrayConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.URL

object VpnNetworkHelper {
    private const val TAG = "VpnNetworkHelper"

    fun measureProfilePing(
        profile: XrayConfigManager.ServerProfile,
        pingMsFlow: MutableStateFlow<Int?>,
        bypassSocketProtect: ((Socket) -> Unit)? = null
    ) {
        pingMsFlow.value = null
        if (profile.address.isEmpty()) {
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val measuredPing = try {
                val ipToPing = try {
                    InetAddress.getByName(profile.address).hostAddress
                } catch (e: Exception) {
                    profile.address
                }
                val startTime = System.currentTimeMillis()
                Socket().use { socket ->
                    bypassSocketProtect?.invoke(socket)
                    socket.connect(InetSocketAddress(ipToPing, profile.port), 2000)
                }
                (System.currentTimeMillis() - startTime).toInt()
            } catch (e: Exception) {
                null
            }
            pingMsFlow.value = measuredPing
        }
    }

    suspend fun suspendFetchPublicIp(socksPort: Int = 0): String? {
        val endpoints = listOf(
            "https://api.ipify.org",
            "https://icanhazip.com",
            "https://checkip.amazonaws.com"
        )
        val proxy = if (socksPort > 0) {
            Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", socksPort))
        } else {
            Proxy.NO_PROXY
        }

        var resultIp: String? = null
        for (urlStr in endpoints) {
            try {
                val url = URL(urlStr)
                val connection = url.openConnection(proxy) as java.net.HttpURLConnection
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                connection.useCaches = false
                connection.requestMethod = "GET"
                
                val text = connection.inputStream.bufferedReader().use { it.readText() }.trim()
                val isIpv4 = text.matches(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}"""))
                val isIpv6 = !text.contains("<") && text.contains(":") && text.length in 3..45
                if (isIpv4 || isIpv6) {
                    resultIp = text
                    break
                }
            } catch (e: Exception) {
                Log.d(TAG, "Static fetch IP failed for $urlStr: ${e.message}")
            }
        }
        return resultIp
    }

    fun fetchPublicIp(
        socksPort: Int = 0,
        publicIpFlow: MutableStateFlow<String?>,
        bypassSocketProtect: ((Socket) -> Unit)? = null
    ) {
        publicIpFlow.value = null
        CoroutineScope(Dispatchers.IO).launch {
            val ip = suspendFetchPublicIp(socksPort)
            if (ip != null) {
                publicIpFlow.value = ip
            }
        }
    }
}
