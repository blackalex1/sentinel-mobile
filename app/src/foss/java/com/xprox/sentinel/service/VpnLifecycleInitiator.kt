package com.xprox.sentinel.service

import android.content.Context

object VpnLifecycleInitiator {
    fun init(context: Context) {
        VpnLifecycleProvider.listener = FossVpnListener()
    }
}
