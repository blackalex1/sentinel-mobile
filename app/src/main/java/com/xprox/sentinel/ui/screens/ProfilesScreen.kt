package com.xprox.sentinel.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.config.XrayProfilePersistence
import com.xprox.sentinel.data.string
import com.xprox.sentinel.service.VpnManagerService
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.screens.profiles.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun notifyConfigChanged(context: Context) {
    if (VpnManagerService.isRunningFlow.value) {
        val intent = Intent(context, VpnManagerService::class.java).apply {
            action = VpnManagerService.ACTION_RELOAD_CONFIG
        }
        context.startService(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen() {
    val context = LocalContext.current

    // Active sub-tab state (0 = Smart Rules, 1 = Apps Routing, 2 = Custom Rules & Geo)
    var activeSubTab by remember { mutableStateOf(0) }

    // Smart Preset Rules State
    var bypassRu by remember { mutableStateOf(true) }
    var bypassTorrents by remember { mutableStateOf(true) }
    var blockQuic by remember { mutableStateOf(true) }
    var bypassLan by remember { mutableStateOf(true) }

    // App Split Tunneling State
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    // Custom Rules State (Direct, Proxy, Block)
    var customDirectRules by remember { mutableStateOf(emptyList<String>()) }
    var customProxyRules by remember { mutableStateOf(emptyList<String>()) }
    var customBlockRules by remember { mutableStateOf(emptyList<String>()) }

    // GeoIP & GeoSite Routing State
    var geoIpRules by remember { mutableStateOf(emptySet<String>()) }
    var geoSiteRules by remember { mutableStateOf(emptySet<String>()) }

    // Load persisted values at startup
    LaunchedEffect(Unit) {
        installedApps = getInstalledApps(context)

        // Load Smart Rules
        bypassRu = XrayProfilePersistence.loadBypassRuSites(context)
        bypassTorrents = XrayProfilePersistence.loadBypassTorrents(context)
        blockQuic = XrayProfilePersistence.loadBlockQuic(context)
        bypassLan = XrayProfilePersistence.loadBypassLan(context)

        // Load Custom Rules
        customDirectRules = XrayProfilePersistence.loadCustomDirectRules(context)
        customProxyRules = XrayProfilePersistence.loadCustomProxyRules(context)
        customBlockRules = XrayProfilePersistence.loadCustomBlockRules(context)

        // Restore app split tunneling static properties
        VpnManagerService.allowedAppsList = XrayProfilePersistence.loadAllowedApps(context)
        VpnManagerService.isBypassMode = XrayProfilePersistence.loadBypassMode(context)
        VpnManagerService.geoipRulesList = XrayProfilePersistence.loadGeoIpRules(context)
        VpnManagerService.geositeRulesList = XrayProfilePersistence.loadGeoSiteRules(context)

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
            .background(Color.Transparent)
            .padding(20.dp)
    ) {
        Text(
            text = string("profiles_title"),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
        Text(
            text = string("profiles_subtitle"),
            fontSize = 12.sp,
            color = TextGray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // High-End Agency-Tier 3-Segmented Glass Pill Tab Bar
        Surface(
            color = DarkCardElevated,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .height(46.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    0 to string("routing_tab_smart"),
                    1 to string("routing_tab_apps"),
                    2 to string("routing_tab_custom")
                ).forEach { (index, label) ->
                    val isSelected = activeSubTab == index

                    val tabBg by animateColorAsState(
                        targetValue = if (isSelected) ElectricViolet else Color.Transparent,
                        label = "tabBg"
                    )

                    val tabTextColor by animateColorAsState(
                        targetValue = if (isSelected) TextWhite else TextGray,
                        label = "tabTextColor"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tabBg)
                            .clickable { activeSubTab = index }
                    ) {
                        Text(
                            text = label,
                            color = tabTextColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // Sub Tab Content
        when (activeSubTab) {
            0 -> {
                SmartRoutingPanel(
                    context = context,
                    bypassRu = bypassRu,
                    bypassTorrents = bypassTorrents,
                    blockQuic = blockQuic,
                    bypassLan = bypassLan,
                    onBypassRuChange = { enabled ->
                        bypassRu = enabled
                        XrayProfilePersistence.saveBypassRuSites(context, enabled)
                        notifyConfigChanged(context)
                    },
                    onBypassTorrentsChange = { enabled ->
                        bypassTorrents = enabled
                        XrayProfilePersistence.saveBypassTorrents(context, enabled)
                        notifyConfigChanged(context)
                    },
                    onBlockQuicChange = { enabled ->
                        blockQuic = enabled
                        XrayProfilePersistence.saveBlockQuic(context, enabled)
                        notifyConfigChanged(context)
                    },
                    onBypassLanChange = { enabled ->
                        bypassLan = enabled
                        XrayProfilePersistence.saveBypassLan(context, enabled)
                        notifyConfigChanged(context)
                    }
                )
            }
            1 -> {
                AppRoutingPanel(
                    context = context,
                    installedApps = installedApps,
                    filteredApps = filteredApps,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it }
                )
            }
            2 -> {
                NetworkRoutingPanel(
                    context = context,
                    customDirectRules = customDirectRules,
                    customProxyRules = customProxyRules,
                    customBlockRules = customBlockRules,
                    geoIpRules = geoIpRules,
                    geoSiteRules = geoSiteRules,
                    onAddCustomRule = { target, rule ->
                        when (target) {
                            "Direct" -> {
                                val nextList = (customDirectRules + rule).distinct()
                                customDirectRules = nextList
                                XrayProfilePersistence.saveCustomDirectRules(context, nextList)
                            }
                            "Proxy" -> {
                                val nextList = (customProxyRules + rule).distinct()
                                customProxyRules = nextList
                                XrayProfilePersistence.saveCustomProxyRules(context, nextList)
                            }
                            "Block" -> {
                                val nextList = (customBlockRules + rule).distinct()
                                customBlockRules = nextList
                                XrayProfilePersistence.saveCustomBlockRules(context, nextList)
                            }
                        }
                        notifyConfigChanged(context)
                    },
                    onRemoveCustomRule = { target, rule ->
                        when (target) {
                            "Direct" -> {
                                val nextList = customDirectRules - rule
                                customDirectRules = nextList
                                XrayProfilePersistence.saveCustomDirectRules(context, nextList)
                            }
                            "Proxy" -> {
                                val nextList = customProxyRules - rule
                                customProxyRules = nextList
                                XrayProfilePersistence.saveCustomProxyRules(context, nextList)
                            }
                            "Block" -> {
                                val nextList = customBlockRules - rule
                                customBlockRules = nextList
                                XrayProfilePersistence.saveCustomBlockRules(context, nextList)
                            }
                        }
                        notifyConfigChanged(context)
                    },
                    onGeoIpRuleToggle = { preset, checked ->
                        val nextSet = geoIpRules.toMutableSet()
                        if (checked) nextSet.add(preset) else nextSet.remove(preset)
                        geoIpRules = nextSet
                        VpnManagerService.geoipRulesList = nextSet.toList()
                        XrayProfilePersistence.saveGeoIpRules(context, nextSet.toList())
                        notifyConfigChanged(context)
                    },
                    onGeoSiteRuleToggle = { preset, checked ->
                        val nextSet = geoSiteRules.toMutableSet()
                        if (checked) nextSet.add(preset) else nextSet.remove(preset)
                        geoSiteRules = nextSet
                        VpnManagerService.geositeRulesList = nextSet.toList()
                        XrayProfilePersistence.saveGeoSiteRules(context, nextSet.toList())
                        notifyConfigChanged(context)
                    },
                    onGeoIpRuleRemove = { customRule ->
                        val nextSet = geoIpRules.toMutableSet()
                        nextSet.remove(customRule)
                        geoIpRules = nextSet
                        VpnManagerService.geoipRulesList = nextSet.toList()
                        XrayProfilePersistence.saveGeoIpRules(context, nextSet.toList())
                        notifyConfigChanged(context)
                    },
                    onGeoSiteRuleRemove = { customRule ->
                        val nextSet = geoSiteRules.toMutableSet()
                        nextSet.remove(customRule)
                        geoSiteRules = nextSet
                        VpnManagerService.geositeRulesList = nextSet.toList()
                        XrayProfilePersistence.saveGeoSiteRules(context, nextSet.toList())
                        notifyConfigChanged(context)
                    }
                )
            }
        }
    }
}

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: ImageBitmap? = null
)

private fun drawableToBitmap(drawable: Drawable, targetSize: Int = 96): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        val src = drawable.bitmap
        if (src.width <= targetSize && src.height <= targetSize) {
            return src
        }
        return Bitmap.createScaledBitmap(src, targetSize, targetSize, true)
    }
    val width = if (drawable.intrinsicWidth > 0) minOf(drawable.intrinsicWidth, targetSize) else targetSize
    val height = if (drawable.intrinsicHeight > 0) minOf(drawable.intrinsicHeight, targetSize) else targetSize
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

private suspend fun getInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val appMap = mutableMapOf<String, AppInfo>()

    try {
        val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)
        resolveInfos.forEach { resolveInfo ->
            val appInfo = resolveInfo.activityInfo.applicationInfo
            val label = try { resolveInfo.loadLabel(pm).toString() } catch (t: Throwable) { appInfo.packageName }
            
            val icon = try {
                val drawable = appInfo.loadIcon(pm)
                val bitmap = drawableToBitmap(drawable, targetSize = 96)
                bitmap.asImageBitmap()
            } catch (t: Throwable) {
                null
            }
            
            appMap[appInfo.packageName] = AppInfo(label, appInfo.packageName, icon)
        }
    } catch (t: Throwable) {
        // Ignore
    }

    appMap.values.sortedBy { it.appName }
}
