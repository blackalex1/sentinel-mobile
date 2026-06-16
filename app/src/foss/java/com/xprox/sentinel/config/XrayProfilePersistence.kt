package com.xprox.sentinel.config

import android.content.Context
import android.util.Log
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object XrayProfilePersistence {
    private const val TAG = "XrayProfilePersistence"
    private const val PREFS_NAME = "xprox_prefs"
    private const val KEY_PROFILES = "xray_profiles_list"
    private const val KEY_SELECTED_PROFILE_ID = "xray_selected_profile_id"

    private val _updatesFlow = MutableSharedFlow<Unit>(replay = 1).apply {
        tryEmit(Unit)
    }
    val updatesFlow: SharedFlow<Unit> = _updatesFlow.asSharedFlow()

    private fun notifyUpdate() {
        _updatesFlow.tryEmit(Unit)
    }

    // Cache the EncryptedSharedPreferences instance after first creation.
    // MasterKey.Builder involves an AES256 Keystore operation that can take
    // 100–2000 ms on some devices. Recreating it on every call blocks the caller thread.
    @Volatile private var cachedPrefs: android.content.SharedPreferences? = null

    private fun getEncryptedPrefs(context: Context): android.content.SharedPreferences {
        cachedPrefs?.let { return it }
        return synchronized(this) {
            cachedPrefs ?: run {
                val isUnitTest = try {
                    !(System.getProperty("java.vm.name") ?: "").contains("Dalvik", ignoreCase = true)
                } catch (e: Exception) {
                    false
                }
                val prefs = if (isUnitTest) {
                    Log.i(TAG, "Running in unit test mode, using standard SharedPreferences fallback")
                    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                } else {
                    try {
                        val masterKey = androidx.security.crypto.MasterKey.Builder(
                            context.applicationContext
                        )
                            .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                            .build()
                        androidx.security.crypto.EncryptedSharedPreferences.create(
                            context.applicationContext,
                            PREFS_NAME,
                            masterKey,
                            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                        )
                    } catch (e: Throwable) {
                        Log.e(TAG, "Failed to create EncryptedSharedPreferences, falling back to standard", e)
                        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    }
                }
                cachedPrefs = prefs
                prefs
            }
        }
    }

    fun saveProfiles(context: Context, profiles: List<XrayConfigManager.ServerProfile>) {
        val prefs = getEncryptedPrefs(context)
        val jsonArray = org.json.JSONArray()
        for (profile in profiles) {
            val json = org.json.JSONObject()
            json.put("id", profile.id)
            json.put("name", profile.name)
            json.put("address", profile.address)
            json.put("port", profile.port)
            json.put("type", profile.type)
            json.put("uuid", profile.uuid)
            json.put("path", profile.path)
            json.put("security", profile.security)
            json.put("sni", profile.sni)
            json.put("pbk", profile.pbk)
            json.put("sid", profile.sid)
            json.put("fp", profile.fp)
            json.put("network", profile.network)
            json.put("flow", profile.flow)
            json.put("encryption", profile.encryption)
            json.put("spx", profile.spx)
            json.put("host", profile.host)
            json.put("allowInsecure", profile.allowInsecure)
            json.put("alpn", profile.alpn)
            json.put("headerType", profile.headerType)
            json.put("pinnedPeerCertSha256", profile.pinnedPeerCertSha256)
            json.put("fullJsonConfig", profile.fullJsonConfig)
            jsonArray.put(json)
        }
        prefs.edit().putString(KEY_PROFILES, jsonArray.toString()).apply()
        notifyUpdate()
    }

    fun loadProfiles(context: Context): List<XrayConfigManager.ServerProfile> {
        val prefs = getEncryptedPrefs(context)
        val jsonStr = prefs.getString(KEY_PROFILES, null)
        if (jsonStr.isNullOrEmpty()) {
            val defaultDirect = XrayConfigManager.ServerProfile(
                id = UUID.randomUUID().toString(),
                name = "Анализ трафика (Без VPN)",
                address = "",
                port = 0,
                type = "DIRECT",
                uuid = "",
                security = "none",
                path = ""
            )
            val defaultList = listOf(defaultDirect)
            saveProfiles(context, defaultList)
            return defaultList
        }
        val profiles = mutableListOf<XrayConfigManager.ServerProfile>()
        try {
            val jsonArray = org.json.JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                profiles.add(
                    XrayConfigManager.ServerProfile(
                        id = json.optString("id", UUID.randomUUID().toString()),
                        name = json.optString("name", "Imported Server"),
                        address = json.optString("address", ""),
                        port = json.optInt("port", 443),
                        type = json.optString("type", "VLESS"),
                        uuid = json.optString("uuid", ""),
                        path = json.optString("path", ""),
                        security = json.optString("security", "none"),
                        sni = json.optString("sni", ""),
                        pbk = json.optString("pbk", ""),
                        sid = json.optString("sid", ""),
                        fp = json.optString("fp", "chrome"),
                        network = json.optString("network", "tcp"),
                        flow = json.optString("flow", ""),
                        encryption = json.optString("encryption", "none"),
                        spx = json.optString("spx", ""),
                        host = json.optString("host", ""),
                        allowInsecure = json.optBoolean("allowInsecure", false),
                        alpn = json.optString("alpn", ""),
                        headerType = json.optString("headerType", ""),
                        pinnedPeerCertSha256 = json.optString("pinnedPeerCertSha256", ""),
                        groupId = null,
                        fullJsonConfig = json.optString("fullJsonConfig", "")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load profiles", e)
        }
        return profiles
    }

    fun getSelectedProfileId(context: Context): String? {
        val prefs = getEncryptedPrefs(context)
        return prefs.getString(KEY_SELECTED_PROFILE_ID, null)
    }

    fun setSelectedProfileId(context: Context, id: String?) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putString(KEY_SELECTED_PROFILE_ID, id).commit()
        notifyUpdate()
    }

    private const val KEY_ALLOWED_APPS = "xprox_allowed_apps_list"
    private const val KEY_BYPASS_MODE = "xprox_bypass_mode_flag"
    private const val KEY_GEOIP_RULES = "xprox_geoip_rules_list"
    private const val KEY_GEOSITE_RULES = "xprox_geosite_rules_list"

    private fun saveStringList(context: Context, key: String, list: List<String>) {
        val prefs = getEncryptedPrefs(context)
        val jsonArray = org.json.JSONArray()
        list.forEach { jsonArray.put(it) }
        prefs.edit().putString(key, jsonArray.toString()).apply()
    }

    private fun loadStringList(context: Context, key: String, defaultList: List<String>): List<String> {
        val prefs = getEncryptedPrefs(context)
        val jsonStr = prefs.getString(key, null) ?: return defaultList
        return try {
            val jsonArray = org.json.JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            defaultList
        }
    }

    fun saveAllowedApps(context: Context, apps: List<String>) {
        saveStringList(context, KEY_ALLOWED_APPS, apps)
    }

    fun loadAllowedApps(context: Context): List<String> {
        return loadStringList(context, KEY_ALLOWED_APPS, emptyList())
    }

    fun saveBypassMode(context: Context, bypass: Boolean) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putBoolean(KEY_BYPASS_MODE, bypass).apply()
    }

    fun loadBypassMode(context: Context): Boolean {
        val prefs = getEncryptedPrefs(context)
        return prefs.getBoolean(KEY_BYPASS_MODE, true)
    }

    fun saveGeoIpRules(context: Context, rules: List<String>) {
        saveStringList(context, KEY_GEOIP_RULES, rules)
    }

    fun loadGeoIpRules(context: Context): List<String> {
        return loadStringList(context, KEY_GEOIP_RULES, listOf("geoip:private", "geoip:ru"))
    }

    fun saveGeoSiteRules(context: Context, rules: List<String>) {
        saveStringList(context, KEY_GEOSITE_RULES, rules)
    }

    fun loadGeoSiteRules(context: Context): List<String> {
        return loadStringList(context, KEY_GEOSITE_RULES, listOf("geosite:google", "geosite:category-ads-all"))
    }

    private const val KEY_LAN_SHARING = "xprox_lan_sharing_enabled"
    private const val KEY_LAN_SHARING_AUTH = "xprox_lan_sharing_auth_enabled"

    fun saveLanSharing(context: Context, enabled: Boolean) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putBoolean(KEY_LAN_SHARING, enabled).apply()
    }

    fun loadLanSharing(context: Context): Boolean {
        val prefs = getEncryptedPrefs(context)
        return prefs.getBoolean(KEY_LAN_SHARING, false)
    }

    fun saveLanSharingAuth(context: Context, enabled: Boolean) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putBoolean(KEY_LAN_SHARING_AUTH, enabled).apply()
    }

    fun loadLanSharingAuth(context: Context): Boolean {
        val prefs = getEncryptedPrefs(context)
        return prefs.getBoolean(KEY_LAN_SHARING_AUTH, false)
    }

    private const val KEY_LAN_SHARING_HTTP = "xprox_lan_sharing_http_enabled"
    private const val KEY_LAN_SHARING_SOCKS = "xprox_lan_sharing_socks_enabled"

    fun saveLanSharingHttp(context: Context, enabled: Boolean) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putBoolean(KEY_LAN_SHARING_HTTP, enabled).apply()
    }

    fun loadLanSharingHttp(context: Context): Boolean {
        val prefs = getEncryptedPrefs(context)
        return prefs.getBoolean(KEY_LAN_SHARING_HTTP, true)
    }

    fun saveLanSharingSocks(context: Context, enabled: Boolean) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putBoolean(KEY_LAN_SHARING_SOCKS, enabled).apply()
    }

    fun loadLanSharingSocks(context: Context): Boolean {
        val prefs = getEncryptedPrefs(context)
        return prefs.getBoolean(KEY_LAN_SHARING_SOCKS, true)
    }

    private const val KEY_LAN_SHARING_RANDOMIZE = "xprox_lan_sharing_randomize"

    fun saveLanSharingRandomize(context: Context, enabled: Boolean) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putBoolean(KEY_LAN_SHARING_RANDOMIZE, enabled).apply()
    }

    fun loadLanSharingRandomize(context: Context): Boolean {
        val prefs = getEncryptedPrefs(context)
        return prefs.getBoolean(KEY_LAN_SHARING_RANDOMIZE, false)
    }

    private const val KEY_LAN_SHARING_USERNAME = "xprox_lan_sharing_username"
    private const val KEY_LAN_SHARING_PASSWORD = "xprox_lan_sharing_password"
    private const val KEY_LAN_SHARING_HTTP_PORT = "xprox_lan_sharing_http_port"
    private const val KEY_LAN_SHARING_SOCKS_PORT = "xprox_lan_sharing_socks_port"

    fun saveLanSharingUsername(context: Context, value: String) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putString(KEY_LAN_SHARING_USERNAME, value).apply()
    }

    fun loadLanSharingUsername(context: Context): String {
        val prefs = getEncryptedPrefs(context)
        var value = prefs.getString(KEY_LAN_SHARING_USERNAME, null)
        if (value.isNullOrEmpty() || value == "sentinel_ap") {
            val newCreds = XrayConfigManager.generateSecureCredentials()
            saveLanSharingUsername(context, newCreds.username)
            saveLanSharingPassword(context, newCreds.token)
            value = newCreds.username
        }
        return value
    }

    fun saveLanSharingPassword(context: Context, value: String) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putString(KEY_LAN_SHARING_PASSWORD, value).apply()
    }

    fun loadLanSharingPassword(context: Context): String {
        val prefs = getEncryptedPrefs(context)
        var value = prefs.getString(KEY_LAN_SHARING_PASSWORD, null)
        if (value.isNullOrEmpty() || value == "sentinel_pass") {
            // Delegate to loadLanSharingUsername which generates a paired credential set
            // atomically, so username and password always come from the same SecureRandom call.
            loadLanSharingUsername(context)
            value = prefs.getString(KEY_LAN_SHARING_PASSWORD, "") ?: ""
        }
        return value ?: ""
    }

    fun saveLanSharingHttpPort(context: Context, value: Int) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putInt(KEY_LAN_SHARING_HTTP_PORT, value).apply()
    }

    fun loadLanSharingHttpPort(context: Context): Int {
        val prefs = getEncryptedPrefs(context)
        var value = prefs.getInt(KEY_LAN_SHARING_HTTP_PORT, 0)
        if (value == 0 || value == 10809) {
            value = XrayConfigManager.findRandomOpenPort()
            saveLanSharingHttpPort(context, value)
        }
        return value
    }

    fun saveLanSharingSocksPort(context: Context, value: Int) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putInt(KEY_LAN_SHARING_SOCKS_PORT, value).apply()
    }

    fun loadLanSharingSocksPort(context: Context): Int {
        val prefs = getEncryptedPrefs(context)
        var value = prefs.getInt(KEY_LAN_SHARING_SOCKS_PORT, 0)
        if (value == 0 || value == 10808) {
            val httpPort = loadLanSharingHttpPort(context)
            value = XrayConfigManager.findRandomOpenPort()
            while (value == httpPort) {
                value = XrayConfigManager.findRandomOpenPort()
            }
            saveLanSharingSocksPort(context, value)
        }
        return value
    }

    private const val KEY_LOCAL_PROXY_RANDOMIZE = "xprox_local_proxy_randomize"
    private const val KEY_LOCAL_PROXY_USERNAME = "xprox_local_proxy_username"
    private const val KEY_LOCAL_PROXY_PASSWORD = "xprox_local_proxy_password"
    private const val KEY_LOCAL_PROXY_PORT = "xprox_local_proxy_port"

    fun saveLocalProxyRandomize(context: Context, enabled: Boolean) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putBoolean(KEY_LOCAL_PROXY_RANDOMIZE, enabled).apply()
    }

    fun loadLocalProxyRandomize(context: Context): Boolean {
        val prefs = getEncryptedPrefs(context)
        return prefs.getBoolean(KEY_LOCAL_PROXY_RANDOMIZE, true)
    }

    fun saveLocalProxyUsername(context: Context, value: String) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putString(KEY_LOCAL_PROXY_USERNAME, value).apply()
    }

    fun loadLocalProxyUsername(context: Context): String {
        val prefs = getEncryptedPrefs(context)
        var value = prefs.getString(KEY_LOCAL_PROXY_USERNAME, null)
        if (value.isNullOrEmpty()) {
            val newCreds = XrayConfigManager.generateSecureCredentials()
            saveLocalProxyUsername(context, newCreds.username)
            saveLocalProxyPassword(context, newCreds.token)
            value = newCreds.username
        }
        return value
    }

    fun saveLocalProxyPassword(context: Context, value: String) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putString(KEY_LOCAL_PROXY_PASSWORD, value).apply()
    }

    fun loadLocalProxyPassword(context: Context): String {
        val prefs = getEncryptedPrefs(context)
        val value = prefs.getString(KEY_LOCAL_PROXY_PASSWORD, null)
        if (value.isNullOrEmpty()) {
            // Delegate to loadLocalProxyUsername which generates a paired credential set
            // atomically, so username and password always come from the same SecureRandom call.
            loadLocalProxyUsername(context)
            return prefs.getString(KEY_LOCAL_PROXY_PASSWORD, "") ?: ""
        }
        return value
    }

    fun saveLocalProxyPort(context: Context, value: Int) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putInt(KEY_LOCAL_PROXY_PORT, value).apply()
    }

    fun loadLocalProxyPort(context: Context): Int {
        val prefs = getEncryptedPrefs(context)
        var value = prefs.getInt(KEY_LOCAL_PROXY_PORT, 0)
        if (value == 0) {
            value = XrayConfigManager.findRandomOpenPort()
            saveLocalProxyPort(context, value)
        }
        return value
    }

    private const val KEY_DNS_SERVERS = "xprox_dns_servers_list"

    fun saveDnsServers(context: Context, servers: List<String>) {
        saveStringList(context, KEY_DNS_SERVERS, servers)
    }

    fun loadDnsServers(context: Context): List<String> {
        return loadStringList(context, KEY_DNS_SERVERS, listOf("https://1.1.1.1/dns-query", "8.8.8.8", "1.1.1.1"))
    }

    private const val KEY_BLOCKED_THREAT_APPS = "xprox_blocked_threat_apps"

    fun saveBlockedApps(context: Context, apps: Set<String>) {
        saveStringList(context, KEY_BLOCKED_THREAT_APPS, apps.toList())
    }

    fun loadBlockedApps(context: Context): Set<String> {
        return loadStringList(context, KEY_BLOCKED_THREAT_APPS, emptyList()).toSet()
    }

    private const val KEY_FLAGGED_SYSTEM_APPS = "xprox_flagged_system_apps"

    fun saveFlaggedSystemApps(context: Context, apps: Set<String>) {
        saveStringList(context, KEY_FLAGGED_SYSTEM_APPS, apps.toList())
    }

    fun loadFlaggedSystemApps(context: Context): Set<String> {
        return loadStringList(context, KEY_FLAGGED_SYSTEM_APPS, emptyList()).toSet()
    }

    private const val KEY_SHOW_SPEED_IN_NOTIFICATION = "xprox_show_speed_in_notification"

    fun saveShowSpeedInNotification(context: Context, enabled: Boolean) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putBoolean(KEY_SHOW_SPEED_IN_NOTIFICATION, enabled).apply()
    }

    fun loadShowSpeedInNotification(context: Context): Boolean {
        val prefs = getEncryptedPrefs(context)
        return prefs.getBoolean(KEY_SHOW_SPEED_IN_NOTIFICATION, true)
    }

    private const val KEY_ANALYTICS_CONFIRMED = "xprox_analytics_confirmed"

    fun saveAnalyticsConfirmed(context: Context, confirmed: Boolean) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putBoolean(KEY_ANALYTICS_CONFIRMED, confirmed).apply()
        _updatesFlow.tryEmit(Unit)
    }

    fun loadAnalyticsConfirmed(context: Context): Boolean {
        val prefs = getEncryptedPrefs(context)
        return prefs.getBoolean(KEY_ANALYTICS_CONFIRMED, false)
    }

    private const val KEY_KILL_SWITCH = "xprox_kill_switch_enabled"

    fun saveKillSwitch(context: Context, enabled: Boolean) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putBoolean(KEY_KILL_SWITCH, enabled).apply()
        _updatesFlow.tryEmit(Unit)
    }

    fun loadKillSwitch(context: Context): Boolean {
        val prefs = getEncryptedPrefs(context)
        return prefs.getBoolean(KEY_KILL_SWITCH, false)
    }
}
