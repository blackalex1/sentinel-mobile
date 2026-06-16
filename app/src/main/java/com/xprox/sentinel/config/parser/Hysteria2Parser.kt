package com.xprox.sentinel.config.parser

import android.util.Log
import com.xprox.sentinel.config.XrayConfigManager.ServerProfile
import java.net.URLDecoder
import java.net.URLEncoder

object Hysteria2Parser {
    private const val TAG = "Hysteria2Parser"

    fun parse(link: String): ServerProfile? {
        try {
            val trimmed = link.trim()
            val isHy2 = trimmed.startsWith("hy2://", ignoreCase = true)
            val isHysteria2 = trimmed.startsWith("hysteria2://", ignoreCase = true)
            if (!isHy2 && !isHysteria2) return null

            val offset = if (isHysteria2) 12 else 6
            val uriContent = trimmed.substring(offset)
            val hashIndex = uriContent.indexOf('#')
            val rawName = if (hashIndex != -1) uriContent.substring(hashIndex + 1) else "Imported Hysteria2"
            val name = try {
                URLDecoder.decode(rawName, "UTF-8")
            } catch (e: Exception) {
                rawName
            }

            val cleanContent = if (hashIndex != -1) uriContent.substring(0, hashIndex) else uriContent

            val atIndex = cleanContent.indexOf('@')
            if (atIndex == -1) return null
            val auth = cleanContent.substring(0, atIndex)
            val hostPortParams = cleanContent.substring(atIndex + 1)

            val questionIndex = hostPortParams.indexOf('?')
            val hostPort = if (questionIndex != -1) hostPortParams.substring(0, questionIndex) else hostPortParams
            val paramsStr = if (questionIndex != -1) hostPortParams.substring(questionIndex + 1) else ""

            val colonIndex = hostPort.lastIndexOf(':')
            val serverAddress = if (colonIndex != -1) hostPort.substring(0, colonIndex) else hostPort
            val port = if (colonIndex != -1) {
                hostPort.substring(colonIndex + 1).toIntOrNull() ?: 443
            } else {
                443
            }

            var sni = ""
            var allowInsecure = false
            var alpn = "h3"
            var pinnedPeerCertSha256 = ""

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
                            key.equals("sni", ignoreCase = true) -> sni = value
                            key.equals("insecure", ignoreCase = true) || key.equals("allowInsecure", ignoreCase = true) -> {
                                allowInsecure = value.equals("true", ignoreCase = true) || value == "1" || value.equals("yes", ignoreCase = true)
                            }
                            key.equals("alpn", ignoreCase = true) -> alpn = value
                            key.equals("pinSHA256", ignoreCase = true) || key.equals("pinsha256", ignoreCase = true) || key.equals("pinnedPeerCertSha256", ignoreCase = true) -> {
                                pinnedPeerCertSha256 = value
                            }
                        }
                    }
                }
            }

            return ServerProfile(
                name = name,
                address = serverAddress,
                port = port,
                uuid = auth, // Map auth token to uuid
                type = "HYSTERIA2",
                security = "tls", // Hysteria2 always uses TLS
                sni = sni,
                allowInsecure = allowInsecure,
                alpn = alpn,
                pinnedPeerCertSha256 = pinnedPeerCertSha256,
                network = "udp" // Hysteria2 QUIC relies on UDP transport
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Hysteria2 link", e)
            return null
        }
    }

    fun export(profile: ServerProfile): String {
        val encodedName = try {
            URLEncoder.encode(profile.name, "UTF-8")
        } catch (e: Exception) {
            profile.name
        }

        val queryParams = mutableListOf<String>()
        if (profile.sni.isNotEmpty()) {
            queryParams.add("sni=${try { URLEncoder.encode(profile.sni, "UTF-8") } catch (e: Exception) { profile.sni }}")
        }
        if (profile.allowInsecure) {
            queryParams.add("insecure=1")
        }
        if (profile.alpn.isNotEmpty()) {
            queryParams.add("alpn=${try { URLEncoder.encode(profile.alpn, "UTF-8") } catch (e: Exception) { profile.alpn }}")
        }
        if (profile.pinnedPeerCertSha256.isNotEmpty()) {
            queryParams.add("pinSHA256=${try { URLEncoder.encode(profile.pinnedPeerCertSha256, "UTF-8") } catch (e: Exception) { profile.pinnedPeerCertSha256 }}")
        }

        val queryString = if (queryParams.isNotEmpty()) "?" + queryParams.joinToString("&") else ""
        return "hysteria2://${profile.uuid}@${profile.address}:${profile.port}$queryString#$encodedName"
    }
}
