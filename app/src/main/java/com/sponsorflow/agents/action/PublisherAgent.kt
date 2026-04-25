package com.sponsorflow.agents.action

import javax.inject.Inject

import android.content.Intent
import android.util.Log
import com.sponsorflow.agents.TypedSponsorflowAgent
import com.sponsorflow.models.AgentPayload
import com.sponsorflow.models.AgentResponseData
import com.sponsorflow.models.AgentResult
import com.sponsorflow.models.AgentTask
import com.sponsorflow.models.SquadType
import com.sponsorflow.models.SwarmError
import com.sponsorflow.models.SwarmResult
import com.sponsorflow.models.SwarmTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
data class PublisherPayload(val actionType: String?, val contentToPublish: String?) : AgentPayload
data class PublisherData(val publishStatus: String) : AgentResponseData

/**
 * PUBLISHER AGENT (Escuadrón de Acción - El Publisher)
 * 
 * Sustituye completamente al viejo y acoplado `SocialMediaPublisher`.
 * Recibe una "ActionIntent" validada y se encarga de instanciar los Intents nativos
 * de Android (o llamar al Accesibility Mimic en Fases Posteriores).
 * Es un agente seguro contra fallos del Sistema Operativo.
 */
class PublisherAgent @Inject constructor() : TypedSponsorflowAgent<PublisherPayload, PublisherData>() {
    private const val TAG = "NEXUS_PublisherAgent"
    
    override val agentName: String = "PublisherAgent"
    override val squadron: SquadType = SquadType.ACTION
    override val capabilities: List<String> = listOf("social_media_posting", "ui_automation_wrapper")

    override fun mapLegacyTaskToPayload(task: AgentTask): PublisherPayload {
        return PublisherPayload(
            actionType = task.proposedAction?.type,
            contentToPublish = task.proposedAction?.payload
        )
    }

    override suspend fun executeTypedInternal(task: SwarmTask<PublisherPayload>): SwarmResult<PublisherData, SwarmError> {
        return withContext(Dispatchers.IO) {
            try {
                val actionType = task.payload.actionType
                val contentToPublish = task.payload.contentToPublish
                
                // Defensa contra acciones equivocadas (Safety Net)
                if (actionType != "PUBLISH_POST" || contentToPublish == null) {
                    return@withContext SwarmResult.Failure(SwarmError.InternalException("Acción vacía o tipo incorrecto"))
                }

                val context = task.message.context

                Log.i(TAG, "📸 PublisherAgent iniciando secuencia de publicación limpia...")
                
                // SISTEMA ANTI-CRASH (SRE):
                // Si enviamos un intent y el usuario desinstaló Instagram o Facebook,
                // las apps monolíticas crashean con un "ActivityNotFoundException".
                // Este agente primero verifica la resolución segura del paquete.
                
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, contentToPublish)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val chooser = Intent.createChooser(shareIntent, "Sponsorflow: Selecciona red social").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                // Comprobamos si Android tiene alguna app capaz de manejar esta publicación
                if (shareIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(chooser)
                    Log.i(TAG, "✅ Panel delegado al OS (Modo Seguro).")
                    
                    SwarmResult.Success(PublisherData("intent_fired"), 1.0)
                } else {
                    Log.w(TAG, "⚠️ No hay aplicaciones disponibles para manejar la publicación. Abortando sin Crash.")
                    SwarmResult.Failure(SwarmError.InternalException("No hay app para manejar el Intent"))
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Error crítico protegido en PublisherAgent: ${e.message}")
                SwarmResult.Failure(SwarmError.InternalException("Excepción en PublisherAgent"))
            }
        }
    }
}
