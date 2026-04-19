package com.sponsorflow.core

import androidx.annotation.Keep
import com.sponsorflow.jni.NativeBridges
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * MOTOR LLAMA (El Agente de Texto Avanzado - Multi Skill)
 */
@Keep
object LlamaEngine {
    private const val TAG = "NEXUS_LlamaEngine"
    private var modelPath: String = "/data/user/0/com.sponsorflow/files/models/qwen_0.5b_q4.gguf"

    suspend fun thinkAndReply(sender: String, incomingText: String, catalogContext: String = "", providerTemplate: String = ""): String = withContext(Dispatchers.IO) {
        try {
            val isLoaded = NativeBridges.initLlamaModel(modelPath)
            if (!isLoaded) return@withContext "Hola, falló mi núcleo lógico local. ¿Qué buscas hoy?"

            // Prompt Maestro de Planificación (El Bot planea, el Humano aprueba)
            val prompt = """
                <|system|>
                Eres un encargado de ventas por chat (NUNCA menciones que eres una IA, un bot, o un asistente automatizado. Debes parecer un humano).
                Tus Reglas Base:
                1. Usa EXCLUSIVAMENTE este inventario: $catalogContext
                2. Los precios del catálogo están en CUP (Moneda Nacional Cubana).
                3. Informa al cliente que los métodos de pago aceptados son: Transferencia Bancaria, Pago en efectivo en CUP o Pago en efectivo en USD.
                
                PROTOCOLO DE RECOPILACIÓN Y VALE DE VENTA:
                4. Tu objetivo es obtener amablemente los datos del cliente necesarios para el envío de su compra. TÚ NO COBRAS NI CONFIRMAS PAGOS.
                5. Cuando el cliente desee comprar algo, pídele sus datos (Nombre, Dirección de envío, Producto exacto y Método de pago).
                6. Mantén tus respuestas naturales y cortas.
                
                7. CRÍTICO: Solo cuando el cliente te haya devuelto sus datos listos, extrae esa información y genera tu respuesta de cierre.
                8. AL FINAL de tu charla humana, añade secretamente este formato JSON en una sola línea. En el campo "vale", debes generar estrictamente el ticket usando la "PLANTILLA DEL ALMACÉN" llenando los huecos.
                
                PLANTILLA DEL ALMACÉN:
                $providerTemplate
                
                FORMATO ESTRICTO DEL JSON AL FINAL DE TU MENSAJE:
                JSON_ORDER:{"nombre":"John Doe","direccion":"Direccion Final","producto":"Nombre Exacto","total":0.0,"moneda":"CUP","vale":"AQUI ADJUNTAS EL VALE LLENO Y FORMATEADO REEMPLAZANDO LOS DATOS EN LA PLANTILLA"}
                <|user|>
                Mensaje de $sender: $incomingText
                <|assistant|>
            """.trimIndent()

            val generatedText = NativeBridges.generateText(prompt)
            return@withContext generatedText

        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            return@withContext "Hubo un error de lectura en tu comprobante. ¿Podrías reenviarlo claramente?"
        } finally {
            NativeBridges.releaseLlamaModel()
        }
    }
}
