package com.xprox.sentinel.config.parser

import android.util.Log
import com.xprox.sentinel.config.XrayConfigManager.ServerProfile
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Base64

object ShadowsocksParser {
    private const val TAG = "ShadowsocksParser"

    private fun decodeBase64(input: String): ByteArray {
        val cleaned = input.trim().replace("\\s".toRegex(), "")
        return try {
            Base64.getDecoder().decode(cleaned)
        } catch (e: Exception) {
            try {
                Base64.getUrlDecoder().decode(cleaned)
            } catch (e2: Exception) {
                Base64.getMimeDecoder().decode(cleaned)
            }
        }
    }

    fun parse(link: String): ServerProfile? {
        try {
            val trimmed = link.trim()
            if (!trimmed.startsWith("ss://", ignoreCase = true)) return null

            val uriContent = trimmed.substring(5)
            val hashIndex = uriContent.indexOf('#')
            val rawName = if (hashIndex != -1) uriContent.substring(hashIndex + 1) else "Imported Shadowsocks"
            val name = try {
                URLDecoder.decode(rawName, "UTF-8")
            } catch (e: Exception) {
                rawName
            }

            val cleanContent = if (hashIndex != -1) uriContent.substring(0, hashIndex) else uriContent

            // Separate potential query parameters outside Base64
            val questionIndex = cleanContent.indexOf('?')
            val base64AndHostPort = if (questionIndex != -1) cleanContent.substring(0, questionIndex) else cleanContent
            val paramsStr = if (questionIndex != -1) cleanContent.substring(questionIndex + 1) else ""

            var method = "aes-256-gcm"
            var password = ""
            var serverAddress = ""
            var port = 8388

            val atIndex = base64AndHostPort.indexOf('@')
            if (atIndex != -1) {
                // Case 1: ss://base64(method:password)@host:port
                val userInfoBase64 = base64AndHostPort.substring(0, atIndex)
                val hostPort = base64AndHostPort.substring(atIndex + 1)

                val decodedBytes = decodeBase64(userInfoBase64)
                
                val methodAndPassword = String(decodedBytes, Charsets.UTF_8)
                val colonIndex = methodAndPassword.indexOf(':')
                if (colonIndex != -1) {
                    method = methodAndPassword.substring(0, colonIndex)
                    password = methodAndPassword.substring(colonIndex + 1)
                } else {
                    password = methodAndPassword
                }

                val portColonIndex = hostPort.lastIndexOf(':')
                serverAddress = if (portColonIndex != -1) hostPort.substring(0, portColonIndex) else hostPort
                port = if (portColonIndex != -1) hostPort.substring(portColonIndex + 1).toIntOrNull() ?: 8388 else 8388
            } else {
                // Case 2: ss://base64(method:password@host:port)
                val decodedBytes = decodeBase64(base64AndHostPort)
                val fullDecoded = String(decodedBytes, Charsets.UTF_8)

                val decodedAtIndex = fullDecoded.lastIndexOf('@')
                if (decodedAtIndex == -1) return null

                val methodAndPassword = fullDecoded.substring(0, decodedAtIndex)
                val hostPort = fullDecoded.substring(decodedAtIndex + 1)

                val colonIndex = methodAndPassword.indexOf(':')
                if (colonIndex != -1) {
                    method = methodAndPassword.substring(0, colonIndex)
                    password = methodAndPassword.substring(colonIndex + 1)
                } else {
                    password = methodAndPassword
                }

                val portColonIndex = hostPort.lastIndexOf(':')
                serverAddress = if (portColonIndex != -1) hostPort.substring(0, portColonIndex) else hostPort
                port = if (portColonIndex != -1) hostPort.substring(portColonIndex + 1).toIntOrNull() ?: 8388 else 8388
            }

            // Parse optional query parameters for Shadowsocks transport/plugin options
            var network = "tcp"
            var path = method // Store encryption method in path
            var hostParam = ""
            var allowInsecure = false

            if (paramsStr.isNotEmpty()) {
                val pairs = paramsStr.split('&')
                for (pair in pairs) {
                    val idx = pair.indexOf('=')
                    if (idx != -1) {
                        val key = pair.substring(0, idx)
                        val value = try {
                            URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                        } catch (e: Exception) {
                            pair.substring(idx + 1)
                        }
                        when {
                            key.equals("type", ignoreCase = true) || key.equals("network", ignoreCase = true) -> network = value
                            key.equals("host", ignoreCase = true) -> hostParam = value
                            key.equals("allowInsecure", ignoreCase = true) -> allowInsecure = value.equals("true", ignoreCase = true)
                        }
                    }
                }
            }

            return ServerProfile(
                name = name,
                address = serverAddress,
                port = port,
                uuid = password, // Map password to uuid
                type = "SHADOWSOCKS",
                path = path, // Map method to path as expected by OutboundConfigBuilder
                network = network,
                host = hostParam,
                allowInsecure = allowInsecure
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Shadowsocks link", e)
            return null
        }
    }

    fun export(profile: ServerProfile): String {
        val encodedName = try {
            URLEncoder.encode(profile.name, "UTF-8")
        } catch (e: Exception) {
            profile.name
        }

        val method = profile.path.ifEmpty { "aes-256-gcm" }
        val password = profile.uuid
        val rawCredentials = "$method:$password"
        val credentialsBase64 = Base64.getEncoder().withoutPadding().encodeToString(
            rawCredentials.toByteArray(Charsets.UTF_8)
        )

        return "ss://$credentialsBase64@${profile.address}:${profile.port}#$encodedName"
    }
}
