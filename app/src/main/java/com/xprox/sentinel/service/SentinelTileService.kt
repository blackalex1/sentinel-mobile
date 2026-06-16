package com.xprox.sentinel.service

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.xprox.sentinel.MainActivity
import com.xprox.sentinel.config.XrayConfigManager
import com.xprox.sentinel.config.XrayProfilePersistence
import kotlinx.coroutines.*

class SentinelTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var job: Job? = null

    companion object {
        private const val TAG = "SentinelTileService"

        fun requestTileUpdate(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    requestListeningState(
                        context,
                        android.content.ComponentName(context, SentinelTileService::class.java)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to request tile update", e)
                }
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        Log.i(TAG, "Tile started listening")
        
        // Listen to VPN running state changes reactively and update the tile
        job?.cancel()
        job = serviceScope.launch {
            VpnManagerService.isRunningFlow.collect { isRunning ->
                updateTileState(isRunning)
            }
        }
    }

    override fun onStopListening() {
        job?.cancel()
        super.onStopListening()
        Log.i(TAG, "Tile stopped listening")
    }

    override fun onClick() {
        super.onClick()
        Log.i(TAG, "Tile clicked")
        
        val isRunning = VpnManagerService.isRunningFlow.value
        val context = applicationContext

        if (isRunning) {
            val isCapturing = com.xprox.sentinel.service.ThreatDetectionManager.isAnyAppCapturingPcap()
            if (isCapturing) {
                val intent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("EXTRA_SHOW_DISCONNECT_CONFIRM", true)
                }
                try {
                    if (Build.VERSION.SDK_INT >= 34) {
                        val pendingIntent = android.app.PendingIntent.getActivity(
                            this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE
                        )
                        startActivityAndCollapse(pendingIntent)
                    } else {
                        startActivityAndCollapse(intent)
                    }
                } catch (e: Exception) {
                    startActivityAndCollapse(intent)
                }
            } else {
                // Disconnect VPN tunnel
                val intent = Intent(context, VpnManagerService::class.java).apply {
                    action = VpnManagerService.ACTION_DISCONNECT
                }
                context.startService(intent)
            }
        } else {
            // Connect VPN tunnel
            // First load active server profile to ensure one is configured
            val activeId = XrayProfilePersistence.getSelectedProfileId(context)
            val loadedProfiles = XrayProfilePersistence.loadProfiles(context)
            val selected = loadedProfiles.firstOrNull { it.id == activeId } ?: loadedProfiles.firstOrNull()

            if (selected == null) {
                launchApp()
                return
            }

            val isDirect = selected.type.uppercase() == "DIRECT"
            if (!isDirect && (selected.address.isEmpty() || selected.uuid.isEmpty())) {
                // No profile configured: launch MainActivity to configure one
                launchApp()
                return
            }

            // Check if VPN permission is granted
            val vpnIntent = VpnService.prepare(context)
            if (vpnIntent != null) {
                // Permission not granted yet: launch MainActivity to request it
                launchApp()
            } else {
                // Permission granted: load selected profile details and start the service
                VpnManagerService.selectedProfile = selected
                
                // Load all routing options in case the service starts fresh
                VpnManagerService.allowedAppsList = XrayProfilePersistence.loadAllowedApps(context)
                VpnManagerService.isBypassMode = XrayProfilePersistence.loadBypassMode(context)
                VpnManagerService.geoipRulesList = XrayProfilePersistence.loadGeoIpRules(context)
                VpnManagerService.geositeRulesList = XrayProfilePersistence.loadGeoSiteRules(context)

                val intent = Intent(context, VpnManagerService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
    }

    private fun launchApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                startActivityAndCollapse(intent)
            }
        } catch (e: Exception) {
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTileState(isRunning: Boolean) {
        val tile = qsTile ?: return
        
        if (isRunning) {
            tile.state = Tile.STATE_ACTIVE
            val isCapturing = com.xprox.sentinel.service.ThreatDetectionManager.isAnyAppCapturingPcap()
            tile.label = VpnManagerService.selectedProfile.name.ifEmpty { "Sentinel" }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (isCapturing) {
                    if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") "🔴 Идет сбор трафика" else "🔴 Capturing"
                } else {
                    "Connected"
                }
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Sentinel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Disconnected"
            }
        }
        
        tile.updateTile()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
