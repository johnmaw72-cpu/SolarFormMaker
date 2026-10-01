from pathlib import Path

root = Path("/tmp/igp-build/IGP")

build = root / "app/build.gradle.kts"
text = build.read_text()
assert 'versionCode = 68' in text
assert 'versionName = "5.0.0-rc34"' in text
text = text.replace('versionCode = 68', 'versionCode = 69', 1)
text = text.replace('versionName = "5.0.0-rc34"', 'versionName = "5.0.0-rc35"', 1)
build.write_text(text)

# ICU4J supplies Arabic shaping for both Arabic and Sorani/Kurdish letters.
text = build.read_text()
dep = '    implementation("com.ibm.icu:icu4j:78.3")\n'
anchor = '    implementation("com.tom-roush:pdfbox-android:2.0.27.0")\n'
assert anchor in text
if dep not in text:
    text = text.replace(anchor, anchor + dep, 1)
build.write_text(text)

pdf = root / "app/src/main/java/com/infinitygreenpower/organizerform/export/pdf/PdfExporter.kt"
text = pdf.read_text()
old = """            // RC34: draw the visible Notes with StaticLayout directly on the PDF Canvas.
            // This keeps Android's correct Arabic/Sorani shaping/BiDi while producing PDF text
            // drawing operations instead of embedding a rasterized Notes image.
            canvas!!.save()
            canvas!!.translate(left.toFloat(), top.toFloat())
            textLayout.draw(canvas!!)
            canvas!!.restore()

"""
assert old in text
new = """            // RC35: do not draw Notes through Android/Skia. StaticLayout is used only for
            // wrapping and geometry. UnicodeNoteTextLayer is the single visible+extractable
            // PDF text representation, which prevents duplicate presentation-form extraction.

"""
text = text.replace(old, new, 1)
pdf.write_text(text)

notes = root / "RC35_UPDATES.md"
notes.write_text("""# RC35 — Single-layer Arabic/Kurdish PDF Notes

- Base: exact RC34 source.
- Version: 5.0.0-rc35 / versionCode 69.
- Removed the Android/Skia visible Notes text stream that caused duplicate presentation-form extraction.
- StaticLayout is retained only for wrapping and line geometry.
- PDFBox now paints the Notes exactly once using shaped RTL visual text.
- The same PDF text span carries /ActualText with the clean logical Arabic/Sorani Unicode.
- No bitmap fallback and no second Notes text representation remain.
""")

assert 'RC35: do not draw Notes through Android/Skia' in pdf.read_text()
assert 'versionCode = 69' in build.read_text()
assert 'versionName = "5.0.0-rc35"' in build.read_text()
