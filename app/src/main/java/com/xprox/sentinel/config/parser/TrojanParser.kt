package com.xprox.sentinel.config.parser

import android.util.Log
import com.xprox.sentinel.config.XrayConfigManager.ServerProfile
import java.net.URLDecoder
import java.net.URLEncoder

object TrojanParser {
    private const val TAG = "TrojanParser"

    fun parse(link: String): ServerProfile? {
        try {
            val trimmed = link.trim()
            if (!trimmed.startsWith("trojan://", ignoreCase = true)) return null
            
            val uriContent = trimmed.substring(9)
            val hashIndex = uriContent.indexOf('#')
            val rawName = if (hashIndex != -1) uriContent.substring(hashIndex + 1) else "Imported Trojan"
            val name = try {
                URLDecoder.decode(rawName, "UTF-8")
            } catch (e: Exception) {
                rawName
            }
            
            val cleanContent = if (hashIndex != -1) uriContent.substring(0, hashIndex) else uriContent
            
            val atIndex = cleanContent.indexOf('@')
            if (atIndex == -1) return null
            val password = cleanContent.substring(0, atIndex)
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
            
            var security = "tls" // Trojan default is TLS
            var path = ""
            var sni = ""
            var fp = "chrome"
            var network = "tcp"
            var hostParam = ""
            var allowInsecure = false
            var alpn = ""
            var headerType = ""
            
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
                            key.equals("security", ignoreCase = true) -> security = value
                            key.equals("path", ignoreCase = true) -> path = value
                            key.equals("sni", ignoreCase = true) -> sni = value
                            key.equals("fp", ignoreCase = true) -> fp = value
                            key.equals("type", ignoreCase = true) || key.equals("network", ignoreCase = true) -> network = value
                            key.equals("host", ignoreCase = true) || key.equals("wsHost", ignoreCase = true) -> hostParam = value
                            key.equals("allowInsecure", ignoreCase = true) || key.equals("allowinsecure", ignoreCase = true) -> {
                                allowInsecure = value.equals("true", ignoreCase = true) || value == "1" || value.equals("yes", ignoreCase = true)
                            }
                            key.equals("alpn", ignoreCase = true) -> alpn = value
                            key.equals("headerType", ignoreCase = true) || key.equals("headertype", ignoreCase = true) -> headerType = value
                        }
                    }
                }
            }
            
            return ServerProfile(
                name = name,
                address = serverAddress,
                port = port,
                uuid = password,
                type = "TROJAN",
                security = security,
                path = path,
                sni = sni,
                fp = fp,
                network = network,
                host = hostParam,
                allowInsecure = allowInsecure,
                alpn = alpn,
                headerType = headerType
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Trojan link", e)
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
        if (profile.security.isNotEmpty()) queryParams.add("security=${profile.security}")
        if (profile.sni.isNotEmpty()) queryParams.add("sni=${try { URLEncoder.encode(profile.sni, "UTF-8") } catch (e: Exception) { profile.sni }}")
        if (profile.fp.isNotEmpty()) queryParams.add("fp=${try { URLEncoder.encode(profile.fp, "UTF-8") } catch (e: Exception) { profile.fp }}")
        if (profile.network.isNotEmpty()) queryParams.add("type=${try { URLEncoder.encode(profile.network, "UTF-8") } catch (e: Exception) { profile.network }}")
        if (profile.host.isNotEmpty()) queryParams.add("host=${try { URLEncoder.encode(profile.host, "UTF-8") } catch (e: Exception) { profile.host }}")
        if (profile.allowInsecure) queryParams.add("allowInsecure=true")
        if (profile.alpn.isNotEmpty()) queryParams.add("alpn=${try { URLEncoder.encode(profile.alpn, "UTF-8") } catch (e: Exception) { profile.alpn }}")
        if (profile.headerType.isNotEmpty()) queryParams.add("headerType=${try { URLEncoder.encode(profile.headerType, "UTF-8") } catch (e: Exception) { profile.headerType }}")
        if (profile.path.isNotEmpty()) queryParams.add("path=${try { URLEncoder.encode(profile.path, "UTF-8") } catch (e: Exception) { profile.path }}")

        val queryString = if (queryParams.isNotEmpty()) "?" + queryParams.joinToString("&") else ""
        return "trojan://${profile.uuid}@${profile.address}:${profile.port}$queryString#$encodedName"
    }
}
