package com.sponsorflow.agents.direction

import javax.inject.Inject

import android.util.Log
import com.sponsorflow.agents.TypedSponsorflowAgent
import com.sponsorflow.models.ActionIntent
import com.sponsorflow.models.AgentPayload
import com.sponsorflow.models.AgentResponseData
import com.sponsorflow.models.AgentTask
import com.sponsorflow.models.SquadType
import com.sponsorflow.models.SwarmError
import com.sponsorflow.models.SwarmResult
import com.sponsorflow.models.SwarmTask

data class CommandHandlerPayload(
    val rawCommand: String
) : AgentPayload

data class CommandHandlerData(
    val parsedCommand: String,
    val parsedArgs: Map<String, String>,
    val proposedAction: ActionIntent?
) : AgentResponseData

/**
 * [ACTUALIZACIÓN 2026 - MIGRADO A TIPADO ESTRICTO E INYECCIÓN]
 * COMMAND HANDLER AGENT (La Terminal de Intenciones - Nivel Dios)
 * 
 * Intercepta comandos crudos estilo CLI (> publish --platform ig) generados 
 * por el dueño del dispositivo o por subsistemas internos (como el RetargetingAgent).
 * Traduce estos comandos de alto nivel a instrucciones biomecánicas para la Mano Fantasma.
 */
class CommandHandlerAgent @Inject constructor() : TypedSponsorflowAgent<CommandHandlerPayload, CommandHandlerData>() {
    private const val TAG = "NEXUS_CommandHandler"

    override val agentName: String = "CommandHandlerAgent"
    override val squadron: SquadType = SquadType.DIRECTION
    override val capabilities: List<String> = listOf("cli_parsing", "tactical_orchestration")

    override fun mapLegacyTaskToPayload(task: AgentTask): CommandHandlerPayload {
        return CommandHandlerPayload(rawCommand = task.message.text.trim())
    }

    override fun extractLegacyProposedAction(data: CommandHandlerData): ActionIntent? {
        return data.proposedAction
    }

    override suspend fun executeTypedInternal(task: SwarmTask<CommandHandlerPayload>): SwarmResult<CommandHandlerData, SwarmError> {
        val rawCommand = task.payload.rawCommand
        
        Log.i(TAG, "💻 [TERMINAL DE INTENCIONES] Procesando directiva: \$rawCommand")
        
        // PARSER DE FUERZA BRUTA (Cero IA, split matemático)
        // Ejemplo esperado: > publish --platform ig --caption "Oferta!" --user "valeria_99"
        
        try {
            val parts = rawCommand.split(" ")
            if (parts.size < 2) return SwarmResult.Failure(SwarmError.InternalException("Falta baseCommand en Terminal"))

            val baseCommand = parts[1] // Ignoramos el ">" en index 0
            
            val args = mutableMapOf<String, String>()
            var currentKey = ""
            var currentValue = StringBuilder()
            
            for (i in 2 until parts.size) {
                val token = parts[i]
                if (token.startsWith("--")) {
                    if (currentKey.isNotEmpty()) {
                        args[currentKey] = currentValue.toString().trim().removeSurrounding("\"")
                        currentValue.clear()
                    }
                    currentKey = token.removePrefix("--")
                } else {
                    currentValue.append("\$token ")
                }
            }
            if (currentKey.isNotEmpty()) {
                args[currentKey] = currentValue.toString().trim().removeSurrounding("\"")
            }

            Log.d(TAG, "Instrucciones Parseadas: Comando=[\$baseCommand], Argumentos=\$args")

            // ENSAMBLAJE DE LA MISIÓN PARA LA MANO FANTASMA (AccessibilityMimic)
            val actionType = when (baseCommand) {
                "publish" -> "MIMIC_PUBLISH_STORY"
                "reply" -> "MIMIC_REPLY_COMMENT"
                "dm" -> "MIMIC_SEND_DM"
                else -> "UNKNOWN_COMMAND"
            }
            
            if (actionType == "UNKNOWN_COMMAND") {
                Log.w(TAG, "⚠️ Comando de terminal no soportado: \$baseCommand")
                return SwarmResult.Failure(SwarmError.InternalException("Comando desconocido: \$baseCommand"))
            }

            // Encapsulamos todas las variables en un Serializado para enviárselo 
            // al Accessibility Mimic mediante un ActionIntent
            val payloadData = args.map { "\${it.key}:::\${it.value}" }.joinToString("|||")

            return SwarmResult.Success(
                confidenceScore = 1.0,
                data = CommandHandlerData(
                    parsedCommand = baseCommand, 
                    parsedArgs = args,
                    proposedAction = ActionIntent(type = actionType, payload = payloadData)
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error de sintaxis en la Terminal de Intenciones: \${e.message}")
            return SwarmResult.Failure(SwarmError.InternalException("Error parseando comando: \${e.message}"))
        }
    }
}
