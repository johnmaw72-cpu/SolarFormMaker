from pathlib import Path

root = Path('/tmp/igp-build/IGP')
pkg = root / 'app/src/main/java/com/infinitygreenpower/organizerform'

# Top/system-bar + keyboard behavior
app = pkg / 'app/IGPOrganizerApp.kt'
t = app.read_text()
if 'import androidx.compose.foundation.layout.ime\n' not in t:
    t = t.replace(
        'import androidx.compose.foundation.layout.fillMaxSize\n',
        'import androidx.compose.foundation.layout.fillMaxSize\nimport androidx.compose.foundation.layout.ime\nimport androidx.compose.foundation.layout.safeDrawing\n',
        1,
    )
if 'import androidx.compose.ui.platform.LocalDensity\n' not in t:
    t = t.replace(
        'import androidx.compose.ui.platform.LocalContext\n',
        'import androidx.compose.ui.platform.LocalContext\nimport androidx.compose.ui.platform.LocalDensity\n',
        1,
    )
old = '''    val expanded = LocalConfiguration.current.screenWidthDp >= 600
    val select: (Destination) -> Unit = { destination ->'''
new = '''    val expanded = LocalConfiguration.current.screenWidthDp >= 600
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val select: (Destination) -> Unit = { destination ->'''
assert old in t, 'OrganizerNavigation anchor missing'
t = t.replace(old, new, 1)
assert 'contentWindowInsets = WindowInsets(0),' in t, 'top-level inset anchor missing'
t = t.replace('contentWindowInsets = WindowInsets(0),', 'contentWindowInsets = WindowInsets.safeDrawing,', 1)
assert 'if (!expanded) NavigationBar {' in t, 'bottom navigation anchor missing'
t = t.replace('if (!expanded) NavigationBar {', 'if (!expanded && !imeVisible) NavigationBar {', 1)
app.write_text(t)

manifest = root / 'app/src/main/AndroidManifest.xml'
t = manifest.read_text()
old = '''        <activity
            android:name=".MainActivity"
            android:exported="true">'''
new = '''        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">'''
assert old in t, 'MainActivity manifest anchor missing'
manifest.write_text(t.replace(old, new, 1))

form = pkg / 'feature/form/FormScreen.kt'
t = form.read_text()
old = '''    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state.step <= 4) FormActions(
                step = state.step,
                lang = lang,
                onSave = vm::saveDraft,
                onBack = { if (state.step > 1) vm.goTo(state.step - 1) },
                onContinue = { if (state.step < 4) vm.goTo(state.step + 1) else vm.preview() }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp, 48.dp, 18.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {'''
new = '''    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state.step <= 4) {
                Box(Modifier.imePadding()) {
                    FormActions(
                        step = state.step,
                        lang = lang,
                        onSave = vm::saveDraft,
                        onBack = { if (state.step > 1) vm.goTo(state.step - 1) },
                        onContinue = { if (state.step < 4) vm.goTo(state.step + 1) else vm.preview() }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(18.dp, 20.dp, 18.dp, 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {'''
assert old in t, 'Form scaffold anchor missing'
t = t.replace(old, new, 1)
form.write_text(t)

# PDF: Approx. moves into a dedicated Note column, Quantity stays numeric.
pdf = pkg / 'export/pdf/PdfExporter.kt'
t = pdf.read_text()
start = t.index('        fun materials(s: FormUiState, settings: Settings) {')
end = t.index('        private fun totalRow', start)
replacement = r'''        fun materials(s: FormUiState, settings: Settings) {
            section(t(lang, "Required Materials", "المواد المطلوبة", "ماددە پێویستەکان"), keepNext = 54)
            materialHeader(settings)

            s.items.forEachIndexed { index, item ->
                val itemWidth = if (settings.showPrices) 304 else 388
                val quantityWidth = 55
                val noteWidth = 72
                val priceWidth = if (settings.showPrices) 84 else 0
                val priceX: Int
                val noteX: Int
                val quantityX: Int
                val itemX: Int
                if (rtl) {
                    priceX = left
                    noteX = left + priceWidth
                    quantityX = noteX + noteWidth
                    itemX = quantityX + quantityWidth
                } else {
                    itemX = left
                    quantityX = left + itemWidth
                    noteX = quantityX + quantityWidth
                    priceX = noteX + noteWidth
                }
                val textWidth = itemWidth - 14
                val name = "${index + 1}. ${item.name.trim()}"
                val specification = item.specification.trim()
                val nameHeight = measure(name, textWidth, 9, true, Layout.Alignment.ALIGN_NORMAL)
                val specHeight = if (specification.isBlank()) 0 else measure(specification, textWidth, 8, false, Layout.Alignment.ALIGN_NORMAL)
                val rowHeight = maxOf(28, 4 + nameHeight + (if (specHeight > 0) 1 + specHeight else 0) + 4)
                ensure(rowHeight) { materialHeader(settings) }
                paint.color = if (index % 2 == 0) rowTint else Color.WHITE
                canvas!!.drawRect(left.toFloat(), y.toFloat(), right.toFloat(), (y + rowHeight).toFloat(), paint)
                drawCell(name, itemX + 7f, y + 4, textWidth, 9, true, navy, Layout.Alignment.ALIGN_NORMAL)
                if (specification.isNotBlank()) {
                    drawCell(specification, itemX + 7f, y + 4 + nameHeight + 1, textWidth, 8, false, muted, Layout.Alignment.ALIGN_NORMAL)
                }

                val quantity = item.quantity.toString()
                val quantityHeight = measure(quantity, quantityWidth, 9, true, Layout.Alignment.ALIGN_CENTER)
                drawCell(quantity, quantityX.toFloat(), y + ((rowHeight - quantityHeight) / 2), quantityWidth, 9, true, navy, Layout.Alignment.ALIGN_CENTER)

                val note = if (item.approximate) t(lang, "Approx.", "تقريباً", "نزیکەی") else ""
                if (note.isNotBlank()) {
                    val noteHeight = measure(note, noteWidth - 6, 8, true, Layout.Alignment.ALIGN_CENTER)
                    drawCell(note, (noteX + 3).toFloat(), y + ((rowHeight - noteHeight) / 2), noteWidth - 6, 8, true, muted, Layout.Alignment.ALIGN_CENTER)
                }

                if (settings.showPrices) {
                    val price = item.unitPrice.ifBlank { "-" }
                    val priceHeight = measure(price, priceWidth - 8, 9, false, Layout.Alignment.ALIGN_CENTER)
                    drawCell(price, (priceX + 4).toFloat(), y + ((rowHeight - priceHeight) / 2), priceWidth - 8, 9, false, navy, Layout.Alignment.ALIGN_CENTER)
                }

                paint.color = border
                canvas!!.drawLine(left.toFloat(), (y + rowHeight).toFloat(), right.toFloat(), (y + rowHeight).toFloat(), paint)
                if (rtl) {
                    canvas!!.drawLine(itemX.toFloat(), y.toFloat(), itemX.toFloat(), (y + rowHeight).toFloat(), paint)
                    canvas!!.drawLine(quantityX.toFloat(), y.toFloat(), quantityX.toFloat(), (y + rowHeight).toFloat(), paint)
                    if (settings.showPrices) canvas!!.drawLine(noteX.toFloat(), y.toFloat(), noteX.toFloat(), (y + rowHeight).toFloat(), paint)
                } else {
                    canvas!!.drawLine(quantityX.toFloat(), y.toFloat(), quantityX.toFloat(), (y + rowHeight).toFloat(), paint)
                    canvas!!.drawLine(noteX.toFloat(), y.toFloat(), noteX.toFloat(), (y + rowHeight).toFloat(), paint)
                    if (settings.showPrices) canvas!!.drawLine(priceX.toFloat(), y.toFloat(), priceX.toFloat(), (y + rowHeight).toFloat(), paint)
                }
                y += rowHeight
            }

            if (settings.showEstimatedTotal) {
                val hasAnyEnteredPrice = s.items.any { it.unitPrice.toBigDecimalOrNull() != null }
                totalRow(
                    t(lang, "Estimated Total", "المجموع التقديري", "کۆی خەمڵاندراو"),
                    if (hasAnyEnteredPrice) "${s.estimatedTotal.stripTrailingZeros()} ${s.currency}" else "-"
                )
            }
        }

        private fun materialHeader(settings: Settings) {
            val itemWidth = if (settings.showPrices) 304 else 388
            val quantityWidth = 55
            val noteWidth = 72
            val priceWidth = if (settings.showPrices) 84 else 0
            val priceX: Int
            val noteX: Int
            val quantityX: Int
            val itemX: Int
            if (rtl) {
                priceX = left
                noteX = left + priceWidth
                quantityX = noteX + noteWidth
                itemX = quantityX + quantityWidth
            } else {
                itemX = left
                quantityX = left + itemWidth
                noteX = quantityX + quantityWidth
                priceX = noteX + noteWidth
            }
            val headerHeight = 24
            ensure(headerHeight + 2)
            paint.color = teal
            canvas!!.drawRect(left.toFloat(), y.toFloat(), right.toFloat(), (y + headerHeight).toFloat(), paint)
            drawCell(t(lang, "Item / Specification", "المادة / المواصفات", "ماددە / تایبەتمەندی"), (itemX + 7).toFloat(), y + 5, itemWidth - 14, 8, true, Color.WHITE, Layout.Alignment.ALIGN_NORMAL)
            drawCell(t(lang, "Qty", "الكمية", "بڕ"), quantityX.toFloat(), y + 5, quantityWidth, 8, true, Color.WHITE, Layout.Alignment.ALIGN_CENTER)
            drawCell(t(lang, "Note", "ملاحظة", "تێبینی"), noteX.toFloat(), y + 5, noteWidth, 8, true, Color.WHITE, Layout.Alignment.ALIGN_CENTER)
            if (settings.showPrices) {
                drawCell(t(lang, "Unit Price", "سعر الوحدة", "نرخی دانە"), (priceX + 4).toFloat(), y + 5, priceWidth - 8, 8, true, Color.WHITE, Layout.Alignment.ALIGN_CENTER)
            }
            paint.color = Color.argb(90, 255, 255, 255)
            if (rtl) {
                canvas!!.drawLine(itemX.toFloat(), y.toFloat(), itemX.toFloat(), (y + headerHeight).toFloat(), paint)
                canvas!!.drawLine(quantityX.toFloat(), y.toFloat(), quantityX.toFloat(), (y + headerHeight).toFloat(), paint)
                if (settings.showPrices) canvas!!.drawLine(noteX.toFloat(), y.toFloat(), noteX.toFloat(), (y + headerHeight).toFloat(), paint)
            } else {
                canvas!!.drawLine(quantityX.toFloat(), y.toFloat(), quantityX.toFloat(), (y + headerHeight).toFloat(), paint)
                canvas!!.drawLine(noteX.toFloat(), y.toFloat(), noteX.toFloat(), (y + headerHeight).toFloat(), paint)
                if (settings.showPrices) canvas!!.drawLine(priceX.toFloat(), y.toFloat(), priceX.toFloat(), (y + headerHeight).toFloat(), paint)
            }
            y += headerHeight
        }

'''
t = t[:start] + replacement + t[end:]
pdf.write_text(t)

build = root / 'app/build.gradle.kts'
t = build.read_text()
assert 'versionCode = 45' in t, 'Expected RC11 versionCode missing'
assert 'versionName = "5.0.0-rc11"' in t, 'Expected RC11 versionName missing'
t = t.replace('versionCode = 45', 'versionCode = 46', 1)
t = t.replace('versionName = "5.0.0-rc11"', 'versionName = "5.0.0-rc12"', 1)
build.write_text(t)

# Sanity checks
assert 'contentWindowInsets = WindowInsets.safeDrawing' in app.read_text()
assert 'imeVisible = WindowInsets.ime.getBottom' in app.read_text()
assert 'android:windowSoftInputMode="adjustResize"' in manifest.read_text()
assert 'Modifier.fillMaxSize().padding(padding).imePadding()' in form.read_text()
assert 'KeyboardType.Text' in form.read_text()
assert 'Add AC & civil-work exclusions' in form.read_text()
assert 't(lang, "Note", "ملاحظة", "تێبینی")' in pdf.read_text()
assert 'val note = if (item.approximate)' in pdf.read_text()
assert 'val quantity = item.quantity.toString()' in pdf.read_text()
