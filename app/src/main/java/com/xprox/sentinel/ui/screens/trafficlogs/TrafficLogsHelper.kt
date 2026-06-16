package com.xprox.sentinel.ui.screens.trafficlogs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.xprox.sentinel.log.LogManager
import com.xprox.sentinel.ui.screens.AppSelectorItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable) {
        if (drawable.bitmap != null) {
            return drawable.bitmap
        }
    }
    val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    } else {
        Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    }
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

suspend fun getSelectorApps(context: Context): List<AppSelectorItem> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val items = mutableListOf<AppSelectorItem>()

    // 1. Add standard system services from AppResolver
    val systemUids = mapOf(
        "android.system.kernel" to "Kernel / Root",
        "android.uid.system" to "Android System",
        "android.uid.phone" to "Telephony / Radio",
        "android.uid.media" to "Media Server",
        "android.uid.gps" to "GPS Daemon",
        "android.uid.nfc" to "NFC Service",
        "android.uid.dnsresolver" to "DNS Resolver",
        "android.uid.netd" to "Network Daemon (netd)",
        "android.uid.webview_zygote" to "WebView Zygote"
    )
    systemUids.forEach { (pkg, name) ->
        items.add(AppSelectorItem(name, pkg, null, isSystem = true))
    }

    // 2. Query Launcher Activities to capture all user-facing apps
    val appMap = mutableMapOf<String, AppSelectorItem>()
    try {
        val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)
        resolveInfos.forEach { resolveInfo ->
            val appInfo = resolveInfo.activityInfo.applicationInfo
            val label = resolveInfo.loadLabel(pm).toString()
            
            val icon = try {
                val drawable = appInfo.loadIcon(pm)
                val bitmap = drawableToBitmap(drawable)
                bitmap.asImageBitmap()
            } catch (e: Exception) {
                null
            }
            
            appMap[appInfo.packageName] = AppSelectorItem(label, appInfo.packageName, icon, isSystem = false)
        }
    } catch (e: Exception) {
        // Ignore
    }
    
    items.addAll(appMap.values.sortedBy { it.appName })
    items
}

fun exportAllLogs(
    context: Context,
    selectedAppPackage: String,
    filterSensitiveOnly: Boolean,
    selectedSessionIndex: Int,
    currentSessionLogs: List<String>
) {
    try {
        val activePorts = LogManager.loadActivePorts(context)
        val combinedText = StringBuilder()
        var hasAnyLogs = false

        // Iterate through Active session (0) and historical sessions (1..5)
        for (i in 0..5) {
            val sessionName = if (i == 0) "АКТИВНАЯ СЕССИЯ (LIVE)" else "ПРЕДЫДУЩАЯ СЕССИЯ $i"
            
            val rawLogs = if (selectedSessionIndex == i) {
                currentSessionLogs.reversed()
            } else {
                LogManager.readLogs(context, i)
            }
            
            // Apply current filters to this session's logs
            val filteredLines = rawLogs.filter { line ->
                val portMatch = Regex("""Port\s+(\d+)""").find(line)
                val port = portMatch?.groupValues?.get(1)?.toIntOrNull()
                val isEntrySensitive = port != null && activePorts.contains(port)
                
                val matchesSensitive = !filterSensitiveOnly || isEntrySensitive
                
                val match = Regex("""App:\s+([^\(]+)\s+\(([^)]+)\)""").find(line)
                val packageName = match?.groupValues?.get(2)?.trim() ?: "android.system.kernel"
                val matchesApp = selectedAppPackage == "all" || packageName == selectedAppPackage
                
                matchesSensitive && matchesApp
            }.map { line ->
                formatLogLineDynamically(line, activePorts)
            }

            if (filteredLines.isNotEmpty()) {
                hasAnyLogs = true
                combinedText.append("=========================================\n")
                combinedText.append("==   ${sessionName.padEnd(30)}  ==\n")
                combinedText.append("=========================================\n")
                filteredLines.forEach { line ->
                    combinedText.append(line).append("\n")
                }
                combinedText.append("\n\n")
            }
        }

        if (!hasAnyLogs) {
            Toast.makeText(context, "Нет записей для экспорта во всех сессиях", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Create temporary .txt file inside internal files directory (aligned with file_paths.xml)
        val file = java.io.File(context.filesDir, "x_prox_connection_logs.txt")
        file.writeText(combinedText.toString())

        // 2. Get secure FileProvider content URI (using the authority from AndroidManifest.xml)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.xprox.sentinel.fileprovider",
            file
        )

        // 3. Create ACTION_SEND Intent sharing the actual .txt file via EXTRA_STREAM
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Sentinel Connection Logs (All Sessions)")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = android.content.Intent.createChooser(intent, "Экспорт логов всех сессий в TXT")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка экспорта: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun exportXrayLogs(context: Context, xrayLogs: List<String>) {
    try {
        if (xrayLogs.isEmpty()) {
            Toast.makeText(context, "Нет записей для экспорта", Toast.LENGTH_SHORT).show()
            return
        }

        val combinedText = xrayLogs.reversed().joinToString("\n")

        val file = java.io.File(context.filesDir, "xray_core_logs.txt")
        file.writeText(combinedText)

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.xprox.sentinel.fileprovider",
            file
        )

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Xray Core Process Logs")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = android.content.Intent.createChooser(intent, "Экспорт логов Xray в TXT")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка экспорта: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
