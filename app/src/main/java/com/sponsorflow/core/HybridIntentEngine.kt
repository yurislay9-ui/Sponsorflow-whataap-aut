package com.sponsorflow.core

import javax.inject.Inject
import javax.inject.Singleton

import android.content.Context
import android.util.Log
import java.util.BitSet
import dagger.hilt.android.qualifiers.ApplicationContext

// ... enum and FSMDatabase definitions ...

/**
 * MÁQUINA DE ESTADOS FINITOS (FSM) DE TONALIDADES E INTENCIONES
 * Operación O(1) por acceso de memoria directa que reemplaza IF/ELSE secuenciales.
 */
enum class IntentCategory { 
    GREETING, 
    SEARCH_CATALOG_POLICY, 
    CODE_ORDER, 
    BLOCKED_SECURITY,
    OBJECTION_PRICE,      // Nueva Intención: El cliente dice que está caro
    OBJECTION_TRUST,      // Nueva Intención: El cliente desconfía
    OBJECTION_THINKING    // Nueva Intención: El cliente dice "lo voy a pensar"
}
enum class ToneCategory { ESTANDAR, ENTUSIASTA, ENOJADO }

object FSMDatabase {
    val matrix = Array(IntentCategory.values().size) {
        Array(ToneCategory.values().size) { emptyArray<String>() }
    }

    init {
        // [GREETING]
        matrix[IntentCategory.GREETING.ordinal][ToneCategory.ESTANDAR.ordinal] = arrayOf("Hola, un placer. ¿En qué te ayudo?")
        matrix[IntentCategory.GREETING.ordinal][ToneCategory.ENTUSIASTA.ordinal] = arrayOf("¡Hola! ¡Qué gusto saludarte! ¿Cómo te asisto?")
        matrix[IntentCategory.GREETING.ordinal][ToneCategory.ENOJADO.ordinal] = arrayOf("Hola, lamento la confusión. Dime, ¿qué ocurrió y lo resolvemos?")

        // [CATALOG / POLICY]
        matrix[IntentCategory.SEARCH_CATALOG_POLICY.ordinal][ToneCategory.ESTANDAR.ordinal] = arrayOf("Claro, te muestro info:")
        matrix[IntentCategory.SEARCH_CATALOG_POLICY.ordinal][ToneCategory.ENTUSIASTA.ordinal] = arrayOf("¡Genial! Aquí lo tienes:")
        matrix[IntentCategory.SEARCH_CATALOG_POLICY.ordinal][ToneCategory.ENOJADO.ordinal] = arrayOf("Entendido. Aquí está la política que solicitas para aclararlo:")

        // [ORDER]
        matrix[IntentCategory.CODE_ORDER.ordinal][ToneCategory.ESTANDAR.ordinal] = arrayOf("✅ He tomado nota de tus datos y tu solicitud para: {var_prod}. En breve un gestor se contactará contigo para confirmar si prefieres recogida en local o envío a domicilio y acordar el pago contra-entrega.")
        matrix[IntentCategory.CODE_ORDER.ordinal][ToneCategory.ENTUSIASTA.ordinal] = arrayOf("🎉 ¡Excelente elección! Ya copié los datos de tu {var_prod}. No tienes que pagar nada por aquí. Enseguida un humano te escribirá para coordinar la entrega o recogida.")
        matrix[IntentCategory.CODE_ORDER.ordinal][ToneCategory.ENOJADO.ordinal] = arrayOf("Tomé nota de tus datos: {var_prod}. Tu registro está asegurado. Un gestor te escribirá a la brevedad para coordinar la mensajería y el método de pago.")
        
        // [SECURITY]
        matrix[IntentCategory.BLOCKED_SECURITY.ordinal][ToneCategory.ESTANDAR.ordinal] = arrayOf("La operación está protegida.")
        matrix[IntentCategory.BLOCKED_SECURITY.ordinal][ToneCategory.ENTUSIASTA.ordinal] = arrayOf("La operación está protegida.")
        matrix[IntentCategory.BLOCKED_SECURITY.ordinal][ToneCategory.ENOJADO.ordinal] = arrayOf("La operación está protegida.")

        // [OBJECTIONS - EL ROMPE OBJECIONES V4.0]
        
        // El cliente dice: "Está muy caro", "no tengo dinero"
        matrix[IntentCategory.OBJECTION_PRICE.ordinal][ToneCategory.ESTANDAR.ordinal] = arrayOf("Entiendo que busques cuidar tu presupuesto. Nuestros productos tienen garantía certificada, por lo que no gastarás dos veces. ¿Te ofrezco un 10% de descuento si cierras hoy?", "Comprendo. Piensa que la calidad de nuestros materiales te asegura una vida útil tres veces mayor. ¿Podríamos ajustarnos si te doy envío gratis?")
        matrix[IntentCategory.OBJECTION_PRICE.ordinal][ToneCategory.ENTUSIASTA.ordinal] = arrayOf("¡Claro que cuesta, pero lo vale cada centavo! Es una inversión que te durará años. ¡Venga, te regalo el envío si lo pedimos ya!", "¡Entiendo el punto! Pero créeme que al tenerlo en tus manos notarás la diferencia de calidad. ¿Hacemos el trato con un 10% OFF hoy?")
        matrix[IntentCategory.OBJECTION_PRICE.ordinal][ToneCategory.ENOJADO.ordinal] = arrayOf("Comprendo tu postura sobre el precio y respeto que busques opciones. Nuestra calidad es premium y por ello el costo. Si decides volver, aquí estaremos para atenderte.", "Entiendo perfectamente si prefieres algo más económico por ahora. Si en el futuro requieres durabilidad garantizada, somos tu mejor opción.")

        // El cliente dice: "es una estafa?", "no confio", "como se que llega"
        matrix[IntentCategory.OBJECTION_TRUST.ordinal][ToneCategory.ESTANDAR.ordinal] = arrayOf("Es muy normal desconfiar comprando en línea. Te comparto nuestro link de testimonios y guías de rastreo reales. ¿Te gustaría ver un video del producto antes de enviarlo?", "Entiendo tus dudas. Para tu tranquilidad, pago contra entrega está disponible en tu zona. ¿Te lo envío así?")
        matrix[IntentCategory.OBJECTION_TRUST.ordinal][ToneCategory.ENTUSIASTA.ordinal] = arrayOf("¡Es normal tener dudas! Pero mira nuestro Instagram, tenemos más de 500 clientes felices etiquetados. ¡Te aseguro que te va a encantar cuando te llegue!", "¡No te preocupes! Tenemos 5 años en el mercado y cientos de envíos exitosos. ¡Podemos hacer videollamada si quieres ver el producto!")
        matrix[IntentCategory.OBJECTION_TRUST.ordinal][ToneCategory.ENOJADO.ordinal] = arrayOf("Entiendo tu molestia y desconfianza. Lamento si algo te dio esa impresión. Somos una empresa registrada. Si prefieres no avanzar, lo respeto absolutamente.", "Lamento la desconfianza genuinamente. Nuestra reputación es intachable. Te invito a leer los comentarios de nuestros clientes en las fotos.")

        // El cliente dice: "lo voy a pensar", "luego te aviso"
        matrix[IntentCategory.OBJECTION_THINKING.ordinal][ToneCategory.ESTANDAR.ordinal] = arrayOf("¡Por supuesto, piénsalo con calma! Solo ten en cuenta que de este modelo nos quedan muy pocas unidades. Avísame cualquier duda.", "Claro que sí, tómate tu tiempo. ¿Hay alguna duda puntual sobre garantías o envío que te esté deteniendo?")
        matrix[IntentCategory.OBJECTION_THINKING.ordinal][ToneCategory.ENTUSIASTA.ordinal] = arrayOf("¡Piénsalo tranquilo! Pero no te tardes mucho que vuelan de nuestro inventario 🔥. ¡Escríbeme cuando estés listo!", "¡Claro! Guárdame en tus contactos y cuando decidas, aquí estaré. ¡Solo recuerda que el descuento actual vence mañana!")
        matrix[IntentCategory.OBJECTION_THINKING.ordinal][ToneCategory.ENOJADO.ordinal] = arrayOf("Claro, tómate el tiempo necesario para evaluarlo. Estaremos aquí cuando lo decidas.", "Entiendo. Quedamos a tu disposición para cualquier consulta futura cuando estés listo.")
    }
}

/**
 * ESTRUCTURA DE AHO-CORASICK EXTREMADAMENTE LIGERA
 * O(n) sobre la longitud de entrada. Reemplaza Regex masivos.
 */
class SimpleAhoCorasick(val dictionary: Map<String, IntentCategory>) {
    // Simplificación extrema: Buscará patrones en O(n) cruzando el mapa eficientemente.
    fun search(text: String): IntentCategory? {
        val normalized = " \$text " // Padding 
        for ((keyword, intent) in dictionary) {
            if (normalized.contains(" \$keyword ")) return intent
        }
        return null
    }
}

/**
 * GUARDIÁN NIVEL 0: FILTRO DE BLOOM (BitSet Ultra Rápido)
 */
class OptimizationFilter(size: Int = 1024) {
    private val bitset = BitSet(size)
    private val mask = size - 1
    
    fun addPattern(word: String) {
        val h = word.hashCode()
        bitset.set(h and mask)
        bitset.set((h shr 16) and mask)
    }
    
    fun mightContain(word: String): Boolean {
        val h = word.hashCode()
        return bitset.get(h and mask) && bitset.get((h shr 16) and mask)
    }
}

/**
 * EL CEREBRO HÍBRIDO 4-NIVELES INTENT ENGINE (Mythos v4.0 Ultra-Optimizado)
 */
@Singleton
class HybridIntentEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    val onnxEngine: ONNXSemanticEngine
) : CognitiveEngine<IntentAnalysisResult> {
    override val engineName: String = "HybridIntentEngine"
    
    private const val TAG = "NEXUS_HybridEngine"
    
    // LRU Cache Absoluto para intenciones repetitivas (e.g., "Hola", "precio")
    private val intentCache = object : java.util.LinkedHashMap<String, IntentAnalysisResult>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, IntentAnalysisResult>): Boolean {
            return size > 250 // Más grande que el embedding porque ocupa menos memoria (solo data class)
        }
    }
    
    // Limpieza externa (llamado por el OS en onLowMemory)
    fun purgeMemory() {
        synchronized(intentCache) { intentCache.clear() }
        onnxEngine.clearCache()
        Log.i(TAG, "🧹 Caché del Motor Híbrido purgada a 0 bytes.")
    }

    private val bloomFilter = OptimizationFilter(2048)
    
    private val ahoDict = mapOf(
        "comprar" to IntentCategory.CODE_ORDER,
        "pedido" to IntentCategory.CODE_ORDER,
        "direccion" to IntentCategory.CODE_ORDER,
        "catalogo" to IntentCategory.SEARCH_CATALOG_POLICY,
        "precio" to IntentCategory.SEARCH_CATALOG_POLICY,
        "politica" to IntentCategory.SEARCH_CATALOG_POLICY,
        
        // EL ROMPE OBJECIONES (Fase de Detección)
        "caro" to IntentCategory.OBJECTION_PRICE,
        "dinero" to IntentCategory.OBJECTION_PRICE,
        "rebaja" to IntentCategory.OBJECTION_PRICE,
        "estafa" to IntentCategory.OBJECTION_TRUST,
        "confianza" to IntentCategory.OBJECTION_TRUST,
        "llega" to IntentCategory.OBJECTION_TRUST,
        "pensar" to IntentCategory.OBJECTION_THINKING,
        "aviso" to IntentCategory.OBJECTION_THINKING,
        "luego" to IntentCategory.OBJECTION_THINKING
    )
    private val ahoCorasick = SimpleAhoCorasick(ahoDict)
    
    // Modelamos un Hash Estadístico SimHash de catálogo
    private val catalogSimHashBase: Long = onnxEngine.fastSimHash("catalogo precio venta informacion modelo")

    init {
        ahoDict.keys.forEach { bloomFilter.addPattern(it) }
    }

    override suspend fun analyze(input: String): EngineResult<IntentAnalysisResult> {
        try {
            val result = analyzeQuery(input)
            return EngineResult.Success(result, result.confidence)
        } catch (e: Exception) {
            return EngineResult.Failure("Fallo crítico en el motor híbrido.", e)
        }
    }

    suspend fun analyzeQuery(text: String, userIsAdmin: Boolean = false): IntentAnalysisResult {
        val input = text.lowercase().trim()
        
        synchronized(intentCache) {
            intentCache[input]?.let { 
                Log.d(TAG, "⚡ [LRU Cache] Evitando NLP completo para: '\$input'")
                return it 
            }
        }
        
        val tokens = input.replace(Regex("[^a-z0-9 ]"), "").split(" ")

        // NIVEL 4 (SEGURIDAD INQUEBRANTABLE A PRIORI)
        if (input.contains("hack") || input.contains("database") || input.contains("sudo")) {
            val decision = if (userIsAdmin) "TOOL_SYS_EXEC_ADMIN" else "BLOCKED_SECURITY"
            val res = IntentAnalysisResult(decision, confidence = 1.0f, tone = ToneCategory.ESTANDAR.name)
            synchronized(intentCache) { intentCache[input] = res }
            return res
        }

        // TONE DETECTOR (Fuzzy / Rápido)
        var detectedTone = ToneCategory.ESTANDAR
        val angryHashes = listOf("pesimo".hashCode(), "mal".hashCode(), "estafa".hashCode(), "molesto".hashCode())
        val happyHashes = listOf("excelente".hashCode(), "encanta".hashCode(), "hermoso".hashCode(), "gracias".hashCode())
        for (t in tokens) {
            val h = t.hashCode()
            if (angryHashes.contains(h)) detectedTone = ToneCategory.ENOJADO
            else if (happyHashes.contains(h)) detectedTone = ToneCategory.ENTUSIASTA
        }

        // NIVEL 0: BLOOM FILTER (El Portero del Boliche)
        var passesBloom = false
        for (t in tokens) {
            if (bloomFilter.mightContain(t)) { passesBloom = true; break }
        }

        if (!passesBloom) {
            // Saltamos Aho-Corasick por eficiencia extrema
            val res = IntentAnalysisResult(IntentCategory.GREETING.name, confidence = 0.60f, tone = detectedTone.name)
            synchronized(intentCache) { intentCache[input] = res }
            return res
        }

        // NIVEL 1: AHO-CORASICK (O(n) sobre input) -> Rápido de Verdad
        val fastMatch = ahoCorasick.search(input)
        if (fastMatch != null) {
            val res = IntentAnalysisResult(fastMatch.name, confidence = 0.99f, tone = detectedTone.name)
            synchronized(intentCache) { intentCache[input] = res }
            return res
        }

        // NIVEL 3: SIMHASH O(1) XOR FALLBACK DEL ONNX ENGINE
        val localHash = onnxEngine.fastSimHash(input)
        if (onnxEngine.isSimHashClose(localHash, catalogSimHashBase, 15)) {
            Log.i(TAG, "🧠 [SimHash Match] Intención detectada matemáticamente por bits.")
            val res = IntentAnalysisResult(IntentCategory.SEARCH_CATALOG_POLICY.name, confidence = 0.85f, tone = detectedTone.name)
            synchronized(intentCache) { intentCache[input] = res }
            return res
        }

        val defaultRes = IntentAnalysisResult(IntentCategory.GREETING.name, confidence = 0.90f, tone = detectedTone.name)
        synchronized(intentCache) { intentCache[input] = defaultRes }
        return defaultRes
    }
}

data class IntentAnalysisResult(
    val intentAction: String, 
    val confidence: Float,
    val tone: String 
)
