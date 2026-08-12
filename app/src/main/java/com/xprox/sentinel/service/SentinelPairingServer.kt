package com.xprox.sentinel.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.xprox.sentinel.config.XrayProfilePersistence
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

class SentinelPairingServer(
    private val context: Context,
    private val listenPort: Int = 18080
) {
    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newFixedThreadPool(4)
    @Volatile
    private var isRunning = false

    fun start(
        socksPort: Int,
        httpPort: Int,
        username: String?,
        token: String?
    ) {
        try {
            stop()
            serverSocket = ServerSocket(listenPort)
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
            Log.i("SentinelPairingServer", "Sentinel Pairing Server started on port $listenPort")
        } catch (e: Exception) {
            Log.e("SentinelPairingServer", "Failed to start pairing server", e)
        }
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

                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            context,
                            "🔑 Сопряжение с $clientName (Код: $pinCode) ОДОБРЕНО!",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    val isSocksEnabled = XrayProfilePersistence.loadLanSharingSocks(context)
                    val jsonResp = JSONObject().apply {
                        put("success", true)
                        put("proxyType", if (isSocksEnabled) "SOCKS5" else "HTTP")
                        put("port", if (isSocksEnabled) socksPort else httpPort)
                        if (!username.isNullOrEmpty()) put("username", username)
                        if (!token.isNullOrEmpty()) put("password", token)
                        put("pinVerified", pinCode)
                    }

                    val respBytes = jsonResp.toString().toByteArray(Charsets.UTF_8)
                    val response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/json; charset=UTF-8\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Content-Length: ${respBytes.size}\r\n" +
                            "Connection: close\r\n\r\n"
                    out.write(response.toByteArray(Charsets.UTF_8))
                    out.write(respBytes)
                    out.flush()
                    return
                }

                val errorBytes = "{\"success\":false}".toByteArray(Charsets.UTF_8)
                val errorResp = "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Type: application/json\r\n" +
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
