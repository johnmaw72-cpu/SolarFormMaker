from pathlib import Path
import re
import sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else '/tmp/igp-build/IGP')
pkg = root / 'app/src/main/java/com/infinitygreenpower/organizerform'
form = pkg / 'feature/form/FormScreen.kt'
pdf = pkg / 'export/pdf/PdfExporter.kt'
build = root / 'app/build.gradle.kts'

# ---------------- FormScreen ----------------
t = form.read_text()

# Imports for zoomable PDF and scrollable editor sheets.
imports = {
    'import androidx.compose.foundation.gestures.detectTransformGestures\n': 'import androidx.compose.foundation.Image\n',
    'import androidx.compose.foundation.verticalScroll\n': 'import androidx.compose.foundation.text.KeyboardOptions\n',
    'import androidx.compose.ui.draw.clip\n': 'import androidx.compose.ui.Alignment\n',
    'import androidx.compose.ui.geometry.Offset\n': 'import androidx.compose.ui.draw.clip\n',
    'import androidx.compose.ui.graphics.graphicsLayer\n': 'import androidx.compose.ui.geometry.Offset\n',
    'import androidx.compose.ui.input.pointer.pointerInput\n': 'import androidx.compose.ui.graphics.graphicsLayer\n',
}
for line, anchor in imports.items():
    if line not in t:
        assert anchor in t, f'import anchor missing for {line.strip()}'
        t = t.replace(anchor, anchor + line, 1)

# RC12 double-counted the IME: adjustResize + LazyColumn imePadding + bottom-bar imePadding.
# Keep adjustResize at the Activity level and let Scaffold resize naturally.
t = t.replace('Box(Modifier.imePadding()) {\n                    FormActions(', 'Box(Modifier.fillMaxWidth()) {\n                    FormActions(', 1)
t = t.replace('Modifier.fillMaxSize().padding(padding).imePadding(),', 'Modifier.fillMaxSize().padding(padding),', 1)
t = t.replace('Surface(Modifier.imePadding(),shadowElevation=6.dp)', 'Surface(Modifier.fillMaxWidth(),shadowElevation=6.dp)', 1)
t = t.replace('Surface(Modifier.imePadding(), shadowElevation=6.dp)', 'Surface(Modifier.fillMaxWidth(), shadowElevation=6.dp)', 1)

# Money must never be displayed in scientific notation (120 -> 120, not 1.2E+2).
t = re.sub(r'\.stripTrailingZeros\(\)(?!\.toPlainString\(\))', '.stripTrailingZeros().toPlainString()', t)

# Quantity editor: allow the user to clear the field while typing instead of immediately restoring 1.
# Add a local text state while preserving Int storage in DraftItem.
item_start = t.find('@OptIn(ExperimentalMaterial3Api::class) @Composable private fun ItemEditorSheet')
assert item_start >= 0, 'ItemEditorSheet not found'
item_end = t.find('@OptIn(ExperimentalMaterial3Api::class) @Composable private fun OrganizerSheet', item_start)
assert item_end > item_start, 'ItemEditorSheet end not found'
item_block = t[item_start:item_end]
if 'quantityText' not in item_block:
    item_block = item_block.replace('var item by remember{mutableStateOf(base)};', 'var item by remember(base.id){mutableStateOf(base)};var quantityText by remember(base.id){mutableStateOf(base.quantity.toString())};', 1)

item_block = re.sub(
    r'Field\(item\.quantity\.toString\(\),\{item=item\.copy\(quantity=it\.toIntOrNull\(\)\?\.coerceAtLeast\(1\)\?:1\)\},',
    'Field(quantityText,{quantityText=it.filter(Char::isDigit)},',
    item_block,
    count=1,
)

# If formatting changed, use a broader fallback.
if 'Field(quantityText,{quantityText=it.filter(Char::isDigit)}' not in item_block:
    item_block, n = re.subn(
        r'Field\(item\.quantity\.toString\(\),\{[^}]*quantity[^}]*\},',
        'Field(quantityText,{quantityText=it.filter(Char::isDigit)},',
        item_block,
        count=1,
    )
    assert n == 1, 'Quantity Field anchor missing'

# Save only after a valid positive quantity is present; blank is allowed during editing.
# Preserve all RC11 approximate controls and other fields.
if 'onSave(item.copy(quantity=q))' not in item_block:
    save_matches = list(re.finditer(r'PrimaryButton\((.*?)\{onSave\(item\)\}', item_block, flags=re.S))
    assert save_matches, 'Item editor Save Material button not found'
    m = save_matches[-1]
    old = m.group(0)
    new = old.replace('{onSave(item)}', '{quantityText.toIntOrNull()?.takeIf{it>0}?.let{q->onSave(item.copy(quantity=q))}}')
    item_block = item_block[:m.start()] + new + item_block[m.end():]

# Remove sheet-level imePadding (adjustResize already changes the available height) and make editor scrollable.
item_block = item_block.replace('Column(Modifier.padding(18.dp).imePadding())', 'Column(Modifier.fillMaxHeight(.9f).verticalScroll(rememberScrollState()).padding(18.dp).navigationBarsPadding())')
t = t[:item_start] + item_block + t[item_end:]

# Organizer sheet also had extra IME padding; LazyColumn already scrolls and adjustResize handles keyboard height.
t = t.replace('LazyColumn(Modifier.padding(18.dp).imePadding())', 'LazyColumn(Modifier.fillMaxHeight(.9f).padding(18.dp))', 1)

# Product picker: categories always visible above item results, with All + category filters.
cat_start = t.find('@OptIn(ExperimentalMaterial3Api::class) @Composable private fun CatalogSheet')
assert cat_start >= 0, 'CatalogSheet not found'
cat_end = t.find('@OptIn(ExperimentalMaterial3Api::class) @Composable private fun ItemEditorSheet', cat_start)
assert cat_end > cat_start, 'CatalogSheet end not found'
new_catalog = '''@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogSheet(
    catalog: List<CatalogItemEntity>,
    lang: String,
    onDismiss: () -> Unit,
    onAdd: (CatalogItemEntity) -> Unit
) {
    var q by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val categories = remember(catalog) {
        catalog.map { it.category.trim() }.filter { it.isNotBlank() }.distinct().sortedBy { it.lowercase() }
    }
    val filtered = remember(catalog, q, selectedCategory) {
        catalog.filter { item ->
            val categoryMatch = selectedCategory == null || item.category.equals(selectedCategory, ignoreCase = true)
            val searchMatch = q.isBlank() || item.name.contains(q, true) || item.category.contains(q, true) || item.legacyDescription.contains(q, true)
            categoryMatch && searchMatch
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxHeight(.9f).padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(tr(lang, "Product Catalog", "كتالوج المنتجات", "کاتەلۆگی بەرهەم"), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Field(q, { q = it }, tr(lang, "Search", "بحث", "گەڕان"))
            Text(tr(lang, "Categories", "الفئات", "پۆلەکان"), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text(tr(lang, "All", "الكل", "هەموو")) }
                )
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category, maxLines = 1) }
                    )
                }
            }
            Text(
                tr(lang, "${filtered.size} items", "${filtered.size} مادة", "${filtered.size} ماددە"),
                color = Muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            LazyColumn(Modifier.weight(1f)) {
                items(filtered, key = { it.id }) { x ->
                    ListItem(
                        headlineContent = { Text(x.name) },
                        supportingContent = { Text(x.category) },
                        trailingContent = {
                            IconButton({ onAdd(x) }) {
                                Icon(Icons.Outlined.AddCircle, null, tint = InfinityTeal)
                            }
                        }
                    )
                }
            }
        }
    }
}
'''
t = t[:cat_start] + new_catalog + t[cat_end:]

# Higher-resolution PDF raster for zooming.
t = t.replace('val scale = minOf(2.2f, 1400f / page.width.toFloat()).coerceAtLeast(1f)', 'val scale = minOf(3.2f, 2000f / page.width.toFloat()).coerceAtLeast(1f)', 1)

# Replace preview dialog with pinch-zoom/pan implementation while retaining page navigation.
prev_start = t.find('@Composable\nprivate fun PdfPreviewDialog'.replace('\\n','\n'))
assert prev_start >= 0, 'PdfPreviewDialog start not found'
prev_end = t.find('@Composable private fun FormActions', prev_start)
assert prev_end > prev_start, 'PdfPreviewDialog end not found'
new_preview = '''@Composable
private fun PdfPreviewDialog(path: String, lang: String, onDismiss: () -> Unit) {
    var pageIndex by remember(path) { mutableIntStateOf(0) }
    var pageCount by remember(path) { mutableIntStateOf(0) }
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
    var loading by remember(path) { mutableStateOf(true) }
    var error by remember(path) { mutableStateOf<String?>(null) }
    var zoom by remember(path, pageIndex) { mutableFloatStateOf(1f) }
    var offset by remember(path, pageIndex) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(path, pageIndex) {
        loading = true
        error = null
        zoom = 1f
        offset = Offset.Zero
        val result = withContext(Dispatchers.IO) { runCatching { renderPdfPage(File(path), pageIndex) } }
        result.onSuccess { rendered ->
            pageCount = rendered.pageCount
            if (pageIndex != rendered.pageIndex) pageIndex = rendered.pageIndex
            bitmap = rendered.bitmap
        }.onFailure {
            bitmap = null
            error = tr(lang, "Could not render the PDF preview.", "تعذر عرض معاينة PDF.", "نەتوانرا پێشبینینی PDF پیشان بدرێت.")
        }
        loading = false
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = AppBackground) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(tr(lang, "PDF Preview", "معاينة PDF", "پێشبینینی PDF"), color = Navy, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        if (pageCount > 0) Text(
                            tr(lang, "Page ${pageIndex + 1} of $pageCount", "الصفحة ${pageIndex + 1} من $pageCount", "لاپەڕە ${pageIndex + 1} لە $pageCount"),
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }
                    TextButton(onClick = { zoom = 1f; offset = Offset.Zero }) { Text("${(zoom * 100).toInt()}%") }
                    IconButton(onDismiss) { Icon(Icons.Outlined.Close, contentDescription = null, tint = Navy) }
                }
                HorizontalDivider()
                Box(
                    Modifier.fillMaxWidth().weight(1f).padding(8.dp).clip(RoundedCornerShape(10.dp))
                        .pointerInput(bitmap, pageIndex) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                val next = (zoom * gestureZoom).coerceIn(1f, 5f)
                                zoom = next
                                offset = if (next <= 1.01f) Offset.Zero else offset + pan
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        loading -> CircularProgressIndicator()
                        error != null -> Text(error.orEmpty(), color = Danger)
                        bitmap != null -> Surface(
                            Modifier.fillMaxSize(),
                            shadowElevation = 3.dp,
                            color = androidx.compose.ui.graphics.Color.White
                        ) {
                            Image(
                                bitmap!!.asImageBitmap(),
                                contentDescription = tr(lang, "PDF page", "صفحة PDF", "لاپەڕەی PDF"),
                                modifier = Modifier.fillMaxSize().graphicsLayer {
                                    scaleX = zoom
                                    scaleY = zoom
                                    translationX = offset.x
                                    translationY = offset.y
                                },
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
                Text(
                    tr(lang, "Pinch to zoom • drag to move", "قرّب بإصبعين • اسحب للتحريك", "بە دوو پەنجە زووم بکە • ڕابکێشە بۆ جوڵاندن"),
                    color = Muted,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 2.dp)
                )
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) },
                        enabled = !loading && pageIndex > 0,
                        modifier = Modifier.weight(1f)
                    ) { Text(tr(lang, "Previous", "السابق", "پێشوو")) }
                    Button(
                        onClick = { pageIndex = (pageIndex + 1).coerceAtMost((pageCount - 1).coerceAtLeast(0)) },
                        enabled = !loading && pageCount > 0 && pageIndex < pageCount - 1,
                        modifier = Modifier.weight(1f)
                    ) { Text(tr(lang, "Next", "التالي", "دواتر")) }
                }
            }
        }
    }
}

'''
t = t[:prev_start] + new_preview + t[prev_end:]

form.write_text(t)

# ---------------- PDF money formatting ----------------
p = pdf.read_text()
p = re.sub(r'\.stripTrailingZeros\(\)(?!\.toPlainString\(\))', '.stripTrailingZeros().toPlainString()', p)
pdf.write_text(p)

# ---------------- Version ----------------
b = build.read_text()
b, n1 = re.subn(r'versionCode\s*=\s*47', 'versionCode = 48', b, count=1)
assert n1 == 1, 'RC13 versionCode 47 not found'
b, n2 = re.subn(r'versionName\s*=\s*"5\.0\.0-rc13"', 'versionName = "5.0.0-rc14"', b, count=1)
assert n2 == 1, 'RC13 versionName not found'
build.write_text(b)

# Source-level validation.
ft = form.read_text()
pt = pdf.read_text()
checks = {
    'blank quantity editing': 'var quantityText' in ft and 'quantityText=it.filter(Char::isDigit)' in ft,
    'plain UI money': '.stripTrailingZeros().toPlainString()' in ft,
    'plain PDF money': '.stripTrailingZeros().toPlainString()' in pt,
    'category filters': 'selectedCategory' in ft and 'FilterChip' in ft and 'Categories' in ft,
    'pinch zoom': 'detectTransformGestures' in ft and 'graphicsLayer' in ft and 'coerceIn(1f, 5f)' in ft,
    'no form double IME': 'Modifier.fillMaxSize().padding(padding).imePadding()' not in ft,
    'no action double IME': 'Surface(Modifier.imePadding()' not in ft,
    'rc14 code': 'versionCode = 48' in build.read_text(),
}
missing = [k for k,v in checks.items() if not v]
assert not missing, f'RC14 validation failed: {missing}'
print('RC14 patch validation OK:', ', '.join(checks))
