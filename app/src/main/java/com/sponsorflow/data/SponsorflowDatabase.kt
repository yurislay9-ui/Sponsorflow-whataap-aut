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
    version = 3, 
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

        fun getDatabase(context: Context): SponsorflowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SponsorflowDatabase::class.java,
                    "sponsorflow_local_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING) // ANTI-RACE CONDITIONS
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
