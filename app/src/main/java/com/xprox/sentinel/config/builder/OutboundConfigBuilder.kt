package com.xprox.sentinel.config.builder

import com.xprox.sentinel.config.XrayConfigManager.ServerProfile

object OutboundConfigBuilder {
    fun buildStreamSettingsJson(profile: ServerProfile): String {
        val type = profile.type.uppercase()
        if (type == "HYSTERIA2") {
            val tlsSettingsJson = buildString {
                append("{\n")
                append("    \"serverName\": \"${profile.sni.ifEmpty { profile.address }}\",\n")
                append("    \"alpn\": [\"h3\"]")
                if (profile.allowInsecure) {
                    append(",\n    \"allowInsecure\": true")
                }
                if (profile.pinnedPeerCertSha256.isNotEmpty()) {
                    append(",\n    \"pinnedPeerCertSha256\": \"${profile.pinnedPeerCertSha256}\"")
                }
                append("\n  }")
            }
            return """
            {
              "network": "hysteria",
              "security": "tls",
              "tlsSettings": $tlsSettingsJson,
              "hysteriaSettings": {
                "version": 2,
                "auth": "${profile.uuid}"
              }
            }
            """.trimIndent()
        }

        return buildString {
            append("{\n")
            append("  \"network\": \"${profile.network.ifEmpty { "tcp" }}\",\n")
            append("  \"security\": \"${profile.security}\",\n")
            
            // Security settings
            if (profile.security == "reality") {
                append("  \"realitySettings\": {\n")
                append("    \"show\": false,\n")
                append("    \"fingerprint\": \"${profile.fp.ifEmpty { "chrome" }}\",\n")
                append("    \"serverName\": \"${profile.sni.ifEmpty { profile.address }}\",\n")
                append("    \"publicKey\": \"${profile.pbk}\",\n")
                append("    \"shortId\": \"${profile.sid}\",\n")
                append("    \"spiderX\": \"${profile.spx}\"")
                if (profile.allowInsecure) {
                    append(",\n    \"allowInsecure\": true")
                }
                if (profile.alpn.isNotEmpty()) {
                    val alpnList = profile.alpn.split(",").map { "\"${it.trim()}\"" }.joinToString(", ")
                    append(",\n    \"alpn\": [$alpnList]")
                }
                append("\n  },\n")
            } else if (profile.security == "tls") {
                append("  \"tlsSettings\": {\n")
                append("    \"serverName\": \"${profile.sni.ifEmpty { profile.address }}\"")
                if (profile.allowInsecure) {
                    append(",\n    \"allowInsecure\": true")
                }
                if (profile.alpn.isNotEmpty()) {
                    val alpnList = profile.alpn.split(",").map { "\"${it.trim()}\"" }.joinToString(", ")
                    append(",\n    \"alpn\": [$alpnList]")
                }
                append("\n  },\n")
            }
            
            // Network transport settings
            val net = profile.network.lowercase()
            if (net == "ws") {
                append("  \"wsSettings\": {\n")
                append("    \"path\": \"${profile.path}\"")
                if (profile.host.isNotEmpty()) {
                    append(",\n    \"headers\": {\n")
                    append("      \"Host\": \"${profile.host}\"\n")
                    append("    }")
                }
                append("\n  }\n")
            } else if (net == "grpc") {
                append("  \"grpcSettings\": {\n")
                append("    \"serviceName\": \"${profile.path}\"\n")
                append("  }\n")
            } else {
                if (net == "tcp" && profile.headerType.isNotEmpty()) {
                    append("  \"tcpSettings\": {\n")
                    append("    \"header\": {\n")
                    append("      \"type\": \"${profile.headerType}\"")
                    if (profile.host.isNotEmpty()) {
                        append(",\n      \"request\": {\n")
                        append("        \"headers\": {\n")
                        append("          \"Host\": [\n")
                        append("            \"${profile.host}\"\n")
                        append("          ]\n")
                        append("        }\n")
                        append("      }")
                    }
                    append("\n    }\n")
                    append("  }\n")
                } else {
                    append("  \"tcpSettings\": {}\n")
                }
            }
            append(",\n")
            append("  \"sockopt\": {\n")
            append("    \"tcpKeepAliveInterval\": 15,\n")
            append("    \"tcpFastOpen\": true\n")
            append("  }\n")
            append("}")
        }
    }

    fun buildSettingsJson(profile: ServerProfile): String {
        return buildString {
            val type = profile.type.uppercase()
            if (type == "HYSTERIA2") {
                append("{\n")
                append("  \"version\": 2,\n")
                append("  \"address\": \"${profile.address}\",\n")
                append("  \"port\": ${profile.port}\n")
                append("}")
            } else if (type == "TROJAN") {
                append("{\n")
                append("  \"servers\": [\n")
                append("    {\n")
                append("      \"address\": \"${profile.address}\",\n")
                append("      \"port\": ${profile.port},\n")
                append("      \"password\": \"${profile.uuid}\",\n")
                append("      \"level\": 0\n")
                append("    }\n")
                append("  ]\n")
                append("}")
            } else if (type == "SHADOWSOCKS") {
                append("{\n")
                append("  \"servers\": [\n")
                append("    {\n")
                append("      \"address\": \"${profile.address}\",\n")
                append("      \"port\": ${profile.port},\n")
                append("      \"password\": \"${profile.uuid}\",\n")
                val method = profile.path.ifEmpty { "aes-256-gcm" }
                append("      \"method\": \"$method\",\n")
                append("      \"level\": 0\n")
                append("    }\n")
                append("  ]\n")
                append("}")
            } else if (type == "SOCKS") {
                append("{\n")
                append("  \"servers\": [\n")
                append("    {\n")
                append("      \"address\": \"${profile.address}\",\n")
                append("      \"port\": ${profile.port},\n")
                if (profile.uuid.isNotEmpty()) {
                    append("      \"users\": [\n")
                    append("        {\n")
                    append("          \"user\": \"${profile.uuid}\",\n")
                    append("          \"pass\": \"${profile.path}\"\n")
                    append("        }\n")
                    append("      ]\n")
                } else {
                    append("      \"users\": []\n")
                }
                append("}")
            } else {
                append("{\n")
                append("  \"vnext\": [\n")
                append("    {\n")
                append("      \"address\": \"${profile.address}\",\n")
                append("      \"port\": ${profile.port},\n")
                append("      \"users\": [\n")
                append("        {\n")
                append("          \"id\": \"${profile.uuid}\",\n")
                if (profile.flow.isNotEmpty()) {
                    append("          \"flow\": \"${profile.flow}\",\n")
                }
                append("          \"encryption\": \"${profile.encryption.ifEmpty { "none" }}\",\n")
                append("          \"level\": 0\n")
                append("        }\n")
                append("      ]\n")
                append("    }\n")
                append("  ]\n")
                append("}")
            }
        }
    }
}
