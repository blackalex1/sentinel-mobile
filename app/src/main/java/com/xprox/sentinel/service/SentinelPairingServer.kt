package com.xprox.sentinel.service

import android.content.Context
import android.util.Log
import com.xprox.sentinel.config.XrayProfilePersistence
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
                Log.w("SentinelPairingServer", "Port $p is busy or unavailable, trying next...")
            }
        }

        if (boundSocket == null) {
            Log.e("SentinelPairingServer", "All candidate pairing ports are occupied!")
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
                    Log.e("SentinelPairingServer", "Error accepting client", e)
                }
            }
        }
        Log.i("SentinelPairingServer", "Sentinel Pairing Server successfully started on port $activePort")
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
                var line = reader.readLine()
                while (!line.isNullOrEmpty()) {
                    if (line.startsWith("Content-Length:", ignoreCase = true)) {
                        contentLength = line.substring(15).trim().toIntOrNull() ?: 0
                    }
                    line = reader.readLine()
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

                // Handle fast ping / health probe
                if (requestLine.startsWith("GET /pair/ping", ignoreCase = true) ||
                    requestLine.startsWith("GET /ping", ignoreCase = true) ||
                    requestLine.startsWith("GET /pair/status", ignoreCase = true)) {
                    val pingBytes = "{\"status\":\"ok\",\"service\":\"SentinelPairingServer\",\"activePort\":$activePort}".toByteArray(Charsets.UTF_8)
                    val response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/json; charset=UTF-8\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                            "Access-Control-Allow-Headers: *\r\n" +
                            "Content-Length: ${pingBytes.size}\r\n" +
                            "Connection: close\r\n\r\n"
                    out.write(response.toByteArray(Charsets.UTF_8))
                    out.write(pingBytes)
                    out.flush()
                    return
                }

                // Handle direct active proxy configuration request
                if (requestLine.startsWith("GET /pair/config", ignoreCase = true) ||
                    requestLine.startsWith("GET /config", ignoreCase = true)) {
                    val isSocksEnabled = XrayProfilePersistence.loadLanSharingSocks(context)
                    val isHttpEnabled = XrayProfilePersistence.loadLanSharingHttp(context)
                    val proxyType = if (isSocksEnabled) "SOCKS5" else if (isHttpEnabled) "HTTP" else "SOCKS5"
                    val port = if (proxyType == "SOCKS5") socksPort else httpPort
                    val isAuth = XrayProfilePersistence.loadLanSharingAuth(context)

                    val jsonResp = JSONObject().apply {
                        put("status", "ok")
                        put("success", true)
                        put("proxyType", proxyType)
                        put("port", port)
                        put("socksPort", socksPort)
                        put("httpPort", httpPort)
                        put("authRequired", isAuth)
                        if (isAuth && !username.isNullOrEmpty()) put("username", username)
                        if (isAuth && !token.isNullOrEmpty()) put("password", token)
                    }

                    val respBytes = jsonResp.toString().toByteArray(Charsets.UTF_8)
                    val response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/json; charset=UTF-8\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                            "Access-Control-Allow-Headers: *\r\n" +
                            "Content-Length: ${respBytes.size}\r\n" +
                            "Connection: close\r\n\r\n"
                    out.write(response.toByteArray(Charsets.UTF_8))
                    out.write(respBytes)
                    out.flush()
                    return
                }

                if (requestLine.startsWith("POST /pair/request", ignoreCase = true)) {
                    val bodyChars = CharArray(contentLength)
                    if (contentLength > 0) {
                        var readSoFar = 0
                        while (readSoFar < contentLength) {
                            val r = reader.read(bodyChars, readSoFar, contentLength - readSoFar)
                            if (r == -1) break
                            readSoFar += r
                        }
                    }
                    val requestBody = String(bodyChars)
                    val jsonReq = if (requestBody.isNotBlank()) JSONObject(requestBody) else JSONObject()
                    val clientName = jsonReq.optString("clientName", "Windows PC")
                    val pinCode = jsonReq.optString("pinCode", "0000")

                    // Asynchronously request interactive approval on Android screen
                    val latch = CountDownLatch(1)
                    var userApproved = false

                    SentinelPairingManager.requestApproval(clientName, pinCode) { approved ->
                        userApproved = approved
                        latch.countDown()
                    }

                    // Wait up to 30 seconds for user to tap [Разрешить] on phone screen
                    val responded = latch.await(30, TimeUnit.SECONDS)

                    if (responded && userApproved) {
                        val isSocksEnabled = XrayProfilePersistence.loadLanSharingSocks(context)
                        val isHttpEnabled = XrayProfilePersistence.loadLanSharingHttp(context)
                        val proxyType = if (isSocksEnabled) "SOCKS5" else if (isHttpEnabled) "HTTP" else "SOCKS5"
                        val port = if (proxyType == "SOCKS5") socksPort else httpPort
                        val isAuth = XrayProfilePersistence.loadLanSharingAuth(context)

                        val jsonResp = JSONObject().apply {
                            put("success", true)
                            put("proxyType", proxyType)
                            put("port", port)
                            put("socksPort", socksPort)
                            put("httpPort", httpPort)
                            put("authRequired", isAuth)
                            if (isAuth && !username.isNullOrEmpty()) put("username", username)
                            if (isAuth && !token.isNullOrEmpty()) put("password", token)
                            put("pinVerified", pinCode)
                        }

                        val respBytes = jsonResp.toString().toByteArray(Charsets.UTF_8)
                        val response = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: application/json; charset=UTF-8\r\n" +
                                "Access-Control-Allow-Origin: *\r\n" +
                                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                                "Access-Control-Allow-Headers: *\r\n" +
                                "Content-Length: ${respBytes.size}\r\n" +
                                "Connection: close\r\n\r\n"
                        out.write(response.toByteArray(Charsets.UTF_8))
                        out.write(respBytes)
                        out.flush()
                        return
                    } else {
                        // User rejected or timed out
                        val errorBytes = "{\"success\":false,\"error\":\"Сопряжение отклонено пользователем или истекло время\"}".toByteArray(Charsets.UTF_8)
                        val errorResp = "HTTP/1.1 403 Forbidden\r\n" +
                                "Content-Type: application/json; charset=UTF-8\r\n" +
                                "Access-Control-Allow-Origin: *\r\n" +
                                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                                "Access-Control-Allow-Headers: *\r\n" +
                                "Content-Length: ${errorBytes.size}\r\n" +
                                "Connection: close\r\n\r\n"
                        out.write(errorResp.toByteArray(Charsets.UTF_8))
                        out.write(errorBytes)
                        out.flush()
                        return
                    }
                }

                val errorBytes = "{\"success\":false}".toByteArray(Charsets.UTF_8)
                val errorResp = "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Type: application/json; charset=UTF-8\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                        "Access-Control-Allow-Headers: *\r\n" +
                        "Content-Length: ${errorBytes.size}\r\n" +
                        "Connection: close\r\n\r\n"
                out.write(errorResp.toByteArray(Charsets.UTF_8))
                out.write(errorBytes)
                out.flush()
            }
        } catch (e: Exception) {
            Log.e("SentinelPairingServer", "Error handling socket request", e)
        }
    }

    fun stop() {
        try {
            isRunning = false
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            Log.e("SentinelPairingServer", "Error stopping pairing server", e)
        }
    }
}
