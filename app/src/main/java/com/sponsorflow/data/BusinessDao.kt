package com.sponsorflow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Controlador de Analíticas, CRM y Logística
 */
@Dao
interface BusinessDao {
    // ---- CRM ----
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun saveCustomerIfNotExists(customer: CustomerEntity)

    // Actualiza la marca de consolidación y tiempo, y anexa el nuevo chat al búfer del día
    // SRE Guard: Se usa substr() para forzar un Rolling Buffer y evitar el crash por CursorWindow (>2MB por fila de SQLite)
    // Amnesia Controlada: Reducido exponencialmente de 150000 a 4000 caracteres para preservar la RAM
    @Query("UPDATE customers SET unconsolidatedHistory = substr(unconsolidatedHistory || '\n' || :chatLine, -4000), needsMemoryConsolidation = 1, lastInteraction = :timestamp WHERE senderId = :senderId")
    suspend fun appendChatHistory(senderId: String, chatLine: String, timestamp: Long)

    // Agregamos inyección directa del CRM a Orquestador
    @Query("SELECT * FROM customers WHERE senderId = :senderId LIMIT 1")
    suspend fun getCustomerById(senderId: String): CustomerEntity?

    // Para el Turno de Noche (WorkManager)
    @Query("SELECT * FROM customers WHERE needsMemoryConsolidation = 1")
    suspend fun getCustomersNeedingConsolidation(): List<CustomerEntity>

    // Para el Turno de Noche Mecánico (KAIROS Daemon)
    @Query("UPDATE customers SET perfilCognitivo = :nuevoPerfil, tags = :nuevosTags, unconsolidatedHistory = '', needsMemoryConsolidation = 0 WHERE senderId = :senderId")
    suspend fun commitMechanicalMemory(senderId: String, nuevoPerfil: String, nuevosTags: String)

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomers(): List<CustomerEntity>

    // Flujos reactivos para la UI (Dashboard CRM)
    @Query("SELECT * FROM customers ORDER BY lastInteraction DESC")
    fun getAllCustomersFlow(): kotlinx.coroutines.flow.Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE funnelStage IN ('NEGOCIANDO', 'CASI_CIERRE') AND lastInteraction < :timeThreshold")
    suspend fun getGhostedCustomers(timeThreshold: Long): List<CustomerEntity>

    @Query("UPDATE customers SET funnelStage = :stage WHERE senderId = :senderId")
    suspend fun updateFunnelStage(senderId: String, stage: String)

    // Actualización manual (Override Humano) desde el panel de control
    @Query("UPDATE customers SET perfilCognitivo = :perfil, tags = :tags, funnelStage = :funnelStage WHERE senderId = :senderId")
    suspend fun manuallyUpdateCustomer(senderId: String, perfil: String, tags: String, funnelStage: String)

    @Query("SELECT COUNT(*) FROM customers")
    fun getTotalInteractionsFlow(): Flow<Int>

    // ---- LOGÍSTICA ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOrder(order: OrderEntity)

    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getPendingOrdersFlow(): Flow<List<OrderEntity>>
    
    @Query("SELECT * FROM orders")
    suspend fun getAllPendingOrdersStatic(): List<OrderEntity>

    // LÓGICA DE TRANSACCIÓN PARA EVITAR RACE CONDITIONS (Idempotencia)
    @androidx.room.Transaction
    suspend fun saveOrderIdempotent(order: OrderEntity) {
        // En una base de datos distribuida o paralela, evaluamos si ya existe una orden IDÉNTICA
        // en los últimos 20 segundos para el mismo cliente (Anti-Doble Cobro).
        val recentOrders = getRecentOrdersForClient(order.clientName, System.currentTimeMillis() - 20_000L)
        val isDuplicate = recentOrders.any { 
            it.productDetails == order.productDetails && it.totalAmount == order.totalAmount 
        }
        
        if (!isDuplicate) {
            saveOrder(order)
        }
    }

    @Query("SELECT * FROM orders WHERE clientName = :clientName AND timestamp >= :threshold")
    suspend fun getRecentOrdersForClient(clientName: String, threshold: Long): List<OrderEntity>

    // ---- ANALÍTICAS FINANCIERAS ----
    @Query("SELECT SUM(totalAmount) FROM orders WHERE status != 'CANCELLED'")
    fun getTotalRevenueFlow(): Flow<Double?>
}
