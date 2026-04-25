package com.sponsorflow.agents.action

import javax.inject.Inject

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import com.sponsorflow.agents.TypedSponsorflowAgent
import com.sponsorflow.models.AgentPayload
import com.sponsorflow.models.AgentResponseData
import com.sponsorflow.models.AgentTask
import com.sponsorflow.models.SquadType
import com.sponsorflow.models.SwarmError
import com.sponsorflow.models.SwarmResult
import com.sponsorflow.models.SwarmTask
import java.io.File
import java.io.FileOutputStream

data class ComposerPayload(
    val appContext: Context,
    val intentCategory: String?,
    val productTitle: String,
    val catalogInfo: String
) : AgentPayload

data class ComposerData(
    val flyerGenerationSkipped: Boolean,
    val generatedFlyerPath: String?
) : AgentResponseData

/**
 * [ACTUALIZACIÓN 2026 - MIGRADO A TIPADO ESTRICTO E INYECCIÓN]
 * COMPOSER AGENT (Action Squadron)
 * 
 * Genera flyers e imágenes promocionales 2D on-the-fly.
 * Utiliza el Canvas nativo de Android. Consumo de RAM y CPU mínimo (milisegundos).
 * Ideal para adjuntar imágenes a las cotizaciones antes de pedir aprobación humana.
 */
class ComposerAgent @Inject constructor() : TypedSponsorflowAgent<ComposerPayload, ComposerData>() {
    private const val TAG = "NEXUS_ComposerAgent"

    override val agentName: String = "ComposerAgent"
    override val squadron: SquadType = SquadType.ACTION
    override val capabilities: List<String> = listOf("image_generation", "canvas_rendering", "flyer_composition")

    override fun mapLegacyTaskToPayload(task: AgentTask): ComposerPayload {
        return ComposerPayload(
            appContext = task.message.context.applicationContext,
            intentCategory = task.metadata?.get("raw_intent_category") as? String,
            productTitle = task.metadata?.get("order_product") as? String ?: "Producto Destacado",
            catalogInfo = task.metadata?.get("catalog_context") as? String ?: "¡Precio Especial!"
        )
    }

    override suspend fun executeTypedInternal(task: SwarmTask<ComposerPayload>): SwarmResult<ComposerData, SwarmError> {
        Log.i(TAG, "🎨 Iniciando composición visual (Flyer Generator)...")

        val intentCategory = task.payload.intentCategory
        
        // Solo dibujamos si detectamos intención de producto, búsqueda o cotización
        if (intentCategory != "CODE_ORDER" && intentCategory != "SEARCH_CATALOG_POLICY") {
            return SwarmResult.Success(
                confidenceScore = 1.0,
                data = ComposerData(flyerGenerationSkipped = true, generatedFlyerPath = null)
            )
        }

        val productTitle = task.payload.productTitle
        val catalogInfo = task.payload.catalogInfo

        try {
            // Ejemplo de Renderización Ultra-Rápida con Android Canvas
            val width = 1080
            val height = 1080
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. Fondo (Branding Base) - En producción podríamos cargar un Asset / Template PNG
            canvas.drawColor(Color.parseColor("#121212")) // Dark Tech Theme

            // 2. Elementos Decorativos
            val paint = Paint()
            paint.isAntiAlias = true
            
            // Un círculo de fondo
            paint.color = Color.parseColor("#3498DB")
            canvas.drawCircle(width / 2f, height / 2f, 400f, paint)

            // 3. Tipografía: Título
            paint.color = Color.WHITE
            paint.textSize = 80f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            
            // Preocuparse de no salir del margen (simple approach)
            canvas.drawText(productTitle.take(25).uppercase(), width / 2f, 200f, paint)

            // 4. Tipografía: Texto Descriptivo
            paint.textSize = 50f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("¡Promoción Disponible Automática!", width / 2f, height - 200f, paint)

            // 5. Cifra/Contexto (sacado del Catalog)
            paint.color = Color.parseColor("#E74C3C") // Rojo para el highlight
            paint.textSize = 70f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(catalogInfo.take(40), width / 2f, height / 2f + 50f, paint)

            // Guardado físico local para compartir luego
            val cachesDir = task.payload.appContext.cacheDir
            val file = File(cachesDir, "flyer_temp_NEXUS.png")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            bitmap.recycle()

            Log.i(TAG, "🖼️ Flyer 2D generado exitosamente en: \${file.absolutePath}")

            // Devolvemos la ruta del flyer en la metadata para el BuddyReviewer y Publisher
            return SwarmResult.Success(
                confidenceScore = 1.0,
                data = ComposerData(
                    flyerGenerationSkipped = false,
                    generatedFlyerPath = file.absolutePath
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "🚨 Error en renderizado del Canvas 2D: \${e.message}")
            return SwarmResult.Failure(SwarmError.InternalException("Error renderizando imagen: \${e.message}"))
        }
    }
}
