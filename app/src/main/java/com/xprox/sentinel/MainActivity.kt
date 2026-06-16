package com.xprox.sentinel

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.theme.XProxTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
  companion object {
    val showDisconnectConfirmFlow = MutableStateFlow(false)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    LanguageManager.init(applicationContext)
    com.xprox.sentinel.service.VpnLifecycleInitiator.init(applicationContext)
    handleIntent(intent)

    enableEdgeToEdge()
    setContent {
      XProxTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent?) {
    if (intent?.getBooleanExtra("EXTRA_SHOW_DISCONNECT_CONFIRM", false) == true) {
      showDisconnectConfirmFlow.value = true
    }
  }
}
