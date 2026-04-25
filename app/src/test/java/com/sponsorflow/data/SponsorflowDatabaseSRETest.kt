package com.sponsorflow.data

import android.content.Context
import android.database.sqlite.SQLiteBlobTooBigException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sponsorflow.services.MemoryConsolidatorWorker
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry

/**
 * PRUEBAS DE DESTRUCCIÓN Y MIGRACIÓN EN LA BASE DE DATOS ROOM
 * [SRE Database Resilience]
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SponsorflowDatabaseSRETest {

    private lateinit var db: SponsorflowDatabase
    private lateinit var dao: BusinessDao

    @Before
    fun createDb() {
        // Configuramos una base en RAM para destruirla tranquilamente
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SponsorflowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.businessDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testSQLiteCursorWindow_LimitOverflowPrevention() = runBlocking {
        // En Android, un CursorWindow normalmente falla cerca de 2MB. 
        // Vamos a simular un spammer inyectanto un buffer de Chat superior a los 2MB.
        val giantSpamString = "A".repeat(2_500_000) // 2.5 Millones de caracteres (~2.5 MB)
        
        dao.saveCustomerIfNotExists(CustomerEntity(senderId = "Spammer_Pro_Max"))
        
        // El Orquestador manda el string gigante. Si no existiera nuestra precaución de `substr(...) -150000`, 
        // la BBDD se tragaría los 2.5MB en la columna y tratar de leerla luego rompería el App Cursor.
        dao.appendChatHistory("Spammer_Pro_Max", giantSpamString, System.currentTimeMillis())
        
        val target = dao.getCustomerById("Spammer_Pro_Max")
        // La protección de SQLite SUBSTR en BusinessDao debería recortarlo al máximo seguro que hemos fijado (-150000 chars)
        // Por seguridad el limit será alrededor de 150001 porque le hace append del \n
        assertTrue("El buffer acumulado no debió exceder el límite seguro y previene un Cursor Crash", target!!.unconsolidatedHistory.length <= 150050)
    }

    @Test
    fun testAtomicity_UnderBatteryFailure() = runBlocking {
        // Configurar Escenario: Memoria Mecánica
        val client = CustomerEntity(senderId = "Atomico_1", unconsolidatedHistory = "Me encanta", needsMemoryConsolidation = true)
        dao.saveCustomerIfNotExists(client)
        dao.appendChatHistory("Atomico_1", "quiero comprar ya urgente", System.currentTimeMillis())
        
        // Simular ejecución del DAO que hace Commit del perfil (La purga).
        // DAO commitMechanicalMemory es en un QUERY: UPDATE ...
        // En SQLite un UPDATE simple es intrínsecamente atómico, pero si lo interrupo artificialmente (Ej cancelando corrutina)....
        
        var exceptionThrown = false
        try {
             db.runInTransaction {
                 runBlocking {
                     dao.commitMechanicalMemory("Atomico_1", "Perfil Nuevo", "['Satisfecho']")
                     // ¡EL TELÉFONO SE APAGA POR BATERÍA AQUÍ ANTES DEL END TRANSACTION!
                     throw InterruptedException("Battery Died Unexpectedly")
                 }
             }
        } catch (e: Exception) {
             exceptionThrown = true
        }

        assertTrue("Forzamos una interrupción de batería", exceptionThrown)
        
        // ¿La base de datos quedó en una lectura sucia donde el historial se borró pero el perfil no se guardó?
        // Comprobemos. Como la transacción reventó (rollback), los datos DEBEN estar exactamente como antes de empezar.
        val targetAfterCrash = dao.getCustomerById("Atomico_1")
        assertEquals("El perfil no debió guardarse (Rollback Exitoso)", "Cliente nuevo.", targetAfterCrash!!.perfilCognitivo)
        assertTrue("El historial crudo no debió purgarse, debe seguir ahí para el siguiente arranque", targetAfterCrash.needsMemoryConsolidation)
    }
}
