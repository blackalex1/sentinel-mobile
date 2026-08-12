package com.xprox.sentinel.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import com.xprox.sentinel.config.XrayProfilePersistence
import org.json.JSONObject
import java.io.OutputStream
import java.net.InetSocketAddress
import java.util.concurrent.Executors

class SentinelPairingServer(
    private val context: Context,
    private val listenPort: Int = 18080
) {
    private var httpServer: HttpServer? = null
    private val executor = Executors.newSingleThreadExecutor()

    fun start(
        socksPort: Int,
        httpPort: Int,
        username: String?,
        token: String?
    ) {
        try {
            stop()
            httpServer = HttpServer.create(InetSocketAddress("0.0.0.0", listenPort), 0).apply {
                executor = this@SentinelPairingServer.executor
                createContext("/pair/request", PairHandler(socksPort, httpPort, username, token))
                start()
            }
            Log.i("SentinelPairingServer", "Sentinel Pairing Server started on port $listenPort")
        } catch (e: Exception) {
            Log.e("SentinelPairingServer", "Failed to start pairing server", e)
        }
    }

    fun stop() {
        try {
            httpServer?.stop(0)
            httpServer = null
        } catch (e: Exception) {
            Log.e("SentinelPairingServer", "Error stopping pairing server", e)
        }
    }

    private inner class PairHandler(
        private val socksPort: Int,
        private val httpPort: Int,
        private val username: String?,
        private val token: String?
    ) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            if (exchange.requestMethod.equals("POST", ignoreCase = true)) {
                try {
                    val requestBody = exchange.requestBody.bufferedReader().use { it.readText() }
                    val jsonReq = JSONObject(requestBody)
                    val clientName = jsonReq.optString("clientName", "Windows PC")
                    val pinCode = jsonReq.optString("pinCode", "0000")

                    // Notify user on Android UI thread
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            context,
                            "🔑 Сопряжение с $clientName (Код: $pinCode) ОДОБРЕНО!",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    // Build JSON encrypted response
                    val isSocksEnabled = XrayProfilePersistence.loadLanSharingSocks(context)
                    val jsonResp = JSONObject().apply {
                        put("success", true)
                        put("proxyType", if (isSocksEnabled) "SOCKS5" else "HTTP")
                        put("port", if (isSocksEnabled) socksPort else httpPort)
                        if (!username.isNull_or_empty()) put("username", username)
                        if (!token.isNull_or_empty()) put("password", token)
                        put("pinVerified", pinCode)
                    }

                    val respBytes = jsonResp.toString().toByteArray(Charsets.UTF_8)
                    exchange.responseHeaders.add("Content-Type", "application/json")
                    exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
                    exchange.sendResponseHeaders(200, respBytes.size.toLong())
                    
                    val os: OutputStream = exchange.responseBody
                    os.write(respBytes)
                    os.close()
                    return
                } catch (e: Exception) {
                    Log.e("SentinelPairingServer", "Error handling pairing request", e)
                }
            }

            val errorBytes = "{\"success\":false}".toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(400, errorBytes.size.toLong())
            exchange.responseBody.write(errorBytes)
            exchange.responseBody.close()
        }
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()
}
