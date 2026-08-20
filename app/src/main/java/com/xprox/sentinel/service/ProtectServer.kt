package com.xprox.sentinel.service

import android.net.LocalServerSocket
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.net.VpnService
import android.util.Log
import java.io.File
import java.io.FileDescriptor

/**
 * ProtectServer creates a Unix Domain Socket (protect_path) to receive socket file descriptors
 * from native processes (e.g. Xray / V2Ray) via SCM_RIGHTS and calls [VpnService.protect] on them.
 *
 * This allows native outbound sockets to bypass the Android VPN tunnel and route directly
 * through the underlying physical network (WiFi/Cellular), preventing routing loops and socket timeouts.
 */
class ProtectServer(
    private val vpnService: VpnService
) {
    companion object {
        private const val TAG = "ProtectServer"
        const val PROTECT_SOCKET_NAME = "protect_path"
    }

    private var serverSocket: LocalServerSocket? = null
    @Volatile private var isRunning = false
    private var workerThread: Thread? = null

    val socketFile: File = File(vpnService.filesDir, PROTECT_SOCKET_NAME)

    fun start() {
        if (isRunning) return
        isRunning = true

        workerThread = Thread({
            try {
                if (socketFile.exists()) {
                    socketFile.delete()
                }

                val localSocket = LocalSocket()
                localSocket.bind(LocalSocketAddress(socketFile.absolutePath, LocalSocketAddress.Namespace.FILESYSTEM))
                serverSocket = LocalServerSocket(localSocket.fileDescriptor)
                Log.i(TAG, "ProtectServer listening on ${socketFile.absolutePath}")

                while (isRunning) {
                    val client = try {
                        serverSocket?.accept()
                    } catch (e: Exception) {
                        null
                    } ?: break

                    try {
                        val input = client.inputStream
                        val buf = ByteArray(1)
                        val readBytes = input.read(buf)

                        if (readBytes > 0) {
                            val fds: Array<FileDescriptor>? = client.ancillaryFileDescriptors
                            if (fds != null && fds.isNotEmpty()) {
                                for (fd in fds) {
                                    val fdInt = getRawFd(fd)
                                    if (fdInt != -1) {
                                        val protected = vpnService.protect(fdInt)
                                        Log.d(TAG, "Protected native socket FD $fdInt -> success=$protected")
                                    }
                                }
                            }
                        }

                        // Send confirmation byte back to native dialer
                        client.outputStream.write(0)
                        client.outputStream.flush()
                    } catch (e: Exception) {
                        Log.d(TAG, "Failed handling protect request: ${e.message}")
                    } finally {
                        try {
                            client.close()
                        } catch (e: Exception) {}
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e(TAG, "ProtectServer encountered error", e)
                }
            } finally {
                stop()
            }
        }, "SentinelProtectServer").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        serverSocket = null
        try {
            if (socketFile.exists()) {
                socketFile.delete()
            }
        } catch (e: Exception) {}
    }

    private fun getRawFd(fd: FileDescriptor): Int {
        return try {
            val field = FileDescriptor::class.java.getDeclaredField("descriptor").apply {
                isAccessible = true
            }
            field.getInt(fd)
        } catch (e: Exception) {
            -1
        }
    }
}
