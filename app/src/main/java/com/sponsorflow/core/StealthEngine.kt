package com.sponsorflow.core

import android.util.Log
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.random.Random

/**
 * MOTOR DE CAMUFLAJE (El Algoritmo de la Paciencia / Anti-Baneo)
 * Protege el número de WhatsApp de los radares de bots de Meta.
 */
object StealthEngine {
    private const val TAG = "NEXUS_Stealth"

    /**
     * Define si el bot debería estar inactivo para simular que el dueño está durmiendo.
     * Meta banea bots que responden instantáneamente a las 4 AM.
     */
    fun isSleepingTime(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        // Duerme de 2 AM a 7 AM
        return hour in 2..7 
    }

    /**
     * Pausa asíncrona matemática. Simula el tiempo que tarda un humano
     * en sacar el teléfono del bolsillo, leer la notificación y pensar una respuesta.
     */
    suspend fun emulateHumanReading(incomingLength: Int) {
        val baseTime = 3000L // Mínimo 3 segundos para reaccionar
        val timePerChar = 60L // 60 milisegundos de lectura por cada letra que nos enviaron
        val randomFactor = Random.nextLong(1000L, 6000L) // Distracción aleatoria entre 1 y 6 segundos

        var totalDelay = baseTime + (incomingLength * timePerChar) + randomFactor
        
        // SRE Guard: Limitar delay brutal para mensajes inmensos copiados y pegados.
        // Nunca demorar más de 12 segundos, para no travar la pool de corrutinas del sistema.
        if (totalDelay > 12000L) {
            totalDelay = 12000L
        }

        Log.i(TAG, "🛡️ Anti-Detección Activa: Esperando \${totalDelay}ms para simular que " +
                "leemos el mensaje y escribimos. (Camuflaje Humano)")
        delay(totalDelay)
    }
}
