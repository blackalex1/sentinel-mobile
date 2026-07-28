package com.xprox.sentinel.service.downloader

import android.util.Log
import com.xprox.sentinel.service.XrayReleaseInfo
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object XrayGithubApiClient {

    private const val TAG = "XrayGithubApiClient"
    private const val GITHUB_RELEASES_URL = "https://api.github.com/repos/XTLS/Xray-core/releases"

    fun fetchAvailableReleases(allowPrerelease: Boolean): List<XrayReleaseInfo> {
        val releaseList = mutableListOf<XrayReleaseInfo>()
        try {
            val url = URL(GITHUB_RELEASES_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "Sentinel-Android-App")

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val isDraft = obj.optBoolean("draft", false)
                    val isPrerelease = obj.optBoolean("prerelease", false)
                    val tagName = obj.optString("tag_name", "")
                    val publishedAt = obj.optString("published_at", "")

                    if (isDraft) continue
                    if (isPrerelease && !allowPrerelease) continue

                    if (tagName.isNotEmpty()) {
                        releaseList.add(
                            XrayReleaseInfo(
                                tagName = tagName,
                                isPrerelease = isPrerelease,
                                publishedAt = publishedAt
                            )
                        )
                    }
                }
            } else {
                Log.e(TAG, "GitHub API returned code HTTP ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query Xray-core releases from GitHub API", e)
        }
        return releaseList
    }

    fun fetchLatestVersionTag(allowPrerelease: Boolean): String? {
        val releases = fetchAvailableReleases(allowPrerelease)
        return releases.firstOrNull()?.tagName
    }
}
