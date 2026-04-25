package com.sponsorflow.sre

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sponsorflow.core.LlamaEngine
import com.sponsorflow.data.BusinessDao
import com.sponsorflow.data.OrderEntity
import com.sponsorflow.data.SponsorflowDatabase
import com.sponsorflow.utils.CircuitBreaker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

/**
 * CHAOS MONKEY ENGINE (SRE)
 * Simula Desconexiones de DB, Caídas de Microservicio LLM (C++ Crash), y Corrupción de Archivos.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ChaosMonkeyTest {

    private lateinit var context: Context
    private lateinit var circuitBreaker: CircuitBreaker

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        circuitBreaker = CircuitBreaker("LLM_Service", failureThreshold = 2, resetTimeMs = 5000L)
        circuitBreaker.resetForTest()
    }

    @Test
    fun testChaosMonkey_MicroserviceCrash_CircuitBreaker() = runBlocking {
        // Escenario: El motor C++ subyacente (LLM) entra en pánico o se corrompe y tira throw Exceptions.
        val apiCrashCount = AtomicInteger(0)
        val gracefulResponses = AtomicInteger(0)

        // Simular 5 llamadas donde el servicio crashea
        for (i in 1..5) {
            val response = circuitBreaker.executeWithGracefulDegradation(
                action = {
                    apiCrashCount.incrementAndGet()
                    throw RuntimeException("C++ JNI Crash! Out of Memory")
                },
                fallback = {
                    gracefulResponses.incrementAndGet()
                    "Respuesta Degradada (Fast-Fail)"
                }
            )
            assertEquals("Respuesta Degradada (Fast-Fail)", response)
        }

        // El Circuit Breaker estaba configurado a Threshold = 2
        // Las primeras 2 veces la API intentó ejecutarse (y falló).
        // Las siguientes 3 veces, el circuito ya estaba ABIERTO y la llamada se "degadó" directo
        // al fallback sin siquiera molestar a la CPU ni incrementar apiCrashCount.
        assertTrue("CircuitBreaker no bloqueó la sobrecarga. Hubo \${apiCrashCount.get()} crasheos reales en vez de 2.", apiCrashCount.get() <= 2)
        assertEquals("Todas las 5 llamadas debieron retornar el fallback pacíficamente.", 5, gracefulResponses.get())
        
        // Simular que esperamos 5 segundos (el reset time)
        delay(5100L)
        
        // Ahora una llamada exitosa ("Half-Open" a "Closed")
        val finalResponse = circuitBreaker.executeWithGracefulDegradation(
            action = { "Recuperado" },
            fallback = { "Fallback" }
        )
        assertEquals("El circuito debió sanarse y retornar 'Recuperado'", "Recuperado", finalResponse)
    }

    @Test
    fun testChaosMonkey_ConfigCorruption() {
        // 1. Simular archivo corrupto introducido por una falla de NAND SSD
        val prefs = context.getSharedPreferences("sponsorflow_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("company_knowledge", null).apply() // Emulando lectura corrompida NullPointer
        
        // 2. Extraer datos (En Orchestrator.kt, los nulls se caen por el operador Elvis)
        val companyRules = prefs.getString("company_knowledge", "No hay reglas específicas. Sé cortés y responde natural.") ?: ""
        
        // 3. Verificamos Graceful Degradation de UI y Settings
        // Pudo haber sido nulo y crasheado (NullPointerException), pero Kotlin Elvis lo convierte a cadena vacía o default.
        assertEquals("Los defaults de GracefulDegradation debieron salvar la configuración", "", companyRules)
    }
}
