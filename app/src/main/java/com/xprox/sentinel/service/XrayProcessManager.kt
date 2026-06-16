package com.xprox.sentinel.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.io.File
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.channels.BufferOverflow

/**
 * Handles secure execution and subprocess lifecycle
 * of the official XTLS Xray-core binary compiled for Android.
 */
object XrayProcessManager {
    private const val TAG = "XrayProcessManager"
    private const val BINARY_NAME = "xray"
    
    private var xrayProcess: Process? = null

    private val _xrayLogFlow = MutableSharedFlow<String>(
        replay = 100,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val xrayLogFlow: SharedFlow<String> = _xrayLogFlow.asSharedFlow()

    /**
     * Safe execution checker. Checks for both the native binary and the required GeoIP/GeoSite routing databases.
     */
    fun isInstalled(context: Context): Boolean {
        val binDir = File(context.filesDir, "bin")
        val xrayFile = File(binDir, BINARY_NAME)
        val geoip = File(binDir, "geoip.dat")
        val geosite = File(binDir, "geosite.dat")
        return xrayFile.exists() && xrayFile.canExecute() && geoip.exists() && geosite.exists()
    }

    /**
     * Launches Xray-core as an isolated native subprocess bound to our secure configuration.
     */
    @Synchronized
    fun startProcess(context: Context, configFilePath: String, tunFd: Int? = null): Boolean {
        if (xrayProcess != null) {
            stopProcess()
        }

        val binDir = File(context.filesDir, "bin")
        val xrayFile = File(binDir, BINARY_NAME)
        
        if (!xrayFile.exists()) {
            Log.e(TAG, "Xray-core binary not found. Cannot start process.")
            return false
        }

        try {
            Log.i(TAG, "Launching native Xray process with config: $configFilePath")
            
            // Build the process strictly within the app files sandbox directory
            val builder = ProcessBuilder(xrayFile.absolutePath, "-config", configFilePath)
                .directory(context.filesDir)
                .redirectErrorStream(true)

            // Inject local secure path variables
            builder.environment()["assets"] = context.filesDir.absolutePath
            
            if (tunFd != null) {
                Log.i(TAG, "Duplicating TUN FD $tunFd to parent stdin (FD 0) and inheriting in child")
                try {
                    val tunFdObj = java.io.FileDescriptor()
                    val descriptorField = java.io.FileDescriptor::class.java.getDeclaredField("descriptor").apply {
                        isAccessible = true
                    }
                    descriptorField.setInt(tunFdObj, tunFd)
                    
                    // Direct POSIX dup2 system call to duplicate our TUN FD to FD 0
                    android.system.Os.dup2(tunFdObj, 0)
                    
                    // Inherit stdin directly in the child process without opening proc files
                    builder.redirectInput(ProcessBuilder.Redirect.INHERIT)
                    
                    builder.environment()["xray.tun.fd"] = "0"
                    builder.environment()["XRAY_TUN_FD"] = "0"
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to duplicate TUN FD to parent stdin, falling back", e)
                    builder.environment()["xray.tun.fd"] = tunFd.toString()
                    builder.environment()["XRAY_TUN_FD"] = tunFd.toString()
                }
            }

            val logFile = File(context.filesDir, "xray.log")
            try {
                if (logFile.exists()) {
                    logFile.delete()
                }
                logFile.createNewFile()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset xray.log", e)
            }

            xrayProcess = builder.start()

            // Monitor process log outputs on a background thread
            Thread {
                try {
                    val p = xrayProcess
                    val reader = p?.inputStream?.bufferedReader()
                    reader?.forEachLine { line ->
                        Log.d("XrayCoreProcess", line)
                        // Stream connection events reactively from Xray logs to the connection audit logger
                        ConnectionAuditParser.parseAndLog(context, line)
                        
                        // Append to xray.log
                        try {
                            logFile.appendText(line + "\n")
                        } catch (e: Exception) {
                            // ignore
                        }

                        // Emit line to flow
                        _xrayLogFlow.tryEmit(line)
                    }
                    val exitVal = p?.waitFor()
                    Log.i(TAG, "Xray process exited with code $exitVal")
                    
                    synchronized(XrayProcessManager) {
                        if (xrayProcess != null && xrayProcess == p) {
                            xrayProcess = null
                            Log.w(TAG, "Xray process terminated unexpectedly! Sending restart intent to VpnManagerService.")
                            val intent = Intent(context, VpnManagerService::class.java).apply {
                                action = VpnManagerService.ACTION_RESTART_PROCESS
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (e is java.io.IOException) {
                        Log.i(TAG, "Process input stream closed (expected during shutdown/restart)")
                    } else {
                        Log.e(TAG, "Error in process monitor thread", e)
                    }
                }
            }.start()

            Log.i(TAG, "Xray-core native subprocess successfully executed")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Xray-core subprocess", e)
        }
        return false
    }

    /**
     * Terminate the running Xray subprocess.
     */
    @Synchronized
    fun stopProcess() {
        xrayProcess?.let {
            Log.i(TAG, "Terminating running Xray native subprocess")
            it.destroy()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val terminated = it.waitFor(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                    if (!terminated) {
                        Log.w(TAG, "Process did not exit after 500ms, destroying forcibly")
                        it.destroyForcibly()
                        it.waitFor(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                    }
                } else {
                    it.waitFor()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error waiting for process to terminate", e)
            }
            xrayProcess = null
            
            // Yield to let OS release socket bindings and prevent EADDRINUSE collisions
            try {
                Thread.sleep(200)
            } catch (e: Exception) {}
        }

        // Restore parent stdin (FD 0) to release reference to the TUN interface
        try {
            val devNull = android.system.Os.open("/dev/null", android.system.OsConstants.O_RDONLY, 0)
            android.system.Os.dup2(devNull, 0)
            android.system.Os.close(devNull)
            Log.i(TAG, "Successfully redirected parent stdin (FD 0) back to /dev/null to release TUN FD reference")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to redirect parent stdin back to /dev/null", e)
        }
    }

    fun getXrayLogs(context: Context): List<String> {
        val logFile = File(context.filesDir, "xray.log")
        return if (logFile.exists()) {
            try {
                logFile.readLines(Charsets.UTF_8)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    fun clearXrayLogs(context: Context) {
        val logFile = File(context.filesDir, "xray.log")
        if (logFile.exists()) {
            try {
                logFile.writeText("")
            } catch (e: Exception) {}
        }
    }
}
