package com.xprox.sentinel.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Base64
import android.util.Log
import com.xprox.sentinel.config.XrayProfilePersistence
import com.xprox.sentinel.crypto.SentinelCrypto
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SentinelPairingServer(
    private val context: Context,
    private val preferredPort: Int = 18080
) {
    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newFixedThreadPool(4)
    @Volatile
    private var isRunning = false
    var activePort: Int = preferredPort
        private set

    companion object {
        private const val TAG = "SentinelPairingServer"
        val CANDIDATE_PORTS = listOf(18080, 18081, 18082, 19080, 19081)
    }

    fun start(
        socksPort: Int,
        httpPort: Int,
        username: String?,
        token: String?
    ) {
        stop()

        val portsToTry = listOf(preferredPort) + CANDIDATE_PORTS.filter { it != preferredPort }
        var boundSocket: ServerSocket? = null
        var boundPort = preferredPort

        for (p in portsToTry) {
            try {
                val ss = ServerSocket(p)
                ss.reuseAddress = true
                boundSocket = ss
                boundPort = p
                break
            } catch (e: Exception) {
                Log.w(TAG, "Port $p is busy or unavailable, trying next...")
            }
        }

        if (boundSocket == null) {
            Log.e(TAG, "All candidate pairing ports are occupied!")
            return
        }

        serverSocket = boundSocket
        activePort = boundPort
        isRunning = true

        executor.execute {
            while (isRunning) {
                try {
                    val client = serverSocket?.accept() ?: break
                    executor.execute {
                        handleClient(client, socksPort, httpPort, username, token)
                    }
                } catch (e: Exception) {
                    if (!isRunning) break
                    Log.e(TAG, "Error accepting client", e)
                }
            }
        }
        Log.i(TAG, "Sentinel Pairing Server successfully started on port $activePort")
    }

    private fun handleClient(
        socket: Socket,
        socksPort: Int,
        httpPort: Int,
        username: String?,
        token: String?
    ) {
        try {
            socket.use { s ->
                val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                val out: OutputStream = s.getOutputStream()

                val requestLine = reader.readLine() ?: return
                var contentLength = 0
                var authHeader: String? = null
                var line = reader.readLine()
                while (!line.isNullOrEmpty()) {
                    if (line.startsWith("Content-Length:", ignoreCase = true)) {
                        contentLength = line.substring(15).trim().toIntOrNull() ?: 0
                    } else if (line.startsWith("Authorization:", ignoreCase = true)) {
                        authHeader = line.substring(14).trim()
                    }
                    line = reader.readLine()
                }

                val bearerToken = if (authHeader?.startsWith("Bearer ", ignoreCase = true) == true) {
                    authHeader.substring(7).trim()
                } else null

                // Read request body if present
                var requestBody = ""
                if (contentLength > 0) {
                    val bodyChars = CharArray(contentLength)
                    var readSoFar = 0
                    while (readSoFar < contentLength) {
                        val r = reader.read(bodyChars, readSoFar, contentLength - readSoFar)
                        if (r == -1) break
                        readSoFar += r
                    }
                    requestBody = String(bodyChars, 0, readSoFar)
                }

                // Handle CORS preflight OPTIONS request
                if (requestLine.startsWith("OPTIONS", ignoreCase = true)) {
                    val response = "HTTP/1.1 204 No Content\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                            "Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With, *\r\n" +
                            "Access-Control-Max-Age: 86400\r\n" +
                            "Connection: close\r\n\r\n"
                    out.write(response.toByteArray(Charsets.UTF_8))
                    out.flush()
                    return
                }

                // Fast ping / health probe
                if (requestLine.startsWith("GET /pair/ping", ignoreCase = true) ||
                    requestLine.startsWith("GET /ping", ignoreCase = true) ||
                    requestLine.startsWith("GET /pair/status", ignoreCase = true)) {
                    val activeClient = XrayProfilePersistence.activeHotspotSessionClient.value
                    val isLanSharing = XrayProfilePersistence.loadLanSharing(context)
                    val pingJson = JSONObject().apply {
                        put("status", "ok")
                        put("service", "SentinelPairingServer")
                        put("activePort", activePort)
                        put("hotspotActive", isLanSharing)
                        if (!activeClient.isNullOrEmpty()) {
                            put("activeClient", activeClient)
                        }
                    }
                    val pingBytes = pingJson.toString().toByteArray(Charsets.UTF_8)
                    sendJsonResponse(out, 200, "OK", pingBytes)
                    return
                }

                // 1. Initial Pairing Request: POST /pair/request
                if (requestLine.startsWith("POST /pair/request", ignoreCase = true)) {
                    val jsonReq = if (requestBody.isNotBlank()) JSONObject(requestBody) else JSONObject()
                    val clientName = jsonReq.optString("clientName", "Sentinel Desktop Windows")
                    val pinCode = if (jsonReq.has("pinCode")) jsonReq.optString("pinCode", "0000") else jsonReq.optString("pin", "0000")

                    // Asynchronously request interactive approval on Android screen
                    val latch = CountDownLatch(1)
                    var userApproved = false

                    HotspotNotificationHelper.showPairingAttemptNotification(context, clientName, pinCode)

                    SentinelPairingManager.requestApproval(clientName, pinCode) { approved ->
                        userApproved = approved
                        latch.countDown()
                    }

                    // Wait up to 30 seconds for user to tap [Разрешить] on phone screen
                    val responded = latch.await(30, TimeUnit.SECONDS)
                    HotspotNotificationHelper.dismissPairingAttemptNotification(context)

                    if (responded && userApproved) {
                        // 1. Generate 256-bit MasterKey and DeviceToken
                        val masterKeyBytes = SentinelCrypto.generate256BitKey()
                        val masterKeyBase64 = Base64.encodeToString(masterKeyBytes, Base64.NO_WRAP)
                        val deviceToken = SentinelCrypto.generateDeviceToken()

                        // 2. Save paired device persistently
                        XrayProfilePersistence.savePairedDevice(context, deviceToken, masterKeyBase64, clientName)
                        XrayProfilePersistence.setHotspotActiveSession(clientName)

                        // 3. Automatically enable Hotspot / LAN sharing in persistence
                        XrayProfilePersistence.saveLanSharing(context, true)
                        XrayProfilePersistence.saveLanSharingSocks(context, true)
                        XrayProfilePersistence.saveLanSharingHttp(context, true)

                        // 4. Reload VPN service if running to open inbound ports immediately
                        reloadVpnServiceIfRunning()

                        val isSocksEnabled = XrayProfilePersistence.loadLanSharingSocks(context)
                        val isHttpEnabled = XrayProfilePersistence.loadLanSharingHttp(context)
                        val proxyType = if (isSocksEnabled) "SOCKS5" else if (isHttpEnabled) "HTTP" else "SOCKS5"

                        val activeLiveSocks = VpnManagerService.activeLanSocksPort.value
                        val activeLiveHttp = VpnManagerService.activeLanHttpPort.value
                        val effectiveSocksPort = if (activeLiveSocks > 0) activeLiveSocks else socksPort
                        val effectiveHttpPort = if (activeLiveHttp > 0) activeLiveHttp else httpPort
                        val port = if (proxyType == "SOCKS5") effectiveSocksPort else effectiveHttpPort
                        val isAuth = XrayProfilePersistence.loadLanSharingAuth(context)

                        // 5. Build confidential payload
                        val confidentialPayload = JSONObject().apply {
                            put("token", deviceToken)
                            put("masterKey", masterKeyBase64)
                            put("proxyType", proxyType)
                            put("port", port)
                            put("socksPort", effectiveSocksPort)
                            put("httpPort", effectiveHttpPort)
                            put("authRequired", isAuth)
                            if (isAuth && !username.isNullOrEmpty()) put("username", username)
                            if (isAuth && !token.isNullOrEmpty()) put("password", token)
                            put("pinVerified", pinCode)
                        }

                        // Encrypt with PIN-derived key (AES-256-GCM)
                        val pinKey = SentinelCrypto.deriveKeyFromPin(pinCode)
                        val encryptedPayload = SentinelCrypto.encrypt(confidentialPayload.toString(), pinKey)

                        val jsonResp = JSONObject().apply {
                            put("success", true)
                            put("encryptedPayload", encryptedPayload)
                            // Legacy/fallback unencrypted fields for backward compatibility
                            put("token", deviceToken)
                            put("masterKey", masterKeyBase64)
                            put("proxyType", proxyType)
                            put("port", port)
                            put("socksPort", effectiveSocksPort)
                            put("httpPort", effectiveHttpPort)
                            put("authRequired", isAuth)
                            if (isAuth && !username.isNullOrEmpty()) put("username", username)
                            if (isAuth && !token.isNullOrEmpty()) put("password", token)
                        }

                        val respBytes = jsonResp.toString().toByteArray(Charsets.UTF_8)
                        HotspotNotificationHelper.showHotspotProxyingNotification(context, clientName, effectiveSocksPort)
                        sendJsonResponse(out, 200, "OK", respBytes)
                        return
                    } else {
                        // User rejected or timed out
                        val errorBytes = "{\"success\":false,\"error\":\"Сопряжение отклонено пользователем или истекло время\"}".toByteArray(Charsets.UTF_8)
                        sendJsonResponse(out, 403, "Forbidden", errorBytes)
                        return
                    }
                }

                // 2. Encrypted Session Start: POST /hotspot/session/start
                if (requestLine.startsWith("POST /hotspot/session/start", ignoreCase = true) ||
                    requestLine.startsWith("POST /pair/connect", ignoreCase = true)) {
                    val tokenToVerify = bearerToken ?: extractTokenFromRawBody(requestBody)
                    if (tokenToVerify == null) {
                        val err = "{\"success\":false,\"error\":\"Отсутствует токен авторизации устройства\"}".toByteArray(Charsets.UTF_8)
                        sendJsonResponse(out, 401, "Unauthorized", err)
                        return
                    }

                    val masterKey = XrayProfilePersistence.getPairedDeviceKey(context, tokenToVerify)
                    if (masterKey == null) {
                        val err = "{\"success\":false,\"error\":\"Неизвестное или неавторизованное устройство\"}".toByteArray(Charsets.UTF_8)
                        sendJsonResponse(out, 401, "Unauthorized", err)
                        return
                    }

                    // Decrypt request body if encrypted
                    var clientName = "Sentinel Desktop"
                    try {
                        val encryptedBody = extractEncryptedPayload(requestBody)
                        if (encryptedBody.isNotEmpty()) {
                            val decrypted = SentinelCrypto.decrypt(encryptedBody, masterKey)
                            val decJson = JSONObject(decrypted)
                            clientName = decJson.optString("client", "Sentinel Desktop")
                            val timestamp = decJson.optLong("timestamp", 0L)
                            val now = System.currentTimeMillis() / 1000
                            if (timestamp > 0 && Math.abs(now - timestamp) > 120) {
                                val err = "{\"success\":false,\"error\":\"Таймаут запроса (возможна атака повтора)\"}".toByteArray(Charsets.UTF_8)
                                sendJsonResponse(out, 400, "Bad Request", err)
                                return
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not decrypt start session payload, continuing with token authorization", e)
                    }

                    // Dynamically enable hotspot proxying inbounds
                    XrayProfilePersistence.saveLanSharing(context, true)
                    XrayProfilePersistence.saveLanSharingSocks(context, true)
                    XrayProfilePersistence.saveLanSharingHttp(context, true)
                    XrayProfilePersistence.setHotspotActiveSession(clientName)

                    reloadVpnServiceIfRunning()

                    val isSocksEnabled = XrayProfilePersistence.loadLanSharingSocks(context)
                    val isHttpEnabled = XrayProfilePersistence.loadLanSharingHttp(context)
                    val proxyType = if (isSocksEnabled) "SOCKS5" else if (isHttpEnabled) "HTTP" else "SOCKS5"

                    val activeLiveSocks = VpnManagerService.activeLanSocksPort.value
                    val activeLiveHttp = VpnManagerService.activeLanHttpPort.value
                    val effectiveSocksPort = if (activeLiveSocks > 0) activeLiveSocks else socksPort
                    val effectiveHttpPort = if (activeLiveHttp > 0) activeLiveHttp else httpPort
                    val port = if (proxyType == "SOCKS5") effectiveSocksPort else effectiveHttpPort
                    val isAuth = XrayProfilePersistence.loadLanSharingAuth(context)

                    val responseData = JSONObject().apply {
                        put("status", "ok")
                        put("active", true)
                        put("proxyType", proxyType)
                        put("port", port)
                        put("socksPort", effectiveSocksPort)
                        put("httpPort", effectiveHttpPort)
                        put("authRequired", isAuth)
                        if (isAuth && !username.isNullOrEmpty()) put("username", username)
                        if (isAuth && !token.isNullOrEmpty()) put("password", token)
                    }

                    val encResponse = SentinelCrypto.encrypt(responseData.toString(), masterKey)
                    val respJson = JSONObject().apply {
                        put("success", true)
                        put("encryptedPayload", encResponse)
                        put("port", port)
                        put("socksPort", effectiveSocksPort)
                        put("httpPort", effectiveHttpPort)
                        put("proxyType", proxyType)
                    }

                    HotspotNotificationHelper.showHotspotProxyingNotification(context, clientName, effectiveSocksPort)
                    sendJsonResponse(out, 200, "OK", respJson.toString().toByteArray(Charsets.UTF_8))
                    return
                }

                // 3. Encrypted Session Stop: POST /hotspot/session/stop
                if (requestLine.startsWith("POST /hotspot/session/stop", ignoreCase = true) ||
                    requestLine.startsWith("POST /pair/disconnect", ignoreCase = true)) {
                    val tokenToVerify = bearerToken ?: extractTokenFromRawBody(requestBody)
                    if (tokenToVerify == null) {
                        val err = "{\"success\":false,\"error\":\"Отсутствует токен авторизации устройства\"}".toByteArray(Charsets.UTF_8)
                        sendJsonResponse(out, 401, "Unauthorized", err)
                        return
                    }

                    val masterKey = XrayProfilePersistence.getPairedDeviceKey(context, tokenToVerify)
                    if (masterKey == null) {
                        val err = "{\"success\":false,\"error\":\"Неизвестное устройство\"}".toByteArray(Charsets.UTF_8)
                        sendJsonResponse(out, 401, "Unauthorized", err)
                        return
                    }

                    XrayProfilePersistence.setHotspotActiveSession(null)
                    // Disable hotspot inbounds and switch back to Standby
                    XrayProfilePersistence.saveLanSharing(context, false)
                    reloadVpnServiceIfRunning()
                    HotspotNotificationHelper.dismissHotspotProxyingNotification(context)

                    val responseData = JSONObject().apply {
                        put("status", "stopped")
                        put("active", false)
                    }
                    val encResponse = SentinelCrypto.encrypt(responseData.toString(), masterKey)
                    val respJson = JSONObject().apply {
                        put("success", true)
                        put("encryptedPayload", encResponse)
                    }

                    sendJsonResponse(out, 200, "OK", respJson.toString().toByteArray(Charsets.UTF_8))
                    return
                }

                // 4. Encrypted Proxy Configuration: GET /pair/config
                if (requestLine.startsWith("GET /pair/config", ignoreCase = true) ||
                    requestLine.startsWith("GET /config", ignoreCase = true)) {
                    if (bearerToken == null) {
                        val err = "{\"success\":false,\"error\":\"Требуется Bearer токен авторизации\"}".toByteArray(Charsets.UTF_8)
                        sendJsonResponse(out, 401, "Unauthorized", err)
                        return
                    }

                    val masterKey = XrayProfilePersistence.getPairedDeviceKey(context, bearerToken)
                    if (masterKey == null) {
                        val err = "{\"success\":false,\"error\":\"Недействительный токен устройства\"}".toByteArray(Charsets.UTF_8)
                        sendJsonResponse(out, 401, "Unauthorized", err)
                        return
                    }

                    val isSocksEnabled = XrayProfilePersistence.loadLanSharingSocks(context)
                    val isHttpEnabled = XrayProfilePersistence.loadLanSharingHttp(context)
                    val proxyType = if (isSocksEnabled) "SOCKS5" else if (isHttpEnabled) "HTTP" else "SOCKS5"
                    val activeLiveSocks = VpnManagerService.activeLanSocksPort.value
                    val activeLiveHttp = VpnManagerService.activeLanHttpPort.value
                    val effectiveSocksPort = if (activeLiveSocks > 0) activeLiveSocks else socksPort
                    val effectiveHttpPort = if (activeLiveHttp > 0) activeLiveHttp else httpPort
                    val port = if (proxyType == "SOCKS5") effectiveSocksPort else effectiveHttpPort
                    val isAuth = XrayProfilePersistence.loadLanSharingAuth(context)

                    val confidentialPayload = JSONObject().apply {
                        put("status", "ok")
                        put("success", true)
                        put("proxyType", proxyType)
                        put("port", port)
                        put("socksPort", effectiveSocksPort)
                        put("httpPort", effectiveHttpPort)
                        put("authRequired", isAuth)
                        if (isAuth && !username.isNullOrEmpty()) put("username", username)
                        if (isAuth && !token.isNullOrEmpty()) put("password", token)
                    }

                    val encPayload = SentinelCrypto.encrypt(confidentialPayload.toString(), masterKey)
                    val respJson = JSONObject().apply {
                        put("success", true)
                        put("encryptedPayload", encPayload)
                    }

                    HotspotNotificationHelper.showSyncNotification(context, "Sentinel Desktop")
                    sendJsonResponse(out, 200, "OK", respJson.toString().toByteArray(Charsets.UTF_8))
                    return
                }

                val errorBytes = "{\"success\":false,\"error\":\"Неизвестный запрос\"}".toByteArray(Charsets.UTF_8)
                sendJsonResponse(out, 400, "Bad Request", errorBytes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling socket request", e)
        }
    }

    private fun reloadVpnServiceIfRunning() {
        if (VpnManagerService.isRunningFlow.value) {
            try {
                val reloadIntent = Intent(context, VpnManagerService::class.java).apply {
                    action = VpnManagerService.ACTION_RELOAD_CONFIG
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(reloadIntent)
                } else {
                    context.startService(reloadIntent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to reload VpnManagerService", e)
            }
        }
    }

    private fun extractTokenFromRawBody(body: String): String? {
        return try {
            if (body.isBlank()) return null
            val j = JSONObject(body)
            j.optString("token", "").ifEmpty { null }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractEncryptedPayload(body: String): String {
        return try {
            if (body.isBlank()) return ""
            val j = JSONObject(body)
            j.optString("encryptedPayload", j.optString("payload", ""))
        } catch (e: Exception) {
            ""
        }
    }

    private fun sendJsonResponse(out: OutputStream, statusCode: Int, statusText: String, jsonBytes: ByteArray) {
        val response = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: *\r\n" +
                "Content-Length: ${jsonBytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        out.write(response.toByteArray(Charsets.UTF_8))
        out.write(jsonBytes)
        out.flush()
    }

    fun stop() {
        try {
            isRunning = false
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping pairing server", e)
        }
    }
}
