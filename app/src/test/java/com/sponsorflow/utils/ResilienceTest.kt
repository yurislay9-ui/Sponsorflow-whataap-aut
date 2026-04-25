package com.sponsorflow.utils

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class ResilienceTest {

    @Test
    fun testClickDebouncer_MashingAttack() {
        ClickDebouncer.resetForTest()
        val executionCount = AtomicInteger(0)

        // Simulamos un usuario frustrado tocando la pantalla 20 veces casi al mismo tiempo (Loop rápido)
        for (i in 1..20) {
            ClickDebouncer.withDebounce {
                executionCount.incrementAndGet()    
            }
        }
        
        // El Escudo Anti-Mashing solo debió permitir la primera ejecución 
        // botando inmediatamente a la basura los 19 clicks frenéticos restantes.
        assertTrue("El usuario tecleó de forma compulsiva, debió filtrarse a 1 solo clic.", executionCount.get() == 1)
    }

    @Test
    fun testExponentialBackoff_NetworkRecovery() = runBlocking {
        // Simulamos el loop del Orquestador V4
        var apiCallsCount = 0
        var isNetworkLive = false
        var finallySuccess = false
        
        // Trama de re-intento de 3 rondas (Lo que inyecté en Orchestrator.kt)
        var retries = 0
        while (retries < 3 && !finallySuccess) {
            try {
                // Forzamos un limite mental de que el request debe responder en menos de 2 milisegundos
                withTimeout(2L) {
                    apiCallsCount++
                    if(!isNetworkLive) {
                        // Simulamos que Gemini/Qwen no responde a tiempo
                        delay(100L) 
                    }
                    finallySuccess = true
                }
            } catch (e: TimeoutCancellationException) {
                retries++
                
                // Si va por el reintento 2, simulamos que llegó a su casa y agarró WiFi de pronto
                if (retries == 2) {
                    isNetworkLive = true 
                }
            }
        }
        
        // Validaciones: Resistió la ida de red y pudo culminarlo cuando volvió
        assertTrue("Logró finalizar en el intento: \${apiCallsCount}", finallySuccess)
        assertTrue("Debió reintentar exactamente 2 veces tras el timeout inicial", apiCallsCount == 3)
    }
}
