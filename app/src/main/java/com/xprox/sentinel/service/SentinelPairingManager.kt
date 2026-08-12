package com.xprox.sentinel.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class PairingRequest(
    val id: String = UUID.randomUUID().toString(),
    val clientName: String,
    val pinCode: String,
    val timestamp: Long = System.currentTimeMillis(),
    val onDecision: (Boolean) -> Unit
)

object SentinelPairingManager {
    private val _activePairingRequest = MutableStateFlow<PairingRequest?>(null)
    val activePairingRequest: StateFlow<PairingRequest?> = _activePairingRequest.asStateFlow()

    fun requestApproval(clientName: String, pinCode: String, onDecision: (Boolean) -> Unit) {
        _activePairingRequest.value = PairingRequest(
            clientName = clientName,
            pinCode = pinCode,
            onDecision = { approved ->
                _activePairingRequest.value = null
                onDecision(approved)
            }
        )
    }

    fun approveCurrent() {
        _activePairingRequest.value?.onDecision?.invoke(true)
    }

    fun rejectCurrent() {
        _activePairingRequest.value?.onDecision?.invoke(false)
    }
}
