package com.sponsorflow.utils

import android.util.Log

/**
 * PATRÓN CIRCUIT BREAKER (SRE): Evita la sobrecarga de dependencias inestables o corruptas.
 */
class CircuitBreaker(
    private val name: String,
    private val failureThreshold: Int = 3,
    private val resetTimeMs: Long = 30000L // 30 segundos en estado abierto antes de intentar probar de nuevo
) {
    private var state: State = State.CLOSED
    private var failureCount = 0
    private var lastFailureTime = 0L

    enum class State { CLOSED, OPEN, HALF_OPEN }

    @Synchronized
    fun <T> executeWithGracefulDegradation(
        action: () -> T,
        fallback: (Exception) -> T
    ): T {
        val now = System.currentTimeMillis()

        // Evaluar estado actual del circuito
        if (state == State.OPEN) {
            if (now - lastFailureTime >= resetTimeMs) {
                // El tiempo de castigo expiró, entra a estado Half-Open para probar si ya funciona
                state = State.HALF_OPEN
                Log.w("NEXUS_SRE", "Circuit [\$name]: Pasando a HALF-OPEN para probar disponibilidad.")
            } else {
                // Circuito abierto, fallará rápidamente para ahorrar CPU/Batería y ejecuta degradación limpia.
                Log.w("NEXUS_SRE", "Circuit [\$name]: OPEN. Ejecutando Degradación Graciosa Fast-Fail.")
                return fallback(CircuitBreakerOpenException("Circuito Abierto"))
            }
        }

        return try {
            val result = action()
            // Acción fue exitosa
            if (state == State.HALF_OPEN) {
                state = State.CLOSED
                failureCount = 0
                Log.i("NEXUS_SRE", "Circuit [\$name]: Servicio recuperado. Pasando a CLOSED.")
            }
            result
        } catch (e: Exception) {
            recordFailure()
            Log.e("NEXUS_SRE", "Circuit [\$name]: Falla subyacente detectada: \${e.message}")
            fallback(e)
        }
    }

    private fun recordFailure() {
        failureCount++
        lastFailureTime = System.currentTimeMillis()
        if (failureCount >= failureThreshold && state == State.CLOSED) {
            state = State.OPEN
            Log.e("NEXUS_SRE", "Circuit [\$name]: ¡COLAPSO! Límite excedido. Circuito se dispara a OPEN.")
        }
    }

    class CircuitBreakerOpenException(message: String) : Exception(message)

    // Solo para pruebas Unitarias Chaos Monkey
    fun resetForTest() {
        state = State.CLOSED
        failureCount = 0
    }
}
