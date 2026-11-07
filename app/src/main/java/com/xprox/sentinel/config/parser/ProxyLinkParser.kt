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
     * Enforces RFC 3986 bracketed IPv6 host formatting across all client versions.
     */
    fun export(profile: ServerProfile): String {
        val rawUri = SentinelCore.generateUri(profile)
        if (rawUri.isEmpty()) return ""
        return formatUriWithBracketedIpv6(rawUri)
    }

    private fun formatUriWithBracketedIpv6(uri: String): String {
        val schemeIdx = uri.indexOf("://")
        if (schemeIdx == -1) return uri
        val prefix = uri.substring(0, schemeIdx + 3)
        val rest = uri.substring(schemeIdx + 3)

        val cutIdx = rest.indexOfAny(charArrayOf('/', '?', '#'))
        val authority = if (cutIdx != -1) rest.substring(0, cutIdx) else rest
        val suffix = if (cutIdx != -1) rest.substring(cutIdx) else ""

        if (authority.contains('[') && authority.contains(']')) {
            return uri
        }

        val atIdx = authority.lastIndexOf('@')
        val userInfo = if (atIdx != -1) authority.substring(0, atIdx + 1) else ""
        val hostPort = if (atIdx != -1) authority.substring(atIdx + 1) else authority

        val lastColon = hostPort.lastIndexOf(':')
        if (lastColon != -1 && hostPort.count { it == ':' } > 1) {
            val possibleHost = hostPort.substring(0, lastColon)
            val possiblePort = hostPort.substring(lastColon + 1)
            if (possiblePort.toIntOrNull() != null) {
                return "$prefix$userInfo[$possibleHost]:$possiblePort$suffix"
            }
        }
        return uri
    }
}
