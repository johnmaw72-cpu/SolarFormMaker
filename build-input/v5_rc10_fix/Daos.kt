package com.infinitygreenpower.organizerform.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FormDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(form: FormEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertItems(items: List<FormItemEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertPhotos(photos: List<FormPhotoEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertLoadNote(note: LoadNoteEntity)
    @Query("DELETE FROM form_items WHERE formId = :formId") suspend fun deleteItems(formId: String)
    @Query("DELETE FROM form_photos WHERE formId = :formId") suspend fun deletePhotos(formId: String)
    @Transaction @Query("SELECT * FROM forms WHERE id = :id") fun observe(id: String): Flow<FormAggregate?>
    @Transaction @Query("SELECT * FROM forms WHERE isDraft = 1 ORDER BY updatedAt DESC LIMIT 1") fun observeActiveDraft(): Flow<FormAggregate?>
    @Query("SELECT * FROM forms WHERE isDraft = 0 ORDER BY updatedAt DESC") fun observeSaved(): Flow<List<FormEntity>>
    @Transaction @Query("SELECT * FROM forms WHERE isDraft = 0 ORDER BY updatedAt DESC") fun observeSavedAggregates(): Flow<List<FormAggregate>>
    @Query("UPDATE forms SET isDraft = :isDraft, updatedAt = :updatedAt WHERE id = :id") suspend fun setDraft(id: String, isDraft: Boolean, updatedAt: Long)
    @Query("DELETE FROM forms WHERE id = :id") suspend fun delete(id: String)
    @Query("SELECT COUNT(*) FROM forms") suspend fun countAll(): Int
    @Query("SELECT COUNT(*) FROM forms WHERE migratedFromLegacy = 1") suspend fun countMigrated(): Int
    @Query("SELECT COUNT(*) FROM forms WHERE legacyId = :legacyId") suspend fun countByLegacyId(legacyId: String): Int
    @Query("SELECT COUNT(*) FROM forms WHERE legacyId IN (:legacyIds)") suspend fun countByLegacyIds(legacyIds: List<String>): Int
}

@Dao
interface CatalogDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertSeed(items: List<CatalogItemEntity>): List<Long>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: CatalogItemEntity)
    @Query("SELECT COUNT(*) FROM catalog_items") suspend fun count(): Int
    @Query("SELECT * FROM catalog_items ORDER BY category, name") fun observeAll(): Flow<List<CatalogItemEntity>>
    @Query("SELECT * FROM catalog_items WHERE id = :id LIMIT 1") suspend fun get(id: String): CatalogItemEntity?
    @Query("SELECT * FROM catalog_items WHERE favorite = 1 ORDER BY name") fun observeFavorites(): Flow<List<CatalogItemEntity>>
    @Query("SELECT * FROM catalog_items WHERE lastUsedAt IS NOT NULL ORDER BY lastUsedAt DESC LIMIT :limit") fun observeRecent(limit: Int = 20): Flow<List<CatalogItemEntity>>
    @Query("UPDATE catalog_items SET favorite = :favorite WHERE id = :id") suspend fun setFavorite(id: String, favorite: Boolean)
    @Query("UPDATE catalog_items SET lastUsedAt = :usedAt, lastUsedUnitPrice = :price, lastUsedCurrency = :currency WHERE id = :id") suspend fun rememberUse(id: String, usedAt: Long, price: String?, currency: String?)
    @Query("UPDATE catalog_items SET lastUsedAt = NULL") suspend fun clearRecent()
    @Query("UPDATE catalog_items SET favorite = 0") suspend fun clearFavorites()
    @Query("SELECT * FROM catalog_items WHERE name = :name LIMIT 1") suspend fun findByName(name: String): CatalogItemEntity?
    @Query("UPDATE catalog_items SET favorite = 1 WHERE name IN (:names)") suspend fun markFavoritesByName(names: List<String>)
    @Query("UPDATE catalog_items SET lastUsedAt = :usedAt WHERE name = :name") suspend fun markRecentByName(name: String, usedAt: Long)
}

@Dao
interface OrganizerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(organizer: OrganizerEntity)
    @Update suspend fun update(organizer: OrganizerEntity)
    @Query("SELECT * FROM organizers ORDER BY isDefault DESC, name") fun observeAll(): Flow<List<OrganizerEntity>>
    @Query("UPDATE organizers SET isDefault = 0") suspend fun clearDefault()
    @Query("SELECT * FROM organizers WHERE isDefault = 1 LIMIT 1") suspend fun getDefault(): OrganizerEntity?
    @Query("DELETE FROM organizers WHERE id = :id") suspend fun delete(id: String)
}

@Dao
interface MigrationReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(report: MigrationReportEntity)
    @Query("SELECT * FROM migration_reports ORDER BY completedAt DESC LIMIT 1") fun observeLatest(): Flow<MigrationReportEntity?>
}
