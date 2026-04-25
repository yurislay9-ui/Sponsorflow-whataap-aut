package com.sponsorflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad CRM (V2): Memoria Cognitiva de Largo Plazo.
 * El núcleo del agente para personalizar interacciones y recordar historiales.
 */
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val senderId: String, // El nombre o número en WhatsApp/IG
    val platform: String = "WhatsApp", // WA, WA_BIZ, IG, FB
    val lastInteraction: Long = System.currentTimeMillis(),
    val totalPurchases: Double = 0.0,
    
    // --- NUEVAS COLUMNAS COGNITIVAS (V2) ---
    // Resumen semántico guardado por Qwen (Ej: "Hablar de usted. Interesado en zapatos. Regatea.")
    val perfilCognitivo: String = "Cliente nuevo.",
    
    // Matriz de JSON ligero con productos mencionados o preferencias específicas
    val tags: String = "[]",
    
    // Flag atómico para indicar al WorkManager que este cliente tuvo chats nuevos hoy
    // y necesita que Qwen consolide su memoria de noche.
    val needsMemoryConsolidation: Boolean = false,
    
    // Búfer temporal de chats del día. El WorkManager lo lee, lo resume en perfilCognitivo y lo vacía.
    val unconsolidatedHistory: String = "",
    
    // Estado en el pipeline de ventas CRM (Mapeado a KanbanState Sealed Class)
    val funnelStage: String = "CONTACTO_INICIAL"
)
