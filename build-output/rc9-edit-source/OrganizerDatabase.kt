package com.infinitygreenpower.organizerform.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FormEntity::class, FormItemEntity::class, LoadNoteEntity::class, FormPhotoEntity::class, OrganizerEntity::class, CatalogItemEntity::class, MigrationReportEntity::class],
    version = 2,
    exportSchema = true
)
abstract class OrganizerDatabase : RoomDatabase() {
    abstract fun formDao(): FormDao
    abstract fun catalogDao(): CatalogDao
    abstract fun organizerDao(): OrganizerDao
    abstract fun migrationReportDao(): MigrationReportDao

    companion object {
        @Volatile private var instance: OrganizerDatabase? = null
        fun get(context: Context): OrganizerDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, OrganizerDatabase::class.java, "igp_organizer.db")
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
        }
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `migration_reports` (`id` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, `completedAt` INTEGER NOT NULL, `sourceRecords` INTEGER NOT NULL, `importedRecords` INTEGER NOT NULL, `skippedRecords` INTEGER NOT NULL, `settingsImported` INTEGER NOT NULL, `status` TEXT NOT NULL, `details` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }
    }
}
