package com.xprox.sentinel.config.parser

import android.util.Log
import com.xprox.sentinel.config.XrayConfigManager.ServerProfile
import java.net.URLDecoder
import java.net.URLEncoder

object SocksParser {
    private const val TAG = "SocksParser"

    fun parse(link: String): ServerProfile? {
        try {
            val trimmed = link.trim()
            val isSocks5 = trimmed.startsWith("socks5://", ignoreCase = true)
            val isSocks = trimmed.startsWith("socks://", ignoreCase = true)
            if (!isSocks5 && !isSocks) return null

            val offset = if (isSocks5) 9 else 8
            val uriContent = trimmed.substring(offset)
            val hashIndex = uriContent.indexOf('#')
            val rawName = if (hashIndex != -1) uriContent.substring(hashIndex + 1) else "Imported SOCKS5"
            val name = try {
                URLDecoder.decode(rawName, "UTF-8")
            } catch (e: Exception) {
                rawName
            }

            val cleanContent = if (hashIndex != -1) uriContent.substring(0, hashIndex) else uriContent

            var username = ""
            var password = ""
            var serverAddress = ""
            var port = 1080

            val atIndex = cleanContent.indexOf('@')
            if (atIndex != -1) {
                // Authenticated Socks5: socks5://username:password@host:port
                val credentials = cleanContent.substring(0, atIndex)
                val hostPort = cleanContent.substring(atIndex + 1)

                val colonIndex = credentials.indexOf(':')
                if (colonIndex != -1) {
                    username = credentials.substring(0, colonIndex)
                    password = credentials.substring(colonIndex + 1)
                } else {
                    username = credentials
                }

                val portColonIndex = hostPort.lastIndexOf(':')
                serverAddress = if (portColonIndex != -1) hostPort.substring(0, portColonIndex) else hostPort
                port = if (portColonIndex != -1) hostPort.substring(portColonIndex + 1).toIntOrNull() ?: 1080 else 1080
            } else {
                // Anonymous Socks5: socks5://host:port
                val portColonIndex = cleanContent.lastIndexOf(':')
                serverAddress = if (portColonIndex != -1) cleanContent.substring(0, portColonIndex) else cleanContent
                port = if (portColonIndex != -1) cleanContent.substring(portColonIndex + 1).toIntOrNull() ?: 1080 else 1080
            }

            return ServerProfile(
                name = name,
                address = serverAddress,
                port = port,
                uuid = username, // Map username to uuid
                path = password, // Map password to path
                type = "SOCKS",
                security = "none"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse SOCKS5 link", e)
            return null
        }
    }

    fun export(profile: ServerProfile): String {
        val encodedName = try {
            URLEncoder.encode(profile.name, "UTF-8")
        } catch (e: Exception) {
            profile.name
        }

        return if (profile.uuid.isNotEmpty()) {
            "socks5://${profile.uuid}:${profile.path}@${profile.address}:${profile.port}#$encodedName"
        } else {
            "socks5://${profile.address}:${profile.port}#$encodedName"
        }
    }
}
