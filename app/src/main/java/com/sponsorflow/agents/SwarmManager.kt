package com.sponsorflow.agents

import javax.inject.Inject
import javax.inject.Singleton

import android.util.Log
import com.sponsorflow.agents.action.CommsAgent
import com.sponsorflow.agents.action.PublisherAgent
import com.sponsorflow.agents.cognitive.CatalogAgent
import com.sponsorflow.agents.cognitive.GreetingAgent
import com.sponsorflow.agents.cognitive.OrderParsingAgent
import com.sponsorflow.agents.cognitive.PolicyAgent
import com.sponsorflow.agents.cognitive.SynthesizerAgent
import com.sponsorflow.agents.direction.BuddyReviewerAgent
import com.sponsorflow.agents.direction.RouterAgent
import com.sponsorflow.agents.kairos.PrivacyAgent
import com.sponsorflow.models.AgentResult
import com.sponsorflow.models.AgentTask
import com.sponsorflow.models.InboundMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import java.util.UUID

/**
 * Evento de trazabilidad en tiempo real para Observabilidad (El "Radar" del Swarm).
 */
data class SwarmTraceEvent(
    val traceId: String,
    val nodeName: String,
    val status: String,
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Estados estrictos de la ejecución del Swarm.
 */
enum class SwarmStateStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    FAILED,
    ABORTED
}

/**
 * Contexto global de ejecución para un único mensaje viajando por el pipeline.
 */
data class SwarmExecutionContext(
    val initialMessage: InboundMessage,
    var currentMessage: InboundMessage,
    val metadata: com.sponsorflow.models.SwarmMetadata = com.sponsorflow.models.SwarmMetadata(),
    var status: SwarmStateStatus = SwarmStateStatus.IDLE,
    val executionLog: MutableList<String> = mutableListOf(),
    var selectedAction: com.sponsorflow.models.ActionIntent? = null
)

/**
 * [ACTUALIZACIÓN 2026 - SWARM OMNICANAL REACCTIVO (DI HILT)]
 * SWARM MANAGER (El Cuartel General V4.5 - Channel Pipeline Orchestrator)
 *
 * Implementa un modelo de Productor-Consumidor usando Channels para evitar 
 * "Thread Starvation" y permitir el procesamiento multi-mensaje simultáneo (Cero Fricción).
 */
@Singleton
class SwarmManager @Inject constructor(
    private val agentRegistry: Map<String, @JvmSuppressWildcards SponsorflowAgent>,
    private val privacyAgent: PrivacyAgent,
    private val routerAgent: RouterAgent,
    private val synthesizerAgent: SynthesizerAgent,
    private val composerAgent: com.sponsorflow.agents.action.ComposerAgent,
    private val reviewerAgent: BuddyReviewerAgent,
    private val behaviorAgent: com.sponsorflow.agents.action.BehaviorSimulationAgent,
    private val commsAgent: CommsAgent,
    private val reasoningAgent: com.sponsorflow.agents.cognitive.ReasoningAgent
) {
    private val TAG = "NEXUS_SwarmManager"
    
    // El hilo maestro supervisor de nuestro Enjambre
    private val swarmScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Radar de Observabilidad (Caja de Cristal)
    private val _telemetry = MutableSharedFlow<SwarmTraceEvent>(replay = 50, extraBufferCapacity = 100)
    val telemetry = _telemetry.asSharedFlow()
    
    // UI Ready Radar
    private val _radarLogs = MutableStateFlow<List<SwarmTraceEvent>>(emptyList())
    val radarLogs = _radarLogs.asStateFlow()

    // 📥 Tubería (Channel) de Recepción de Mensajes (Buffer Ilimitado para picos de tráfico)
    private val inboundChannel = Channel<SwarmExecutionContext>(Channel.BUFFERED)

    init {
        swarmScope.launch {
            _telemetry.collect { trace ->
                val current = _radarLogs.value.toMutableList()
                current.add(0, trace)
                if (current.size > 200) current.removeLast()
                _radarLogs.value = current
            }
        }
        
        // Encender la Máquina de Procesamiento del Pipeline Principal (Escucha Eterna)
        swarmScope.launch {
            startSwarmPipeline()
        }
    }

    fun emitTrace(traceId: String, nodeName: String, status: String, details: String = "") {
        _telemetry.tryEmit(SwarmTraceEvent(traceId, nodeName, status, details))
    }

    /**
     * Válvula de entrada externa. Única vía para empujar datos al Swarm.
     */
    fun onNewMessageReceived(message: InboundMessage) {
        val traceId = UUID.randomUUID().toString().take(8)
        emitTrace(traceId, "SwarmManager", "INBOUND_RECEIVED", "Nuevo mensaje detectado de ${message.sender}")

        val context = SwarmExecutionContext(
            initialMessage = message,
            currentMessage = message,
            status = SwarmStateStatus.RUNNING
        )
        context.metadata.traceId = traceId
        
        // Empujamos asíncronamente a la boca de la Tubería Principal
        inboundChannel.trySend(context)
    }

    /**
     * Motor de orquestación (Channels Pipeline). 
     * Cada fase se auto-suspende sin matar los hilos subyacentes operacionales de Android.
     */
    private suspend fun startSwarmPipeline() = coroutineScope {
        Log.i(TAG, "🟢 Swarm Pipeline Engine activado en Background.")

        // Fase 1: Privacy -> Router -> Paralelismo
        val routedFlow = processPrivacyAndRouter(inboundChannel)
        
        // Fase 2: Paralelismo Intra-Nodo -> Synthesizer
        val synthesisFlow = processCognitiveAndSynthesis(routedFlow)
        
        // Fase 3: Composer -> Auditoría -> Delivery -> DB
        val terminalFlow = processComposerAuditAndDelivery(synthesisFlow)

        // Sumergidero (Sumidero Final)
        for (context in terminalFlow) {
            val traceId = context.metadata.traceId
            if (context.status == SwarmStateStatus.RUNNING) {
                context.status = SwarmStateStatus.COMPLETED
            }
            emitTrace(traceId, "SwarmManager", "SWARM_FINISHED", "Estado final: ${context.status}")
            Log.i(TAG, "🏁 Ejecución Workflow finalizada. Estado: ${context.status}")
        }
    }

    private fun CoroutineScope.processPrivacyAndRouter(input: ReceiveChannel<SwarmExecutionContext>) = produce {
        for (ctx in input) {
            val traceId = ctx.metadata.traceId.ifEmpty { "anon" }
            try {
                // 1. Privacy
                emitTrace(traceId, "PrivacyMiddleware", "STARTING")
                val privacyPayload = com.sponsorflow.agents.kairos.PrivacyPayload(ctx.currentMessage.text)
                val safeText = when (val res = privacyAgent.executeTypedTask(com.sponsorflow.models.SwarmTask("p-\$traceId", ctx.currentMessage, privacyPayload))) {
                    is com.sponsorflow.models.SwarmResult.Success -> res.data.safeText
                    else -> ctx.currentMessage.text
                }
                ctx.currentMessage = ctx.currentMessage.copy(text = safeText)
                emitTrace(traceId, "PrivacyMiddleware", "COMPLETED")

                // 2. Router
                emitTrace(traceId, "RouterDecision", "STARTING")
                val routerPayload = com.sponsorflow.agents.direction.RouterPayload(ctx.currentMessage.text, ctx.currentMessage.context.applicationContext)
                when (val routeResult = routerAgent.executeTypedTask(com.sponsorflow.models.SwarmTask("r-\$traceId", ctx.currentMessage, routerPayload))) {
                    is com.sponsorflow.models.SwarmResult.Success -> {
                        val data = routeResult.data
                        if (data.routedTo.isEmpty() || data.routedTo.contains("CommandHandlerAgent")) {
                            ctx.status = SwarmStateStatus.ABORTED // Bypass de Terminal ignorado para ahorrar codigo aquí
                            send(ctx)
                            continue
                        }
                        ctx.metadata.customerTone = data.customerTone
                        ctx.metadata.rawIntentCategory = data.rawIntentCategory
                        ctx.metadata.routedNodes = data.routedTo
                        emitTrace(traceId, "RouterDecision", "COMPLETED")
                        send(ctx) // Pasa a Siguiente Fase
                    }
                    else -> {
                        ctx.status = SwarmStateStatus.ABORTED
                        send(ctx)
                    }
                }
            } catch(e: Exception) {
                ctx.status = SwarmStateStatus.FAILED
                send(ctx)
            }
        }
    }

    private fun CoroutineScope.processCognitiveAndSynthesis(input: ReceiveChannel<SwarmExecutionContext>) = produce {
        for (ctx in input) {
            if (ctx.status != SwarmStateStatus.RUNNING) { send(ctx); continue }
            val traceId = ctx.metadata.traceId.ifEmpty { "anon" }
            
            // Cognitive Parallel Layers
            emitTrace(traceId, "CognitiveParallelLayer", "STARTING")
            val routedAgents = ctx.metadata.routedNodes
            
            // Fan-Out: Multi-hilado asíncrono gestionado por Channels (Evitando awaitAll y Thread Starvation)
            val cognitiveChannel = Channel<AgentResult>(Channel.BUFFERED)
            var activeAgentsCount = 0

            routedAgents.forEach { agentName ->
                val agent = agentRegistry[agentName]
                if (agent != null) {
                    activeAgentsCount++
                    launch {
                        try {
                            val result = agent.executeTask(AgentTask(ctx.currentMessage, ctx.metadata.toLegacyMap()))
                            cognitiveChannel.send(result)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error en agente cognitivo \$agentName: \${e.message}")
                            cognitiveChannel.send(AgentResult.Failure(e.message ?: "Unknown Error"))
                        }
                    }
                }
            }

            // Fan-In: Recolectamos resultados conforme terminan, tubería 100% suspendible
            repeat(activeAgentsCount) {
                val result = cognitiveChannel.receive()
                if (result is AgentResult.Success && result.extractedData != null) {
                    ctx.metadata.importLegacyMap(result.extractedData)
                }
            }
            cognitiveChannel.close()
            emitTrace(traceId, "CognitiveParallelLayer", "COMPLETED")
            
            // Synthesis
            emitTrace(traceId, "SynthesizerPhase", "STARTING")
            val synthesizerPayload = com.sponsorflow.agents.cognitive.SynthesizerPayload(ctx.metadata.toLegacyMap())
            when (val finalResult = synthesizerAgent.executeTypedTask(com.sponsorflow.models.SwarmTask("s-\$traceId", ctx.currentMessage, synthesizerPayload))) {
                is com.sponsorflow.models.SwarmResult.Success -> {
                    if (finalResult.data.proposedAction != null) {
                        ctx.selectedAction = finalResult.data.proposedAction
                        emitTrace(traceId, "SynthesizerPhase", "COMPLETED")
                    } else { ctx.status = SwarmStateStatus.FAILED }
                }
                else -> ctx.status = SwarmStateStatus.FAILED
            }
            send(ctx) // Pasa a Siguiente Fase
        }
    }

    private fun CoroutineScope.processComposerAuditAndDelivery(input: ReceiveChannel<SwarmExecutionContext>) = produce {
        for (ctx in input) {
            if (ctx.status != SwarmStateStatus.RUNNING) { send(ctx); continue }
            val traceId = ctx.metadata.traceId.ifEmpty { "anon" }
            
            // Composer (Opcional - Errores se tragan silenciosamente)
            val composerPayload = com.sponsorflow.agents.action.ComposerPayload(
                appContext = ctx.currentMessage.context.applicationContext,
                intentCategory = ctx.metadata.rawIntentCategory,
                productTitle = ctx.metadata.orderProduct ?: "Producto Destacado",
                catalogInfo = ctx.metadata.catalogContext ?: "¡Precio Especial!"
            )
            val composerRes = composerAgent.executeTypedTask(com.sponsorflow.models.SwarmTask("c-\$traceId", ctx.currentMessage, composerPayload))
            if (composerRes is com.sponsorflow.models.SwarmResult.Success && composerRes.data.generatedFlyerPath != null) {
                ctx.metadata.generatedFlyerPath = composerRes.data.generatedFlyerPath
            }

            // Auditoria
            emitTrace(traceId, "AuditPhase", "STARTING")
            val proposedAction = ctx.selectedAction
            if (proposedAction == null) {
                ctx.status = SwarmStateStatus.FAILED
                send(ctx)
                continue
            }
            
            // 🚀 ACTUALIZACIÓN: Inyectado
            val reviewerPayload = com.sponsorflow.agents.direction.BuddyReviewerPayload(
                clientName = ctx.initialMessage.sender.split(":").lastOrNull() ?: "amigo",
                hasGeneratedImage = ctx.metadata.generatedFlyerPath != null,
                isSalesClosing = ctx.metadata.rawIntentCategory == "CODE_ORDER" || ctx.metadata.orderReady,
                originalAction = proposedAction
            )
            val reviewRes = reviewerAgent.executeTypedTask(com.sponsorflow.models.SwarmTask("a-\$traceId", ctx.currentMessage, reviewerPayload))
            if (reviewRes is com.sponsorflow.models.SwarmResult.Success && reviewRes.data.verifiedAction != null) {
                ctx.selectedAction = reviewRes.data.verifiedAction
                emitTrace(traceId, "AuditPhase", "COMPLETED")
            } else {
                ctx.status = SwarmStateStatus.ABORTED
                emitTrace(traceId, "AuditPhase", "ABORTED")
                send(ctx)
                continue
            }

            // Delivery Físico
            emitTrace(traceId, "DeliveryPhase", "STARTING")
            val behaviorPayload = com.sponsorflow.agents.action.BehaviorPayload(
                inputLength = ctx.currentMessage.text.length,
                isFirstMessageOfDay = false, // Podría sacarse del DB si fuera necesario
                isNightOwl = ctx.metadata.userPrefEmojis // Placeholder por compatibilidad
            )
            behaviorAgent.executeTypedTask(com.sponsorflow.models.SwarmTask("b-\$traceId", ctx.currentMessage, behaviorPayload))
            
            val commsPayload = com.sponsorflow.agents.action.CommsPayload(
                textToSend = ctx.selectedAction?.payload,
                messageContext = ctx.currentMessage
            )
            val commsTask = com.sponsorflow.models.SwarmTask("delivery-\$traceId", ctx.currentMessage, commsPayload)
            val commsRes = commsAgent.executeTypedTask(commsTask)
            
            if (commsRes is com.sponsorflow.models.SwarmResult.Failure) { // Fallback Recovery
                val errorReason = when(val err = commsRes.error) {
                    is com.sponsorflow.models.SwarmError.InternalException -> err.reason
                    is com.sponsorflow.models.SwarmError.LlmTimeout -> "Timeout \${err.ms}ms"
                    is com.sponsorflow.models.SwarmError.InvalidSchema -> "Invalid Schema"
                    is com.sponsorflow.models.SwarmError.HumanInterventionRequired -> err.reason
                }
                ctx.metadata.lastError = errorReason
                val reasoningPayload = com.sponsorflow.agents.cognitive.ReasoningPayload(
                    previousError = errorReason,
                    failedStep = "DeliveryPhase"
                )
                val recoveryResult = reasoningAgent.executeTypedTask(com.sponsorflow.models.SwarmTask("rec-\$traceId", ctx.currentMessage, reasoningPayload))
                if (recoveryResult is com.sponsorflow.models.SwarmResult.Success && recoveryResult.data.proposedAction != null) {
                    ctx.selectedAction = recoveryResult.data.proposedAction
                    val retryPayload = com.sponsorflow.agents.action.CommsPayload(
                        textToSend = ctx.selectedAction?.payload,
                        messageContext = ctx.currentMessage
                    )
                    commsAgent.executeTypedTask(com.sponsorflow.models.SwarmTask("retry-\$traceId", ctx.currentMessage, retryPayload))
                } else {
                    ctx.status = SwarmStateStatus.FAILED
                    send(ctx)
                    continue
                }
            }
            emitTrace(traceId, "DeliveryPhase", "COMPLETED")
            
            // Persistencia (Final)
            emitTrace(traceId, "PersistencePhase", "STARTING")
            try {
                val db = com.sponsorflow.data.SponsorflowDatabase.getDatabase(ctx.initialMessage.context)
                val dao = db.businessDao()
                dao.saveCustomerIfNotExists(com.sponsorflow.data.CustomerEntity(ctx.initialMessage.sender, "UNKNOWN"))
                dao.appendChatHistory(ctx.initialMessage.sender, "Customer: \${ctx.initialMessage.text}", System.currentTimeMillis())
                ctx.selectedAction?.payload?.let { 
                    dao.appendChatHistory(ctx.initialMessage.sender, "NEXUS: \$it", System.currentTimeMillis() + 1)
                }
                emitTrace(traceId, "PersistencePhase", "COMPLETED")
            } catch (e: Exception) {
                Log.e(TAG, "Persistence fail: \${e.message}")
            }
            send(ctx)
        }
    }
}
