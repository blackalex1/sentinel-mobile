package com.xprox.sentinel.service

import android.util.Log
import com.xprox.sentinel.config.XrayConfigManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.Socket

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
            val results = com.xprox.sentinel.core.SentinelCore.batchPing(
                listOf(com.xprox.sentinel.core.models.PingTarget(id = profile.id, address = profile.address, port = profile.port)),
                timeoutMs = 2000
            )
            val first = results.firstOrNull()
            if (first != null && first.success) {
                pingMsFlow.value = first.latencyMs.toInt()
            } else {
                pingMsFlow.value = null
            }
        }
    }

    /**
     * Obtains the public IP address strictly delegated to Sentinel-Core Go engine.
     */
    suspend fun suspendFetchPublicIp(
        socksPort: Int = 0,
        username: String? = null,
        token: String? = null
    ): String? = withContext(Dispatchers.IO) {
        val nativeInfo = com.xprox.sentinel.core.SentinelCore.getPublicIP(
            socksPort = socksPort,
            authUsername = username ?: "",
            authPassword = token ?: "",
            timeoutMs = 3500
        )
        nativeInfo?.ip?.takeIf { it.isNotEmpty() }
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

