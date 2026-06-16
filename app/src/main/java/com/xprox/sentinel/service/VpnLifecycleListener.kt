package com.xprox.sentinel.service

import android.content.Context
import kotlinx.coroutines.CoroutineScope

interface VpnLifecycleListener {
    fun onServiceStart(context: Context, scope: CoroutineScope, profileId: String?)
    fun onServiceStop()
}
