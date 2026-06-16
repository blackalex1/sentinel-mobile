package com.xprox.sentinel.config.parser

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.Locale

object HwidGenerator {
    private val SALT_ARR = intArrayOf(35, 21, 77, 36, 64, 11, 63, 94, 8, 70)
    private val SA = intArrayOf(74, 123, 46, 93, 31, 99, 72, 55, 108, 25, 85, 58, 113, 14, 66, 105)

    fun getHwid(context: Context): String {
        val parts = mutableListOf<String>()
        try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (!androidId.isNullOrEmpty() && androidId != "9774d56d682e549c") {
                parts.add(androidId)
            }
        } catch (e: Exception) {}
        
        parts.add(Build.MANUFACTURER ?: "")
        parts.add(Build.MODEL ?: "")
        parts.add(Build.BRAND ?: "")
        parts.add(Build.DEVICE ?: "")
        parts.add(Build.PRODUCT ?: "")
        parts.add(Build.BOARD ?: "")
        parts.add(Build.HARDWARE ?: "")

        val deviceString = parts.joinToString("|")
        
        val string2 = sha256(deviceString).lowercase(Locale.ROOT)
        
        val saltBuilder = StringBuilder()
        for (i in 0 until 10) {
            saltBuilder.append((SALT_ARR[i] xor SA[i % 16]).toChar())
        }
        val concat = saltBuilder.toString() + string2
        
        val finalHash = sha256(concat).lowercase(Locale.ROOT)
        
        return (finalHash.substring(0, 8) + "-" +
                finalHash.substring(8, 12) + "-" +
                finalHash.substring(12, 16) + "-" +
                finalHash.substring(16, 20) + "-" +
                finalHash.substring(20, 32)).uppercase(Locale.ROOT)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
