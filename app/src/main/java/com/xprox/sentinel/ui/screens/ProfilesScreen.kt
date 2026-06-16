package com.xprox.sentinel.ui.screens

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.config.XrayConfigManager
import com.xprox.sentinel.config.XrayProfilePersistence
import com.xprox.sentinel.service.VpnManagerService
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.data.string
import com.xprox.sentinel.ui.screens.profiles.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen() {
    val context = LocalContext.current

    // Active sub-tab state (0 = Apps Routing, 1 = Network Routing)
    var activeSubTab by remember { mutableStateOf(0) }

    // App Split Tunneling variables
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var allowedApps by remember { mutableStateOf(emptySet<String>()) }
    var searchQuery by remember { mutableStateOf("") }
    var isBypassMode by remember { mutableStateOf(true) }

    // GeoIP & GeoSite Routing variables
    var geoIpRules by remember { mutableStateOf(emptySet<String>()) }
    var geoSiteRules by remember { mutableStateOf(emptySet<String>()) }
    var customGeoIpInput by remember { mutableStateOf("") }
    var customGeoSiteInput by remember { mutableStateOf("") }

    // Presets
    val geoIpPresets = listOf("geoip:private", "geoip:ru", "geoip:cn", "geoip:us")
    val geoSitePresets = listOf(
        "geosite:google",
        "geosite:category-ads-all",
        "geosite:youtube",
        "geosite:netflix",
        "geosite:instagram",
        "geosite:facebook",
        "geosite:twitter"
    )

    // Load persisted values at startup
    LaunchedEffect(Unit) {
        installedApps = getInstalledApps(context)

        // Restore service static properties from disk
        VpnManagerService.allowedAppsList = XrayProfilePersistence.loadAllowedApps(context)
        VpnManagerService.isBypassMode = XrayProfilePersistence.loadBypassMode(context)
        VpnManagerService.geoipRulesList = XrayProfilePersistence.loadGeoIpRules(context)
        VpnManagerService.geositeRulesList = XrayProfilePersistence.loadGeoSiteRules(context)

        // Initialize UI states
        allowedApps = VpnManagerService.allowedAppsList.toSet()
        isBypassMode = VpnManagerService.isBypassMode
        geoIpRules = VpnManagerService.geoipRulesList.toSet()
        geoSiteRules = VpnManagerService.geositeRulesList.toSet()
    }



    // Filter apps based on search query
    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isEmpty()) {
            installedApps
        } else {
            installedApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp)
    ) {
        // Headers
        Text(
            text = string("profiles_title"),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CyberTeal
        )
        Text(
            text = string("profiles_subtitle"),
            fontSize = 12.sp,
            color = TextGray
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Cyber Segmented Tab Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCard)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (activeSubTab == 0) CyberTeal.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { activeSubTab = 0 }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = string("routing_tab_apps"),
                    color = if (activeSubTab == 0) CyberTeal else TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (activeSubTab == 1) CyberTeal.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { activeSubTab = 1 }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = string("routing_tab_network"),
                    color = if (activeSubTab == 1) CyberTeal else TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        // Sub Tab Content
        if (activeSubTab == 0) {
            AppRoutingPanel(
                context = context,
                installedApps = installedApps,
                filteredApps = filteredApps,
                allowedApps = allowedApps,
                isBypassMode = isBypassMode,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onBypassModeChange = { bypass ->
                    isBypassMode = bypass
                    VpnManagerService.isBypassMode = bypass
                    XrayProfilePersistence.saveBypassMode(context, bypass)
                },
                onAllowedAppToggle = { pkgName, checked ->
                    val nextSet = allowedApps.toMutableSet()
                    if (checked) {
                        nextSet.add(pkgName)
                    } else {
                        nextSet.remove(pkgName)
                    }
                    allowedApps = nextSet
                    VpnManagerService.allowedAppsList = nextSet.toList()
                    XrayProfilePersistence.saveAllowedApps(context, nextSet.toList())
                }
            )
        } else {
            NetworkRoutingPanel(
                context = context,
                geoIpRules = geoIpRules,
                geoSiteRules = geoSiteRules,
                customGeoIpInput = customGeoIpInput,
                customGeoSiteInput = customGeoSiteInput,
                onCustomGeoIpInputChange = { customGeoIpInput = it },
                onCustomGeoSiteInputChange = { customGeoSiteInput = it },
                onGeoIpRuleToggle = { preset, checked ->
                    val nextSet = geoIpRules.toMutableSet()
                    if (checked) nextSet.add(preset) else nextSet.remove(preset)
                    geoIpRules = nextSet
                    VpnManagerService.geoipRulesList = nextSet.toList()
                    XrayProfilePersistence.saveGeoIpRules(context, nextSet.toList())
                },
                onGeoSiteRuleToggle = { preset, checked ->
                    val nextSet = geoSiteRules.toMutableSet()
                    if (checked) nextSet.add(preset) else nextSet.remove(preset)
                    geoSiteRules = nextSet
                    VpnManagerService.geositeRulesList = nextSet.toList()
                    XrayProfilePersistence.saveGeoSiteRules(context, nextSet.toList())
                },
                onCustomGeoIpAdd = {
                    if (customGeoIpInput.isNotEmpty()) {
                        val nextSet = geoIpRules.toMutableSet()
                        nextSet.add(customGeoIpInput.trim().lowercase())
                        geoIpRules = nextSet
                        VpnManagerService.geoipRulesList = nextSet.toList()
                        XrayProfilePersistence.saveGeoIpRules(context, nextSet.toList())
                        customGeoIpInput = ""
                    }
                },
                onCustomGeoSiteAdd = {
                    if (customGeoSiteInput.isNotEmpty()) {
                        val nextSet = geoSiteRules.toMutableSet()
                        nextSet.add(customGeoSiteInput.trim().lowercase())
                        geoSiteRules = nextSet
                        VpnManagerService.geositeRulesList = nextSet.toList()
                        XrayProfilePersistence.saveGeoSiteRules(context, nextSet.toList())
                        customGeoSiteInput = ""
                    }
                },
                onGeoIpRuleRemove = { customRule ->
                    val nextSet = geoIpRules.toMutableSet()
                    nextSet.remove(customRule)
                    geoIpRules = nextSet
                    VpnManagerService.geoipRulesList = nextSet.toList()
                    XrayProfilePersistence.saveGeoIpRules(context, nextSet.toList())
                },
                onGeoSiteRuleRemove = { customRule ->
                    val nextSet = geoSiteRules.toMutableSet()
                    nextSet.remove(customRule)
                    geoSiteRules = nextSet
                    VpnManagerService.geositeRulesList = nextSet.toList()
                    XrayProfilePersistence.saveGeoSiteRules(context, nextSet.toList())
                }
            )
        }
    }
}

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: ImageBitmap? = null
)

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

private suspend fun getInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val appMap = mutableMapOf<String, AppInfo>()

    // Query Launcher Activities to capture all user-facing launchable apps
    try {
        val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)
        resolveInfos.forEach { resolveInfo ->
            val appInfo = resolveInfo.activityInfo.applicationInfo
            val label = resolveInfo.loadLabel(pm).toString()
            
            // Load application icon and convert it to ImageBitmap safely on IO thread
            val icon = try {
                val drawable = appInfo.loadIcon(pm)
                val bitmap = drawableToBitmap(drawable)
                bitmap.asImageBitmap()
            } catch (e: Exception) {
                null
            }
            
            appMap[appInfo.packageName] = AppInfo(label, appInfo.packageName, icon)
        }
    } catch (e: Exception) {
        // Ignore
    }

    appMap.values.sortedBy { it.appName }
}
