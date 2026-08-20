package com.xprox.sentinel.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

val SentinelJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
    prettyPrint = false
}

@Serializable
data class ConfigSpec(
    @SerialName("targetCore") val targetCore: String = "xray",
    @SerialName("coreVersion") val coreVersion: String? = null,
    @SerialName("strictMode") val strictMode: Boolean = false,
    @SerialName("serverNode") val serverNode: CoreServerProfile? = null,
    @SerialName("clientInbound") val clientInbound: ClientInboundSpec? = null,
    @SerialName("serverInbounds") val serverInbounds: List<ServerInboundSpec>? = null,
    @SerialName("routing") val routing: RoutingSpec? = null,
    @SerialName("dns") val dns: DNSSpec? = null,
    @SerialName("logLevel") val logLevel: String? = "info",
    @SerialName("logPath") val logPath: String? = null,
    @SerialName("accessLog") val accessLog: String? = null,
    @SerialName("errorLog") val errorLog: String? = null,
    @SerialName("clashApiAddress") val clashApiAddress: String? = null
)

@Serializable
data class CoreServerProfile(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("protocol") val protocol: String = "vless",
    @SerialName("address") val address: String = "",
    @SerialName("port") val port: Int = 443,
    @SerialName("transport") val transport: String = "tcp",
    @SerialName("security") val security: String = "none",
    @SerialName("uuid") val uuid: String = "",
    @SerialName("password") val password: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("sni") val sni: String = "",
    @SerialName("alpn") val alpn: List<String> = emptyList(),
    @SerialName("fingerprint") val fingerprint: String = "chrome",
    @SerialName("insecure") val insecure: Boolean = false,
    @SerialName("publicKey") val publicKey: String = "",
    @SerialName("shortId") val shortId: String = "",
    @SerialName("spiderX") val spiderX: String = "",
    @SerialName("postQuantum") val postQuantum: Boolean = false,
    @SerialName("flow") val flow: String = "",
    @SerialName("encryption") val encryption: String = "none",
    @SerialName("mux") val mux: Boolean = false,
    @SerialName("path") val path: String = "",
    @SerialName("host") val host: String = "",
    @SerialName("serviceName") val serviceName: String = "",
    @SerialName("headers") val headers: Map<String, String> = emptyMap(),
    @SerialName("cipher") val cipher: String = "",
    @SerialName("shadowTlsVersion") val shadowTlsVersion: Int = 0,
    @SerialName("shadowTlsPassword") val shadowTlsPassword: String = "",
    @SerialName("shadowTlsSni") val shadowTlsSni: String = "",
    @SerialName("bandwidthUp") val bandwidthUp: String = "",
    @SerialName("bandwidthDown") val bandwidthDown: String = "",
    @SerialName("obfsType") val obfsType: String = "",
    @SerialName("obfsPassword") val obfsPassword: String = "",
    @SerialName("portHopping") val portHopping: String = "",
    @SerialName("pinnedPeerCertSha256") val pinnedPeerCertSha256: String = "",
    @SerialName("congestionControl") val congestionControl: String = "",
    @SerialName("udpRelayMode") val udpRelayMode: String = "",
    @SerialName("zeroRttHandshake") val zeroRttHandshake: Boolean = false,
    @SerialName("privateKey") val privateKey: String = "",
    @SerialName("peerPublicKey") val peerPublicKey: String = "",
    @SerialName("preSharedKey") val preSharedKey: String = "",
    @SerialName("localAddress") val localAddress: List<String> = emptyList(),
    @SerialName("mtu") val mtu: Int = 0
)

@Serializable
data class ClientInboundSpec(
    @SerialName("mode") val mode: String = "mobile_vpn", // mobile_vpn, desktop_tun, system_proxy
    @SerialName("socksPort") val socksPort: Int = 0,
    @SerialName("httpPort") val httpPort: Int = 0,
    @SerialName("listenAddress") val listenAddress: String? = null,
    @SerialName("authEnabled") val authEnabled: Boolean = false,
    @SerialName("authUsername") val authUsername: String? = null,
    @SerialName("authPassword") val authPassword: String? = null,
    @SerialName("tunInterfaceName") val tunInterfaceName: String? = null,
    @SerialName("tunStack") val tunStack: String? = null,
    @SerialName("mtu") val mtu: Int = 0,
    @SerialName("strictRoute") val strictRoute: Boolean = true,
    @SerialName("autoRoute") val autoRoute: Boolean = true,
    @SerialName("endpointIp") val endpointIp: String? = null,
    @SerialName("includePackages") val includePackages: List<String>? = null,
    @SerialName("excludePackages") val excludePackages: List<String>? = null,
    @SerialName("lanSharingEnabled") val lanSharingEnabled: Boolean = false,
    @SerialName("lanListenAddress") val lanListenAddress: String? = null,
    @SerialName("lanHttpPort") val lanHttpPort: Int = 0,
    @SerialName("lanSocksPort") val lanSocksPort: Int = 0,
    @SerialName("lanAuthEnabled") val lanAuthEnabled: Boolean = false,
    @SerialName("lanUsername") val lanUsername: String? = null,
    @SerialName("lanPassword") val lanPassword: String? = null
)

@Serializable
data class ServerInboundSpec(
    @SerialName("tag") val tag: String = "",
    @SerialName("protocol") val protocol: String = "socks",
    @SerialName("listenAddress") val listenAddress: String = "0.0.0.0",
    @SerialName("port") val port: Int = 10808
)

@Serializable
data class RoutingSpec(
    @SerialName("defaultAction") val defaultAction: String = "proxy", // proxy, direct, block
    @SerialName("rules") val rules: List<RoutingRule> = emptyList(),
    @SerialName("autoDetectInterface") val autoDetectInterface: Boolean = true,
    @SerialName("overrideDns") val overrideDns: Boolean = true,
    @SerialName("ruleSets") val ruleSets: List<String>? = null
)

@Serializable
data class RoutingRule(
    @SerialName("action") val action: String = "proxy", // proxy, direct, block
    @SerialName("outboundTag") val outboundTag: String? = null,
    @SerialName("domains") val domains: List<String>? = null,
    @SerialName("ips") val ips: List<String>? = null,
    @SerialName("ports") val ports: List<String>? = null,
    @SerialName("protocols") val protocols: List<String>? = null,
    @SerialName("users") val users: List<String>? = null,
    @SerialName("packageUids") val packageUids: List<String>? = null,
    @SerialName("processNames") val processNames: List<String>? = null,
    @SerialName("inboundTags") val inboundTags: List<String>? = null
)

@Serializable
data class DNSSpec(
    @SerialName("servers") val servers: List<String> = listOf("8.8.8.8", "1.1.1.1"),
    @SerialName("finalServer") val finalServer: String = "8.8.8.8",
    @SerialName("strategy") val strategy: String = "prefer_ipv4",
    @SerialName("disableCache") val disableCache: Boolean = false,
    @SerialName("disableFallback") val disableFallback: Boolean = false
)

@Serializable
data class BuildResult(
    @SerialName("targetCore") val targetCore: String = "",
    @SerialName("configJson") val configJson: String = "",
    @SerialName("warnings") val warnings: List<NegotiationWarning>? = null,
    @SerialName("error") val error: String? = null
)

@Serializable
data class NegotiationWarning(
    @SerialName("feature") val feature: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("action") val action: String = ""
)

@Serializable
data class RoutingPreset(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("defaultTarget") val defaultTarget: String = "direct",
    @SerialName("domains") val domains: List<String>? = null,
    @SerialName("ips") val ips: List<String>? = null,
    @SerialName("protocols") val protocols: List<String>? = null
)

@Serializable
data class AndroidAuditRequest(
    @SerialName("package_name") val packageName: String,
    @SerialName("app_name") val appName: String,
    @SerialName("destination_ip") val destinationIp: String,
    @SerialName("port") val port: Int,
    @SerialName("protocol") val protocol: String = "TCP",
    @SerialName("ip_length") val ipLength: Int = 0,
    @SerialName("ttl") val ttl: Int = 0,
    @SerialName("ip_flags") val ipFlags: String = "N/A",
    @SerialName("tcp_flags") val tcpFlags: String = "N/A",
    @SerialName("tcp_seq") val tcpSeq: Long = 0L,
    @SerialName("tcp_ack") val tcpAck: Long = 0L,
    @SerialName("tcp_window") val tcpWindow: Int = 0,
    @SerialName("audit_ports") val auditPorts: List<Int>? = null,
    @SerialName("max_threshold") val maxThreshold: Int = 2
)

@Serializable
data class AndroidAuditVerdict(
    @SerialName("is_blocked") val isBlocked: Boolean = false,
    @SerialName("should_block") val shouldBlock: Boolean = false,
    @SerialName("is_system_flagged") val isSystemFlagged: Boolean = false,
    @SerialName("threat_detected") val threatDetected: Boolean = false,
    @SerialName("threat_type") val threatType: String = "NONE",
    @SerialName("description") val description: String = "",
    @SerialName("action") val action: String = "ALLOW",
    @SerialName("risk_score") val riskScore: Int = 0,
    @SerialName("attempts_count") val attemptsCount: Int = 0,
    @SerialName("timestamp") val timestamp: Long = 0L
)

@Serializable
data class DissectedPacketInfo(
    @SerialName("timestamp") val timestamp: Long = 0L,
    @SerialName("ip_version") val ipVersion: Int = 4,
    @SerialName("header_length") val headerLength: Int = 20,
    @SerialName("total_length") val totalLength: Int = 0,
    @SerialName("ttl") val ttl: Int = 64,
    @SerialName("protocol") val protocol: String = "TCP",
    @SerialName("ip_flags") val ipFlags: String = "None",
    @SerialName("source_ip") val sourceIp: String = "",
    @SerialName("destination_ip") val destinationIp: String = "",
    @SerialName("source_port") val sourcePort: Int = 0,
    @SerialName("destination_port") val destinationPort: Int = 0,
    @SerialName("tcp_flags") val tcpFlags: String = "",
    @SerialName("tcp_seq") val tcpSeq: Long = 0L,
    @SerialName("tcp_ack") val tcpAck: Long = 0L,
    @SerialName("tcp_window") val tcpWindow: Int = 0,
    @SerialName("payload_length") val payloadLength: Int = 0,
    @SerialName("payload_hex") val payloadHex: String = "",
    @SerialName("payload_ascii") val payloadAscii: String = "",
    @SerialName("detected_protocol") val detectedProtocol: String = "",
    @SerialName("app_info") val appInfo: String = "",
    @SerialName("extra_metadata") val extraMetadata: Map<String, String> = emptyMap()
)

@Serializable
data class ParsedConnectionLog(
    @SerialName("protocol") val protocol: String = "TCP",
    @SerialName("src_ip") val srcIp: String = "",
    @SerialName("src_port") val srcPort: Int = 0,
    @SerialName("dest_ip") val destIp: String = "",
    @SerialName("dest_port") val destPort: Int = 0
)

@Serializable
data class PingTarget(
    @SerialName("id") val id: String = "",
    @SerialName("address") val address: String,
    @SerialName("port") val port: Int = 443
)

@Serializable
data class BatchPingResult(
    @SerialName("id") val id: String = "",
    @SerialName("address") val address: String = "",
    @SerialName("port") val port: Int = 0,
    @SerialName("success") val success: Boolean = false,
    @SerialName("latencyMs") val latencyMs: Double = 0.0,
    @SerialName("error") val error: String? = null
)

@Serializable
data class ProxyPingResult(
    @SerialName("success") val success: Boolean = false,
    @SerialName("latencyMs") val latencyMs: Double = 0.0,
    @SerialName("error") val error: String? = null
)

@Serializable
data class PublicIPInfo(
    @SerialName("ip") val ip: String,
    @SerialName("country") val country: String? = null,
    @SerialName("countryCode") val countryCode: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("region") val region: String? = null,
    @SerialName("org") val org: String? = null,
    @SerialName("asn") val asn: String? = null
)

@Serializable
data class AndroidLogEntry(
    @SerialName("id") val id: String = "",
    @SerialName("timestamp") val timestamp: Long = 0L,
    @SerialName("packageName") val packageName: String,
    @SerialName("appName") val appName: String,
    @SerialName("sourceIp") val sourceIp: String? = null,
    @SerialName("sourcePort") val sourcePort: Int? = null,
    @SerialName("destinationIp") val destinationIp: String,
    @SerialName("destinationPort") val destinationPort: Int,
    @SerialName("protocol") val protocol: String = "TCP",
    @SerialName("serviceName") val serviceName: String? = null,
    @SerialName("action") val action: String = "direct",
    @SerialName("threatType") val threatType: String = "NONE",
    @SerialName("riskScore") val riskScore: Int = 0
)

@Serializable
data class AppStat(
    @SerialName("packageName") val packageName: String,
    @SerialName("appName") val appName: String,
    @SerialName("count") val count: Long
)

@Serializable
data class PortStat(
    @SerialName("port") val port: Int,
    @SerialName("serviceName") val serviceName: String,
    @SerialName("count") val count: Long
)

@Serializable
data class AndroidLogStats(
    @SerialName("totalConnections") val totalConnections: Long = 0L,
    @SerialName("activeAppsCount") val activeAppsCount: Int = 0,
    @SerialName("threatCount") val threatCount: Long = 0L,
    @SerialName("protocolBreakdown") val protocolBreakdown: Map<String, Long> = emptyMap(),
    @SerialName("topApps") val topApps: List<AppStat> = emptyList(),
    @SerialName("topPorts") val topPorts: List<PortStat> = emptyList()
)
