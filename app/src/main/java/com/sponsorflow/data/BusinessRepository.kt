package com.sponsorflow.data

import kotlinx.coroutines.flow.Flow

/**
 * Patrón Repositorio (Business Repository)
 * Abstrayendo el DAO de la Capa de UI y del Enjambre.
 * Aquí se ubica la lógica de negocio purificada sobre el acceso a datos.
 */
class BusinessRepository(private val businessDao: BusinessDao) {

    suspend fun enrollCustomer(senderId: String, platform: String = "WhatsApp") {
        businessDao.saveCustomerIfNotExists(
            CustomerEntity(senderId = senderId, platform = platform)
        )
    }

    suspend fun getCustomerCognitiveContext(senderId: String): CustomerEntity? {
        return businessDao.getCustomerById(senderId)
    }

    suspend fun registerPhysicalOrder(order: OrderEntity) {
        businessDao.saveOrderIdempotent(order) // Maneja race conditions internamente
    }

    fun watchPendingOrders(): Flow<List<OrderEntity>> {
        return businessDao.getPendingOrdersFlow()
    }
    
    fun watchCrmClients(): Flow<List<CustomerEntity>> {
        return businessDao.getAllCustomersFlow()
    }
    
    suspend fun overrideFunnelStage(senderId: String, stage: String) {
        businessDao.updateFunnelStage(senderId, stage)
    }
}
