package com.sponsorflow.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Batería de Pruebas Extrema (Edge Cases) para Sanitización de Formularios B2B.
 */
class DataValidationEdgeCaseTest {

    @Test
    fun testSanitizeEntityText_50kCharacters() {
        // Simulamos un TROLL copiando la Biblia en el cuadro de texto (50,000 "A"s)
        val evilInput = "A".repeat(50_000)
        
        // Configuramos la barrera para proteger SQlite
        val safeOutput = DataSanitizer.sanitizeEntityText(evilInput, maxLength = 1000)
        
        // Afirmar que bloqueamos la corrupción
        assertEquals("El texto debe ser cortado a exactamente 1000 caracteres.", 1000, safeOutput.length)
    }

    @Test
    fun testSanitizeEntityText_EmojisAndAsianCharacters() {
        // Probamos si los recortadores arruinan carácteres Kanji o Emojis Compuestos
        val complexInput = "こんにちは世界 👨‍👩‍👧‍👦 🚀 Hello"
        val safeOutput = DataSanitizer.sanitizeEntityText(complexInput, maxLength = 50)
        
        // No debe perder información porque está dentro del límite
        assertEquals("Debe preservar caracteres asiáticos y emojis de 4 byte intactos.", complexInput, safeOutput)
    }

    @Test
    fun testSanitizeEntityText_NullOrBlank() {
        assertEquals("Null debe ser devuelto como String vacío para evitar NullPointerEx.", "", DataSanitizer.sanitizeEntityText(null))
        assertEquals("Blancos deben colapsar a cero.", "", DataSanitizer.sanitizeEntityText("   \n\t  "))
    }

    @Test
    fun testParseSafePositivePrice_NegativeNumbers() {
        // ¿Qué pasa si el humano se equivoca y pone precio negativo?
        val input = "-150.50"
        val result = DataSanitizer.parseSafePositivePrice(input)
        
        assertNull("Los números negativos deben ser bloqueados (null) para disparar el mensaje de error de UI.", result)
    }

    @Test
    fun testParseSafePositivePrice_LettersAndEmojis() {
        val trollInput = "100.5 USD 💵"
        val result = DataSanitizer.parseSafePositivePrice(trollInput)
        
        assertNull("Debe fallar (NumberFormatException) y retornar null al detectar basura en precio.", result)
    }

    @Test
    fun testParseSafePositivePrice_OverflowInfinity() {
        // Intentona de causar Number Overflow en SQLite
        val infiniteInput = "9".repeat(400) // 9999...999 400 veces
        val result = DataSanitizer.parseSafePositivePrice(infiniteInput)
        
        assertNull("Double Infinity debe ser atrapado. Desbordamientos están prohibidos.", result)
        
        val strictMax = "1000000000" // 1 Billón, el límite impuesto es 999,999,999
        val maxResult = DataSanitizer.parseSafePositivePrice(strictMax)
        assertNull("Números que superen los 999 Millones son bloqueados para evitar crashes contables.", maxResult)
    }

    @Test
    fun testParseSafePositivePrice_ValidFormats() {
        // Comas vs Puntos geográficos
        assertEquals(1050.75, DataSanitizer.parseSafePositivePrice("1050.75")!!, 0.001)
        assertEquals(1050.75, DataSanitizer.parseSafePositivePrice(" 1050,75   ")!!, 0.001) // Soporte numérico europeo/LATAM
    }
}
