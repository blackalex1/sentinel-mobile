package com.xprox.sentinel.service

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.xprox.sentinel.log.LogManager
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*

object ThreatForensics {
    private const val TAG = "ThreatForensics"

    /**
     * Gathers extremely thorough, system-wide hardware, OS, and status metadata for security forensics.
     */
    fun getDeviceMetadata(context: Context): JSONObject {
        val json = JSONObject()
        try {
            // Hardware Specs
            json.put("manufacturer", Build.MANUFACTURER)
            json.put("brand", Build.BRAND)
            json.put("model", Build.MODEL)
            json.put("device", Build.DEVICE)
            json.put("board", Build.BOARD)
            json.put("hardware", Build.HARDWARE)
            json.put("supportedAbis", org.json.JSONArray(Build.SUPPORTED_ABIS.toList()))

            // OS details
            json.put("androidVersion", Build.VERSION.RELEASE)
            json.put("sdkInt", Build.VERSION.SDK_INT)
            json.put("buildId", Build.ID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                json.put("securityPatch", Build.VERSION.SECURITY_PATCH)
            }
            json.put("fingerprint", Build.FINGERPRINT)
            json.put("bootloader", Build.BOOTLOADER)

            // System Specs (RAM)
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            json.put("totalRamGb", String.format(Locale.US, "%.2f", memInfo.totalMem.toDouble() / (1024 * 1024 * 1024)))
            json.put("availRamGb", String.format(Locale.US, "%.2f", memInfo.availMem.toDouble() / (1024 * 1024 * 1024)))

            // Storage Specs
            val path = android.os.Environment.getDataDirectory()
            val stat = android.os.StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availBlocks = stat.availableBlocksLong
            json.put("totalStorageGb", String.format(Locale.US, "%.2f", (totalBlocks * blockSize).toDouble() / (1024 * 1024 * 1024)))
            json.put("availStorageGb", String.format(Locale.US, "%.2f", (availBlocks * blockSize).toDouble() / (1024 * 1024 * 1024)))

            // Root status check
            var isRooted = false
            val paths = arrayOf(
                "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
                "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
                "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su"
            )
            for (p in paths) {
                if (java.io.File(p).exists()) {
                    isRooted = true
                    break
                }
            }
            if (Build.TAGS != null && Build.TAGS.contains("test-keys")) {
                isRooted = true
            }
            json.put("rootDetected", isRooted)

            // General specs
            json.put("locale", Locale.getDefault().toString())
            json.put("timezone", TimeZone.getDefault().id)
            json.put("uptimeHrs", String.format(Locale.US, "%.2f", android.os.SystemClock.elapsedRealtime().toDouble() / (1000 * 60 * 60)))

        } catch (e: Exception) {
            Log.e(TAG, "Failed to compile device metadata", e)
        }
        return json
    }

    /**
     * Gathers active process runtime specifications (PID, importance, memory usage) for threat forensics.
     */
    fun getTriggeringProcessMetadata(context: Context, packageName: String): JSONObject {
        val json = JSONObject()
        try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            val runningProcesses = actManager?.runningAppProcesses
            if (!runningProcesses.isNullOrEmpty()) {
                for (info in runningProcesses) {
                    if (info.pkgList != null && info.pkgList.contains(packageName)) {
                        json.put("pid", info.pid)
                        json.put("processName", info.processName)
                        json.put("importance", info.importance)
                        
                        val importanceStr = when (info.importance) {
                            android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "FOREGROUND (АКТИВНОЕ НА ЭКРАНЕ)"
                            android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE -> "FOREGROUND_SERVICE (АКТИВНАЯ СЛУЖБА)"
                            android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE -> "PERCEPTIBLE (ФОНОВОЕ ЗАМЕТНОЕ)"
                            android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "VISIBLE (АКТИВНОЕ ВИДИМОЕ)"
                            android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "BACKGROUND_SERVICE (ФОНОВАЯ СЛУЖБА)"
                            android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND -> "BACKGROUND (ФОНОВОЕ)"
                            else -> "OTHER (${info.importance})"
                        }
                        json.put("importanceString", importanceStr)

                        val memoryInfoArray = actManager.getProcessMemoryInfo(intArrayOf(info.pid))
                        if (!memoryInfoArray.isNullOrEmpty()) {
                            val mem = memoryInfoArray[0]
                            json.put("processTotalPssKb", mem.totalPss)
                            json.put("processTotalPrivateDirtyKb", mem.totalPrivateDirty)
                        }
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compile process metadata", e)
        }
        return json
    }

    fun isPermissionDangerous(permission: String): Boolean {
        val dangerous = setOf(
            "android.permission.SEND_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_WAP_PUSH",
            "android.permission.RECEIVE_MMS",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.GET_ACCOUNTS",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.RECORD_AUDIO",
            "android.permission.CAMERA",
            "android.permission.READ_PHONE_STATE",
            "android.permission.READ_PHONE_NUMBERS",
            "android.permission.CALL_PHONE",
            "android.permission.ANSWER_PHONE_CALLS",
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.WRITE_SETTINGS",
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            "android.permission.REQUEST_INSTALL_PACKAGES",
            "android.permission.QUERY_ALL_PACKAGES"
        )
        return dangerous.contains(permission)
    }

    /**
     * Generates a thorough, structured JSON & text report containing rich package metadata.
     */
    fun generateForensicReport(
        context: Context,
        packageName: String,
        appName: String,
        destinationIp: String,
        port: Int,
        attempts: List<ConnectionRecord>,
        isSystemBypassed: Boolean = false
    ) {
        try {
            val directory = File(context.filesDir, "threats")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val pm = context.packageManager
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val timestampStr = dateFormat.format(Date())

            var version = "Unknown"
            var installTime = "Unknown"
            var apkPath = "Unknown"
            var installer = "Unknown"
            var systemUid = "Unknown"
            var targetSdk = "Unknown"
            var minSdk = "Unknown"
            var isSystemApp = "No"
            var isDebuggableApp = "No"
            
            // Extended metadata
            var lastUpdateTime = "Unknown"
            var dataDir = "Unknown"
            var nativeLibDir = "Unknown"
            var appClassName = "None"
            var sharedUserId = "None"
            var isAppEnabled = "Yes"
            var appCategoryName = "Unknown"
            val appFlagsSummary = mutableListOf<String>()

            // Permissions statuses
            val permissionsList = mutableListOf<JSONObject>()
            val permissionsTextList = mutableListOf<String>()
            var grantedCount = 0
            var deniedCount = 0

            // Components counts
            var activitiesCount = 0
            var servicesCount = 0
            var receiversCount = 0
            var providersCount = 0
            val componentsJson = JSONObject()
            
            // X.509 Certificate details
            val certDetailsJson = JSONObject()
            var certDetailsText = "Сведения о сертификате отсутствуют"

            try {
                // Request comprehensive flags
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    PackageManager.GET_PERMISSIONS or 
                    PackageManager.GET_SIGNING_CERTIFICATES or
                    PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_RECEIVERS or
                    PackageManager.GET_PROVIDERS
                } else {
                    @Suppress("DEPRECATION")
                    PackageManager.GET_PERMISSIONS or 
                    PackageManager.GET_SIGNATURES or
                    PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_RECEIVERS or
                    PackageManager.GET_PROVIDERS
                }
                
                val packageInfo = pm.getPackageInfo(packageName, flags)
                
                version = "${packageInfo.versionName} (${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode})"
                installTime = dateFormat.format(Date(packageInfo.firstInstallTime))
                lastUpdateTime = dateFormat.format(Date(packageInfo.lastUpdateTime))
                sharedUserId = packageInfo.sharedUserId ?: "None"
                
                val appInfo = packageInfo.applicationInfo ?: pm.getApplicationInfo(packageName, 0)
                apkPath = appInfo.sourceDir ?: "Unknown APK Path"
                systemUid = appInfo.uid.toString()
                
                targetSdk = appInfo.targetSdkVersion.toString()
                minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) appInfo.minSdkVersion.toString() else "Unknown"
                
                isSystemApp = if ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) "Yes" else "No"
                isDebuggableApp = if ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) "Yes" else "No"
                isAppEnabled = if (appInfo.enabled) "Yes" else "No"
                dataDir = appInfo.dataDir ?: "Unknown"
                nativeLibDir = appInfo.nativeLibraryDir ?: "Unknown"
                appClassName = appInfo.className ?: "None"
                
                // Retrieve app category (Android 8.0+ / API 26+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appCategoryName = when (appInfo.category) {
                        android.content.pm.ApplicationInfo.CATEGORY_GAME -> "GAME"
                        android.content.pm.ApplicationInfo.CATEGORY_AUDIO -> "AUDIO"
                        android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> "VIDEO"
                        android.content.pm.ApplicationInfo.CATEGORY_IMAGE -> "IMAGE"
                        android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> "SOCIAL"
                        android.content.pm.ApplicationInfo.CATEGORY_NEWS -> "NEWS"
                        android.content.pm.ApplicationInfo.CATEGORY_MAPS -> "MAPS"
                        android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> "PRODUCTIVITY"
                        else -> "UNDEFINED"
                    }
                }
                
                // Extract flags
                if ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0) appFlagsSummary.add("EXTERNAL_STORAGE")
                if ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_STOPPED) != 0) appFlagsSummary.add("STOPPED")
                if ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP) != 0) appFlagsSummary.add("LARGE_HEAP")
                if ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_HAS_CODE) != 0) appFlagsSummary.add("HAS_CODE")
                
                // Extract and check permissions
                val reqPerms = packageInfo.requestedPermissions
                if (!reqPerms.isNullOrEmpty()) {
                    reqPerms.forEach { perm ->
                        val isGranted = pm.checkPermission(perm, packageName) == PackageManager.PERMISSION_GRANTED
                        if (isGranted) grantedCount++ else deniedCount++
                        
                        val isDangerous = isPermissionDangerous(perm)
                        val shortName = perm.substringAfterLast(".")
                        
                        val permJson = JSONObject().apply {
                            put("permission", perm)
                            put("shortName", shortName)
                            put("granted", isGranted)
                            put("isDangerous", isDangerous)
                        }
                        permissionsList.add(permJson)
                        
                        val statusSymbol = if (isGranted) {
                            if (isDangerous) "🚨 [GRANTED]" else "✅ [GRANTED]"
                        } else {
                            if (isDangerous) "⚠️ [DENIED ]" else "❌ [DENIED ]"
                        }
                        permissionsTextList.add("  $statusSymbol $perm")
                    }
                }

                // Components Counting
                activitiesCount = packageInfo.activities?.size ?: 0
                servicesCount = packageInfo.services?.size ?: 0
                receiversCount = packageInfo.receivers?.size ?: 0
                providersCount = packageInfo.providers?.size ?: 0
                
                componentsJson.put("activitiesCount", activitiesCount)
                componentsJson.put("servicesCount", servicesCount)
                componentsJson.put("receiversCount", receiversCount)
                componentsJson.put("providersCount", providersCount)
                
                val activitiesList = packageInfo.activities?.map { it.name } ?: emptyList()
                val servicesList = packageInfo.services?.map { it.name } ?: emptyList()
                val receiversList = packageInfo.receivers?.map { it.name } ?: emptyList()
                val providersList = packageInfo.providers?.map { it.name } ?: emptyList()
                
                componentsJson.put("activities", org.json.JSONArray(activitiesList))
                componentsJson.put("services", org.json.JSONArray(servicesList))
                componentsJson.put("receivers", org.json.JSONArray(receiversList))
                componentsJson.put("providers", org.json.JSONArray(providersList))

                // Extract signatures & Parse X.509 Certificates
                val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.signingInfo?.let { signingInfo ->
                        if (signingInfo.hasMultipleSigners()) {
                            signingInfo.apkContentsSigners
                        } else {
                            signingInfo.signingCertificateHistory
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.signatures
                }
                
                if (!signatures.isNullOrEmpty()) {
                    val certFactory = CertificateFactory.getInstance("X.509")
                    val sha256List = mutableListOf<String>()
                    val certInfosJson = org.json.JSONArray()
                    val certTextBuilder = java.lang.StringBuilder()

                    signatures.forEachIndexed { idx, sig ->
                        val md = java.security.MessageDigest.getInstance("SHA-256")
                        val digest = md.digest(sig.toByteArray())
                        val sha256Hex = digest.joinToString(":") { String.format("%02X", it) }
                        sha256List.add(sha256Hex)

                        try {
                            val certBytes = sig.toByteArray()
                            val cert = certFactory.generateCertificate(ByteArrayInputStream(certBytes)) as? X509Certificate
                            if (cert != null) {
                                val subject = cert.subjectDN.toString()
                                val issuer = cert.issuerDN.toString()
                                val serial = cert.serialNumber.toString()
                                val validFromStr = dateFormat.format(cert.notBefore)
                                val validToStr = dateFormat.format(cert.notAfter)
                                val sigAlgo = cert.sigAlgName
                                val certVersion = cert.version
                                
                                val singleCertJson = JSONObject().apply {
                                    put("index", idx)
                                    put("subject", subject)
                                    put("issuer", issuer)
                                    put("serialNumber", serial)
                                    put("validFrom", validFromStr)
                                    put("validTo", validToStr)
                                    put("signatureAlgorithm", sigAlgo)
                                    put("version", certVersion)
                                    put("sha256", sha256Hex)
                                }
                                certInfosJson.put(singleCertJson)
                                
                                certTextBuilder.append("  Сертификат #${idx + 1}:\n")
                                certTextBuilder.append("    Версия         : v$certVersion\n")
                                certTextBuilder.append("    Субъект (DN)   : $subject\n")
                                certTextBuilder.append("    Издатель (DN)  : $issuer\n")
                                certTextBuilder.append("    Серийный Номер : $serial\n")
                                certTextBuilder.append("    Алгоритм       : $sigAlgo\n")
                                certTextBuilder.append("    Срок Действия  : С $validFromStr по $validToStr\n")
                                certTextBuilder.append("    SHA-256 Finger : $sha256Hex\n")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse individual certificate bytes", e)
                            certTextBuilder.append("  Сертификат #${idx + 1}: Ошибка декодирования байт подписи (SHA-256: $sha256Hex)\n")
                        }
                    }
                    
                    certDetailsJson.put("certificates", certInfosJson)
                    certDetailsJson.put("sha256List", org.json.JSONArray(sha256List))
                    certDetailsText = certTextBuilder.toString().trimEnd()
                }

                val installerPackage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    pm.getInstallSourceInfo(packageName).installingPackageName
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstallerPackageName(packageName)
                }
                installer = installerPackage ?: "Sideloaded / Unknown Installer"
            } catch (e: Exception) {
                Log.e(TAG, "Failed to extract detailed package info", e)
            }

            // Fetch all connection logs across all active and historical rotated sessions for this specific app package to provide complete statistics
            val allSessionLogs = mutableListOf<String>()
            for (i in 5 downTo 0) {
                try {
                    val logs = LogManager.readLogs(context, i)
                    allSessionLogs.addAll(logs)
                } catch (e: Exception) {}
            }
            val appSessionHistory = allSessionLogs.filter { it.contains("($packageName)") }
            val appHistoryBuilder = java.lang.StringBuilder()
            if (appSessionHistory.isNotEmpty()) {
                appSessionHistory.forEach { appHistoryBuilder.append(it).append("\n") }
            } else {
                appHistoryBuilder.append("Записи сетевой активности в истории сессий отсутствуют.\n")
            }

            val threshold = ThreatDetectionManager.THRESHOLD
            // 1. JSON Forensic Report
            val jsonReport = JSONObject().apply {
                put("forensicTimestamp", timestampStr)
                put("appName", appName)
                put("packageName", packageName)
                put("systemUid", systemUid)
                put("apkPath", apkPath)
                put("version", version)
                put("installTime", installTime)
                put("lastUpdateTime", lastUpdateTime)
                put("installSource", installer)
                put("targetSdk", targetSdk)
                put("minSdk", minSdk)
                put("isSystemApp", isSystemApp == "Yes")
                put("isDebuggable", isDebuggableApp == "Yes")
                put("isAppEnabled", isAppEnabled == "Yes")
                put("sharedUserId", sharedUserId)
                put("appCategory", appCategoryName)
                put("appFlags", org.json.JSONArray(appFlagsSummary))
                put("dataDirectory", dataDir)
                put("nativeLibraryDirectory", nativeLibDir)
                put("applicationClassName", appClassName)
                put("certificatesDetails", certDetailsJson)
                put("permissionsDetails", org.json.JSONArray(permissionsList))
                put("componentsDetails", componentsJson)
                put("deviceMetadata", getDeviceMetadata(context))
                put("triggerProcessMetadata", getTriggeringProcessMetadata(context, packageName))
                if (isSystemBypassed) {
                    put("triggerReason", "Exceeded $threshold connections per minute limit on audited ports. SYSTEM BYPASS ACTIVE.")
                    put("status", "SYSTEM_BYPASS")
                } else {
                    put("triggerReason", "Exceeded $threshold connections per minute limit on audited ports")
                    put("status", "ISOLATED")
                }
                put("triggerPort", port)
                put("triggerIP", destinationIp)
                put("sessionConnectionHistory", org.json.JSONArray(appSessionHistory))
                put("triggerConnectionHistory", attempts.map { "${dateFormat.format(Date(it.timestamp))} -> ${it.destinationIp}:${it.port}" })
            }

            val jsonFile = File(directory, "report_${packageName}.json")
            jsonFile.writeText(jsonReport.toString(4), Charsets.UTF_8)

            // Format pre-trigger attempts
            val historyBuilder = java.lang.StringBuilder()
            attempts.forEachIndexed { index, record ->
                val timeStr = dateFormat.format(Date(record.timestamp))
                val isTriggerAttempt = (index == attempts.size - 1)
                val packetDetails = "[${record.protocol} (Flags: ${record.tcpFlags}, Seq: ${record.tcpSeq}, Ack: ${record.tcpAck}, Win: ${record.tcpWindow}) Size: ${record.ipLength} bytes, TTL: ${record.ttl}, IPFlags: ${record.ipFlags}]"
                
                if (isTriggerAttempt) {
                    if (isSystemBypassed) {
                        historyBuilder.append("[$timeStr] [WARNING: SUSPICIOUS SYSTEM ACTIVITY (Port ${record.port})] App: $appName ($packageName) -> Dest: ${record.destinationIp}:${record.port} $packetDetails (LIMIT EXCEEDED. SYSTEM BYPASS ACTIVE - NO ISOLATION APPLIED)\n")
                    } else {
                        historyBuilder.append("[$timeStr] [ALERT: THREAT TRIGGERED (Port ${record.port})] App: $appName ($packageName) -> Dest: ${record.destinationIp}:${record.port} $packetDetails (LIMIT EXCEEDED. TOTAL NETWORK BLACKHOLE APPLIED)\n")
                    }
                } else {
                    historyBuilder.append("[$timeStr] [AUDITED CONNECTION (Port ${record.port})] App: $appName ($packageName) -> Dest: ${record.destinationIp}:${record.port} $packetDetails\n")
                }
            }

            val statusStr = if (isSystemBypassed) {
                "ПРОПУЩЕНО (СИСТЕМНЫЙ ПРОЦЕСС ОБХОДА)"
            } else {
                "ТОТАЛЬНЫЙ БЛЕКХОЛИНГ СЕТИ (АКТИВЕН)"
            }
            
            val warningStr = if (isSystemBypassed) {
                "\n------------------------------------------------------------------\n⚠️ ВНИМАНИЕ: Данное приложение является системно-важным компонентом ОС.\nВ целях предотвращения сбоя работы Android сетевая изоляция была пропущена.\nРекомендуется проверить приложение на предмет компрометации (вируса/трояна).\n------------------------------------------------------------------"
            } else ""

            // 2. Human-Readable Text Report
            val textReport = """
                ==================================================================
                Sentinel Secure Shield - Сетевая Форензика и Анализ Угроз
                ==================================================================
                Дата Обнаружения: $timestampStr
                Имя Приложения  : $appName
                Имя Пакета      : $packageName
                Системный UID   : $systemUid
                Версия          : $version
                Дата Установки  : $installTime
                Обновлено       : $lastUpdateTime
                Источник        : $installer
                Путь к APK      : $apkPath
                Target / Min SDK: Target SDK $targetSdk / Min SDK $minSdk
                Класс Application: $appClassName
                Общий UID (Shared): $sharedUserId
                Активно (Enabled): $isAppEnabled
                Категория Пакета : $appCategoryName
                Флаги Пакета    : ${if (appFlagsSummary.isNotEmpty()) appFlagsSummary.joinToString(", ") else "НЕТ"}
                Путь к Данным   : $dataDir
                Нативные Библио : $nativeLibDir
                
                ==================================================================
                СЕРТИФИКАТ ПОДПИСИ (X.509 Certificate):
                $certDetailsText
                
                ==================================================================
                СТАТУС КРИТИЧЕСКИХ РАЗРЕШЕНИЙ (PERMISSION AUDIT):
                ${if (permissionsTextList.isNotEmpty()) permissionsTextList.joinToString("\n") else "  НЕТ ЗАДЕКЛАРИРОВАННЫХ РАЗРЕШЕНИЙ"}
                * Всего разрешений: ${permissionsList.size} (Предоставлено: $grantedCount, Отклонено: $deniedCount)
                
                ==================================================================
                ДЕКЛАРИРОВАННЫЕ КОМПОНЕНТЫ ПАКЕТА (MANIFEST COMPONENTS):
                  - Экранные Формы (Activities): $activitiesCount
                  - Фоновые Службы (Services)   : $servicesCount
                  - Приемники Событий (Receivers): $receiversCount
                  - Провайдеры Данных (Providers): $providersCount
                ------------------------------------------------------------------
                ПРИЧИНА ИЗОЛЯЦИИ: Превышен лимит запросов в минуту ($threshold за 60с)
                Первичный триггер: Соединение на Порт $port ($destinationIp:$port)
                Статус Изоляции  : $statusStr$warningStr
                ==================================================================
                ИСТОРИЯ СЕТЕВОЙ АКТИВНОСТИ В ТЕКУЩЕЙ И ПРЕДЫДУЩИХ СЕССИЯХ:
                
                ${appHistoryBuilder.toString()}
                ==================================================================
                ХРОНОЛОГИЯ СРАБАТЫВАНИЯ ТРИГГЕРА (ПОСЛЕДНИЕ 60С):
                
                ${historyBuilder.toString()}
            """.trimIndent()

            val textFile = File(directory, "report_${packageName}.txt")
            textFile.writeText(textReport, Charsets.UTF_8)
            
            Log.d(TAG, "Successfully generated forensic files for threat app: $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compile forensic reports", e)
        }
    }

    fun logBlockedTraffic(
        context: Context,
        packageName: String,
        appName: String,
        destinationIp: String,
        port: Int,
        isTrigger: Boolean = false,
        protocol: String = "TCP",
        ipLength: Int = 0,
        ttl: Int = 0,
        ipFlags: String = "N/A",
        tcpFlags: String = "N/A",
        tcpSeq: Long = 0L,
        tcpAck: Long = 0L,
        tcpWindow: Int = 0
    ) {
        try {
            val directory = File(context.filesDir, "threats")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val logFile = File(directory, "threat_${packageName}.log")
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            
            val packetDetails = "[$protocol (Flags: $tcpFlags, Seq: $tcpSeq, Ack: $tcpAck, Win: $tcpWindow) Size: $ipLength bytes, TTL: $ttl, IPFlags: $ipFlags]"
            
            val logLine = if (isTrigger) {
                "[$timestamp] [ALERT: THREAT TRIGGERED (Port $port)] App: $appName ($packageName) -> Dest: $destinationIp:$port $packetDetails (LIMIT EXCEEDED. TOTAL NETWORK BLACKHOLE APPLIED)\n"
            } else {
                "[$timestamp] [BLOCKED NETWORK PACKET (Port $port)] App: $appName ($packageName) -> Dest: $destinationIp:$port $packetDetails (ACCESS DENIED)\n"
            }

            FileOutputStream(logFile, true).use { stream ->
                stream.write(logLine.toByteArray(Charsets.UTF_8))
            }
            
            // Also append to the user-readable forensic text file if it's not the trigger (since trigger is already in the main report body)
            if (!isTrigger) {
                writeToForensicReport(context, packageName, logLine)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write isolated threat log", e)
        }
    }

    fun writeToForensicReport(context: Context, packageName: String, logLine: String) {
        try {
            val directory = File(context.filesDir, "threats")
            val textFile = File(directory, "report_${packageName}.txt")
            if (textFile.exists()) {
                FileOutputStream(textFile, true).use { stream ->
                    stream.write(logLine.toByteArray(Charsets.UTF_8))
                }
            }
        } catch (e: Exception) {}
    }

    fun deleteThreatReport(context: Context, packageName: String) {
        try {
            val directory = File(context.filesDir, "threats")
            File(directory, "threat_${packageName}.log").delete()
            File(directory, "report_${packageName}.json").delete()
            File(directory, "report_${packageName}.txt").delete()
            File(directory, "report_${packageName}.pcap").delete()
        } catch (e: Exception) {}
    }

    /**
     * Reads all logged blocked traffic for a given package name.
     */
    fun readThreatLogs(context: Context, packageName: String): List<String> {
        return try {
            val file = File(File(context.filesDir, "threats"), "threat_${packageName}.log")
            if (file.exists()) file.readLines(Charsets.UTF_8) else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Returns the human-readable text forensic report path if it exists.
     */
    fun getForensicReportFile(context: Context, packageName: String): File? {
        val file = File(File(context.filesDir, "threats"), "report_${packageName}.txt")
        return if (file.exists()) file else null
    }

    /**
     * Returns the binary PCAP report path if it exists.
     */
    fun getPcapReportFile(context: Context, packageName: String): File? {
        val file = File(File(context.filesDir, "threats"), "report_${packageName}.pcap")
        return if (file.exists()) file else null
    }
}
