package com.xprox.sentinel.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.theme.XProxTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfilesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testAppSplitTunnelingAndNetworkRoutingTabs() {
        // Retrieve dynamic localized text for test assertions
        val profilesTitle = LanguageManager.getString("profiles_title")
        val tabAppsText = LanguageManager.getString("routing_tab_apps")
        val tabNetworkText = LanguageManager.getString("routing_tab_network")
        val searchPlaceholder = LanguageManager.getString("search_apps")
        val bypassModeText = LanguageManager.getString("bypass_mode")
        val selectionModeText = LanguageManager.getString("selection_mode")
        val geoIpPrivatePresetText = "geoip:private"
        val geositeGooglePresetText = "geosite:google"

        // 1. Set screen content
        composeTestRule.setContent {
            XProxTheme {
                ProfilesScreen()
            }
        }

        // 2. Verify that headers, segmented tabs, and default App Routing panel render correctly
        composeTestRule.onNodeWithText(profilesTitle).assertExists()
        composeTestRule.onNodeWithText(tabAppsText).assertExists()
        composeTestRule.onNodeWithText(tabNetworkText).assertExists()
        
        // Inside App panel
        composeTestRule.onNodeWithText(bypassModeText).assertExists()
        composeTestRule.onNodeWithText(selectionModeText).assertExists()
        composeTestRule.onNodeWithText(searchPlaceholder).assertExists()

        // 3. Wait for launcher apps to load from the emulator's PackageManager (max 5s)
        // Standard Android emulators always have the Settings app package: com.android.settings
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("com.android.settings", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        // Verify com.android.settings exists in the list
        composeTestRule.onNodeWithText("com.android.settings", substring = true).assertExists()

        // 4. Perform search query filtering
        // Type standard settings package to filter out other apps
        composeTestRule.onNodeWithText(searchPlaceholder).performTextInput("com.android.settings")
        
        // Assert settings exists, and another non-related app package doesn't exist
        composeTestRule.onNodeWithText("com.android.settings", substring = true).assertExists()
        composeTestRule.onNodeWithText("com.android.chrome", substring = true).assertDoesNotExist()

        // 5. Toggle Settings app routing checkbox state
        composeTestRule.onNodeWithText("com.android.settings", substring = true).performClick()

        // 6. Navigate to the Network Routing Tab
        composeTestRule.onNodeWithText(tabNetworkText).performClick()

        // 7. Verify Network Panel presets are present
        composeTestRule.onNodeWithText(geoIpPrivatePresetText, substring = true).assertExists()
        composeTestRule.onNodeWithText(geositeGooglePresetText, substring = true).assertExists()
    }
}
