package com.xprox.sentinel.service

import android.util.Log
import com.xprox.sentinel.config.XrayConfigManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.URL
import java.util.regex.Pattern

object VpnNetworkHelper {
    private const val TAG = "VpnNetworkHelper"

    private val IPV4_PATTERN = Pattern.compile("""\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\b""")
    private val IPV6_PATTERN = Pattern.compile("""\b(?:[A-Fa-f0-9]{1,4}:){7}[A-Fa-f0-9]{1,4}\b""")

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

    private fun fetchSingleEndpoint(urlStr: String, proxy: Proxy, timeoutMs: Int = 3000): String? {
        return try {
            val url = URL(urlStr)
            val connection = (url.openConnection(proxy) as java.net.HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                useCaches = false
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                setRequestProperty("Accept", "text/plain, application/json, */*")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }.trim()
            val v4Matcher = IPV4_PATTERN.matcher(body)
            if (v4Matcher.find()) {
                return v4Matcher.group(0)
            }
            val v6Matcher = IPV6_PATTERN.matcher(body)
            if (v6Matcher.find()) {
                return v6Matcher.group(0)
            }
            null
        } catch (e: Exception) {
            Log.d(TAG, "Fetch IP failed for $urlStr: ${e.message}")
            null
        }
    }

    suspend fun suspendFetchPublicIp(
        socksPort: Int = 0,
        username: String? = null,
        token: String? = null
    ): String? = withContext(Dispatchers.IO) {
        // Primary fastest & most reliable services chosen by user
        val primaryEndpoints = listOf(
            "https://ipwho.is/?output=text",
            "https://ifconfig.co/ip"
        )
        // Fallback endpoints in case primary are unreachable
        val fallbackEndpoints = listOf(
            "https://api.ipify.org",
            "https://checkip.amazonaws.com"
        )

        if (socksPort > 0 && !username.isNullOrEmpty() && !token.isNullOrEmpty()) {
            java.net.Authenticator.setDefault(object : java.net.Authenticator() {
                override fun getPasswordAuthentication(): java.net.PasswordAuthentication {
                    return java.net.PasswordAuthentication(username, token.toCharArray())
                }
            })
        }

        val proxy = if (socksPort > 0) {
            Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", socksPort))
        } else {
            Proxy.NO_PROXY
        }

        // 1. Race primary endpoints concurrently
        val primaryJobs = primaryEndpoints.map { url ->
            async { fetchSingleEndpoint(url, proxy, timeoutMs = 2500) }
        }

        // Pick first non-null result
        for (job in primaryJobs) {
            val res = job.await()
            if (!res.isNullOrEmpty()) {
                primaryJobs.forEach { it.cancel() }
                return@withContext res
            }
        }

        // 2. If primary failed, try fallback endpoints
        for (url in fallbackEndpoints) {
            val res = fetchSingleEndpoint(url, proxy, timeoutMs = 3000)
            if (!res.isNullOrEmpty()) {
                return@withContext res
            }
        }

        null
    }

    fun fetchPublicIp(
        socksPort: Int = 0,
        username: String? = null,
        token: String? = null,
        publicIpFlow: MutableStateFlow<String?>
    ) {
        publicIpFlow.value = null
        CoroutineScope(Dispatchers.IO).launch {
            val ip = suspendFetchPublicIp(socksPort, username, token)
            if (ip != null) {
                publicIpFlow.value = ip
            }
        }
    }
}

