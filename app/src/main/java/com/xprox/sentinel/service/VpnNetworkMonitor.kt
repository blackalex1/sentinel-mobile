package com.xprox.sentinel.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*

object VpnNetworkMonitor {
    private const val TAG = "VpnNetworkMonitor"

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var hotspotReceiver: BroadcastReceiver? = null

    // Debounce scope: prevents multiple rapid Xray restarts when the network
    // flickers during screen wake (Wi-Fi re-association creates a new Network object
    // for the same SSID, triggering onAvailable again).
    private val debounceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reloadJob: Job? = null

    fun registerNetworkCallback(
        context: Context,
        onReload: () -> Unit
    ) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager = cm
            val callback = object : ConnectivityManager.NetworkCallback() {
                private var lastNetwork: Network? = null
                
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    Log.i(TAG, "Default network changed/available: $network")
                    
                    val capabilities = cm.getNetworkCapabilities(network)
                    val isVpn = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
                    if (isVpn) {
                        Log.i(TAG, "Network $network is a VPN interface. Ignoring.")
                        return
                    }
                    
                    if (lastNetwork != null && lastNetwork != network) {
                        Log.i(TAG, "Active default network switched from $lastNetwork to $network. Scheduling debounced proxy daemon restart.")
                        // Debounce: cancel any pending reload and wait 1.5s before
                        // actually restarting Xray. This absorbs rapid oscillations
                        // that happen when the phone wakes from sleep (the system
                        // creates a new Network handle for the same Wi-Fi, fires
                        // onAvailable, then immediately stabilises).
                        reloadJob?.cancel()
                        reloadJob = debounceScope.launch {
                            delay(1500)
                            Log.i(TAG, "Debounce elapsed — triggering config reload for network switch")
                            onReload()
                        }
                    }
                    lastNetwork = network
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    Log.i(TAG, "Network lost: $network")
                    // Cancel any pending reload — reloading onto a lost network is pointless
                    reloadJob?.cancel()
                }
            }
            networkCallback = callback
            cm.registerDefaultNetworkCallback(callback)
            Log.i(TAG, "Default network callback registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register default network callback", e)
        }
    }

    fun unregisterNetworkCallback() {
        try {
            reloadJob?.cancel()
            reloadJob = null
            networkCallback?.let {
                connectivityManager?.unregisterNetworkCallback(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister network callback", e)
        }
        networkCallback = null
        connectivityManager = null
    }

    fun registerHotspotReceiver(
        context: Context,
        onEnabled: (ips: List<String>) -> Unit,
        onDisabled: () -> Unit
    ) {
        if (hotspotReceiver == null) {
            val filter = IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED")
            hotspotReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val state = intent.getIntExtra("wifi_state", 14)
                    // 11 is WIFI_AP_STATE_DISABLED, 10 is WIFI_AP_STATE_DISABLING
                    if (state == 11 || state == 10) {
                        onDisabled()
                    } else if (state == 13) { // WIFI_AP_STATE_ENABLED
                        val ips = TetheringScanner.getActiveTetheringIps()
                        onEnabled(ips)
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(hotspotReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(hotspotReceiver, filter)
            }
        }
    }

    fun unregisterHotspotReceiver(context: Context) {
        hotspotReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {}
        }
        hotspotReceiver = null
    }
}
