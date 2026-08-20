package com.infinitygreenpower.organizerform.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "forms", indices = [Index("legacyId", unique = true)])
data class FormEntity(
    @PrimaryKey val id: String,
    val legacyId: String? = null,
    val clientName: String = "",
    val phone: String = "",
    val location: String = "",
    val formDate: String = "",
    val systemType: String = "Hybrid",
    val capacity: String = "",
    val phase: String = "Single Phase",
    val organizerId: String? = null,
    val organizerSnapshotName: String = "",
    val customNote: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val isDraft: Boolean = true,
    val migratedFromLegacy: Boolean = false,
    val schemaVersion: Int = 1
)

@Entity(
    tableName = "form_items",
    foreignKeys = [ForeignKey(entity = FormEntity::class, parentColumns = ["id"], childColumns = ["formId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("formId"), Index("catalogItemId")]
)
data class FormItemEntity(
    @PrimaryKey val id: String,
    val formId: String,
    val catalogItemId: String? = null,
    val itemNameSnapshot: String,
    val categorySnapshot: String,
    val specificationSnapshot: String = "",
    val quantity: String,
    val unitPrice: String? = null,
    val currency: String,
    val sortOrder: Int
)

@Entity(
    tableName = "load_notes",
    foreignKeys = [ForeignKey(entity = FormEntity::class, parentColumns = ["id"], childColumns = ["formId"], onDelete = ForeignKey.CASCADE)]
)
data class LoadNoteEntity(
    @PrimaryKey val formId: String,
    val systemCapacity: String = "",
    val systemType: String = "Hybrid",
    val dayFromAmps: String = "",
    val dayToAmps: String = "",
    val nightAmps: String = "",
    val nightHours: String = "",
    val emergencyAmps: String = "",
    val emergencyDuration: String = ""
)

@Entity(
    tableName = "form_photos",
    foreignKeys = [ForeignKey(entity = FormEntity::class, parentColumns = ["id"], childColumns = ["formId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("formId")]
)
data class FormPhotoEntity(
    @PrimaryKey val id: String,
    val formId: String,
    val relativePath: String,
    val displayName: String,
    val mimeType: String,
    val includeInPdf: Boolean = true,
    val sortOrder: Int,
    val width: Int? = null,
    val height: Int? = null,
    val importedAt: Long,
    val unavailable: Boolean = false,
    val error: String? = null
)

@Entity(tableName = "organizers")
data class OrganizerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "catalog_items", indices = [Index("category"), Index("name")])
data class CatalogItemEntity(
    @PrimaryKey val id: String,
    val category: String,
    val name: String,
    val legacyDescription: String,
    val sourceVersion: Int = 4,
    val favorite: Boolean = false,
    val lastUsedAt: Long? = null,
    val lastUsedUnitPrice: String? = null,
    val lastUsedCurrency: String? = null
)

@Entity(tableName = "migration_reports")
data class MigrationReportEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val completedAt: Long,
    val sourceRecords: Int,
    val importedRecords: Int,
    val skippedRecords: Int,
    val settingsImported: Boolean,
    val status: String,
    val details: String
)
