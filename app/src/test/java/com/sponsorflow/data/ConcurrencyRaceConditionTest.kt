package com.sponsorflow.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

/**
 * Auditoría de Sistemas Distribuidos y Concurrencia (Race Conditions).
 * Simula a 50 hilos tratando de insertar el mismo cobro/orden a la misma vez.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ConcurrencyRaceConditionTest {

    private lateinit var db: SponsorflowDatabase
    private lateinit var dao: BusinessDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // DB En Memoria para que sea veloz y efímera
        db = Room.inMemoryDatabaseBuilder(context, SponsorflowDatabase::class.java).build()
        dao = db.businessDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testOrderDuplication_IdempotencyAttack() = runBlocking {
        // Simulamos un escenario catastrófico: Un glitch de red causa que la IA llame
        // a "cerrar_venta" 50 veces en paralelo para exactamente el mismo producto y cliente 
        // en menos de 1 segundo.

        val concurrentRequests = 50
        val clientName = "Cristiano Ronaldo"
        val address = "Turín 777"
        val product = "Zapatillas Jordan"
        
        // Disparamos 50 corrutinas agresivas
        val jobs = List(concurrentRequests) {
            launch(Dispatchers.IO) {
                // Vector de Ataque Exitoso: Cada insercion generará un ID automático (autoGenerate = true)
                // y los 50 cobros se clavarán en la base de datos si no hay llave de idempotencia.
                val attackOrder = OrderEntity(
                    clientName = clientName,
                    address = address,
                    productDetails = product,
                    totalAmount = 250.00
                )
                // Usamos la inyección transaccional con Idempotencia
                dao.saveOrderIdempotent(attackOrder)
            }
        }
        jobs.joinAll()

        // Evaluamos el control de daños
        val allOrders = dao.getAllPendingOrdersStatic()
        
        // Si hay Mutex puro o Llave de Idempotencia por hash/ventana de tiempo,
        // solo el primero debió registrarse, anulando el resto.
        assertTrue("Fallo Crítico: Condición de Carrera. Se registraron \${allOrders.size} órdenes duplicadas en vez de 1.", allOrders.size == 1)
    }
}
