package com.xprox.sentinel.service

import android.content.Context
import kotlinx.coroutines.CoroutineScope

class FossVpnListener : VpnLifecycleListener {
    override fun onServiceStart(context: Context, scope: CoroutineScope, profileId: String?) {
        // No-op for standard build
    }

    override fun onServiceStop() {
        // No-op for standard build
    }
}
