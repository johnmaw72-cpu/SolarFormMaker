package com.infinitygreenpower.organizerform.export.pdf

import android.content.Context
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
 * Adds machine-readable Unicode semantics to the visible Notes text produced by PdfDocument.
 *
 * RC34 deliberately separates the two responsibilities:
 * 1. Android StaticLayout draws the visible Arabic/Sorani Notes directly as PDF text operations,
 *    preserving the device's correct shaping and BiDi behavior without any bitmap conversion.
 * 2. PDFBox appends an invisible Type-0 Unicode text object for each visible line and tags it with
 *    /ActualText containing the original logical Unicode. This makes selection/search/copy and
 *    desktop import deterministic even when a vendor PDF font encodes shaped glyphs unusually.
 *
 * The Unicode font is bundled with the app, so export no longer depends on vendor /system/fonts
 * and there is no bitmap fallback.
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

            // Fail the export immediately if any note character cannot be represented. RC34 must
            // never silently degrade Arabic/Kurdish Notes back into an image-only PDF.
            lines.asSequence()
                .map { sanitize(it.text) }
                .filter { it.isNotBlank() }
                .forEach { font.encode(it) }

            lines.groupBy { it.pageNumber }.forEach { (pageNumber, pageLines) ->
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
                    pageLines.forEach lineLoop@{ line ->
                        val logical = sanitize(line.text)
                        if (logical.isBlank()) return@lineLoop

                        val propsDictionary = COSDictionary().apply {
                            setString(COSName.ACTUAL_TEXT, logical)
                        }
                        val props = PDPropertyList.create(propsDictionary)

                        content.beginMarkedContent(COSName.getPDFName("Span"), props)
                        content.beginText()
                        content.setFont(font, line.fontSize)
                        content.setRenderingMode(RenderingMode.NEITHER)

                        // Position the semantic object on the same baseline as its visible line.
                        // It is intentionally non-painting: the visible glyphs were already drawn
                        // by Android's StaticLayout, while /ActualText supplies canonical Unicode.
                        val yFromBottom = page.mediaBox.height - line.baselineFromTop
                        content.setTextMatrix(Matrix.getTranslateInstance(line.x, yFromBottom))
                        content.showText(logical)
                        content.endText()
                        content.endMarkedContent()
                    }
                }
            }

            FileOutputStream(output).use(document::save)
        }
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
