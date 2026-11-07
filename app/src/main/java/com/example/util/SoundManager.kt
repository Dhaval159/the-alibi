package com.example.util

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SoundManager {

    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 70)
    } catch (_: Exception) {
        null
    }

    private val _isMusicEnabled = MutableStateFlow(true)
    val isMusicEnabled: StateFlow<Boolean> = _isMusicEnabled.asStateFlow()

    private val _isSfxEnabled = MutableStateFlow(true)
    val isSfxEnabled: StateFlow<Boolean> = _isSfxEnabled.asStateFlow()

    private val _isNarrationEnabled = MutableStateFlow(true)
    val isNarrationEnabled: StateFlow<Boolean> = _isNarrationEnabled.asStateFlow()

    fun toggleMusic() {
        _isMusicEnabled.value = !_isMusicEnabled.value
    }

    fun toggleSfx() {
        _isSfxEnabled.value = !_isSfxEnabled.value
    }

    fun toggleNarration() {
        _isNarrationEnabled.value = !_isNarrationEnabled.value
    }

    fun playEvidenceDiscovery() {
        if (!_isSfxEnabled.value) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
        } catch (_: Exception) {}
    }

    fun playPhaseChangeChime() {
        if (!_isSfxEnabled.value) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 350)
        } catch (_: Exception) {}
    }

    fun playNotificationAlert() {
        if (!_isSfxEnabled.value) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 150)
        } catch (_: Exception) {}
    }

    fun playSecretReveal() {
        if (!_isSfxEnabled.value) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 400)
        } catch (_: Exception) {}
    }

    fun playAccusationGong() {
        if (!_isSfxEnabled.value) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_SUP_ERROR, 500)
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
    }
}
