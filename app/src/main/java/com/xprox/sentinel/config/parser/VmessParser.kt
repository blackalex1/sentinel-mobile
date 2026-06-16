package com.xprox.sentinel.config.parser

import android.util.Log
import com.xprox.sentinel.config.XrayConfigManager.ServerProfile
import java.util.Base64

object VmessParser {
    private const val TAG = "VmessParser"

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

    /**
     * Highly robust, platform-independent flat JSON parser.
     * Bypasses Android's org.json stub classes so it runs seamlessly on pure JVM.
     */
    private fun parseFlatJson(json: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val pattern = "\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|(\\d+)|(true|false)|null)".toRegex()
        pattern.findAll(json).forEach { match ->
            val key = match.groupValues[1]
            val stringVal = match.groupValues[2]
            val numberVal = match.groupValues[3]
            val boolVal = match.groupValues[4]
            val value = when {
                stringVal.isNotEmpty() -> stringVal
                numberVal.isNotEmpty() -> numberVal
                boolVal.isNotEmpty() -> boolVal
                else -> ""
            }
            map[key] = value
        }
        return map
    }

    fun parse(link: String): ServerProfile? {
        try {
            val trimmed = link.trim()
            if (!trimmed.startsWith("vmess://", ignoreCase = true)) return null

            val base64Str = trimmed.substring(8)
            val decodedBytes = decodeBase64(base64Str)
            val jsonStr = String(decodedBytes, Charsets.UTF_8)
            
            val json = parseFlatJson(jsonStr)

            val name = json["ps"] ?: "Imported VMess"
            val address = json["add"] ?: ""
            val port = json["port"]?.toIntOrNull() ?: 443
            val uuid = json["id"] ?: ""
            val path = json["path"] ?: ""
            val security = json["tls"] ?: "none"
            val sni = json["sni"] ?: ""
            val network = json["net"] ?: "tcp"
            val host = json["host"] ?: ""
            val scy = json["scy"] ?: "none"
            val headerType = json["type"] ?: ""
            val alpn = json["alpn"] ?: ""

            return ServerProfile(
                name = name,
                address = address,
                port = port,
                uuid = uuid,
                type = "VMESS",
                security = security,
                path = path,
                sni = sni,
                network = network,
                host = host,
                encryption = scy,
                headerType = headerType,
                alpn = alpn
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse VMess link", e)
            return null
        }
    }

    fun export(profile: ServerProfile): String {
        return try {
            val jsonStr = """{"v":"2","ps":"${profile.name}","add":"${profile.address}","port":${profile.port},"id":"${profile.uuid}","aid":0,"scy":"${profile.encryption.ifEmpty { "none" }}","net":"${profile.network.ifEmpty { "tcp" }}","type":"${profile.headerType}","host":"${profile.host}","path":"${profile.path}","tls":"${profile.security}","sni":"${profile.sni}","alpn":"${profile.alpn}"}"""
            val base64Str = Base64.getEncoder().withoutPadding().encodeToString(
                jsonStr.toByteArray(Charsets.UTF_8)
            )
            "vmess://$base64Str"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export VMess profile", e)
            ""
        }
    }
}
