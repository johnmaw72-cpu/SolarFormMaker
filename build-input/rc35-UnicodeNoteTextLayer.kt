package com.infinitygreenpower.organizerform.export.pdf

import android.content.Context
import com.ibm.icu.text.ArabicShaping
import com.ibm.icu.text.Bidi
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.graphics.state.RenderingMode
import com.tom_roush.pdfbox.util.Matrix
import java.io.File
import java.io.FileOutputStream

/**
 * RC35 single-source PDF Notes renderer.
 *
 * Notes are NOT painted by Android PdfDocument/Skia. StaticLayout in PdfExporter only calculates
 * wrapping, line positions and RTL geometry. This class paints each line exactly once with PDFBox.
 *
 * The visible string is Arabic-shaped and converted to visual BiDi order for correct display with
 * a Type-0 font. The very same marked-content span carries /ActualText with the original logical
 * Unicode, so extraction/search/copy/import returns normal Arabic/Sorani rather than presentation
 * forms. Because there is only one painted text object, extractors cannot see a second garbled copy.
 */
object UnicodeNoteTextLayer {
    private const val BUNDLED_FONT_ASSET = "fonts/Amiri-Regular.ttf"

    data class Line(
        val pageNumber: Int,
        val x: Float,
        val baselineFromTop: Float,
        val fontSize: Float,
        val width: Float,
        val rtl: Boolean,
        val text: String
    )

    fun apply(context: Context, input: File, output: File, lines: List<Line>) {
        if (lines.isEmpty()) {
            input.copyTo(output, overwrite = true)
            return
        }

        PDFBoxResourceLoader.init(context.applicationContext)
        PDDocument.load(input).use { document ->
            val font = loadBundledUnicodeFont(context, document)

            val prepared = lines.mapNotNull { line ->
                val logical = sanitize(line.text)
                if (logical.isBlank()) return@mapNotNull null
                val visual = toVisualPdfText(logical, line.rtl)
                font.encode(visual)
                PreparedLine(line, logical, visual)
            }

            prepared.groupBy { it.line.pageNumber }.forEach { (pageNumber, pageLines) ->
                val pageIndex = pageNumber - 1
                if (pageIndex !in 0 until document.numberOfPages) return@forEach
                val page = document.getPage(pageIndex)

                PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
                ).use { content ->
                    pageLines.forEach { preparedLine ->
                        val line = preparedLine.line
                        val logical = preparedLine.logical
                        val visual = preparedLine.visual

                        val propsDictionary = COSDictionary().apply {
                            setString(COSName.ACTUAL_TEXT, logical)
                        }
                        val props = PDPropertyList.create(propsDictionary)

                        content.beginMarkedContent(COSName.getPDFName("Span"), props)
                        content.beginText()
                        content.setFont(font, line.fontSize)
                        content.setRenderingMode(RenderingMode.FILL)
                        content.setNonStrokingColor(18, 50, 79)

                        val naturalWidth = font.getStringWidth(visual) / 1000f * line.fontSize
                        val horizontalScale = if (naturalWidth > 0.01f && line.width > 0.01f) {
                            (line.width / naturalWidth * 100f).coerceIn(60f, 180f)
                        } else {
                            100f
                        }
                        content.setHorizontalScaling(horizontalScale)

                        val yFromBottom = page.mediaBox.height - line.baselineFromTop
                        content.setTextMatrix(Matrix.getTranslateInstance(line.x, yFromBottom))
                        content.showText(visual)
                        content.endText()
                        content.endMarkedContent()
                    }
                }
            }

            FileOutputStream(output).use(document::save)
        }
    }

    private data class PreparedLine(
        val line: Line,
        val logical: String,
        val visual: String
    )

    private fun toVisualPdfText(logical: String, rtl: Boolean): String {
        if (!containsArabicScript(logical)) return logical

        val shaped = ArabicShaping(
            ArabicShaping.LETTERS_SHAPE or ArabicShaping.TEXT_DIRECTION_LOGICAL
        ).shape(logical)

        val bidi = Bidi(
            shaped,
            if (rtl) Bidi.DIRECTION_RIGHT_TO_LEFT else Bidi.DIRECTION_LEFT_TO_RIGHT
        )
        return bidi.writeReordered(
            Bidi.DO_MIRRORING.toInt() or Bidi.KEEP_BASE_COMBINING.toInt()
        )
    }

    private fun containsArabicScript(text: String): Boolean = text.any { ch ->
        ch.code in 0x0600..0x06FF ||
            ch.code in 0x0750..0x077F ||
            ch.code in 0x08A0..0x08FF ||
            ch.code in 0xFB50..0xFDFF ||
            ch.code in 0xFE70..0xFEFF
    }

    private fun loadBundledUnicodeFont(context: Context, document: PDDocument): PDType0Font =
        context.assets.open(BUNDLED_FONT_ASSET).use { stream ->
            PDType0Font.load(document, stream, true)
        }

    private fun sanitize(text: String): String = text
        .replace("\u0000", "")
        .replace("\u202A", "")
        .replace("\u202B", "")
        .replace("\u202C", "")
        .replace("\u2066", "")
        .replace("\u2067", "")
        .replace("\u2069", "")
}
