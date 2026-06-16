package com.xprox.sentinel.log

import android.content.Context
import android.util.Log
import java.io.File

object LogRotator {
    private const val TAG = "LogRotator"
    private const val LOG_FILE_NAME = "x_prox_sensitive_connections.log"

    @Synchronized
    fun rotateLogs(context: Context) {
        try {
            val directory = context.filesDir
            val activeLog = File(directory, LOG_FILE_NAME)

            if (activeLog.exists()) {
                if (activeLog.length() > 0) {
                    val lastModified = activeLog.lastModified()
                    val isExpired = (System.currentTimeMillis() - lastModified) > 24 * 60 * 60 * 1000L
                    val isTooLarge = activeLog.length() > 5 * 1024 * 1024L // 5MB

                    if (!isExpired && !isTooLarge) {
                        Log.i(TAG, "Active log is fresh and small (${activeLog.length()} bytes). Appending instead of rotating.")
                        return
                    }
                } else {
                    // Empty active log, no need to rotate
                    return
                }
            } else {
                // Active log file doesn't exist, create it
                activeLog.createNewFile()
                return
            }

            Log.i(TAG, "Rotating active log: size = ${activeLog.length()} bytes, age expired = ${(System.currentTimeMillis() - activeLog.lastModified()) > 24 * 60 * 60 * 1000L}")

            // 1. Delete the oldest historical file if it exists
            val oldestFile = File(directory, "$LOG_FILE_NAME.5")
            if (oldestFile.exists()) {
                oldestFile.delete()
            }

            // 2. Shift historical files (4 downTo 1)
            for (i in 4 downTo 1) {
                val currentFile = File(directory, "$LOG_FILE_NAME.$i")
                if (currentFile.exists()) {
                    val nextFile = File(directory, "$LOG_FILE_NAME.${i + 1}")
                    currentFile.renameTo(nextFile)
                }
            }

            // 3. Move the active log of the finishing session to history .1
            val firstHistory = File(directory, "$LOG_FILE_NAME.1")
            activeLog.renameTo(firstHistory)

            // 4. Force establish a fresh clean empty log file for the new active session
            val newActive = File(directory, LOG_FILE_NAME)
            newActive.createNewFile()
            Log.i(TAG, "Logs rotated successfully. Active session starts fresh. 5 historical sessions retained.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate logs", e)
        }
    }
}
