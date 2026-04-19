package com.sponsorflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad CRM: Memorización de todos los clientes que nos contactan.
 * Útil para la función "Campaña de Goteo" (Marketing).
 */
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val senderId: String, // El nombre o número en WhatsApp/IG
    val lastInteraction: Long = System.currentTimeMillis(),
    val totalPurchases: Double = 0.0,
    val platform: String = "WhatsApp" // Puede ser IG o FB
)
