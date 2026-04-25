package com.sponsorflow.data

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pruebas de Exhaustividad del Type-Safety (KanbanState Sealed Class)
 */
class KanbanStateCompilerTest {

    // Función de prueba que mapea todas las instancias del Sealed Class.
    // Si en el futuro alguien añade un estado "DEVUELTO" a KanbanState
    // pero olvida documentarlo aquí o en el UI, el IDE de Kotlin y el Compilador
    // LANZARÁN UN ERROR porque la cláusula `when` dejará de ser exhaustiva sin un bloque `else`.
    // Esto es magia pura del tipado para prevenir NoSuchElementExceptions en Runtime.
    private fun renderKanbanStateUi(state: KanbanState): String {
        return when (state) {
            is KanbanState.ContactoInicial -> "Se renderiza como Nuevo"
            is KanbanState.Negociando -> "Se renderiza Ambar"
            is KanbanState.CasiCierre -> "Se renderiza Naranja"
            is KanbanState.Compro -> "Se renderiza Verde"
            is KanbanState.Perdido -> "Se renderiza Rojo"
            // ¡Alerta! Comentado intencionalmente: Si fuera un String libre (Ej. "DEVOLUCION"), 
            // no podríamos atraparlo aquí y las interfaces crashearían de forma asintomática.
            // Con Sealed Classes, logramos Control en Tiempo de Compilación.
        }
    }

    @Test
    fun testKanbanState_ExhaustiveResolution() {
        val uiOutput = renderKanbanStateUi(KanbanState.Compro)
        assertEquals("Se renderiza Verde", uiOutput)
    }

    @Test
    fun testKanbanState_FallbackResolution() {
        // ¿Qué pasa al inyectar basura por SQLite (o por Fuzzing en una exportación)?
        val corruptStateFromDb = "ESTADO_INVENTADO_POR_HACKER"
        
        // Nuestro companion resolver usará un Fallback en vez de estallar:
        val resolvedState = KanbanState.fromId(corruptStateFromDb)
        
        // Cae elegantemente al default y no rompe UI (Graceful Degradation):
        assertTrue("El estado corrupto debió resolverse como ContactoInicial", resolvedState is KanbanState.ContactoInicial)
    }
}
