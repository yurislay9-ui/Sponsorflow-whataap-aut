package com.sponsorflow.core

/**
 * Contrato estricto para cualquier Motor de IA (Engine) dentro del Core.
 * Desacopla la lógica algorítmica pesada de los agentes.
 * 
 * Todo motor debe recibir un texto normalizado y emitir un resultado estándar "EngineResult"
 * abstrayendo si es un modelo ONNX, un script Llama o Expresiones Regulares.
 */
interface CognitiveEngine<T> {
    val engineName: String
    
    /**
     * Procesa la entrada cruda asíncronamente y retorna un tipo genérico de dominio
     */
    suspend fun analyze(input: String): EngineResult<T>
}

/**
 * Respuesta Estandarizada para todos los Motores (Tipado Fuerte + Manejo de Errores)
 */
sealed class EngineResult<out T> {
    data class Success<out T>(val data: T, val confidence: Float) : EngineResult<T>()
    data class Failure(val errorReason: String, val exception: Exception? = null) : EngineResult<Nothing>()
}
