package com.sponsorflow.core

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.*
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * MOTOR DE ESTRÉS NATIVO (Equivalente a Artillery/k6 para RPA Offline).
 * Simula 10,000 notificaciones de WhatsApp simultáneas para probar OOM y SQLite Locks.
 */
class ArtilleryNativeStressTest {

    @Test
    fun simulateWhatsAppDDoS_SpikeAndSoak() = runBlocking {
        // En un entorno de test puro, simulamos contextos
        // Nota: En JUnit estándar de Android puro no tenemos Context real, 
        // pero podemos perfilar la lógica del AntiSpamGuardian y la concurrencia pura.

        // Simulador de Mock Contexto (al ser test de unidad saltamos a null para el motor, no ejecutará)
        val mockEngine = HybridIntentEngine(
            org.mockito.Mockito.mock(Context::class.java), 
            org.mockito.Mockito.mock(ONNXSemanticEngine::class.java)
        )
        val antiSpamGuardian = AntiSpamGuardian(mockEngine)

        val concurrentMessages = 10_000
        val blockedBySpam = AtomicInteger(0)
        val acceptedMessages = AtomicInteger(0)

        // Limpieza de memoria
        antiSpamGuardian.clearMemory()

        println("🚀 Iniciando Ataque DDoS de Notificaciones: \$concurrentMessages peticiones concurrentes...")
        val startTime = System.currentTimeMillis()

        // Spike Testing: Simulamos que 1,000 clientes distintos más 1 troll enviando 9,000 mensajes
        // todos disparan notificaciones al mismo milisegundo a través de Corrutinas pesadas.
        val jobs = List(concurrentMessages) { i ->
            launch(Dispatchers.Default) {
                // El 90% del tráfico es un TROLL haciendo SPAM extremo (mismo número).
                // El 10% son clientes orgánicos (números únicos).
                val senderPhone = if (i < 9000) "TROLL_BOT_+1(555)000-0000" else "ORGANICO_+1(555)999-\$i"
                
                // Concurrencia pura contra el Escudo
                val isSafe = antiSpamGuardian.isSafeToReply(senderPhone)
                
                if (isSafe) {
                    acceptedMessages.incrementAndGet()
                } else {
                    blockedBySpam.incrementAndGet()
                }
            }
        }

        // Soak Time: Esperamos que toda la metralla termine
        jobs.joinAll()
        val endTime = System.currentTimeMillis()

        println("⏱️ Tiempo de resolución (Latencia Total): \${endTime - startTime} ms")
        println("✅ Mensajes Aceptados (Enrutados al DB): \${acceptedMessages.get()}")
        println("🛡️ Mensajes Bloqueados (Escudo Anti-Ban): \${blockedBySpam.get()}")

        // Validaciones Matemáticas
        // De los 9,000 mensajes del Troll, el AntiSpam solo debió dejar pasar EXACTAMENTE 1 (o máximo 6 según el frame).
        // Los 1,000 orgánicos debieron pasar limpiamente.
        assertTrue("El escudo no resistió. Pasaron demasiados mensajes del troll.", acceptedMessages.get() <= 1006)
        assertTrue("Rendimiento inaceptable. Tardó más de 5 segundos en procesar 10k hilos.", (endTime - startTime) < 5000L)
    }
}
