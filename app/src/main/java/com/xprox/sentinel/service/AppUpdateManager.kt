package com.xprox.sentinel.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateManager {
    const val CURRENT_VERSION = "0.0.1"

    private val _hasUpdate = MutableStateFlow(false)
    val hasUpdate: StateFlow<Boolean> = _hasUpdate.asStateFlow()

    private val _latestVersion = MutableStateFlow(CURRENT_VERSION)
    val latestVersion: StateFlow<String> = _latestVersion.asStateFlow()

    private val _releaseNotes = MutableStateFlow("")
    val releaseNotes: StateFlow<String> = _releaseNotes.asStateFlow()

    private val _downloadUrl = MutableStateFlow("")
    val downloadUrl: StateFlow<String> = _downloadUrl.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _statusMessage = MutableStateFlow("Проверка обновлений...")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Automatically check for updates on startup
        checkForUpdates()
    }

    private fun parseVersion(v: String): Triple<Int, Int, Int> {
        val nums = Regex("\\d+").findAll(v).map { it.value.toIntOrNull() ?: 0 }.toList()
        return Triple(
            nums.getOrElse(0) { 0 },
            nums.getOrElse(1) { 0 },
            nums.getOrElse(2) { 0 }
        )
    }

    fun checkForUpdates(force: Boolean = false) {
        if (_isChecking.value) return
        _isChecking.value = true
        _statusMessage.value = "Проверка обновлений на GitHub..."

        scope.launch {
            try {
                val url = URL("https://api.github.com/repos/blackalex1/sentinel-mobile/releases/latest")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("User-Agent", "Sentinel-Android-Client/0.0.1")
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                }

                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val tagName = json.optString("tag_name", "v0.0.1")
                    val cleanLatest = tagName.trimStart('v', 'V')
                    val notes = json.optString("body", "")

                    var apkUrl = ""
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk", ignoreCase = true)) {
                                apkUrl = asset.optString("browser_download_url", "")
                                break
                            }
                        }
                    }

                    val currentParsed = parseVersion(CURRENT_VERSION)
                    val latestParsed = parseVersion(cleanLatest)

                    val isNewer = compareVersions(latestParsed, currentParsed) > 0

                    _latestVersion.value = cleanLatest
                    _releaseNotes.value = notes
                    _downloadUrl.value = apkUrl
                    _hasUpdate.value = isNewer
                    _statusMessage.value = if (isNewer) "Доступно обновление v$cleanLatest!" else "Установлена актуальная версия"
                } else {
                    _statusMessage.value = "Не удалось проверить обновления"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Ошибка проверки обновлений"
            } finally {
                _isChecking.value = false
            }
        }
    }

    private fun compareVersions(a: Triple<Int, Int, Int>, b: Triple<Int, Int, Int>): Int {
        if (a.first != b.first) return a.first.compareTo(b.first)
        if (a.second != b.second) return a.second.compareTo(b.second)
        return a.third.compareTo(b.third)
    }

    suspend fun downloadAndInstallApk(context: Context, onProgress: ((Float, String) -> Unit)? = null): Boolean {
        val urlString = _downloadUrl.value
        if (urlString.isBlank() || _isDownloading.value) return false

        _isDownloading.value = true
        _statusMessage.value = "Загрузка обновления..."

        return withContext(Dispatchers.IO) {
            try {
                val apkFile = File(context.cacheDir, "sentinel_update.apk")
                if (apkFile.exists()) apkFile.delete()

                val url = URL(urlString)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "Sentinel-Android-Client/0.0.1")
                    instanceFollowRedirects = true
                }

                val totalLength = conn.contentLength.takeIf { it > 0 } ?: (75 * 1024 * 1024)
                var downloaded = 0L

                conn.inputStream.use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(65536)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            val prog = (downloaded.toFloat() / totalLength.toFloat()).coerceIn(0f, 1f)
                            _downloadProgress.value = prog
                            val mbText = "${downloaded / (1024 * 1024)} MB"
                            onProgress?.invoke(prog, "Загрузка: ${(prog * 100).toInt()}% ($mbText)")
                        }
                    }
                }

                _statusMessage.value = "Запуск установщика..."
                onProgress?.invoke(1f, "Запуск установщика...")

                // Check install permission on Android 8+
                withContext(Dispatchers.Main) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (!context.packageManager.canRequestPackageInstalls()) {
                            val permIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(permIntent)
                            return@withContext
                        }
                    }

                    val apkUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )

                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(apkUri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(installIntent)
                }
                true
            } catch (e: Exception) {
                _statusMessage.value = "Ошибка при скачивании: ${e.message}"
                false
            } finally {
                _isDownloading.value = false
            }
        }
    }
}
