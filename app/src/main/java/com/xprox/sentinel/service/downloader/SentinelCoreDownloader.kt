package com.xprox.sentinel.service.downloader

import android.content.Context
import android.os.Build
import android.util.Log
import com.xprox.sentinel.core.SentinelCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SentinelReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long,
    val abi: String // "arm64-v8a", "armeabi-v7a", "x86_64", "unknown"
)

data class SentinelReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String,
    val isPrerelease: Boolean,
    val publishedAt: String,
    val assets: List<SentinelReleaseAsset>
)

data class ActiveCoreInfo(
    val version: String,
    val isBundled: Boolean,
    val path: String?,
    val installDate: String?,
    val abi: String
)

object SentinelCoreDownloader {
    private const val TAG = "SentinelCoreDownloader"
    private const val GITHUB_RELEASES_URL = "https://api.github.com/repos/blackalex1/sentinel-core/releases"
    private const val PREFS_NAME = "sentinel_core_updater_prefs"
    private const val KEY_ALLOW_PRERELEASE = "allow_prerelease_releases"
    private const val KEY_CUSTOM_CORE_VERSION = "custom_core_version"
    private const val KEY_CUSTOM_CORE_DATE = "custom_core_install_date"
    private const val CORE_DIR_NAME = "core"
    private const val LIB_FILE_NAME = "libsentinel_core.so"

    /**
     * Returns true if pre-release / beta versions should be included.
     */
    fun getAllowPrerelease(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ALLOW_PRERELEASE, true) // Default to true during beta
    }

    fun setAllowPrerelease(context: Context, allow: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ALLOW_PRERELEASE, allow).apply()
    }

    /**
     * Resolves the primary CPU ABI of the current Android device.
     */
    fun getDeviceAbi(): String {
        val abis = try {
            Build.SUPPORTED_ABIS
        } catch (t: Throwable) {
            null
        } ?: emptyArray()

        for (abi in abis) {
            when {
                abi.contains("arm64", ignoreCase = true) -> return "arm64-v8a"
                abi.contains("armeabi", ignoreCase = true) || abi.contains("armv7", ignoreCase = true) -> return "armeabi-v7a"
                abi.contains("x86_64", ignoreCase = true) -> return "x86_64"
                abi.contains("x86", ignoreCase = true) -> return "x86"
            }
        }
        return "arm64-v8a"
    }

    /**
     * Returns the file handle for a custom downloaded core in private app storage.
     */
    fun getCustomCoreFile(context: Context): File {
        val dir = File(context.filesDir, CORE_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, LIB_FILE_NAME)
    }

    /**
     * Checks if a custom downloaded core is currently active.
     */
    fun isCustomCoreActive(context: Context): Boolean {
        val file = getCustomCoreFile(context)
        return file.exists() && file.length() > 0
    }

    /**
     * Retrieves information about the currently active core (bundled vs custom downloaded).
     */
    fun getActiveCoreInfo(context: Context): ActiveCoreInfo {
        val customFile = getCustomCoreFile(context)
        val isCustom = customFile.exists() && customFile.length() > 0
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val version = if (isCustom) {
            prefs.getString(KEY_CUSTOM_CORE_VERSION, "Custom Core") ?: "Custom Core"
        } else {
            "v1.0.0 (Bundled)"
        }

        val installDate = if (isCustom) {
            prefs.getString(KEY_CUSTOM_CORE_DATE, null)
        } else null

        return ActiveCoreInfo(
            version = version,
            isBundled = !isCustom,
            path = if (isCustom) customFile.absolutePath else "APK (jniLibs)",
            installDate = installDate,
            abi = getDeviceAbi()
        )
    }

    /**
     * Queries GitHub Releases API for all published releases of sentinel-core.
     */
    suspend fun fetchAvailableReleases(allowPrerelease: Boolean = true): List<SentinelReleaseInfo> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SentinelReleaseInfo>()
        var connection: HttpURLConnection? = null
        try {
            val url = URL(GITHUB_RELEASES_URL)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("User-Agent", "Sentinel-Core-Updater")
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val isDraft = obj.optBoolean("draft", false)
                    val isPrerelease = obj.optBoolean("prerelease", false)
                    val tagName = obj.optString("tag_name", "")
                    val name = obj.optString("name", tagName)
                    val body = obj.optString("body", "")
                    val publishedAt = obj.optString("published_at", "")

                    if (isDraft) continue
                    if (isPrerelease && !allowPrerelease) continue

                    val assetsJson = obj.optJSONArray("assets") ?: JSONArray()
                    val assets = mutableListOf<SentinelReleaseAsset>()

                    for (j in 0 until assetsJson.length()) {
                        val a = assetsJson.getJSONObject(j)
                        val aName = a.optString("name", "")
                        val downloadUrl = a.optString("browser_download_url", "")
                        val size = a.optLong("size", 0L)

                        val abi = when {
                            aName.contains("android-arm64", ignoreCase = true) || aName.contains("arm64-v8a", ignoreCase = true) -> "arm64-v8a"
                            aName.contains("android-arm.", ignoreCase = true) || aName.contains("android-armv7", ignoreCase = true) || aName.contains("armeabi-v7a", ignoreCase = true) -> "armeabi-v7a"
                            aName.contains("android-x86_64", ignoreCase = true) -> "x86_64"
                            else -> "unknown"
                        }

                        if (aName.endsWith(".so", ignoreCase = true) && abi != "unknown") {
                            assets.add(SentinelReleaseAsset(name = aName, downloadUrl = downloadUrl, size = size, abi = abi))
                        }
                    }

                    if (tagName.isNotEmpty()) {
                        list.add(
                            SentinelReleaseInfo(
                                tagName = tagName,
                                name = name,
                                body = body,
                                isPrerelease = isPrerelease,
                                publishedAt = publishedAt,
                                assets = assets
                            )
                        )
                    }
                }
            } else {
                Log.w(TAG, "GitHub API returned status: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query Sentinel-Core releases from GitHub", e)
        } finally {
            connection?.disconnect()
        }
        list
    }

    /**
     * Finds the exact downloadable .so asset in the release that matches the device ABI.
     */
    fun findMatchingAsset(release: SentinelReleaseInfo, targetAbi: String = getDeviceAbi()): SentinelReleaseAsset? {
        return release.assets.firstOrNull { it.abi == targetAbi }
            ?: release.assets.firstOrNull { it.name.contains(targetAbi, ignoreCase = true) }
            ?: release.assets.firstOrNull {
                targetAbi == "arm64-v8a" && it.name.contains("arm64", ignoreCase = true)
            }
    }

    /**
     * Downloads the matching .so library for the selected release, verifies it,
     * atomically updates the active core in storage, and reloads JNA.
     */
    suspend fun downloadAndInstall(
        context: Context,
        release: SentinelReleaseInfo,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val asset = findMatchingAsset(release)
        if (asset == null) {
            Log.e(TAG, "No matching Android .so asset found in release ${release.tagName} for ABI ${getDeviceAbi()}")
            return@withContext false
        }

        val targetFile = getCustomCoreFile(context)
        val tempFile = File(targetFile.parentFile, "${LIB_FILE_NAME}.tmp")

        try {
            Log.i(TAG, "Downloading Sentinel-Core ${release.tagName} (${asset.name}) from ${asset.downloadUrl}...")
            val success = downloadSingleFile(asset.downloadUrl, tempFile, onProgress)

            if (!success || !tempFile.exists() || tempFile.length() < 1000) {
                Log.e(TAG, "Download failed or corrupted file (${tempFile.length()} bytes)")
                tempFile.delete()
                return@withContext false
            }

            // Set file permissions (executable/readable for app)
            tempFile.setReadable(true, true)
            tempFile.setWritable(true, true)
            tempFile.setExecutable(true, true)

            // Atomically swap target file
            if (targetFile.exists()) {
                targetFile.delete()
            }
            tempFile.renameTo(targetFile)
            targetFile.setReadable(true, true)
            targetFile.setExecutable(true, true)

            // Update preferences
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date())
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_CUSTOM_CORE_VERSION, release.tagName)
                .putString(KEY_CUSTOM_CORE_DATE, dateStr)
                .apply()

            // Hot-reload SentinelCore JNA engine
            SentinelCore.reload(context)
            Log.i(TAG, "Sentinel-Core successfully updated to ${release.tagName} and reloaded!")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install Sentinel-Core update", e)
            try { tempFile.delete() } catch (ex: Exception) {}
            return@withContext false
        }
    }

    /**
     * Reverts to the bundled core from the APK and reloads JNA.
     */
    fun revertToBundled(context: Context): Boolean {
        val targetFile = getCustomCoreFile(context)
        if (targetFile.exists()) {
            targetFile.delete()
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_CUSTOM_CORE_VERSION)
            .remove(KEY_CUSTOM_CORE_DATE)
            .apply()

        SentinelCore.reload(context)
        Log.i(TAG, "Reverted to APK bundled Sentinel-Core")
        return true
    }

    private suspend fun downloadSingleFile(
        urlString: String,
        targetFile: File,
        onFileProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            var url = URL(urlString)
            var activeConnection = url.openConnection() as HttpURLConnection
            connection = activeConnection
            activeConnection.connectTimeout = 10000
            activeConnection.readTimeout = 15000
            activeConnection.instanceFollowRedirects = true

            var status = activeConnection.responseCode
            var redirectCount = 0
            while ((status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == 307 || status == 308) && redirectCount < 5) {
                val newUrl = activeConnection.getHeaderField("Location")
                url = URL(url, newUrl)
                activeConnection.disconnect()
                activeConnection = url.openConnection() as HttpURLConnection
                connection = activeConnection
                activeConnection.connectTimeout = 10000
                activeConnection.readTimeout = 15000
                activeConnection.instanceFollowRedirects = true
                status = activeConnection.responseCode
                redirectCount++
            }

            if (status != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP $status for $urlString")
                return@withContext false
            }

            val fileLength = activeConnection.contentLengthLong
            if (targetFile.exists()) targetFile.delete()

            BufferedInputStream(activeConnection.inputStream).use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
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
            return@withContext false
        } finally {
            connection?.disconnect()
        }
    }
}
