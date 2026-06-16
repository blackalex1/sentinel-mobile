package com.xprox.sentinel.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.util.Log
import com.xprox.sentinel.config.XrayConfigManager
import com.xprox.sentinel.config.XrayProfilePersistence
import com.xprox.sentinel.data.LanguageManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Standard Android VpnService implementation. Handles loopback protection credentials,
 * dynamic rules compile, routing initialization, and starts the background package tracker.
 */
class VpnManagerService : VpnService() {

    companion object {
        private const val TAG = "VpnManagerService"
        
        const val ACTION_DISCONNECT = "com.xprox.sentinel.action.DISCONNECT"
        const val ACTION_RELOAD_CONFIG = "com.xprox.sentinel.action.RELOAD_CONFIG"
        const val ACTION_RESTART_PROCESS = "com.xprox.sentinel.action.RESTART_PROCESS"

        // Live connection statistics & credentials visible to Jetpack Compose UI
        private val _isRunningFlow = MutableStateFlow(false)
        val isRunningFlow: StateFlow<Boolean> = _isRunningFlow.asStateFlow()

        private val _activePortFlow = MutableStateFlow(0)
        val activePortFlow: StateFlow<Int> = _activePortFlow.asStateFlow()

        private val _activeCredentials = MutableStateFlow<XrayConfigManager.LocalProxyCredentials?>(null)
        val activeCredentials: StateFlow<XrayConfigManager.LocalProxyCredentials?> = _activeCredentials.asStateFlow()

        private val _activeLanCredentials = MutableStateFlow<XrayConfigManager.LocalProxyCredentials?>(null)
        val activeLanCredentials: StateFlow<XrayConfigManager.LocalProxyCredentials?> = _activeLanCredentials.asStateFlow()

        private val _activeTetheringIps = MutableStateFlow<List<String>>(emptyList())
        val activeTetheringIps: StateFlow<List<String>> = _activeTetheringIps.asStateFlow()

        private val _activeLanHttpPort = MutableStateFlow(10809)
        val activeLanHttpPort: StateFlow<Int> = _activeLanHttpPort.asStateFlow()

        private val _activeLanSocksPort = MutableStateFlow(10808)
        val activeLanSocksPort: StateFlow<Int> = _activeLanSocksPort.asStateFlow()

        private val _pingMsFlow = MutableStateFlow<Int?>(null)
        val pingMsFlow: StateFlow<Int?> = _pingMsFlow.asStateFlow()

        private val _publicIpFlow = MutableStateFlow<String?>(null)
        val publicIpFlow: StateFlow<String?> = _publicIpFlow.asStateFlow()

        private val _speedFlow = MutableStateFlow("")
        val speedFlow: StateFlow<String> = _speedFlow.asStateFlow()

        var selectedProfile = XrayConfigManager.ServerProfile(
            name = "My Connection",
            address = "",
            port = 443,
            uuid = "",
            type = "VLESS",
            security = "none",
            path = ""
        )

        fun loadSelectedProfile(context: Context, forceProfileId: String? = null) {
            try {
                val activeId = forceProfileId ?: XrayProfilePersistence.getSelectedProfileId(context)
                val loadedProfiles = XrayProfilePersistence.loadProfiles(context)
                val selected = loadedProfiles.firstOrNull { it.id == activeId } ?: loadedProfiles.firstOrNull()
                if (selected != null) {
                    selectedProfile = selected
                    Log.i(TAG, "Successfully loaded active profile from persistence: ${selected.name}")
                }
            } catch (e: java.lang.Exception) {
                Log.e(TAG, "Failed to load selected profile from persistence", e)
            }
        }

        var allowedAppsList = listOf<String>()
        var blockedAppsList = listOf<String>()
        var isBypassMode = true
        var geoipRulesList = listOf("geoip:private", "geoip:ru")
        var geositeRulesList = listOf("geosite:google", "geosite:category-ads-all")

        fun measureProfilePing(profile: XrayConfigManager.ServerProfile) {
            VpnNetworkHelper.measureProfilePing(profile, _pingMsFlow)
        }

        fun fetchPublicIp(socksPort: Int = 0) {
            VpnNetworkHelper.fetchPublicIp(socksPort, _publicIpFlow)
        }
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null
    private var packetProcessor: VpnPacketProcessor? = null
    private var activeRawFd: Int = -1
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pingJob: Job? = null
    private var speedMonitorJob: Job? = null
    private var resolvedServerIp: String? = null
    private var captureProxyServer: CaptureProxyServer? = null
    private var activeCapturePort: Int = 0

    // WakeLock keeps CPU active during VPN session to prevent Doze from killing Xray
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        LanguageManager.init(this)
        com.xprox.sentinel.service.ThreatDetectionManager.init(this)
        loadSelectedProfile(this)
        VpnNotificationHelper.createNotificationChannel(this)
        registerHotspotReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISCONNECT) {
            Log.i(TAG, "Disconnect action received. Stopping VPN tunnel.")
            stopVpn()
            stopSelf()
            return START_NOT_STICKY
        }
        
        if (intent?.action == ACTION_RELOAD_CONFIG) {
            Log.i(TAG, "Reload config action received. Re-compiling and restarting Xray core.")
            // Always re-read the selected profile from persistence so we switch to the
            // profile the user actually picked — not the stale in-memory reference.
            val profileId = intent.getStringExtra("EXTRA_PROFILE_ID")
            loadSelectedProfile(this, profileId)
            reloadVpnConfig()
            return START_STICKY
        }
        
        if (intent?.action == ACTION_RESTART_PROCESS) {
            Log.i(TAG, "Restart process action received. Relaunching native core.")
            if (_isRunningFlow.value) {
                if (XrayProcessManager.isInstalled(this) && activeRawFd != -1) {
                    val configFile = File(filesDir, "secure_xray_config.json")
                    if (configFile.exists()) {
                        XrayProcessManager.stopProcess()
                        XrayProcessManager.startProcess(this, configFile.absolutePath, activeRawFd)
                        Log.i(TAG, "Native process successfully restarted from unexpected exit")
                    } else {
                        reloadVpnConfig()
                    }
                } else {
                    reloadVpnConfig()
                }
            }
            return START_STICKY
        }
        
        Log.i(TAG, "Starting VpnManagerService service")
        loadSelectedProfile(this)
        
        val initNotification = VpnNotificationHelper.createNotification(
            context = this,
            profileName = selectedProfile.name,
            profileAddress = selectedProfile.address,
            socksPort = _activePortFlow.value
        )
        startForeground(VpnNotificationHelper.NOTIFICATION_ID, initNotification)
        
        serviceScope.launch {
            startVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (_isRunningFlow.value) return

        // Acquire a PARTIAL_WAKE_LOCK to prevent the CPU from sleeping while
        // Xray-core is running. Battery optimisation exclusion alone is not enough
        // to survive Doze Mode on custom firmware (MIUI, ColorOS, One UI).
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock?.release()
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Sentinel:VpnSession"
        ).also { it.acquire() }

        try {
            // Setup Log Rotation at the start of a new VPN session (wipes active, shifts & saves 5 historical sessions)
            com.xprox.sentinel.log.LogManager.rotateLogs(this)
            
            // Load selected profile from persistence to ensure we have the correct active profile name and details
            loadSelectedProfile(this)
            
            // Notify VPN service lifecycle observer
            VpnLifecycleProvider.listener?.onServiceStart(this, serviceScope, selectedProfile.id)
            
            // Pre-resolve VPN server hostname before creating the TUN interface (while network DNS works)
            resolvedServerIp = null
            if (selectedProfile.address.isNotEmpty()) {
                try {
                    val addressObj = java.net.InetAddress.getByName(selectedProfile.address)
                    resolvedServerIp = addressObj.hostAddress
                    Log.i(TAG, "Pre-resolved VPN server hostname ${selectedProfile.address} to $resolvedServerIp")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to pre-resolve VPN server hostname: ${selectedProfile.address}", e)
                }
            }

            // 1. Setup SOCKS5 vulnerability mitigation
            val isLocalProxyRandomize = XrayProfilePersistence.loadLocalProxyRandomize(this)
            val creds = if (isLocalProxyRandomize) {
                XrayConfigManager.generateSecureCredentials()
            } else {
                val user = XrayProfilePersistence.loadLocalProxyUsername(this)
                val pass = XrayProfilePersistence.loadLocalProxyPassword(this)
                val port = XrayProfilePersistence.loadLocalProxyPort(this)
                XrayConfigManager.LocalProxyCredentials(port = port, username = user, token = pass)
            }
            _activeCredentials.value = creds
            _activePortFlow.value = creds.port

            // Set secure SOCKS5 authenticator to allow our own app to query the local proxy safely
            java.net.Authenticator.setDefault(object : java.net.Authenticator() {
                override fun getPasswordAuthentication(): java.net.PasswordAuthentication {
                    if (requestingHost == "127.0.0.1" && requestingPort == creds.port) {
                        return java.net.PasswordAuthentication(creds.username, creds.token.toCharArray())
                    }
                    return super.getPasswordAuthentication()
                }
            })

            // Detect active tethering IPs and load hotspot credentials settings
            val tetherIps = getActiveTetheringIps()
            _activeTetheringIps.value = tetherIps
            
            val isLanSharingRandomize = XrayProfilePersistence.loadLanSharingRandomize(this)
            val lanHttpPort: Int
            val lanSocksPort: Int
            
            if (isLanSharingRandomize) {
                lanHttpPort = XrayConfigManager.findRandomOpenPort()
                var socksPort = XrayConfigManager.findRandomOpenPort()
                while (socksPort == lanHttpPort || socksPort == creds.port) {
                    socksPort = XrayConfigManager.findRandomOpenPort()
                }
                lanSocksPort = socksPort
            } else {
                lanHttpPort = XrayProfilePersistence.loadLanSharingHttpPort(this)
                lanSocksPort = XrayProfilePersistence.loadLanSharingSocksPort(this)
            }
            
            _activeLanHttpPort.value = lanHttpPort
            _activeLanSocksPort.value = lanSocksPort
            
            val isLanAuthEnabled = XrayProfilePersistence.loadLanSharingAuth(this)
            val lanCreds = if (isLanAuthEnabled && XrayProfilePersistence.loadLanSharing(this)) {
                if (isLanSharingRandomize) {
                    val tempCreds = XrayConfigManager.generateSecureCredentials()
                    val c = XrayConfigManager.LocalProxyCredentials(
                        port = lanSocksPort,
                        username = tempCreds.username,
                        token = tempCreds.token
                    )
                    _activeLanCredentials.value = c
                    c
                } else {
                    val user = XrayProfilePersistence.loadLanSharingUsername(this)
                    val pass = XrayProfilePersistence.loadLanSharingPassword(this)
                    val c = XrayConfigManager.LocalProxyCredentials(
                        port = lanSocksPort,
                        username = user,
                        token = pass
                    )
                    _activeLanCredentials.value = c
                    c
                }
            } else {
                _activeLanCredentials.value = null
                null
            }

            // Initialize and start the SOCKS5 Capture Proxy Server
            activeCapturePort = XrayConfigManager.findRandomOpenPort()
            captureProxyServer = CaptureProxyServer(
                context = this,
                listenPort = activeCapturePort,
                targetSocksPort = creds.port,
                targetSocksUsername = creds.username,
                targetSocksToken = creds.token
            ).apply { start() }

            // 2. Generate/Compile client configuration file
            val configFile = XrayConfigManager.compileSecureConfig(
                context = this,
                profile = selectedProfile,
                creds = creds,
                allowedApps = allowedAppsList,
                blockedApps = blockedAppsList,
                geoipRules = geoipRulesList,
                geositeRules = geositeRulesList,
                lanAuthEnabled = isLanAuthEnabled,
                lanCreds = lanCreds,
                tetheringIps = tetherIps,
                lanHttpPort = lanHttpPort,
                lanSocksPort = lanSocksPort,
                captureProxyPort = activeCapturePort
            )

            // 2. Create the Android VPN TUN Interface
            val builder = Builder()
                .setSession("Sentinel Secure Shield")
                .addAddress("10.0.0.2", 24)
                .addAddress("fd00::2", 64)
                .addDnsServer("8.8.8.8")
                .addDnsServer("1.1.1.1")
                .addDnsServer("2001:4860:4860::8888")
                .addDnsServer("2606:4700:4700::1111")
                // Enforce default routing through our secure tunnel interface
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)

            // Implement dynamic App Routing (Split Tunneling)
            if (isBypassMode) {
                // Bypass Mode: proxy all except selected apps
                // We must explicitly exclude our own app package to prevent routing loops
                try {
                    builder.addDisallowedApplication(packageName)
                    Log.i(TAG, "Successfully excluded VPN app $packageName from global routing to prevent loops")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not exclude own VPN app from routing", e)
                }

                allowedAppsList.forEach { appPackage ->
                    try {
                        builder.addDisallowedApplication(appPackage)
                        Log.d(TAG, "Bypass mode: Excluded app $appPackage")
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not exclude app: $appPackage", e)
                    }
                }
            } else {
                // Selection Mode: proxy only selected apps
                allowedAppsList.forEach { appPackage ->
                    if (appPackage != packageName) {
                        try {
                            builder.addAllowedApplication(appPackage)
                            Log.d(TAG, "Selection mode: Allowed app $appPackage")
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not allow app: $appPackage", e)
                        }
                    }
                }
            }

            val pfd = builder.establish()
            if (pfd == null) {
                Log.e(TAG, "Failed to establish TUN interface")
                stopSelf()
                return
            }
            vpnInterface = pfd

            // 3. Start the actual native Xray Core subprocess or fall back to standby mode
            if (XrayProcessManager.isInstalled(this)) {
                // Duplicate the ParcelFileDescriptor so we can detach it safely without revoking the VPN tunnel
                val dupPfd = pfd.dup()
                val rawFd = dupPfd.detachFd()
                activeRawFd = rawFd
                Log.i(TAG, "Duplicated and detached TUN file descriptor: $rawFd")

                // Clear FD_CLOEXEC flag on the detached raw file descriptor
                try {
                    val fdObj = java.io.FileDescriptor()
                    val fdField = java.io.FileDescriptor::class.java.getDeclaredField("descriptor").apply {
                        isAccessible = true
                    }
                    fdField.setInt(fdObj, rawFd)

                    val flags = android.system.Os.fcntlInt(fdObj, android.system.OsConstants.F_GETFD, 0)
                    android.system.Os.fcntlInt(fdObj, android.system.OsConstants.F_SETFD, flags and android.system.OsConstants.FD_CLOEXEC.inv())
                    Log.i(TAG, "Successfully cleared FD_CLOEXEC on detached TUN raw FD: $rawFd")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to clear FD_CLOEXEC on detached TUN raw FD", e)
                }

                // Start native Xray process passing our established TUN FD
                XrayProcessManager.startProcess(this, configFile.absolutePath, rawFd)
            } else {
                Log.w(TAG, "Xray-core binary not downloaded yet. Running in tunnel-auditor standby mode.")
            }

            // 4. Start VpnPacketProcessor Loop thread only if we are in standby mode (no native Xray)
            if (!XrayProcessManager.isInstalled(this)) {
                val appResolver = AppResolver(this)
                val tracker = ConnectionTracker(this, appResolver) {
                    // Connection tracked
                }

                packetProcessor = VpnPacketProcessor(this, vpnInterface!!, tracker)
                vpnThread = Thread(packetProcessor).apply {
                    name = "sentinel-packet-thread"
                    start()
                }
            } else {
                Log.i(TAG, "Native Xray active. Bypassing Java packet processor thread to let Xray handle TUN exclusively.")
            }

            _isRunningFlow.value = true
            Log.i(TAG, "Sentinel secure tunnel established and active")
            
            VpnNetworkMonitor.registerNetworkCallback(this) {
                reloadVpnConfig()
            }

            // 5. Start background speed monitor loop
            speedMonitorJob?.cancel()
            var lastShowSpeed = XrayProfilePersistence.loadShowSpeedInNotification(this)
            speedMonitorJob = VpnSpeedMonitor.start(serviceScope, _isRunningFlow) { speedText ->
                _speedFlow.value = speedText
                val showSpeed = XrayProfilePersistence.loadShowSpeedInNotification(this)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (showSpeed) {
                    val notif = VpnNotificationHelper.createNotification(
                        context = this,
                        profileName = selectedProfile.name,
                        profileAddress = selectedProfile.address,
                        socksPort = _activePortFlow.value,
                        speedText = speedText
                    )
                    manager.notify(VpnNotificationHelper.NOTIFICATION_ID, notif)
                } else if (lastShowSpeed) {
                    // Reset once to standard static notification when toggled off
                    val notif = VpnNotificationHelper.createNotification(
                        context = this,
                        profileName = selectedProfile.name,
                        profileAddress = selectedProfile.address,
                        socksPort = _activePortFlow.value,
                        speedText = ""
                    )
                    manager.notify(VpnNotificationHelper.NOTIFICATION_ID, notif)
                }
                lastShowSpeed = showSpeed
            }

            // 6. Start background ping & public IP measurement once upon connection start
            pingJob?.cancel();
            pingJob = serviceScope.launch {
                // Task 1: Fetch public IP address through SOCKS5 proxy tunnel once using DNS-free endpoints
                launch {
                    delay(1500)
                    val port = _activePortFlow.value
                    if (port > 0) {
                        for (attempt in 1..4) {
                            val ip = VpnNetworkHelper.suspendFetchPublicIp(port)
                            if (ip != null) {
                                _publicIpFlow.value = ip
                                break
                            }
                            if (!_isRunningFlow.value) break
                            delay(3000)
                        }
                    }
                }

                // Task 2: Latency ping measurement with socket protection & DNS-free IP connection once
                launch {
                    delay(1500)
                    VpnNetworkHelper.measureProfilePing(selectedProfile, _pingMsFlow) { socket ->
                        protect(socket)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN interface", e)
            stopSelf()
        }
    }

    private fun reloadVpnConfig() {
        if (!_isRunningFlow.value) return
        try {
            Log.i(TAG, "Reloading VPN and Xray config dynamically...")
            
            // 1. Re-compile secure configuration file
            val isLocalProxyRandomize = XrayProfilePersistence.loadLocalProxyRandomize(this)
            val creds = _activeCredentials.value ?: if (isLocalProxyRandomize) {
                XrayConfigManager.generateSecureCredentials()
            } else {
                val user = XrayProfilePersistence.loadLocalProxyUsername(this)
                val pass = XrayProfilePersistence.loadLocalProxyPassword(this)
                val port = XrayProfilePersistence.loadLocalProxyPort(this)
                XrayConfigManager.LocalProxyCredentials(port = port, username = user, token = pass)
            }
            
            val tetherIps = getActiveTetheringIps()
            val lanHttpPort = _activeLanHttpPort.value
            val lanSocksPort = _activeLanSocksPort.value
            val isLanAuthEnabled = XrayProfilePersistence.loadLanSharingAuth(this)
            val lanCreds = _activeLanCredentials.value

            val configFile = XrayConfigManager.compileSecureConfig(
                context = this,
                profile = selectedProfile,
                creds = creds,
                allowedApps = allowedAppsList,
                blockedApps = blockedAppsList,
                geoipRules = geoipRulesList,
                geositeRules = geositeRulesList,
                lanAuthEnabled = isLanAuthEnabled,
                lanCreds = lanCreds,
                tetheringIps = tetherIps,
                lanHttpPort = lanHttpPort,
                lanSocksPort = lanSocksPort,
                captureProxyPort = activeCapturePort
            )

            // 2. Restart native Xray core subprocess on active raw FD without revoking VPN interface
            if (XrayProcessManager.isInstalled(this) && activeRawFd != -1) {
                Log.i(TAG, "Restarting native Xray core dynamically using Raw FD: $activeRawFd")
                XrayProcessManager.stopProcess()
                XrayProcessManager.startProcess(this, configFile.absolutePath, activeRawFd)
                Log.i(TAG, "Native Xray core successfully restarted with updated Zero Trust blackhole rules")
                
                // Fetch public IP of the new profile connection through SOCKS5 proxy
                serviceScope.launch {
                    delay(1500)
                    val port = creds.port
                    if (port > 0) {
                        for (attempt in 1..4) {
                            val ip = VpnNetworkHelper.suspendFetchPublicIp(port)
                            if (ip != null) {
                                _publicIpFlow.value = ip
                                break
                            }
                            if (!_isRunningFlow.value) break
                            delay(3000)
                        }
                    }
                }
            } else {
                Log.w(TAG, "Cannot reload dynamically: Xray not installed or active raw FD is invalid")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reload VPN config dynamically", e)
        }
    }

    override fun onDestroy() {
        stopVpn()
        VpnNetworkMonitor.unregisterHotspotReceiver(this)
        try {
            serviceScope.cancel()
        } catch (e: Exception) {}
        super.onDestroy()
    }

    private fun stopVpn() {
        Log.i(TAG, "Stopping VpnManagerService service")
        // Release WakeLock so the CPU can sleep normally after VPN is stopped
        try {
            wakeLock?.release()
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock already released", e)
        }
        wakeLock = null
        pingJob?.cancel()
        pingJob = null
        speedMonitorJob?.cancel()
        speedMonitorJob = null
        VpnLifecycleProvider.listener?.onServiceStop()
        _pingMsFlow.value = null
        _publicIpFlow.value = null
        _speedFlow.value = ""
        java.net.Authenticator.setDefault(null)
        captureProxyServer?.stop()
        captureProxyServer = null
        activeCapturePort = 0

        XrayProcessManager.stopProcess() // Terminate Xray core subprocess
        
        // Delete temporary secure Xray configuration file to prevent sensitive data lingering on disk
        try {
            val configFile = File(filesDir, "secure_xray_config.json")
            if (configFile.exists()) {
                configFile.delete()
                Log.i(TAG, "Successfully deleted temporary secure Xray config file from disk")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete temporary secure Xray config file", e)
        }

        packetProcessor?.stop()
        vpnThread?.interrupt()
        
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            // Ignore
        }
        vpnInterface = null
        
        if (activeRawFd != -1) {
            try {
                val fdObj = java.io.FileDescriptor()
                val fdField = java.io.FileDescriptor::class.java.getDeclaredField("descriptor").apply {
                    isAccessible = true
                }
                fdField.setInt(fdObj, activeRawFd)
                android.system.Os.close(fdObj)
                Log.i(TAG, "Successfully closed raw TUN FD: $activeRawFd")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to close raw TUN FD: $activeRawFd", e)
            }
            activeRawFd = -1
        }
        
        packetProcessor = null
        vpnThread = null

        _isRunningFlow.value = false
        _activePortFlow.value = 0
        _activeCredentials.value = null
        _activeLanCredentials.value = null
        _activeTetheringIps.value = emptyList()
        _activeLanHttpPort.value = 10809
        _activeLanSocksPort.value = 10808
        VpnNetworkMonitor.unregisterNetworkCallback()
        stopForeground(true)
    }

    private fun getActiveTetheringIps(): List<String> = TetheringScanner.getActiveTetheringIps()

    private fun registerHotspotReceiver() {
        VpnNetworkMonitor.registerHotspotReceiver(
            context = this,
            onEnabled = { ips ->
                Log.i(TAG, "System Hotspot turned on! Scanning IPs.")
                _activeTetheringIps.value = ips
                if (_isRunningFlow.value && ips.isNotEmpty()) {
                    serviceScope.launch(Dispatchers.Main) {
                        delay(1200) // Small delay to let network interfaces initialize IP
                        val updatedIps = getActiveTetheringIps()
                        _activeTetheringIps.value = updatedIps
                        if (updatedIps.isNotEmpty() && _isRunningFlow.value) {
                            // Restart VPN to bind to the newly active hotspot interface IPs!
                            val restartIntent = Intent(this@VpnManagerService, VpnManagerService::class.java).apply {
                                action = ACTION_RELOAD_CONFIG
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(restartIntent)
                            } else {
                                startService(restartIntent)
                            }
                        }
                    }
                }
            },
            onDisabled = {
                Log.i(TAG, "System Hotspot turned off! Disabling LAN sharing.")
                XrayProfilePersistence.saveLanSharing(this, false)
                _activeTetheringIps.value = emptyList()
                _activeLanCredentials.value = null
                
                // Stop LAN sharing dynamically or trigger restart if running
                if (_isRunningFlow.value) {
                    serviceScope.launch(Dispatchers.Main) {
                        delay(500)
                        if (_isRunningFlow.value) {
                            val restartIntent = Intent(this@VpnManagerService, VpnManagerService::class.java).apply {
                                action = ACTION_RELOAD_CONFIG
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(restartIntent)
                            } else {
                                startService(restartIntent)
                            }
                        }
                    }
                }
            }
        )
    }
}
