package com.infinitygreenpower.organizerform.feature.form

import com.infinitygreenpower.organizerform.data.db.CatalogItemEntity
import com.infinitygreenpower.organizerform.data.db.FormAggregate
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class DraftItem(
    val id: String = UUID.randomUUID().toString(),
    val catalogId: String? = null,
    val name: String = "",
    val category: String = "Custom",
    val specification: String = "",
    val quantity: Int = 1,
    val unitPrice: String = ""
) {
    val lineTotal: BigDecimal? get() = unitPrice.toBigDecimalOrNull()?.multiply(quantity.toBigDecimal())
}

data class DraftPhoto(val id: String, val path: String, val displayName: String, val mimeType: String, val included: Boolean = true)

data class FormUiState(
    val id: String = UUID.randomUUID().toString(),
    val legacyId: String? = null,
    val migratedFromLegacy: Boolean = false,
    val schemaVersion: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val step: Int = 1,
    val clientName: String = "",
    val phone: String = "",
    val location: String = "",
    val date: String = LocalDate.now().toString(),
    val systemType: String = "Hybrid",
    val capacity: String = "",
    val phase: String = "Single Phase",
    val items: List<DraftItem> = emptyList(),
    val organizerId: String? = null,
    val organizerName: String = "",
    val dayFrom: String = "",
    val dayTo: String = "",
    val nightAmps: String = "",
    val nightHours: String = "",
    val emergencyAmps: String = "",
    val emergencyDuration: String = "",
    val customNote: String = "",
    val photos: List<DraftPhoto> = emptyList(),
    val currency: String = "USD",
    val errors: Set<String> = emptySet(),
    val message: String? = null
) {
    val estimatedTotal: BigDecimal get() = items.mapNotNull { it.lineTotal }.fold(BigDecimal.ZERO, BigDecimal::add)
}

fun generatedLoadNote(state: FormUiState, language: String): String {
    val type = when {
        language == "ar" && state.systemType == "Hybrid" -> "هجين"
        language == "ckb" && state.systemType == "Hybrid" -> "هایبرید"
        else -> state.systemType
    }
    return if (language == "ar") "منظومة شمسية بقدرة ${state.capacity} كيلو واط نوع $type يتحمل كالآتي:\n- بالنهار من ${state.dayFrom} الى ${state.dayTo} امبير\n- بالليل ${state.nightAmps} امبير لمدة ${state.nightHours} ساعات\n- وفي حالات الضرورة يتحمل ${state.emergencyAmps} امبير لمدة ${state.emergencyDuration}"
    else if (language == "ckb") "سیستەمێکی خۆرەوی بە توانای ${state.capacity} کیلۆوات، جۆری $type:\n- ڕۆژانە لە ${state.dayFrom} بۆ ${state.dayTo} ئەمپێر\n- شەوانە ${state.nightAmps} ئەمپێر بۆ ${state.nightHours} کاتژمێر\n- لە کاتی پێویستدا ${state.emergencyAmps} ئەمپێر بۆ ${state.emergencyDuration}"
    else "${state.capacity} kW $type solar system load profile:\n- Day: ${state.dayFrom} to ${state.dayTo} A\n- Night: ${state.nightAmps} A for ${state.nightHours} hours\n- Emergency: ${state.emergencyAmps} A for ${state.emergencyDuration}"
}

fun validationErrors(state: FormUiState): Set<String> = buildSet {
    if (state.clientName.isBlank()) add("client")
    if (state.phone.isBlank()) add("phone")
    if (state.location.isBlank()) add("location")
    if (state.date.isBlank()) add("date")
    if (state.capacity.isBlank()) add("capacity")
    if (state.items.isEmpty()) add("items")
    if (state.organizerName.isBlank()) add("organizer")
}
