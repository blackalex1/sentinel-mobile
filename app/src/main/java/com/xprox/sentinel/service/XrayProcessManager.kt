package com.xprox.sentinel.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

object XrayProcessManager {
    private const val TAG = "XrayProcessManager"
    private const val BINARY_NAME = "xray"
    private const val MAX_CONSECUTIVE_RESTARTS = 3

    @Volatile
    private var xrayProcess: Process? = null
    
    @Volatile
    var lastStartTime: Long = 0L
        private set

    @Volatile
    var consecutiveFailures: Int = 0
        private set

    @Volatile
    var lastPortBindFailed: Boolean = false
        private set

    private val _xrayLogFlow = MutableSharedFlow<String>(
        replay = 100,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val xrayLogFlow: SharedFlow<String> = _xrayLogFlow.asSharedFlow()

    /**
     * Terminate any lingering native xray binary subprocesses under our app's UID SYNCHRONOUSLY
     * before a new process is created, preventing race conditions.
     */
    fun killExistingXrayProcessesSync() {
        val p = xrayProcess
        xrayProcess = null
        p?.let {
            try {
                it.destroyForcibly()
                it.waitFor()
            } catch (e: Exception) {}
        }
        try {
            Runtime.getRuntime().exec(arrayOf("pkill", "-9", "xray")).waitFor()
            Log.i(TAG, "Executed pkill -9 xray synchronously before starting core")
        } catch (e: Exception) {
            // Ignore if unexecutable
        }
    }

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
        // Clean up lingering processes SYNCHRONOUSLY first before spawning the new process
        killExistingXrayProcessesSync()

        lastPortBindFailed = false
        val startTime = System.currentTimeMillis()
        lastStartTime = startTime

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
                .directory(binDir)
                .redirectErrorStream(true)

            // Inject local secure asset path variables strictly inside binDir
            builder.environment()["assets"] = binDir.absolutePath
            builder.environment()["xray.location.asset"] = binDir.absolutePath

            // Inject Unix domain socket path for outbound socket protection via VpnService.protect()
            val protectPath = File(context.filesDir, ProtectServer.PROTECT_SOCKET_NAME).absolutePath
            builder.environment()["v2ray.protect.path"] = protectPath
            builder.environment()["xray.protect.path"] = protectPath
            builder.environment()["V2RAY_PROTECT_PATH"] = protectPath
            builder.environment()["XRAY_PROTECT_PATH"] = protectPath
            
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

            val proc = builder.start()
            xrayProcess = proc

            // Monitor process log outputs on a background thread
            Thread {
                try {
                    val reader = proc.inputStream.bufferedReader()
                    reader.forEachLine { line ->
                        Log.d("XrayCoreProcess", line)
                        if (line.contains("bind: address already in use") || line.contains("failed to listen TCP")) {
                            Log.w(TAG, "Detected Xray port binding failure in log output: $line")
                            lastPortBindFailed = true
                        }
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
                    val exitVal = proc.waitFor()
                    val runDuration = System.currentTimeMillis() - startTime
                    Log.i(TAG, "Xray process exited with code $exitVal (duration: ${runDuration}ms)")
                    
                    synchronized(XrayProcessManager) {
                        if (xrayProcess != null && xrayProcess == proc) {
                            xrayProcess = null
                            
                            if (runDuration > 5000L) {
                                consecutiveFailures = 0
                            } else {
                                consecutiveFailures++
                            }

                            if (consecutiveFailures > MAX_CONSECUTIVE_RESTARTS) {
                                Log.e(TAG, "Xray process failed $consecutiveFailures times consecutively (<5s runtime). Halting restart loop!")
                                return@synchronized
                            }

                            val backoffDelay = (consecutiveFailures * 1000L).coerceAtLeast(300L)
                            Log.w(TAG, "Xray process terminated unexpectedly! Consecutive failures: $consecutiveFailures. Scheduling restart in ${backoffDelay}ms.")
                            val intent = Intent(context, VpnManagerService::class.java).apply {
                                action = VpnManagerService.ACTION_RESTART_PROCESS
                                putExtra("EXTRA_PORT_BIND_FAILED", lastPortBindFailed)
                            }
                            Thread {
                                try {
                                    Thread.sleep(backoffDelay)
                                    context.startService(intent)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to send restart intent to VpnManagerService", e)
                                }
                            }.start()
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
     * Terminate the running Xray subprocess asynchronously off the main UI thread.
     * Target strictly the tracked process instance so it never kills newly starting processes.
     */
    fun stopProcess() {
        consecutiveFailures = 0
        val p = synchronized(this) {
            val proc = xrayProcess
            xrayProcess = null
            proc
        }

        if (p == null) return

        Thread {
            try {
                Log.i(TAG, "Terminating running Xray native subprocess instance asynchronously")
                p.destroyForcibly()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    p.waitFor(300, java.util.concurrent.TimeUnit.MILLISECONDS)
                } else {
                    p.waitFor()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error waiting for process to terminate", e)
            }

            // Restore parent stdin (FD 0) asynchronously
            try {
                val devNull = android.system.Os.open("/dev/null", android.system.OsConstants.O_RDONLY, 0)
                android.system.Os.dup2(devNull, 0)
                android.system.Os.close(devNull)
                Log.i(TAG, "Successfully redirected parent stdin (FD 0) back to /dev/null to release TUN FD reference")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to redirect parent stdin back to /dev/null", e)
            }
        }.start()
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
