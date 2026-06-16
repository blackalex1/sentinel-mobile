package com.xprox.sentinel.config.parser

import com.xprox.sentinel.config.XrayConfigManager.ServerProfile

object ProxyLinkParser {

    /**
     * Unified entry point to parse any supported proxy link from clipboard.
     * Delegates parsing to specialized modular strategies.
     */
    fun parse(link: String): ServerProfile? {
        val trimmed = link.trim()
        return when {
            trimmed.startsWith("vless://", ignoreCase = true) -> VlessParser.parse(trimmed)
            trimmed.startsWith("vmess://", ignoreCase = true) -> VmessParser.parse(trimmed)
            trimmed.startsWith("trojan://", ignoreCase = true) -> TrojanParser.parse(trimmed)
            trimmed.startsWith("ss://", ignoreCase = true) -> ShadowsocksParser.parse(trimmed)
            trimmed.startsWith("hysteria2://", ignoreCase = true) || trimmed.startsWith("hy2://", ignoreCase = true) -> Hysteria2Parser.parse(trimmed)
            trimmed.startsWith("socks5://", ignoreCase = true) || trimmed.startsWith("socks://", ignoreCase = true) -> SocksParser.parse(trimmed)
            else -> null
        }
    }

    /**
     * Unified entry point to export a ServerProfile back to a shareable URI.
     */
    fun export(profile: ServerProfile): String {
        return when (profile.type.uppercase()) {
            "VLESS" -> VlessParser.export(profile)
            "VMESS" -> VmessParser.export(profile)
            "TROJAN" -> TrojanParser.export(profile)
            "SHADOWSOCKS" -> ShadowsocksParser.export(profile)
            "HYSTERIA2" -> Hysteria2Parser.export(profile)
            "SOCKS" -> SocksParser.export(profile)
            else -> ""
        }
    }
}
