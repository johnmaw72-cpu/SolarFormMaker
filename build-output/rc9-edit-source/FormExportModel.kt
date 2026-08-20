package com.infinitygreenpower.organizerform.export.json

import org.json.JSONArray
import org.json.JSONObject

data class ExportItem(val catalogId: String?, val category: String, val name: String, val quantity: String, val unitPrice: String?, val currency: String, val lineTotal: String?)
data class ExportPhoto(val order: Int, val includeInPdf: Boolean, val displayName: String)
data class FormExport(
    val id: String,
    val createdAt: String,
    val updatedAt: String,
    val date: String,
    val companyName: String,
    val clientName: String,
    val phone: String,
    val location: String,
    val systemType: String,
    val requiredCapacity: String,
    val phase: String,
    val organizerName: String,
    val dayFromAmps: String,
    val dayToAmps: String,
    val nightAmps: String,
    val nightHours: String,
    val emergencyAmps: String,
    val emergencyDuration: String,
    val customNote: String,
    val items: List<ExportItem>,
    val estimatedTotal: String?,
    val currency: String,
    val photos: List<ExportPhoto>,
    val appVersion: String
) {
    fun toJson(): String = JSONObject().apply {
        put("schema_version", 1)
        put("form_type", "organizer_preliminary_solar_form")
        put("app", JSONObject().put("name", "IGP Organizer Form").put("version", appVersion))
        put("form", JSONObject().put("id", id).put("created_at", createdAt).put("updated_at", updatedAt).put("date", date))
        put("company", JSONObject().put("name", companyName))
        put("client", JSONObject().put("name", clientName).put("phone", phone).put("location", location))
        put("system", JSONObject().put("type", systemType).put("required_capacity", requiredCapacity).put("phase", phase))
        put("inspection", JSONObject().put("organizer_name", organizerName))
        put("load_note", JSONObject().put("day_from_amps", dayFromAmps).put("day_to_amps", dayToAmps).put("night_amps", nightAmps).put("night_hours", nightHours).put("emergency_amps", emergencyAmps).put("emergency_duration", emergencyDuration).put("custom_note", customNote))
        put("items", JSONArray().apply { items.forEach { item -> put(JSONObject().put("catalog_id", item.catalogId ?: JSONObject.NULL).put("category", item.category).put("name", item.name).put("quantity", item.quantity).put("unit_price", item.unitPrice ?: JSONObject.NULL).put("currency", item.currency).put("line_total", item.lineTotal ?: JSONObject.NULL)) } })
        put("estimated_total", JSONObject().put("amount", estimatedTotal ?: JSONObject.NULL).put("currency", currency))
        put("photos", JSONArray().apply { photos.forEach { photo -> put(JSONObject().put("order", photo.order).put("include_in_pdf", photo.includeInPdf).put("display_name", photo.displayName)) } })
    }.toString(2)
}
