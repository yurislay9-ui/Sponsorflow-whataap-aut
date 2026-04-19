package com.sponsorflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Logística: Cuando la IA cierra la venta, guarda los datos aquí 
 * para que el dueño los prepare sin tener que leer el chat entero.
 */
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientName: String,
    val address: String,
    val productDetails: String,
    val totalAmount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING, SHIPPED, DELIVERED
    val providerTicket: String = "" // Etiqueta generada por IA para enviarle al distribuidor
)
