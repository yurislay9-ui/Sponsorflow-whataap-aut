package com.sponsorflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad del Catálogo SQLite (Tabla 'products')
 * Aquí se almacenarán las configuraciones y precios del dropshipper.
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val costPrice: Double, // Lo que le cuesta al usuario
    val sellPrice: Double, // A lo que lo vende el bot
    val stockAvailable: Boolean = true
)
