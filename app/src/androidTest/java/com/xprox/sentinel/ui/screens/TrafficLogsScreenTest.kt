package com.xprox.sentinel.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.log.LogManager
import com.xprox.sentinel.theme.XProxTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrafficLogsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Ensure LogManager starts fresh by clearing historical logs
        LogManager.clearLogs(targetContext)
    }

    @Test
    fun testLiveTrafficLogsHudStreamingAndFiltering() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

        // Retrieve dynamic localized text for test assertions
        val logsTitle = LanguageManager.getString("logs_title")
        val sensitiveOnlyText = LanguageManager.getString("sensitive_only")
        val hudIdleText = LanguageManager.getString("hud_idle")

        // 1. Set screen content
        composeTestRule.setContent {
            XProxTheme {
                TrafficLogsScreen()
            }
        }

        // 2. Verify header elements and initial idle status
        composeTestRule.onNodeWithText(logsTitle).assertExists()
        composeTestRule.onNodeWithText(sensitiveOnlyText).assertExists()
        composeTestRule.onNodeWithText(hudIdleText).assertExists()

        // 3. Emit Live Network Connection Logs using LogManager
        // A. Audited sensitive SSH connection (port 22)
        LogManager.logConnection(
            context = targetContext,
            packageName = "com.sensitive.ssh",
            appName = "SSH Terminal",
            destinationIp = "203.0.113.4",
            port = 22
        )

        // B. Non-audited standard chat connection (port 12345)
        LogManager.logConnection(
            context = targetContext,
            packageName = "com.normal.chat",
            appName = "Messenger Chat",
            destinationIp = "203.0.113.5",
            port = 12345
        )

        // 4. Verify both connections stream and render in the UI Console in real-time
        composeTestRule.onNodeWithText("com.sensitive.ssh", substring = true).assertExists()
        composeTestRule.onNodeWithText("com.normal.chat", substring = true).assertExists()

        // Verify the idle text is now replaced by logs
        composeTestRule.onNodeWithText(hudIdleText).assertDoesNotExist()

        // 5. Toggle "SENSITIVE PORTS ONLY" filtering to exclude normal traffic
        composeTestRule.onNodeWithText(sensitiveOnlyText).performClick()

        // 6. Assert that only the audited sensitive SSH connection remains,
        // and the non-sensitive chat connection is successfully filtered out
        composeTestRule.onNodeWithText("com.sensitive.ssh", substring = true).assertExists()
        composeTestRule.onNodeWithText("com.normal.chat", substring = true).assertDoesNotExist()

        // 7. Toggle "SENSITIVE PORTS ONLY" filtering back off
        composeTestRule.onNodeWithText(sensitiveOnlyText).performClick()

        // Verify both show up again
        composeTestRule.onNodeWithText("com.sensitive.ssh", substring = true).assertExists()
        composeTestRule.onNodeWithText("com.normal.chat", substring = true).assertExists()
    }
}
