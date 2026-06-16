package com.xprox.sentinel.ui.main

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xprox.sentinel.MainActivity
import com.xprox.sentinel.data.LanguageManager
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testNavigationTabsPresenceAndClick() {
        // Retrieve dynamic localized text to ensure 100% locale-independent test assertions
        val dashboardText = LanguageManager.getString("tab_dashboard")
        val profilesText = LanguageManager.getString("tab_profiles")
        val logsText = LanguageManager.getString("tab_logs")
        val settingsText = LanguageManager.getString("tab_settings")

        // 1. Verify that MainActivity compiles and launches successfully, and all bottom navigation tabs render.
        composeTestRule.onNodeWithText(dashboardText).assertExists()
        composeTestRule.onNodeWithText(profilesText).assertExists()
        composeTestRule.onNodeWithText(logsText).assertExists()
        composeTestRule.onNodeWithText(settingsText).assertExists()

        // 2. Perform click to navigate to the Profiles/Routing tab
        composeTestRule.onNodeWithText(profilesText).performClick()

        // 3. Perform click to navigate to the Settings tab
        composeTestRule.onNodeWithText(settingsText).performClick()

        // 4. Perform click to navigate back to the Dashboard tab
        composeTestRule.onNodeWithText(dashboardText).performClick()
    }
}
