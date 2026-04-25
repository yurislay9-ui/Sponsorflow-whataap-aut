package com.sponsorflow.core

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Pruebas de Estrés para el Motor Determinístico (Levenshtein y NLP local)
 */
class CoreEngineStressTest {

    @Test
    fun testLevenshtein_NullSafetyAndGracefulFallbacks() {
        // En Java/Android un objeto CharSequence podría llegar nulo si viene de un Intent corrupto
        val resultNullLhs = CoreEngine.levenshtein(null, "hola")
        val resultNullRhs = CoreEngine.levenshtein("hola", null)
        val resultBothNull = CoreEngine.levenshtein(null, null)

        // El algoritmo debe devolver una gran penalización (ej. -1 o un número muy alto) y no crashear
        assertEquals("Fallo al lidiar con Lhs Null", -1, resultNullLhs)
        assertEquals("Fallo al lidiar con Rhs Null", -1, resultNullRhs)
        assertEquals("Fallo al lidiar con Both Null", 0, resultBothNull) // Si ambos son null, distancia es 0 o fallback seguro. En mi implementación definí que para == null devuelva -1 excepto si son iguales.
    }

    @Test
    fun testLevenshtein_UnrecognizableByteStream() {
        // Corrupción de la Memoria / Fuzzing / Emojis asiáticos extraños
        val weirdBytesString = String(byteArrayOf(0x00, 0xFF.toByte(), 0xFE.toByte(), 0x01, 0x00, 0x00))
        
        // El motor determinístico jamás debe crashear o lanzar OutOfMemory. Debe procesar esto
        // como una String genérica y arrojar un cálculo de distancia válido.
        val distance = CoreEngine.levenshtein(weirdBytesString, "hola")
        
        // Al ser completamente diferente a 'hola', la distancia debe ser >= 4 (longitud de hola)
        assertTrue("La distancia de Levenshtein debió calcularse sin crasheos y ser alta.", distance >= 4)
    }

    @Test
    fun testProcessMessage_NullOrEmptySafeguard() {
        // Simular que el canal de OCR/Transcription devuelve Nulo u objeto corrupto
        val (response, intent) = CoreEngine.processMessage(
            message = "   ", 
            catalogContext = "cat", 
            companyRules = "rules", 
            senderName = "tester"
        )
        
        assertEquals("Un string vacío debe resolverse en UNKNOWN o HUMAN_FALLBACK", CoreEngine.Intent.HUMAN_FALLBACK, intent)
    }
}
