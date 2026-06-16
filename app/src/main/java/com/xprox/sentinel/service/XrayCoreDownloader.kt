package com.xprox.sentinel.service

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object XrayCoreDownloader {
    private const val TAG = "XrayCoreDownloader"
    private const val BINARY_NAME = "xray"
    
    private const val PREFS_NAME = "xprox_prefs"
    private const val KEY_INSTALLED_VERSION = "xray_installed_version"
    
    // Fallback baseline constant if API requests fail or internet is offline
    const val FALLBACK_XRAY_VERSION = "v26.3.27"

    /**
     * Retrieves the installed Xray core version from SharedPreferences.
     */
    fun getInstalledVersion(context: Context): String {
        if (!isInstalled(context)) return "Not Installed"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_INSTALLED_VERSION, FALLBACK_XRAY_VERSION) ?: FALLBACK_XRAY_VERSION
    }

    /**
     * Persists the successfully installed Xray core version to SharedPreferences.
     */
    fun setInstalledVersion(context: Context, version: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_INSTALLED_VERSION, version).apply()
    }

    private fun isInstalled(context: Context): Boolean {
        val binDir = File(context.filesDir, "bin")
        val xrayFile = File(binDir, BINARY_NAME)
        val geoip = File(binDir, "geoip.dat")
        val geosite = File(binDir, "geosite.dat")
        return xrayFile.exists() && xrayFile.canExecute() && geoip.exists() && geosite.exists()
    }

    /**
     * Detects device CPU architecture and returns the corresponding official GitHub download URL.
     */
    fun getDownloadUrl(version: String = FALLBACK_XRAY_VERSION): String {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        Log.d(TAG, "Detected CPU ABI: $abi")
        
        val arch = when {
            abi.contains("arm64") -> "arm64-v8a"
            abi.contains("armeabi") -> "armeabi-v7a"
            abi.contains("x86_64") -> "amd64"
            else -> "arm64-v8a"
        }
        
        val formattedVersion = if (version.startsWith("v")) version else "v$version"
        
        return "https://github.com/XTLS/Xray-core/releases/download/$formattedVersion/Xray-android-$arch.zip"
    }

    /**
     * Fetches the latest stable Xray-core version tag from GitHub Releases.
     */
    suspend fun fetchLatestVersion(): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/XTLS/Xray-core/releases/latest")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "sentinel-android-app")

            val responseCode = connection.responseCode
            Log.d(TAG, "GitHub API returned response code: $responseCode")
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(response)
                val tagName = json.getString("tag_name")
                Log.i(TAG, "Latest Xray-core release tag from GitHub: $tagName")
                return@withContext tagName
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch latest version from GitHub API", e)
        }
        return@withContext null
    }

    /**
     * Downloads and extracts the official Xray zip package asynchronously.
     */
    suspend fun downloadAndInstall(
        context: Context, 
        version: String = FALLBACK_XRAY_VERSION, 
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val downloadUrl = getDownloadUrl(version)
        val binDir = File(context.filesDir, "bin")
        if (!binDir.exists()) binDir.mkdir()
        
        val tempZipFile = File(binDir, "temp_xray.zip")
        
        // 1. Download ZIP to a temporary file first using redirect-following downloader
        val downloadSuccess = downloadSingleFile(downloadUrl, tempZipFile) { progress ->
            onProgress(progress * 0.8f) // Scale download to 80%
        }
        
        if (!downloadSuccess || !tempZipFile.exists()) {
            Log.e(TAG, "Failed to download Xray core ZIP package")
            try { tempZipFile.delete() } catch (e: Exception) {}
            return@withContext false
        }
        
        try {
            Log.i(TAG, "Extracting Xray core ZIP package...")
            ZipInputStream(BufferedInputStream(tempZipFile.inputStream())).use { zipStream ->
                var entry = zipStream.nextEntry
                while (entry != null) {
                    val entryName = entry.name.substringAfterLast('/')
                    if (entryName == BINARY_NAME || entryName == "geoip.dat" || entryName == "geosite.dat") {
                        val targetFile = File(binDir, entryName)
                        val tempTargetFile = File(binDir, "${entryName}.tmp")
                        if (tempTargetFile.exists()) {
                            tempTargetFile.delete()
                        }
                        
                        FileOutputStream(tempTargetFile).use { out ->
                            val buffer = ByteArray(4096)
                            var read: Int
                            while (zipStream.read(buffer).also { read = it } != -1) {
                                out.write(buffer, 0, read)
                            }
                        }
                        
                        // Atomically replace old file with new one
                        if (targetFile.exists()) {
                            targetFile.delete()
                        }
                        tempTargetFile.renameTo(targetFile)
                    }
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
            }
            
            // Delete temp ZIP file
            tempZipFile.delete()
            
            val xrayFile = File(binDir, BINARY_NAME)
            // Harden permissions: set file as executable only for the app process owner (chmod 700 equivalent)
            if (xrayFile.exists()) {
                xrayFile.setReadable(true, true)
                xrayFile.setWritable(true, true)
                xrayFile.setExecutable(true, true)
                
                // Harden permissions/readable for database assets
                val geoip = File(binDir, "geoip.dat")
                if (geoip.exists()) geoip.setReadable(true, true)
                val geosite = File(binDir, "geosite.dat")
                if (geosite.exists()) geosite.setReadable(true, true)
                
                // Persist the installed version
                setInstalledVersion(context, version)
                
                onProgress(1.0f) // Finalize progress
                Log.i(TAG, "Xray-core binary and resource databases successfully installed to ${binDir.absolutePath}")
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract Xray core", e)
        } finally {
            try { tempZipFile.delete() } catch (e: Exception) {}
        }
        return@withContext false
    }

    /**
     * Downloads the latest GeoIP and GeoSite database assets directly from their official community repositories on GitHub.
     * Sourced from:
     * - GeoIP: v2fly/geoip (https://github.com/v2fly/geoip/releases/latest/download/geoip.dat)
     * - GeoSite: v2fly/domain-list-community (https://github.com/v2fly/domain-list-community/releases/latest/download/dlc.dat)
     */
    suspend fun downloadDatabasesOnly(
        context: Context,
        version: String = FALLBACK_XRAY_VERSION, // Keep parameter for signature compatibility
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val binDir = File(context.filesDir, "bin")
        if (!binDir.exists()) binDir.mkdir()

        val geoIpUrl = "https://github.com/v2fly/geoip/releases/latest/download/geoip.dat"
        val geoSiteUrl = "https://github.com/v2fly/domain-list-community/releases/latest/download/dlc.dat"

        try {
            Log.i(TAG, "Downloading latest GeoIP database from $geoIpUrl")
            val geoIpFile = File(binDir, "geoip.dat")
            val successGeoIp = downloadSingleFile(geoIpUrl, geoIpFile) { p ->
                onProgress(p * 0.5f)
            }

            if (!successGeoIp) {
                Log.e(TAG, "Failed to download GeoIP database")
                return@withContext false
            }

            Log.i(TAG, "Downloading latest GeoSite database from $geoSiteUrl")
            val geoSiteFile = File(binDir, "geosite.dat")
            val successGeoSite = downloadSingleFile(geoSiteUrl, geoSiteFile) { p ->
                onProgress(0.5f + p * 0.5f)
            }

            if (!successGeoSite) {
                Log.e(TAG, "Failed to download GeoSite database")
                return@withContext false
            }

            if (geoIpFile.exists()) geoIpFile.setReadable(true, true)
            if (geoSiteFile.exists()) geoSiteFile.setReadable(true, true)

            Log.i(TAG, "Resource databases successfully updated from official community sources.")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download and install community databases", e)
        }
        return@withContext false
    }

    private suspend fun downloadSingleFile(
        urlString: String,
        targetFile: File,
        onFileProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        var connection: java.net.HttpURLConnection? = null
        try {
            var url = URL(urlString)
            var activeConnection = url.openConnection() as java.net.HttpURLConnection
            connection = activeConnection
            activeConnection.connectTimeout = 10000
            activeConnection.readTimeout = 15000
            activeConnection.instanceFollowRedirects = true
            
            // Handle HTTP redirects manually
            var status = activeConnection.responseCode
            var redirectCount = 0
            while ((status == java.net.HttpURLConnection.HTTP_MOVED_TEMP || 
                    status == java.net.HttpURLConnection.HTTP_MOVED_PERM || 
                    status == 307 || status == 308) && redirectCount < 5) {
                val newUrl = activeConnection.getHeaderField("Location")
                Log.d(TAG, "Redirected to: $newUrl")
                url = URL(url, newUrl)
                activeConnection.disconnect()
                activeConnection = url.openConnection() as java.net.HttpURLConnection
                connection = activeConnection
                activeConnection.connectTimeout = 10000
                activeConnection.readTimeout = 15000
                activeConnection.instanceFollowRedirects = true
                status = activeConnection.responseCode
                redirectCount++
            }

            if (status != java.net.HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP status $status for URL: $urlString")
                return@withContext false
            }

            val fileLength = activeConnection.contentLength
            if (targetFile.exists()) {
                targetFile.delete()
            }
            
            BufferedInputStream(activeConnection.inputStream).use { inputStream ->
                FileOutputStream(targetFile).use { out ->
                    val buffer = ByteArray(4096)
                    var read: Int
                    var totalRead = 0L
                    
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                        totalRead += read
                        
                        val progress = if (fileLength > 0) {
                            minOf(1.0f, totalRead.toFloat() / fileLength.toFloat())
                        } else {
                            0f
                        }
                        onFileProgress(progress)
                    }
                }
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading file from $urlString", e)
        } finally {
            connection?.disconnect()
        }
        return@withContext false
    }
}
