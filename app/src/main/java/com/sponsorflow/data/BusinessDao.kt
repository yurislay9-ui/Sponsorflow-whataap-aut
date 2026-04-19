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

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomers(): List<CustomerEntity>

    @Query("SELECT COUNT(*) FROM customers")
    fun getTotalInteractionsFlow(): Flow<Int>

    // ---- LOGÍSTICA ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOrder(order: OrderEntity)

    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getPendingOrdersFlow(): Flow<List<OrderEntity>>

    // ---- ANALÍTICAS FINANCIERAS ----
    @Query("SELECT SUM(totalAmount) FROM orders WHERE status != 'CANCELLED'")
    fun getTotalRevenueFlow(): Flow<Double?>
}
