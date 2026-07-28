package com.xprox.sentinel.service.vpn

import android.content.Context
import android.util.Log
import com.xprox.sentinel.service.XrayProcessManager
import java.io.File

object VpnProcessSupervisor {

    private const val TAG = "VpnProcessSupervisor"

    fun startOrRestartCoreProcess(
        context: Context,
        configFile: File,
        activeRawFd: Int
    ): Boolean {
        if (!XrayProcessManager.isInstalled(context)) {
            Log.e(TAG, "Xray binary is not installed")
            return false
        }
        if (activeRawFd == -1) {
            Log.e(TAG, "Invalid TUN raw file descriptor")
            return false
        }
        if (!configFile.exists()) {
            Log.e(TAG, "Config file does not exist at ${configFile.absolutePath}")
            return false
        }

        try {
            XrayProcessManager.stopProcess()
            XrayProcessManager.startProcess(context, configFile.absolutePath, activeRawFd)
            Log.i(TAG, "Native process started successfully with config: ${configFile.name}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start native process", e)
            return false
        }
    }

    fun stopCoreProcess() {
        try {
            XrayProcessManager.stopProcess()
            Log.i(TAG, "Native core process stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping core process", e)
        }
    }
}
