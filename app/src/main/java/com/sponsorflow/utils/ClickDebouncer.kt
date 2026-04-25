package com.sponsorflow.utils

/**
 * UTILERÍA DE RESILIENCIA UI: ANTI-MASHING (Doble Clicks).
 * Previene que usuarios nerviosos o dispositivos con lag
 * triggeren múltiples operaciones de base de datos o envíos de red.
 */
object ClickDebouncer {
    private var lastClickTime: Long = 0
    private const val DEBOUNCE_DELAY_MS = 1500L // 1.5 segundos de "cooldown" visual
    
    // Método para uso en interfaces Compose y Android puro
    @Synchronized
    fun withDebounce(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime >= DEBOUNCE_DELAY_MS) {
            lastClickTime = now
            action()
        }
    }
    
    // Solo para pruebas TDD limpias
    fun resetForTest() {
        lastClickTime = 0
    }
}
