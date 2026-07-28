package com.xprox.sentinel.service.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VpnStateTracker {

    enum class ConnectionState {
        IDLE,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        ERROR
    }

    private val _stateFlow = MutableStateFlow(ConnectionState.IDLE)
    val stateFlow: StateFlow<ConnectionState> = _stateFlow.asStateFlow()

    private val _statusMessageFlow = MutableStateFlow("")
    val statusMessageFlow: StateFlow<String> = _statusMessageFlow.asStateFlow()

    fun updateState(newState: ConnectionState, message: String = "") {
        _stateFlow.value = newState
        _statusMessageFlow.value = message
    }
}
