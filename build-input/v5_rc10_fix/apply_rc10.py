from pathlib import Path

ROOT = Path('/tmp/igp-build/IGP')
PKG = ROOT / 'app/src/main/java/com/infinitygreenpower/organizerform'


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text()
    if old not in text:
        raise RuntimeError(f'{label}: anchor not found in {path}')
    path.write_text(text.replace(old, new, 1))


# Form model: additive field at the end preserves existing positional constructors.
p = PKG / 'feature/form/FormModels.kt'
replace_once(
    p,
    '    val quantity: Int = 1,\n    val unitPrice: String = ""\n',
    '    val quantity: Int = 1,\n    val unitPrice: String = "",\n    val approximate: Boolean = false\n',
    'DraftItem approximate field',
)
replace_once(
    p,
    'fun generatedLoadNote(state: FormUiState, language: String): String {',
    '''const val STANDARD_NOTE_AC = "لا یشمل مواد واعمال AC"\nconst val STANDARD_NOTE_CIVIL = "لا یشمل اعمال مدنیة ان وجدت"\n\nfun addStandardExclusionNotes(current: String): String {\n    val missing = listOf(STANDARD_NOTE_AC, STANDARD_NOTE_CIVIL).filterNot { current.contains(it) }\n    if (missing.isEmpty()) return current\n    val prefix = current.trimEnd()\n    val addition = missing.joinToString("\\n") { "- $it" }\n    return if (prefix.isBlank()) addition else "$prefix\\n$addition"\n}\n\nfun generatedLoadNote(state: FormUiState, language: String): String {''',
    'standard note helper',
)

# Form persistence: no Room migration. Approximate is encoded as a backward-compatible '~' quantity prefix.
p = PKG / 'feature/form/FormViewModel.kt'
replace_once(
    p,
    'fun addCustom(name: String, category: String, quantity: Int, price: String) {',
    'fun addCustom(name: String, category: String, quantity: Int, price: String, approximate: Boolean = false) {',
    'addCustom signature',
)
replace_once(
    p,
    '                quantity = quantity.coerceAtLeast(1),\n                unitPrice = price\n',
    '                quantity = quantity.coerceAtLeast(1),\n                unitPrice = price,\n                approximate = approximate\n',
    'addCustom approximate value',
)
replace_once(
    p,
    'item.quantity.toString(), item.unitPrice.ifBlank { null }, snapshot.currency, index',
    '((if (item.approximate) "~" else "") + item.quantity.toString()), item.unitPrice.ifBlank { null }, snapshot.currency, index',
    'persist approximate quantity',
)
replace_once(
    p,
    '''        DraftItem(\n            it.id, it.catalogItemId, it.itemNameSnapshot, it.categorySnapshot,\n            it.specificationSnapshot, it.quantity.toIntOrNull() ?: 1, it.unitPrice.orEmpty()\n        )\n''',
    '''        DraftItem(\n            id = it.id,\n            catalogId = it.catalogItemId,\n            name = it.itemNameSnapshot,\n            category = it.categorySnapshot,\n            specification = it.specificationSnapshot,\n            quantity = it.quantity.removePrefix("~").toIntOrNull() ?: 1,\n            unitPrice = it.unitPrice.orEmpty(),\n            approximate = it.quantity.startsWith("~")\n        )\n''',
    'restore approximate quantity',
)

# UI: quick per-item flag, editor flag, preview flag, quick standard-note button.
p = PKG / 'feature/form/FormScreen.kt'
replace_once(
    p,
    'vm.addCustom(item.name, item.category, item.quantity, item.unitPrice)',
    'vm.addCustom(item.name, item.category, item.quantity, item.unitPrice, item.approximate)',
    'custom item approximate save',
)
replace_once(
    p,
    'Text(x.category,color=Muted,fontSize=12.sp); Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween)',
    'Text(x.category,color=Muted,fontSize=12.sp); FilterChip(selected=x.approximate,onClick={vm.updateItem(x.copy(approximate=!x.approximate))},label={Text(tr(lang,"Approx.","تقريباً","نزیکەی"))}); Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween)',
    'item approximate chip',
)
replace_once(
    p,
    'Text("${x.quantity}",fontWeight=FontWeight.Bold)',
    'Text(if(x.approximate) "${tr(lang,"Approx.","تقريباً","نزیکەی")} ${x.quantity}" else "${x.quantity}",fontWeight=FontWeight.Bold)',
    'item approximate quantity label',
)
replace_once(
    p,
    's.items.forEach{Summary(it.name,"${it.quantity} × ${it.unitPrice.ifBlank{"—"}}")}',
    's.items.forEach{Summary(it.name,"${if(it.approximate) tr(lang,"Approx.","تقريباً","نزیکەی")+" " else ""}${it.quantity} × ${it.unitPrice.ifBlank{"—"}}")}',
    'preview approximate label',
)
replace_once(
    p,
    'Field(s.customNote,{v->vm.update{copy(customNote=v)}},tr(lang,"Custom note (optional)","ملاحظة مخصصة (اختياري)","تێبینی تایبەت"),false,singleLine=false) } }',
    'Field(s.customNote,{v->vm.update{copy(customNote=v)}},tr(lang,"Custom note (optional)","ملاحظة مخصصة (اختياري)","تێبینی تایبەت"),false,singleLine=false); OutlinedButton({vm.update{copy(customNote=addStandardExclusionNotes(customNote))}},Modifier.fillMaxWidth().padding(top=6.dp)){Icon(Icons.Outlined.Add,null);Text(tr(lang,"Add AC & civil-work exclusions","إضافة ملاحظات AC والأعمال المدنية","زیادکردنی تێبینی AC و کاری مەدەنی"))} } }',
    'standard note button',
)
replace_once(
    p,
    'Field(item.quantity.toString(),{item=item.copy(quantity=it.toIntOrNull()?.coerceAtLeast(1)?:1)},tr(lang,"Quantity","الكمية","بڕ"),keyboard=KeyboardType.Number);Field(item.unitPrice',
    'Field(item.quantity.toString(),{item=item.copy(quantity=it.toIntOrNull()?.coerceAtLeast(1)?:1)},tr(lang,"Quantity","الكمية","بڕ"),keyboard=KeyboardType.Number);Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Checkbox(item.approximate,{item=item.copy(approximate=it)});Text(tr(lang,"Approximate quantity","الكمية تقريباً","بڕ بە نزیکەیی"))};Field(item.unitPrice',
    'item editor approximate control',
)

# PDF: allow room for the localized Approx. label and export it explicitly.
p = PKG / 'export/pdf/PdfExporter.kt'
replace_once(
    p,
    '                val itemWidth = if (settings.showPrices) 365 else 455\n                val quantityWidth = 60\n',
    '                val itemWidth = if (settings.showPrices) 347 else 437\n                val quantityWidth = 78\n',
    'PDF material widths',
)
replace_once(
    p,
    '            val itemWidth = if (settings.showPrices) 365 else 455\n            val quantityWidth = 60\n',
    '            val itemWidth = if (settings.showPrices) 347 else 437\n            val quantityWidth = 78\n',
    'PDF header widths',
)
replace_once(
    p,
    'val quantity = item.quantity.toString()',
    'val quantity = if (item.approximate) "${t(lang, "Approx.", "تقريباً", "نزیکەی")} ${item.quantity}" else item.quantity.toString()',
    'PDF approximate quantity',
)
replace_once(
    p,
    '                    it.quantity.toString(),\n                    it.unitPrice.ifBlank { null },\n',
    '                    it.quantity.toString(),\n                    it.approximate,\n                    it.unitPrice.ifBlank { null },\n',
    'JSON export mapping',
)

# JSON: additive approximate flag.
p = PKG / 'export/json/FormExportModel.kt'
replace_once(
    p,
    'data class ExportItem(val catalogId: String?, val category: String, val name: String, val quantity: String, val unitPrice: String?, val currency: String, val lineTotal: String?)',
    'data class ExportItem(val catalogId: String?, val category: String, val name: String, val quantity: String, val approximate: Boolean, val unitPrice: String?, val currency: String, val lineTotal: String?)',
    'ExportItem approximate field',
)
replace_once(
    p,
    '.put("quantity", item.quantity).put("unit_price", item.unitPrice ?: JSONObject.NULL)',
    '.put("quantity", item.quantity).put("approximate", item.approximate).put("unit_price", item.unitPrice ?: JSONObject.NULL)',
    'ExportItem JSON approximate value',
)

# Version bump.
build = ROOT / 'app/build.gradle.kts'
text = build.read_text()
text = text.replace('versionCode = 38', 'versionCode = 44')
text = text.replace('versionName = "5.0.0-rc4"', 'versionName = "5.0.0-rc10"')
build.write_text(text)

# Required assertions.
assert 'selected = isSelected' in (PKG / 'feature/form/FormScreen.kt').read_text()
assert 'Add AC & civil-work exclusions' in (PKG / 'feature/form/FormScreen.kt').read_text()
assert 'approximate = it.quantity.startsWith("~")' in (PKG / 'feature/form/FormViewModel.kt').read_text()
assert 'suspend fun upsert(item: CatalogItemEntity)' in (PKG / 'data/db/Daos.kt').read_text()
assert 'Add Catalog Item' in (PKG / 'feature/catalog/CatalogScreen.kt').read_text()
assert 'Edit Catalog Item' in (PKG / 'feature/catalog/CatalogScreen.kt').read_text()
assert 'put("approximate", item.approximate)' in (PKG / 'export/json/FormExportModel.kt').read_text()
assert 'versionCode = 44' in build.read_text()
assert 'versionName = "5.0.0-rc10"' in build.read_text()
