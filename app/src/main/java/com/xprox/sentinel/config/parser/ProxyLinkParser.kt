package com.xprox.sentinel.config.parser

import com.xprox.sentinel.config.XrayConfigManager.ServerProfile
import com.xprox.sentinel.core.SentinelCore

object ProxyLinkParser {

    /**
     * Unified entry point to parse any supported proxy link (VLESS, VMess, Trojan, Shadowsocks, Hysteria 2, TUIC, WireGuard, Socks5, HTTP)
     * using the Sentinel-Core Go parser engine.
     */
    fun parse(link: String): ServerProfile? {
        val trimmed = link.trim()
        if (trimmed.isEmpty()) return null
        return SentinelCore.parseUri(trimmed)
    }

    /**
     * Unified entry point to export a ServerProfile back to a shareable URI via Sentinel-Core.
     */
    fun export(profile: ServerProfile): String {
        return SentinelCore.generateUri(profile)
    }
}
