package com.xprox.sentinel.service.threats

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*

object ThreatLogBuffer {

    private const val TAG = "ThreatLogBuffer"

    fun getDeviceMetadata(context: Context): JSONObject {
        val json = JSONObject()
        try {
            json.put("manufacturer", Build.MANUFACTURER)
            json.put("brand", Build.BRAND)
            json.put("model", Build.MODEL)
            json.put("device", Build.DEVICE)
            json.put("board", Build.BOARD)
            json.put("hardware", Build.HARDWARE)
            json.put("supportedAbis", org.json.JSONArray(Build.SUPPORTED_ABIS.toList()))
            json.put("androidVersion", Build.VERSION.RELEASE)
            json.put("sdkInt", Build.VERSION.SDK_INT)
            json.put("buildId", Build.ID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                json.put("securityPatch", Build.VERSION.SECURITY_PATCH)
            }
            json.put("fingerprint", Build.FINGERPRINT)
            json.put("bootloader", Build.BOOTLOADER)

            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            json.put("totalRamGb", String.format(Locale.US, "%.2f", memInfo.totalMem.toDouble() / (1024 * 1024 * 1024)))
            json.put("availRamGb", String.format(Locale.US, "%.2f", memInfo.availMem.toDouble() / (1024 * 1024 * 1024)))

            val path = android.os.Environment.getDataDirectory()
            val stat = android.os.StatFs(path.path)
            json.put("totalStorageGb", String.format(Locale.US, "%.2f", (stat.blockCountLong * stat.blockSizeLong).toDouble() / (1024 * 1024 * 1024)))
            json.put("availStorageGb", String.format(Locale.US, "%.2f", (stat.availableBlocksLong * stat.blockSizeLong).toDouble() / (1024 * 1024 * 1024)))

            var isRooted = false
            val paths = arrayOf(
                "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
                "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su"
            )
            for (p in paths) {
                if (File(p).exists()) {
                    isRooted = true
                    break
                }
            }
            if (Build.TAGS != null && Build.TAGS.contains("test-keys")) {
                isRooted = true
            }
            json.put("rootDetected", isRooted)

            json.put("locale", Locale.getDefault().toString())
            json.put("timezone", TimeZone.getDefault().id)
            json.put("uptimeHrs", String.format(Locale.US, "%.2f", android.os.SystemClock.elapsedRealtime().toDouble() / (1000 * 60 * 60)))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compile device metadata", e)
        }
        return json
    }

    fun getPackageSignature(context: Context, packageName: String): JSONObject {
        val json = JSONObject()
        try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            val pkgInfo = pm.getPackageInfo(packageName, flags)
            json.put("packageName", packageName)

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.signatures
            }

            if (!signatures.isNullOrEmpty()) {
                val certFactory = CertificateFactory.getInstance("X.509")
                val cert = certFactory.generateCertificate(ByteArrayInputStream(signatures[0].toByteArray())) as X509Certificate
                json.put("subjectDN", cert.subjectDN.name)
                json.put("issuerDN", cert.issuerDN.name)
                json.put("serialNumber", cert.serialNumber.toString(16))
                json.put("sigAlgName", cert.sigAlgName)
                
                val md = java.security.MessageDigest.getInstance("SHA-256")
                val digest = md.digest(signatures[0].toByteArray())
                val hexString = digest.joinToString("") { "%02X".format(it) }
                json.put("sha256Fingerprint", hexString)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inspect package signature for $packageName", e)
        }
        return json
    }
}
