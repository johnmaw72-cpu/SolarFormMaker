from pathlib import Path
import sys

root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path('.')
form = root / 'app/src/main/java/com/infinitygreenpower/organizerform/feature/form/FormScreen.kt'
catalog = root / 'app/src/main/java/com/infinitygreenpower/organizerform/feature/catalog/CatalogScreen.kt'
build = root / 'app/build.gradle.kts'

text = form.read_text()

chip = ' FilterChip(selected=x.approximate,onClick={vm.updateItem(x.copy(approximate=!x.approximate))},label={Text(tr(lang,"Approx.","تقريباً","نزیکەی"))});'
assert chip in text, 'item-card approximate chip anchor missing'
text = text.replace(chip, '', 1)

qty_old = 'Text(if(x.approximate) "${tr(lang,"Approx.","تقريباً","نزیکەی")} ${x.quantity}" else "${x.quantity}",fontWeight=FontWeight.Bold)'
qty_new = 'Text("${x.quantity}",fontWeight=FontWeight.Bold)'
assert qty_old in text, 'item-card approximate quantity anchor missing'
text = text.replace(qty_old, qty_new, 1)

preview_old = 'Summary(it.name,"${if(it.approximate) tr(lang,"Approx.","تقريباً","نزیکەی")+" " else ""}${it.quantity} × ${it.unitPrice.ifBlank{"—"}}")'
preview_new = 'Summary(it.name,"${it.quantity} × ${it.unitPrice.ifBlank{"—"}}")'
assert preview_old in text, 'preview approximate anchor missing'
text = text.replace(preview_old, preview_new, 1)

summary_old = '@Composable private fun Summary(label:String,value:String){Row(Modifier.fillMaxWidth().padding(vertical=5.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(label,color=Muted);Text(value,color=Navy,fontWeight=FontWeight.Medium)}}'
summary_new = '''@Composable
private fun Summary(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, color = Muted, modifier = Modifier.weight(1f))
        Text(
            value,
            color = Navy,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 2,
            modifier = Modifier.widthIn(min = 88.dp, max = 150.dp)
        )
    }
}'''
assert summary_old in text, 'Summary layout anchor missing'
text = text.replace(summary_old, summary_new, 1)

standard_sheet = '''    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxHeight(.9f).padding(horizontal = 16.dp, vertical = 10.dp)) {'''
stable_sheet = '''    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxHeight(.9f).padding(horizontal = 16.dp, vertical = 10.dp)) {'''
assert text.count(standard_sheet) == 2, f'expected Location and Catalog sheets, got {text.count(standard_sheet)}'
text = text.replace(standard_sheet, stable_sheet, 2)

item_sheet_old = '@OptIn(ExperimentalMaterial3Api::class) @Composable private fun ItemEditorSheet(base:DraftItem,lang:String,onDismiss:()->Unit,onSave:(DraftItem)->Unit){var item by remember(base.id){mutableStateOf(base)};var quantityText by remember(base.id){mutableStateOf(base.quantity.toString())};ModalBottomSheet(onDismiss){'
item_sheet_new = '@OptIn(ExperimentalMaterial3Api::class) @Composable private fun ItemEditorSheet(base:DraftItem,lang:String,onDismiss:()->Unit,onSave:(DraftItem)->Unit){var item by remember(base.id){mutableStateOf(base)};var quantityText by remember(base.id){mutableStateOf(base.quantity.toString())};val sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true);ModalBottomSheet(onDismissRequest=onDismiss,sheetState=sheetState){'
assert item_sheet_old in text, 'ItemEditorSheet anchor missing'
text = text.replace(item_sheet_old, item_sheet_new, 1)

organizer_old = 'fun clearEditor(){editing=null;name="";phone="";makeDefault=false};ModalBottomSheet(onDismissRequest=onDismiss){'
organizer_new = 'fun clearEditor(){editing=null;name="";phone="";makeDefault=false};val sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true);ModalBottomSheet(onDismissRequest=onDismiss,sheetState=sheetState){'
assert organizer_old in text, 'OrganizerSheet anchor missing'
text = text.replace(organizer_old, organizer_new, 1)

form.write_text(text)

cat = catalog.read_text()
cat_old = '''    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column('''
cat_new = '''    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column('''
assert cat_old in cat, 'CatalogEditorSheet anchor missing'
cat = cat.replace(cat_old, cat_new, 1)
catalog.write_text(cat)

b = build.read_text()
assert 'versionCode = 49' in b, 'RC15 versionCode anchor missing'
assert 'versionName = "5.0.0-rc15"' in b, 'RC15 versionName anchor missing'
b = b.replace('versionCode = 49', 'versionCode = 50', 1)
b = b.replace('versionName = "5.0.0-rc15"', 'versionName = "5.0.0-rc16"', 1)
build.write_text(b)

(root / 'RC16_UPDATES.md').write_text('''# IGP Organizer Form 5.0.0-rc16

- Stabilized all text-input ModalBottomSheet screens by skipping the partially-expanded anchor, preventing the catalog/location/editor sheets from jumping when the keyboard or IME insets change.
- Applied the same keyboard-stable behavior to Product Catalog search, Location chooser, Item editor, Organizer editor, and standalone Catalog item editor.
- Approximate quantity remains selectable in Add/Edit and remains exported in the PDF, but the Approx. / تقريباً / نزیکەی note is no longer rendered in normal in-app item cards or the form preview.
- Improved form-preview summary width allocation so long item names cannot squeeze quantity/price values into one-character vertical wrapping.
- Version bumped to 5.0.0-rc16 (versionCode 50).
''')

form_text = form.read_text()
assert 'rememberModalBottomSheetState(skipPartiallyExpanded = true)' in form_text
assert 'FilterChip(selected=x.approximate' not in form_text
assert preview_old not in form_text
assert 'val note = if (item.approximate)' in (root / 'app/src/main/java/com/infinitygreenpower/organizerform/export/pdf/PdfExporter.kt').read_text()
print('RC16 patch validation OK')
