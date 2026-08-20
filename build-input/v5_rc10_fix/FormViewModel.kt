package com.infinitygreenpower.organizerform.feature.form

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.infinitygreenpower.organizerform.core.model.RememberedPrice
import com.infinitygreenpower.organizerform.core.model.prefillFor
import com.infinitygreenpower.organizerform.data.db.CatalogItemEntity
import com.infinitygreenpower.organizerform.data.db.FormAggregate
import com.infinitygreenpower.organizerform.data.db.FormEntity
import com.infinitygreenpower.organizerform.data.db.FormItemEntity
import com.infinitygreenpower.organizerform.data.db.FormPhotoEntity
import com.infinitygreenpower.organizerform.data.db.LoadNoteEntity
import com.infinitygreenpower.organizerform.data.db.OrganizerDatabase
import com.infinitygreenpower.organizerform.data.db.OrganizerEntity
import com.infinitygreenpower.organizerform.data.preferences.AppPreferences
import com.infinitygreenpower.organizerform.data.repository.FormRepository
import java.io.File
import java.math.BigDecimal
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FormViewModel(
    application: Application,
    private val initialFormId: String? = null,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val database = OrganizerDatabase.get(application)
    private val forms = FormRepository(database)
    private val appPreferences = AppPreferences(application)
    val catalog = database.catalogDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val organizers = database.organizerDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val mutableState = MutableStateFlow(FormUiState())
    val state = mutableState
    private var initialized = false
    private var workingIsDraft = initialFormId == null
    private var autoSaveJob: Job? = null

    init {
        viewModelScope.launch {
            val activeCurrency = appPreferences.settings.first().currency
            val aggregate = initialFormId?.let { forms.observe(it).first() } ?: forms.observeActiveDraft().first()
            if (aggregate != null) {
                workingIsDraft = aggregate.form.isDraft
                mutableState.value = aggregate.toUi(activeCurrency).copy(step = savedStateHandle[STEP_KEY] ?: 1)
            } else {
                val defaultOrganizer = database.organizerDao().getDefault()
                mutableState.value = mutableState.value.copy(
                    currency = activeCurrency,
                    organizerId = defaultOrganizer?.id,
                    organizerName = defaultOrganizer?.name.orEmpty()
                )
            }
            initialized = true
        }
    }

    fun update(block: FormUiState.() -> FormUiState) {
        applyUpdate(block, autoSave = true)
    }

    fun goTo(step: Int) {
        applyUpdate({ copy(step = step.coerceIn(1, 5)) }, autoSave = false)
        persist()
    }

    fun saveDraft() {
        persist(if (workingIsDraft) "Draft saved" else "Changes saved")
    }

    fun saveForm() = viewModelScope.launch {
        autoSaveJob?.cancel()
        if (persistSafely(isDraft = false, successMessage = "Form saved")) {
            workingIsDraft = false
            if (initialFormId == null) {
                val currency = state.value.currency
                val defaultOrganizer = database.organizerDao().getDefault()
                workingIsDraft = true
                mutableState.value = FormUiState(
                    currency = currency,
                    organizerId = defaultOrganizer?.id,
                    organizerName = defaultOrganizer?.name.orEmpty(),
                    message = "Form saved"
                )
                savedStateHandle[STEP_KEY] = 1
            }
        }
    }

    fun clearMessage() {
        mutableState.value = state.value.copy(message = null)
    }

    fun preview() {
        val errors = validationErrors(state.value)
        if (errors.isEmpty()) {
            applyUpdate({ copy(step = 5) }, autoSave = false)
            persist()
        } else {
            mutableState.value = state.value.copy(
                errors = errors,
                message = "Complete the highlighted required fields"
            )
        }
    }

    fun addCatalogItem(item: CatalogItemEntity) = viewModelScope.launch {
        val price = item.lastUsedUnitPrice?.let {
            RememberedPrice(it, item.lastUsedCurrency.orEmpty()).prefillFor(state.value.currency)
        }.orEmpty()
        update {
            copy(items = items + DraftItem(
                catalogId = item.id,
                name = item.name,
                category = item.category,
                specification = item.legacyDescription,
                unitPrice = price
            ))
        }
    }

    fun addCustom(name: String, category: String, quantity: Int, price: String) {
        update {
            copy(items = items + DraftItem(
                name = name.ifBlank { "Custom item" },
                category = category.ifBlank { "Custom" },
                quantity = quantity.coerceAtLeast(1),
                unitPrice = price
            ))
        }
    }

    fun changeQuantity(id: String, delta: Int) {
        update { copy(items = items.map { if (it.id == id) it.copy(quantity = (it.quantity + delta).coerceAtLeast(1)) else it }) }
    }

    fun updateItem(item: DraftItem) {
        update { copy(items = items.map { if (it.id == item.id) item else it }) }
    }

    fun removeItem(id: String) {
        update { copy(items = items.filterNot { it.id == id }) }
    }

    fun saveOrganizer(name: String, phone: String, makeDefault: Boolean = false) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val organizer = OrganizerEntity(
            UUID.randomUUID().toString(), name.trim(), phone.trim().ifBlank { null }, makeDefault, now, now
        )
        if (makeDefault) database.organizerDao().clearDefault()
        database.organizerDao().upsert(organizer)
        update { copy(organizerId = organizer.id, organizerName = organizer.name) }
    }

    fun updateOrganizer(organizer: OrganizerEntity, name: String, phone: String, makeDefault: Boolean) =
        viewModelScope.launch {
            if (makeDefault) database.organizerDao().clearDefault()
            val updated = organizer.copy(
                name = name.trim(),
                phone = phone.trim().ifBlank { null },
                isDefault = makeDefault,
                updatedAt = System.currentTimeMillis()
            )
            database.organizerDao().upsert(updated)
            if (state.value.organizerId == updated.id) update { copy(organizerName = updated.name) }
        }

    fun setDefaultOrganizer(organizer: OrganizerEntity) = viewModelScope.launch {
        database.organizerDao().clearDefault()
        database.organizerDao().upsert(organizer.copy(isDefault = true, updatedAt = System.currentTimeMillis()))
    }

    fun deleteOrganizer(organizer: OrganizerEntity) = viewModelScope.launch {
        database.organizerDao().delete(organizer.id)
        if (state.value.organizerId == organizer.id) update { copy(organizerId = null, organizerName = "") }
    }

    fun selectOrganizer(organizer: OrganizerEntity) {
        update { copy(organizerId = organizer.id, organizerName = organizer.name) }
    }

    fun importPhotos(uris: List<Uri>) = viewModelScope.launch {
        val context = getApplication<Application>()
        val directory = File(context.filesDir, "photos/${state.value.id}").apply { mkdirs() }
        val added = uris.mapNotNull { uri ->
            runCatching {
                val id = UUID.randomUUID().toString()
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val extension = if (mime.contains("png")) "png" else "jpg"
                val file = File(directory, "$id.$extension")
                val input = context.contentResolver.openInputStream(uri) ?: error("Photo stream unavailable")
                input.use { source -> file.outputStream().use(source::copyTo) }
                DraftPhoto(id, file.absolutePath, file.name, mime)
            }.getOrNull()
        }
        applyUpdate({
            copy(
                photos = photos + added,
                message = if (added.size < uris.size) "Some photos could not be imported" else null
            )
        }, autoSave = false)
        persistSafely(workingIsDraft)
    }

    fun togglePhoto(id: String) {
        update { copy(photos = photos.map { if (it.id == id) it.copy(included = !it.included) else it }) }
    }

    fun movePhoto(id: String, delta: Int) {
        update {
            val list = photos.toMutableList()
            val from = list.indexOfFirst { it.id == id }
            if (from < 0) return@update this
            val to = (from + delta).coerceIn(0, list.lastIndex)
            if (from != to) list.add(to, list.removeAt(from))
            copy(photos = list)
        }
    }

    fun removePhoto(id: String) = viewModelScope.launch {
        val photo = state.value.photos.firstOrNull { it.id == id } ?: return@launch
        applyUpdate({ copy(photos = photos.filterNot { it.id == id }) }, autoSave = false)
        if (persistSafely(workingIsDraft)) runCatching { File(photo.path).delete() }
    }

    private fun applyUpdate(block: FormUiState.() -> FormUiState, autoSave: Boolean) {
        mutableState.value = mutableState.value.block().copy(errors = emptySet(), message = null)
        savedStateHandle[STEP_KEY] = mutableState.value.step
        if (autoSave && initialized) scheduleAutoSave()
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DELAY_MS)
            persistSafely(workingIsDraft)
        }
    }

    private fun persist(message: String? = null) {
        autoSaveJob?.cancel()
        viewModelScope.launch { persistSafely(workingIsDraft, message) }
    }

    private suspend fun persistSafely(isDraft: Boolean, successMessage: String? = null): Boolean =
        runCatching {
            persistNow(isDraft)
            if (successMessage != null) mutableState.value = state.value.copy(message = successMessage)
        }.fold(
            onSuccess = { true },
            onFailure = {
                mutableState.value = state.value.copy(message = "Could not save changes. Please try again.")
                false
            }
        )

    private suspend fun persistNow(isDraft: Boolean) {
        val snapshot = state.value
        val now = System.currentTimeMillis()
        forms.save(
            FormEntity(
                id = snapshot.id,
                legacyId = snapshot.legacyId,
                clientName = snapshot.clientName,
                phone = snapshot.phone,
                location = snapshot.location,
                formDate = snapshot.date,
                systemType = snapshot.systemType,
                capacity = snapshot.capacity,
                phase = snapshot.phase,
                organizerId = snapshot.organizerId,
                organizerSnapshotName = snapshot.organizerName,
                customNote = snapshot.customNote,
                createdAt = snapshot.createdAt,
                updatedAt = now,
                isDraft = isDraft,
                migratedFromLegacy = snapshot.migratedFromLegacy,
                schemaVersion = snapshot.schemaVersion
            ),
            snapshot.items.mapIndexed { index, item ->
                FormItemEntity(
                    item.id, snapshot.id, item.catalogId, item.name, item.category, item.specification,
                    item.quantity.toString(), item.unitPrice.ifBlank { null }, snapshot.currency, index
                )
            },
            snapshot.photos.mapIndexed { index, photo ->
                FormPhotoEntity(
                    photo.id, snapshot.id, photo.path, photo.displayName, photo.mimeType,
                    photo.included, index, importedAt = now
                )
            },
            LoadNoteEntity(
                snapshot.id, snapshot.capacity, snapshot.systemType, snapshot.dayFrom, snapshot.dayTo,
                snapshot.nightAmps, snapshot.nightHours, snapshot.emergencyAmps, snapshot.emergencyDuration
            )
        )
        snapshot.items.filter { it.catalogId != null }.forEach { item ->
            val validPrice = item.unitPrice.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO }?.toPlainString()
            database.catalogDao().rememberUse(item.catalogId!!, now, validPrice, snapshot.currency)
        }
        mutableState.value = state.value.copy(updatedAt = now)
    }

    companion object {
        private const val AUTO_SAVE_DELAY_MS = 400L
        private const val STEP_KEY = "form_step"

        fun factory(application: Application, formId: String?): ViewModelProvider.Factory = viewModelFactory {
            initializer { FormViewModel(application, formId, createSavedStateHandle()) }
        }
    }
}

private fun FormAggregate.toUi(activeCurrency: String) = FormUiState(
    id = form.id,
    legacyId = form.legacyId,
    migratedFromLegacy = form.migratedFromLegacy,
    schemaVersion = form.schemaVersion,
    createdAt = form.createdAt,
    updatedAt = form.updatedAt,
    clientName = form.clientName,
    phone = form.phone,
    location = form.location,
    date = form.formDate,
    systemType = form.systemType,
    capacity = form.capacity,
    phase = form.phase,
    organizerId = form.organizerId,
    organizerName = form.organizerSnapshotName,
    customNote = form.customNote,
    currency = items.firstOrNull()?.currency ?: activeCurrency,
    items = items.sortedBy { it.sortOrder }.map {
        DraftItem(
            it.id, it.catalogItemId, it.itemNameSnapshot, it.categorySnapshot,
            it.specificationSnapshot, it.quantity.toIntOrNull() ?: 1, it.unitPrice.orEmpty()
        )
    },
    photos = photos.sortedBy { it.sortOrder }.map {
        DraftPhoto(it.id, it.relativePath, it.displayName, it.mimeType, it.includeInPdf)
    },
    dayFrom = loadNote?.dayFromAmps.orEmpty(),
    dayTo = loadNote?.dayToAmps.orEmpty(),
    nightAmps = loadNote?.nightAmps.orEmpty(),
    nightHours = loadNote?.nightHours.orEmpty(),
    emergencyAmps = loadNote?.emergencyAmps.orEmpty(),
    emergencyDuration = loadNote?.emergencyDuration.orEmpty()
)
