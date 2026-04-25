package com.sponsorflow.agents.cognitive

import javax.inject.Inject

import android.util.Log
import com.sponsorflow.agents.TypedSponsorflowAgent
import com.sponsorflow.models.AgentPayload
import com.sponsorflow.models.AgentResponseData
import com.sponsorflow.models.AgentTask
import com.sponsorflow.models.SquadType
import com.sponsorflow.models.SwarmError
import com.sponsorflow.models.SwarmResult
import com.sponsorflow.models.SwarmTask
import java.util.Calendar

data class UserLearningPayload(
    val inputText: String,
    val senderId: String
) : AgentPayload

data class UserLearningData(
    val prefersShortMessages: Boolean,
    val isNightOwl: Boolean,
    val usesEmojis: Boolean,
    // Add additional fields as maps for compatibility if required
    val learningMetadata: Map<String, Boolean>
) : AgentResponseData

/**
 * [ACTUALIZACIÓN 2026 - MIGRADO A TIPADO ESTRICTO E INYECCIÓN]
 * USER LEARNING AGENT (Escuadrón Cognitivo - El Científico de Datos)
 * 
 * Basado en algoritmos determinísticos y heurísticos para adaptar
 * dinámicamente el comportamiento del bot según los hábitos del usuario,
 * SIN usar modelos pesados. Modela longitud, ritmo horario y estilo.
 */
class UserLearningAgent @Inject constructor() : TypedSponsorflowAgent<UserLearningPayload, UserLearningData>() {
    private const val TAG = "NEXUS_LearningAgent"
    
    override val agentName: String = "UserLearningAgent"
    override val squadron: SquadType = SquadType.COGNITIVE
    override val capabilities: List<String> = listOf("preference_modeling", "adaptive_behavior", "pattern_detection")

    override fun mapLegacyTaskToPayload(task: AgentTask): UserLearningPayload {
        return UserLearningPayload(
            inputText = task.message.text,
            senderId = task.message.sender
        )
    }

    override suspend fun executeTypedInternal(task: SwarmTask<UserLearningPayload>): SwarmResult<UserLearningData, SwarmError> {
        val input = task.payload.inputText
        val senderId = task.payload.senderId
        
        Log.i(TAG, "🧠 Analizando Patrones de Usuario para: \$senderId")
        
        try {
            // Evaluamos patrones implícitos del mensaje actual
            
            // 1. Longitud referida (Si el usuario escribe corto, prefiere respuestas cortas)
            val prefersShortMessages = input.length < 25
            
            // 2. Horario preferido (Timing Profile)
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isNightOwl = currentHour in 20..23 || currentHour in 0..3
            
            // 3. Estilo de interacción (¿Usa emojis? ¿Va al grano?)
            val usesEmojis = input.contains(Regex("[\\x{1F600}-\\x{1F64F}\\x{1F300}-\\x{1F5FF}\\x{1F680}-\\x{1F6FF}\\x{1F1E0}-\\x{1F1FF}]"))
            
            val learningMetadata = mapOf(
                "user_pref_short_text" to prefersShortMessages,
                "user_pref_night_owl" to isNightOwl,
                "user_pref_emojis" to usesEmojis
            )

            if (prefersShortMessages) {
                Log.d(TAG, "📊 Patrón Detectado: El usuario prefiere interacciones directas y cortas.")
            }
            if (isNightOwl) {
                Log.d(TAG, "📊 Patrón Detectado: El usuario es nocturno. Adaptando umbrales de Behaviour Agent.")
            }

            return SwarmResult.Success(
                confidenceScore = 1.0,
                data = UserLearningData(
                    prefersShortMessages = prefersShortMessages,
                    isNightOwl = isNightOwl,
                    usesEmojis = usesEmojis,
                    learningMetadata = learningMetadata
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "🚨 Error en UserLearningAgent: \${e.message}")
            return SwarmResult.Failure(SwarmError.InternalException("Fallo extrayendo patrones."))
        }
    }
}
