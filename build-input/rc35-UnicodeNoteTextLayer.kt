package com.infinitygreenpower.organizerform.export.pdf

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.multipdf.LayerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList
import java.io.File
import java.io.FileOutputStream

/**
 * RC35 single semantic Notes layer.
 *
 * Android/Skia renders the visible Arabic/Sorani glyphs into a separate transparent PDF page.
 * That page is imported as a Form XObject and drawn exactly once inside a marked-content span
 * whose /ActualText is the original logical Unicode.
 *
 * Result: correct RTL shaping, no bitmap Notes, and only one extractable representation.
 */
object UnicodeNoteTextLayer {
    data class Line(
        val pageNumber: Int,
        val x: Float,
        val baselineFromTop: Float,
        val fontSize: Float,
        val width: Float,
        val rtl: Boolean,
        val text: String
    )

    fun apply(context: Context, basePdf: File, notesOverlayPdf: File, output: File, lines: List<Line>) {
        if (lines.isEmpty()) {
            basePdf.copyTo(output, overwrite = true)
            return
        }

        PDFBoxResourceLoader.init(context.applicationContext)
        PDDocument.load(basePdf).use { document ->
            PDDocument.load(notesOverlayPdf).use { overlay ->
                val layerUtility = LayerUtility(document)
                val byPage = lines.groupBy { it.pageNumber }

                byPage.forEach { (pageNumber, pageLines) ->
                    val pageIndex = pageNumber - 1
                    if (pageIndex !in 0 until document.numberOfPages) return@forEach
                    if (pageIndex !in 0 until overlay.numberOfPages) return@forEach

                    val logicalText = pageLines
                        .map { sanitize(it.text) }
                        .filter { it.isNotBlank() }
                        .joinToString("\n")
                        .trim()
                    if (logicalText.isBlank()) return@forEach

                    val page = document.getPage(pageIndex)
                    layerUtility.wrapInSaveRestore(page)
                    val form = layerUtility.importPageAsForm(overlay, pageIndex)

                    val propsDictionary = COSDictionary().apply {
                        setString(COSName.ACTUAL_TEXT, logicalText)
                    }
                    val props = PDPropertyList.create(propsDictionary)

                    PDPageContentStream(
                        document,
                        page,
                        PDPageContentStream.AppendMode.APPEND,
                        true,
                        true
                    ).use { content ->
                        content.beginMarkedContent(COSName.getPDFName("Span"), props)
                        content.drawForm(form)
                        content.endMarkedContent()
                    }
                }
            }
            FileOutputStream(output).use(document::save)
        }
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
