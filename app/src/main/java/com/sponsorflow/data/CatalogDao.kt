package com.sponsorflow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object (El Agente SQLite)
 * Funciones asíncronas para extraer e inyectar productos a la memoria del LLM
 */
@Dao
interface CatalogDao {
    // Motor de Búsqueda Rápida Local
    @Query("SELECT * FROM products WHERE name LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%' LIMIT 3")
    suspend fun searchRelevantProducts(searchQuery: String): List<ProductEntity>

    // Obtener catálogo entero resumiendo precios (Para cuando el cliente dice "qué vendes?")
    @Query("SELECT * FROM products WHERE stockAvailable = 1 LIMIT 10")
    suspend fun getAvailableCatalogOverview(): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)
}
