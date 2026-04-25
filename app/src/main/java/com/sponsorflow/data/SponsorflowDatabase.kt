package com.sponsorflow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Patrón Singleton para la Base de Datos Sponsorflow.
 * Actualizado a v2: Incluye CRM y Logística
 */
@Database(
    entities = [ProductEntity::class, CustomerEntity::class, OrderEntity::class], 
    version = 5, 
    exportSchema = false
)
abstract class SponsorflowDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun businessDao(): BusinessDao

    companion object {
        @Volatile
        private var INSTANCE: SponsorflowDatabase? = null

        // VECTOR 20 DEFENSA: Migración Estructural Segura
        // Quitamos fallbackToDestructiveMigration() para NO BORRAR los inventarios al actualizar app.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `customers` (`senderId` TEXT NOT NULL, `lastInteraction` INTEGER NOT NULL, `totalPurchases` REAL NOT NULL, `platform` TEXT NOT NULL, PRIMARY KEY(`senderId`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `orders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `clientName` TEXT NOT NULL, `address` TEXT NOT NULL, `productDetails` TEXT NOT NULL, `totalAmount` REAL NOT NULL, `timestamp` INTEGER NOT NULL, `status` TEXT NOT NULL)")
            }
        }
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `orders` ADD COLUMN `providerTicket` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Agregar Módulo de Memoria Cognitiva V2 al CRM
                db.execSQL("ALTER TABLE `customers` ADD COLUMN `perfilCognitivo` TEXT NOT NULL DEFAULT 'Cliente nuevo.'")
                db.execSQL("ALTER TABLE `customers` ADD COLUMN `tags` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `customers` ADD COLUMN `needsMemoryConsolidation` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Integrando el búfer temporal para chats sin resumir
                db.execSQL("ALTER TABLE `customers` ADD COLUMN `unconsolidatedHistory` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): SponsorflowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SponsorflowDatabase::class.java,
                    "sponsorflow_local_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING) // ANTI-RACE CONDITIONS
                .enableMultiInstanceInvalidation() // PREVIENE LOCKED DATABASE Exception
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
