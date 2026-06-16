package com.xprox.sentinel.config.parser

import android.util.Log
import com.xprox.sentinel.config.XrayConfigManager.ServerProfile
import org.json.JSONArray
import org.json.JSONObject

object JsonConfigParser {
    private const val TAG = "JsonConfigParser"

    fun parseArray(jsonStr: String, groupId: String?): List<ServerProfile> {
        val list = mutableListOf<ServerProfile>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val profile = parseSingle(obj, groupId)
                if (profile != null) {
                    list.add(profile)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON array", e)
        }
        return list
    }

    private fun parseSingle(obj: JSONObject, groupId: String?): ServerProfile? {
        try {
            val name = obj.optString("remarks", "Imported Server")
            val outbounds = obj.optJSONArray("outbounds") ?: return null
            
            // Find the first proxy outbound to extract metadata (for ping, type, address, port, etc.)
            var proxyOutbound: JSONObject? = null
            for (i in 0 until outbounds.length()) {
                val out = outbounds.getJSONObject(i)
                val tag = out.optString("tag", "")
                val protocol = out.optString("protocol", "").lowercase()
                if (tag.startsWith("proxy") && (protocol == "vless" || protocol == "vmess" || protocol == "trojan" || protocol == "shadowsocks" || protocol == "ss" || protocol == "hysteria" || protocol == "hysteria2")) {
                    proxyOutbound = out
                    break
                }
            }

            if (proxyOutbound == null) {
                // If no proxy outbound is found, we still return a profile that can run the raw JSON config directly
                return ServerProfile(
                    name = name,
                    address = "JSON Config",
                    port = 0,
                    type = "JSON",
                    groupId = groupId,
                    fullJsonConfig = obj.toString()
                )
            }

            val protocol = proxyOutbound.optString("protocol").lowercase()
            val settings = proxyOutbound.optJSONObject("settings") ?: JSONObject()
            val streamSettings = proxyOutbound.optJSONObject("streamSettings") ?: JSONObject()
            
            val network = streamSettings.optString("network", "tcp")
            val security = streamSettings.optString("security", "none")

            var address = ""
            var port = 443
            var uuid = ""
            var flow = ""
            var encryption = "none"

            if (protocol == "vless" || protocol == "vmess" || protocol == "trojan") {
                val vnextArray = settings.optJSONArray("vnext")
                if (vnextArray != null && vnextArray.length() > 0) {
                    val firstVnext = vnextArray.getJSONObject(0)
                    address = firstVnext.optString("address")
                    port = firstVnext.optInt("port", 443)
                    val users = firstVnext.optJSONArray("users")
                    if (users != null && users.length() > 0) {
                        val user = users.getJSONObject(0)
                        uuid = user.optString("id")
                        flow = user.optString("flow", "")
                        encryption = user.optString("encryption", "none")
                    }
                }
            } else if (protocol == "shadowsocks" || protocol == "ss") {
                val serversArray = settings.optJSONArray("servers")
                if (serversArray != null && serversArray.length() > 0) {
                    val firstServer = serversArray.getJSONObject(0)
                    address = firstServer.optString("address")
                    port = firstServer.optInt("port", 443)
                    uuid = firstServer.optString("password")
                    encryption = firstServer.optString("method")
                }
            } else if (protocol == "hysteria" || protocol == "hysteria2") {
                address = settings.optString("server", "")
                if (address.contains(":")) {
                    val parts = address.split(":")
                    address = parts[0]
                    port = parts[1].toIntOrNull() ?: 443
                }
                uuid = settings.optString("auth", "") ?: settings.optString("auth_str", "") ?: ""
            }

            var pbk = ""
            var sni = ""
            var sid = ""
            var fp = "chrome"
            var path = ""
            var host = ""

            if (security.equals("reality", ignoreCase = true)) {
                val reality = streamSettings.optJSONObject("realitySettings")
                if (reality != null) {
                    pbk = reality.optString("publicKey")
                    sni = reality.optString("serverName")
                    sid = reality.optString("shortId")
                    fp = reality.optString("fingerprint", "chrome")
                }
            } else if (security.equals("tls", ignoreCase = true)) {
                val tls = streamSettings.optJSONObject("tlsSettings")
                if (tls != null) {
                    sni = tls.optString("serverName")
                }
            }

            val wsSettings = streamSettings.optJSONObject("wsSettings")
            if (wsSettings != null) {
                path = wsSettings.optString("path", "")
                val headers = wsSettings.optJSONObject("headers")
                if (headers != null) {
                    host = headers.optString("Host", "")
                }
            }
            val grpcSettings = streamSettings.optJSONObject("grpcSettings")
            if (grpcSettings != null) {
                path = grpcSettings.optString("serviceName", "")
            }

            val typeStr = when(protocol) {
                "vless" -> "VLESS"
                "vmess" -> "VMess"
                "trojan" -> "Trojan"
                "shadowsocks", "ss" -> "Shadowsocks"
                "hysteria", "hysteria2" -> "Hysteria"
                else -> protocol.uppercase()
            }

            return ServerProfile(
                name = name,
                address = address,
                port = port,
                type = typeStr,
                uuid = uuid,
                security = security,
                sni = sni,
                pbk = pbk,
                sid = sid,
                fp = fp,
                network = network,
                flow = flow,
                encryption = encryption,
                path = path,
                host = host,
                groupId = groupId,
                fullJsonConfig = obj.toString() // Save the entire configuration JSON!
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse single config JSON", e)
            return null
        }
    }
}
