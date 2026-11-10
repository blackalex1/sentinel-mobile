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
    private var entryProcess: Process? = null
    
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
     * Checks if any native Xray core subprocess is currently active and alive.
     */
    fun isProcessRunning(): Boolean {
        return (xrayProcess?.isAlive == true) || (entryProcess?.isAlive == true)
    }

    fun isMainProcessRunning(): Boolean = xrayProcess?.isAlive == true
    fun isEntryProcessRunning(): Boolean = entryProcess?.isAlive == true

    /**
     * Terminate any lingering native xray binary subprocesses under our app's UID SYNCHRONOUSLY
     * before a new process is created, preventing race conditions.
     */
    fun killExistingXrayProcessesSync() {
        val pMain = xrayProcess
        val pEntry = entryProcess
        xrayProcess = null
        entryProcess = null

        pMain?.let {
            try {
                it.destroyForcibly()
                it.waitFor()
            } catch (e: Exception) {}
        }
        pEntry?.let {
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
     * Launches Xray-core as isolated native subprocesses bound to our secure configuration.
     * Supports dual-instance architecture:
     * - Optional [entryConfigFilePath]: Entry Subprocess (e.g. subscription balancer gateway)
     * - [configFilePath]: Main VPN Subprocess (TUN interface and Exit node)
     */
    @Synchronized
    fun startProcess(
        context: Context,
        configFilePath: String,
        tunFd: Int? = null,
        entryConfigFilePath: String? = null
    ): Boolean {
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

        val logFile = File(context.filesDir, "xray.log")
        try {
            if (logFile.exists()) {
                logFile.delete()
            }
            logFile.createNewFile()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset xray.log", e)
        }

        val protectPath = File(context.filesDir, ProtectServer.PROTECT_SOCKET_NAME).absolutePath

        try {
            // 1. If entryConfigFilePath is provided, launch Entry Core Subprocess (Hop 1 / Subscription Balancer)
            if (!entryConfigFilePath.isNullOrEmpty()) {
                Log.i(TAG, "Launching native Xray Entry subprocess with config: $entryConfigFilePath")
                val entryBuilder = ProcessBuilder(xrayFile.absolutePath, "-config", entryConfigFilePath)
                    .directory(binDir)
                    .redirectErrorStream(true)

                entryBuilder.environment()["assets"] = binDir.absolutePath
                entryBuilder.environment()["xray.location.asset"] = binDir.absolutePath
                entryBuilder.environment()["v2ray.protect.path"] = protectPath
                entryBuilder.environment()["xray.protect.path"] = protectPath
                entryBuilder.environment()["V2RAY_PROTECT_PATH"] = protectPath
                entryBuilder.environment()["XRAY_PROTECT_PATH"] = protectPath

                val entryProc = entryBuilder.start()
                entryProcess = entryProc

                // Monitor Entry process log outputs on a background thread
                Thread {
                    try {
                        val reader = entryProc.inputStream.bufferedReader()
                        reader.forEachLine { line ->
                            val tagged = "[EntryCore] $line"
                            Log.d("XrayEntryProcess", tagged)
                            try {
                                logFile.appendText(tagged + "\n")
                            } catch (e: Exception) {}
                            _xrayLogFlow.tryEmit(tagged)
                        }
                    } catch (e: Exception) {
                        if (e !is java.io.IOException) {
                            Log.e(TAG, "Error in entry process monitor thread", e)
                        }
                    }
                }.start()

                // Allow entry core to bind its local socket before main core starts dialing
                Thread.sleep(150)
            }

            // 2. Launch Main VPN Xray Subprocess
            Log.i(TAG, "Launching native Xray Main process with config: $configFilePath")
            val builder = ProcessBuilder(xrayFile.absolutePath, "-config", configFilePath)
                .directory(binDir)
                .redirectErrorStream(true)

            // Inject local secure asset path variables strictly inside binDir
            builder.environment()["assets"] = binDir.absolutePath
            builder.environment()["xray.location.asset"] = binDir.absolutePath

            // Inject Unix domain socket path for outbound socket protection via VpnService.protect()
            builder.environment()["v2ray.protect.path"] = protectPath
            builder.environment()["xray.protect.path"] = protectPath
            builder.environment()["V2RAY_PROTECT_PATH"] = protectPath
            builder.environment()["XRAY_PROTECT_PATH"] = protectPath
            
            if (tunFd != null) {
                Log.i(TAG, "Passing inherited TUN FD $tunFd to native child process")
                builder.environment()["xray.tun.fd"] = tunFd.toString()
                builder.environment()["XRAY_TUN_FD"] = tunFd.toString()
                if (tunFd == 0) {
                    builder.redirectInput(ProcessBuilder.Redirect.INHERIT)
                }
            }

            val proc = builder.start()
            xrayProcess = proc

            // Monitor process log outputs on a background thread
            Thread {
                try {
                    val reader = proc.inputStream.bufferedReader()
                    reader.forEachLine { line ->
                        val tagged = "[MainCore] $line"
                        Log.d("XrayCoreProcess", tagged)
                        if (line.contains("bind: address already in use") || line.contains("failed to listen TCP")) {
                            Log.w(TAG, "Detected Xray port binding failure in log output: $line")
                            lastPortBindFailed = true
                        }
                        // Stream connection events reactively from Xray logs to the connection audit logger
                        ConnectionAuditParser.parseAndLog(context, line)
                        
                        // Append to xray.log
                        try {
                            logFile.appendText(tagged + "\n")
                        } catch (e: Exception) {
                            // ignore
                        }

                        // Emit line to flow
                        _xrayLogFlow.tryEmit(tagged)
                    }
                    val exitVal = proc.waitFor()
                    val runDuration = System.currentTimeMillis() - startTime
                    Log.i(TAG, "Main Xray process exited with code $exitVal (duration: ${runDuration}ms)")
                    
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

            Log.i(TAG, "Xray-core native subprocesses successfully executed")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Xray-core subprocesses", e)
        }
        return false
    }

    /**
     * Terminate all running Xray subprocesses asynchronously off the main UI thread.
     */
    fun stopProcess() {
        consecutiveFailures = 0
        val (pMain, pEntry) = synchronized(this) {
            val procM = xrayProcess
            val procE = entryProcess
            xrayProcess = null
            entryProcess = null
            Pair(procM, procE)
        }

        if (pMain == null && pEntry == null) return

        Thread {
            try {
                Log.i(TAG, "Terminating running Xray native subprocess instances asynchronously")
                pMain?.destroyForcibly()
                pEntry?.destroyForcibly()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    pMain?.waitFor(300, java.util.concurrent.TimeUnit.MILLISECONDS)
                    pEntry?.waitFor(300, java.util.concurrent.TimeUnit.MILLISECONDS)
                } else {
                    pMain?.waitFor()
                    pEntry?.waitFor()
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
