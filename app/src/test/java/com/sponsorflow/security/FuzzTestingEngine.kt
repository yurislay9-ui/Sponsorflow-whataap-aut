package com.sponsorflow.security

import android.app.PendingIntent
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sponsorflow.core.InboundMessage
import com.sponsorflow.core.Orchestrator
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/**
 * FUZZ TESTING MOTOR (V1.0)
 * Metralleta de inyecciones corruptas. Somete el API nativo a toneladas de datos basura
 * buscando romper el Parser, las corrutinas o causar StackTraces en logs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class FuzzTestingEngine {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    // Uncaught Exception Handler estático para atrapar si Orquestador explota y rompe el servidor
    private var wasCrashCaught = false

    @Before
    fun attachCrashCatcher() {
        wasCrashCaught = false
        Thread.setDefaultUncaughtExceptionHandler { _, _ ->
            wasCrashCaught = true
        }
    }

    @Test
    fun fuzzTestApiInbound_MalformedPayloads() = runBlocking {
        // Inicializamos el trabajador aislado (para simular el servidor en segundo plano HTTP-like)
        // Ya no es un endpoint HTTP, es una API de cola de Coroutines.
        
        // 1. Array de payloads venenosos
        val poisonPills = listOf(
            // Nulls y strings vacíos
            "",
            " ",
            "null",
            // JSONs rotos esperando desbordar al parser logístico
            "{ \"nombre\": \"Fuzz\", \"producto\": \"Zapatos\"", 
            // Injecciones de Prompt (El usuario intenta mandar binarios interpretados como texto o caracteres chinos inmensos)
            "\\x00\\x00\\xFF\\xFE" + "a".repeat(10000), 
            // Emojis exóticos que rompen decoders C++
            "👾🤖👹👺💀👻👽👾👾👾",
            // Null pointer bomb: Intents ficticios que van nulos,
            // (Se probarán con el constructor InboundMessage que acepta Nulls)
        )

        // 2. Ejecución masiva de metralleta
        for (i in 0..500) {
            val randomPoison = poisonPills[Random.nextInt(poisonPills.size)]
            
            // Forzamos al receptor del NotificationListener/API
            val maliciousPacket = InboundMessage(
                context = context,
                sender = "fuzz_hacker_\${Random.nextInt(50)}",
                text = randomPoison,
                replyIntent = null, // Header Roto/Corrupto
                remoteInputKey = null // Missing Body Token
            )
            
            // Disparamos contra el Orchestrator
            // El API de Firebase y el API de Local Notification consumen 'pushToQueue'
            Orchestrator.pushToQueue(maliciousPacket)
        }

        // Dejamos que el Event Loop procese la carga asíncrona
        delay(2000L)
        
        // RESULTADO DE PARADIGMA: El sistema de colas Catch-All y el Text Sanitizer
        // deben haber absorbido el impacto. El Thread principal del teléfono no puede haber caído.
        org.junit.Assert.assertFalse("FUZZ VULNERABILITY DETECTADA! Un paquete corrompió el Master Thread causando una excepción no atrapada.", wasCrashCaught)
        // La prueba es exitosa (verde) si la variable fue inmutable y el hilo sobrevivió sin detener su loop.
    }
}
