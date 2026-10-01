from pathlib import Path
import shutil

root = Path("/tmp/igp-build/IGP")

build = root / "app/build.gradle.kts"
text = build.read_text()
assert 'versionCode = 68' in text
assert 'versionName = "5.0.0-rc34"' in text
text = text.replace('versionCode = 68', 'versionCode = 69', 1)
text = text.replace('versionName = "5.0.0-rc34"', 'versionName = "5.0.0-rc35"', 1)
build.write_text(text)

pdf = root / "app/src/main/java/com/infinitygreenpower/organizerform/export/pdf/PdfExporter.kt"
text = pdf.read_text()

old = '''        val rawPdf = File(file.parentFile, file.name + ".canvas")
        val noteTextLayers = mutableListOf<UnicodeNoteTextLayer.Line>()
        try {
            // RC34: Notes are drawn directly onto Android's PDF Canvas as text. They are never
            // converted to a bitmap. A bundled Unicode font is then used to append /ActualText
            // semantics so Arabic/Sorani copy, search and PC import remain device-independent.
            renderCanvasPdf(rawPdf, s, settings, resolvedLang, noteTextLayers)
            UnicodeNoteTextLayer.apply(context, rawPdf, file, noteTextLayers)
        } finally {
            rawPdf.delete()
        }
'''
new = '''        val rawPdf = File(file.parentFile, file.name + ".canvas")
        val notesOverlayPdf = File(file.parentFile, file.name + ".notes")
        val noteTextLayers = mutableListOf<UnicodeNoteTextLayer.Line>()
        try {
            // RC35: the base PDF reserves the Notes geometry but does not paint note glyphs.
            // Android renders the Notes once into a transparent PDF overlay (preserving perfect
            // Arabic/Sorani shaping), then PDFBox places that overlay inside one /ActualText span.
            renderCanvasPdf(rawPdf, notesOverlayPdf, s, settings, resolvedLang, noteTextLayers)
            UnicodeNoteTextLayer.apply(context, rawPdf, notesOverlayPdf, file, noteTextLayers)
        } finally {
            rawPdf.delete()
            notesOverlayPdf.delete()
        }
'''
assert old in text
text = text.replace(old, new, 1)

old = '''    private fun renderCanvasPdf(
        target: File,
        s: FormUiState,
'''
new = '''    private fun renderCanvasPdf(
        target: File,
        notesTarget: File,
        s: FormUiState,
'''
assert old in text
text = text.replace(old, new, 1)

old = '''        val pdf = PdfDocument()
        try {
            val writer = Writer(pdf, settings, resolvedLang, noteTextLayers)
'''
new = '''        val pdf = PdfDocument()
        val notesPdf = PdfDocument()
        try {
            val writer = Writer(pdf, notesPdf, settings, resolvedLang, noteTextLayers)
'''
assert old in text
text = text.replace(old, new, 1)

old = '''            FileOutputStream(target).use(pdf::writeTo)
        } finally {
            runCatching { pdf.close() }
        }
'''
new = '''            FileOutputStream(target).use(pdf::writeTo)
            FileOutputStream(notesTarget).use(notesPdf::writeTo)
        } finally {
            runCatching { pdf.close() }
            runCatching { notesPdf.close() }
        }
'''
assert old in text
text = text.replace(old, new, 1)

old = '''    private inner class Writer(
        private val doc: PdfDocument,
        private val settings: Settings,
'''
new = '''    private inner class Writer(
        private val doc: PdfDocument,
        private val notesDoc: PdfDocument,
        private val settings: Settings,
'''
assert old in text
text = text.replace(old, new, 1)

old = '''        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = 0
'''
new = '''        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var notesPage: PdfDocument.Page? = null
        private var notesCanvas: Canvas? = null
        private var y = 0
'''
assert old in text
text = text.replace(old, new, 1)

old = '''            page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, number).create())
            canvas = page!!.canvas.apply { drawColor(Color.WHITE) }
            y = 34
'''
new = '''            val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, number).create()
            page = doc.startPage(info)
            canvas = page!!.canvas.apply { drawColor(Color.WHITE) }
            notesPage = notesDoc.startPage(info)
            notesCanvas = notesPage!!.canvas
            y = 34
'''
assert old in text
text = text.replace(old, new, 1)

old = '''                doc.finishPage(currentPage)
            }
            page = null
            canvas = null
'''
new = '''                doc.finishPage(currentPage)
            }
            notesPage?.let(notesDoc::finishPage)
            page = null
            canvas = null
            notesPage = null
            notesCanvas = null
'''
assert old in text
text = text.replace(old, new, 1)

old = '''            // RC34: draw the visible Notes with StaticLayout directly on the PDF Canvas.
            // This keeps Android's correct Arabic/Sorani shaping/BiDi while producing PDF text
            // drawing operations instead of embedding a rasterized Notes image.
            canvas!!.save()
            canvas!!.translate(left.toFloat(), top.toFloat())
            textLayout.draw(canvas!!)
            canvas!!.restore()

'''
new = '''            // RC35: draw Notes only into the transparent overlay PDF. The base page contains
            // no Notes text at all, so the final PDF has no competing Skia text stream.
            notesCanvas!!.save()
            notesCanvas!!.translate(left.toFloat(), top.toFloat())
            textLayout.draw(notesCanvas!!)
            notesCanvas!!.restore()

'''
assert old in text
text = text.replace(old, new, 1)

pdf.write_text(text)

fonts = root / "app/src/main/assets/fonts"
if fonts.exists():
    shutil.rmtree(fonts)

(root / "RC35_UPDATES.md").write_text("""# RC35 — Single semantic Arabic/Kurdish PDF Notes

- Base: exact RC34 source.
- Version: 5.0.0-rc35 / versionCode 69.
- Notes are rendered by Android into a transparent PDF overlay, not a bitmap.
- The overlay is imported as a PDF Form XObject and drawn exactly once.
- The single marked-content span carries /ActualText with clean logical Arabic/Sorani Unicode.
- This removes RC34's duplicate visible-text + hidden-text extraction problem while retaining correct RTL shaping.
- No bundled font asset is required in RC35.
""")

assert 'notesOverlayPdf' in pdf.read_text()
assert 'notesCanvas!!.save()' in pdf.read_text()
assert 'versionCode = 69' in build.read_text()
assert 'versionName = "5.0.0-rc35"' in build.read_text()
