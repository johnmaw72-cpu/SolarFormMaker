package com.infinitygreenpower.organizerform.export.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.infinitygreenpower.organizerform.data.preferences.Settings
import com.infinitygreenpower.organizerform.export.json.*
import com.infinitygreenpower.organizerform.feature.form.FormUiState
import com.infinitygreenpower.organizerform.feature.form.generatedLoadNote
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.Locale

data class ExportBundle(val pdf: File, val json: File)

class PdfExporter(private val context: Context) {
    fun create(state: FormUiState, settings: Settings, language: String): ExportBundle {
        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        val base = "OrganizerForm_${safe(state.clientName)}_${safe(state.capacity)}_${safe(state.date)}"
        val pdfFile = File(directory, "$base.pdf")
        val jsonFile = File(directory, "$base.json")
        val pdfTemp = File(directory, "$base.pdf.tmp")
        val jsonTemp = File(directory, "$base.json.tmp")
        return try {
            renderPdf(pdfTemp, state, settings, language)
            val json = toExport(state, settings).toJson()
            org.json.JSONObject(json)
            jsonTemp.writeText(json, Charsets.UTF_8)
            pdfTemp.copyTo(pdfFile, overwrite = true)
            jsonTemp.copyTo(jsonFile, overwrite = true)
            ExportBundle(pdfFile, jsonFile)
        } finally {
            pdfTemp.delete()
            jsonTemp.delete()
        }
    }

    private fun renderPdf(file: File, s: FormUiState, settings: Settings, lang: String) {
        val resolvedLang = normalizeLanguage(lang)
        val pdf = PdfDocument()
        try {
            val writer = Writer(pdf, settings, resolvedLang)
            writer.newPage(fullHeader = true)

            writer.section(t(resolvedLang, "Client Details", "بيانات العميل", "زانیاری کڕیار"))
            writer.pair(t(resolvedLang, "Client", "العميل", "کڕیار"), s.clientName)
            writer.pair(t(resolvedLang, "Phone", "الهاتف", "تەلەفۆن"), s.phone)
            writer.pair(t(resolvedLang, "Location", "الموقع", "شوێن"), s.location)
            writer.pair(t(resolvedLang, "Date", "التاريخ", "بەروار"), s.date)

            writer.section(t(resolvedLang, "System & Inspection Details", "تفاصيل المنظومة والكشف", "وردەکاری سیستەم و پشکنین"))
            writer.pair(t(resolvedLang, "System type", "نوع المنظومة", "جۆری سیستەم"), localizedSystemType(s.systemType, resolvedLang))
            writer.pair(t(resolvedLang, "Required capacity", "السعة المطلوبة", "توانای پێویست"), localizedCapacity(s.capacity, resolvedLang))
            writer.pair(t(resolvedLang, "Phase", "الطور", "فاز"), localizedPhase(s.phase, resolvedLang))
            if (settings.showOrganizer) {
                writer.pair(t(resolvedLang, "Inspection Organizer", "منظم الكشف", "ڕێکخەری پشکنین"), s.organizerName)
            }

            writer.materials(s, settings)

            if (settings.showLoadNote) {
                writer.section(t(resolvedLang, "Load / Notes", "التحميل / الملاحظات", "بار / تێبینی"), keepNext = 40)
                writer.paragraph(generatedLoadNote(s, resolvedLang))
                if (s.customNote.isNotBlank()) writer.paragraph(s.customNote)
            }

            writer.finishPage()
            if (settings.includePhotos) {
                s.photos.filter { it.included }.forEachIndexed { index, photo ->
                    writer.photoPage(
                        t(resolvedLang, "Site Photo", "صورة الموقع", "وێنەی شوێن") + " ${index + 1}",
                        File(photo.path)
                    )
                }
            }
            FileOutputStream(file).use(pdf::writeTo)
        } finally {
            runCatching { pdf.close() }
        }
    }

    private inner class Writer(
        private val doc: PdfDocument,
        private val settings: Settings,
        private val lang: String
    ) {
        private val pageWidth = 595
        private val pageHeight = 842
        private val left = 40
        private val right = 555
        private val contentWidth = right - left
        private val footerY = 815
        private val bottom = 798

        private val navy = Color.rgb(18, 50, 79)
        private val teal = Color.rgb(19, 167, 122)
        private val muted = Color.rgb(104, 118, 130)
        private val border = Color.rgb(218, 226, 232)
        private val paleTeal = Color.rgb(241, 249, 246)
        private val rowTint = Color.rgb(248, 251, 253)

        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = 0
        private var number = 0

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        private val rtl get() = lang != "en"

        fun newPage(fullHeader: Boolean = false) {
            number++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, number).create())
            canvas = page!!.canvas.apply { drawColor(Color.WHITE) }
            y = 34
            if (fullHeader) header() else continuationHeader()
        }

        fun finishPage() {
            page?.let { currentPage ->
                paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                paint.color = teal
                paint.textSize = 8.5f
                paint.textAlign = if (rtl) Paint.Align.RIGHT else Paint.Align.LEFT
                canvas!!.drawText(
                    "${settings.companyName} · ${t(lang, "Organizer Form", "استمارة المنظم", "فۆرمی ڕێکخەر")}",
                    if (rtl) right.toFloat() else left.toFloat(),
                    footerY.toFloat(),
                    paint
                )
                paint.textAlign = Paint.Align.CENTER
                canvas!!.drawText(number.toString(), (pageWidth / 2).toFloat(), footerY.toFloat(), paint)
                doc.finishPage(currentPage)
            }
            page = null
            canvas = null
        }

        private fun header() {
            var hasLogo = false
            if (settings.showCompanyLogo) {
                settings.logoPath?.let { path ->
                    sampledBitmap(File(path), 140, 100)?.let { bitmap ->
                        val scale = minOf(64f / bitmap.width, 46f / bitmap.height)
                        val logoWidth = bitmap.width * scale
                        val logoHeight = bitmap.height * scale
                        val logoTop = 29f
                        canvas!!.drawBitmap(
                            bitmap,
                            null,
                            RectF(left.toFloat(), logoTop, left + logoWidth, logoTop + logoHeight),
                            paint
                        )
                        bitmap.recycle()
                        hasLogo = true
                    }
                }
            }

            val textStart = if (hasLogo && !rtl) left + 78 else left
            val titleWidth = if (hasLogo) contentWidth - 78 else contentWidth

            draw(
                settings.companyName.uppercase(Locale.ROOT),
                if (rtl) right.toFloat() else textStart.toFloat(),
                31,
                20,
                true,
                navy,
                titleWidth
            )
            draw(
                localizedTitle(settings.pdfTitle),
                if (rtl) right.toFloat() else textStart.toFloat(),
                58,
                11,
                false,
                teal,
                titleWidth
            )

            paint.color = teal
            canvas!!.drawRoundRect(left.toFloat(), 82f, right.toFloat(), 85f, 1.5f, 1.5f, paint)
            y = 101
        }

        private fun localizedTitle(default: String) =
            if (default != "Organizer Preliminary Solar Form") default
            else when (lang) {
                "ar" -> "استمارة أولية للمنظومة الشمسية"
                "ckb", "ku" -> "فۆرمی سەرەتایی سیستەمی خۆرەوی"
                else -> default
            }

        private fun continuationHeader() {
            draw(
                t(lang, "Organizer Form - continued", "استمارة المنظم - تكملة", "فۆرمی ڕێکخەر - بەردەوام"),
                if (rtl) right.toFloat() else left.toFloat(),
                34,
                13,
                true,
                navy,
                contentWidth
            )
            paint.color = teal
            canvas!!.drawRect(left.toFloat(), 57f, right.toFloat(), 60f, paint)
            y = 77
        }

        private fun ensure(height: Int, onNewPage: (() -> Unit)? = null) {
            if (y + height > bottom) {
                finishPage()
                newPage()
                onNewPage?.invoke()
            }
        }

        fun section(title: String, keepNext: Int = 28) {
            val barHeight = 25
            ensure(barHeight + 7 + keepNext)
            paint.color = navy
            canvas!!.drawRoundRect(
                left.toFloat(),
                y.toFloat(),
                right.toFloat(),
                (y + barHeight).toFloat(),
                7f,
                7f,
                paint
            )
            // Top padding instead of baseline-like positioning prevents the section text from being clipped.
            draw(title, if (rtl) right - 10f else left + 10f, y + 6, 10, true, Color.WHITE, contentWidth - 20)
            y += barHeight + 6
        }

        fun pair(label: String, value: String) {
            val labelWidth = 132
            val gap = 8
            val maxValueWidth = contentWidth - labelWidth - gap
            val labelHeight = measure(label, labelWidth, 9, true, Layout.Alignment.ALIGN_NORMAL)

            // In RTL rows the value may start with Latin text or numbers (for example "erbil",
            // a phone number, a date, or an organizer name). FIRSTSTRONG then treats that value
            // as LTR, so drawing it inside the old full-width value cell pushed it all the way to
            // the left edge of the page. Use a content-sized value cell anchored beside the label
            // instead. Long values still get a generous wrapping width.
            val valueCellWidth = if (rtl) compactValueWidth(value, maxValueWidth) else maxValueWidth
            val valueHeight = measure(value, valueCellWidth, 9, false, Layout.Alignment.ALIGN_NORMAL)
            val h = maxOf(23, maxOf(labelHeight, valueHeight) + 9)
            ensure(h)

            val top = y + 4
            if (rtl) {
                val valueRight = right - labelWidth - gap
                drawCell(
                    label,
                    (right - labelWidth).toFloat(),
                    top,
                    labelWidth,
                    9,
                    true,
                    muted,
                    Layout.Alignment.ALIGN_NORMAL
                )
                drawCell(
                    value,
                    (valueRight - valueCellWidth).toFloat(),
                    top,
                    valueCellWidth,
                    9,
                    false,
                    navy,
                    Layout.Alignment.ALIGN_NORMAL
                )
            } else {
                drawCell(
                    label,
                    left.toFloat(),
                    top,
                    labelWidth,
                    9,
                    true,
                    muted,
                    Layout.Alignment.ALIGN_NORMAL
                )
                drawCell(
                    value,
                    (left + labelWidth + gap).toFloat(),
                    top,
                    maxValueWidth,
                    9,
                    false,
                    navy,
                    Layout.Alignment.ALIGN_NORMAL
                )
            }

            paint.color = border
            canvas!!.drawLine(left.toFloat(), (y + h - 2).toFloat(), right.toFloat(), (y + h - 2).toFloat(), paint)
            y += h
        }

        private fun compactValueWidth(value: String, maxWidth: Int): Int {
            textPaint.textSize = 9f
            textPaint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            val measured = kotlin.math.ceil(textPaint.measureText(value.trim()).toDouble()).toInt() + 8
            // Keep short values visually close to their labels, while leaving enough room for
            // readable wrapping of long client/location/organizer text.
            return measured.coerceIn(48, minOf(maxWidth, 260))
        }

        fun materials(s: FormUiState, settings: Settings) {
            section(t(lang, "Required Materials", "المواد المطلوبة", "ماددە پێویستەکان"), keepNext = 54)
            materialHeader(settings)

            s.items.forEachIndexed { index, item ->
                val itemWidth = if (settings.showPrices) 365 else 455
                val quantityWidth = 60
                val priceWidth = if (settings.showPrices) 90 else 0
                val priceX: Int
                val quantityX: Int
                val itemX: Int
                if (rtl) {
                    // True RTL table order: item/specification on the right, then quantity, then unit price.
                    priceX = left
                    quantityX = left + priceWidth
                    itemX = quantityX + quantityWidth
                } else {
                    itemX = left
                    quantityX = left + itemWidth
                    priceX = quantityX + quantityWidth
                }
                val textWidth = itemWidth - 14

                val name = "${index + 1}. ${item.name.trim()}"
                val specification = item.specification.trim()
                val nameHeight = measure(name, textWidth, 9, true, Layout.Alignment.ALIGN_NORMAL)
                val specHeight = if (specification.isBlank()) 0 else measure(
                    specification,
                    textWidth,
                    8,
                    false,
                    Layout.Alignment.ALIGN_NORMAL
                )
                val rowHeight = maxOf(28, 4 + nameHeight + (if (specHeight > 0) 1 + specHeight else 0) + 4)

                ensure(rowHeight) { materialHeader(settings) }

                paint.color = if (index % 2 == 0) rowTint else Color.WHITE
                canvas!!.drawRect(left.toFloat(), y.toFloat(), right.toFloat(), (y + rowHeight).toFloat(), paint)

                drawCell(name, itemX + 7f, y + 4, textWidth, 9, true, navy, Layout.Alignment.ALIGN_NORMAL)
                if (specification.isNotBlank()) {
                    drawCell(
                        specification,
                        itemX + 7f,
                        y + 4 + nameHeight + 1,
                        textWidth,
                        8,
                        false,
                        muted,
                        Layout.Alignment.ALIGN_NORMAL
                    )
                }

                val quantity = item.quantity.toString()
                val quantityHeight = measure(quantity, quantityWidth, 9, true, Layout.Alignment.ALIGN_CENTER)
                drawCell(
                    quantity,
                    quantityX.toFloat(),
                    y + ((rowHeight - quantityHeight) / 2),
                    quantityWidth,
                    9,
                    true,
                    navy,
                    Layout.Alignment.ALIGN_CENTER
                )

                if (settings.showPrices) {
                    val price = item.unitPrice.ifBlank { "-" }
                    val priceHeight = measure(price, priceWidth - 10, 9, false, Layout.Alignment.ALIGN_CENTER)
                    drawCell(
                        price,
                        (priceX + 5).toFloat(),
                        y + ((rowHeight - priceHeight) / 2),
                        priceWidth - 10,
                        9,
                        false,
                        navy,
                        Layout.Alignment.ALIGN_CENTER
                    )
                }

                paint.color = border
                canvas!!.drawLine(left.toFloat(), (y + rowHeight).toFloat(), right.toFloat(), (y + rowHeight).toFloat(), paint)
                val firstDivider = if (rtl) itemX else quantityX
                canvas!!.drawLine(firstDivider.toFloat(), y.toFloat(), firstDivider.toFloat(), (y + rowHeight).toFloat(), paint)
                if (settings.showPrices) {
                    val secondDivider = if (rtl) quantityX else priceX
                    canvas!!.drawLine(secondDivider.toFloat(), y.toFloat(), secondDivider.toFloat(), (y + rowHeight).toFloat(), paint)
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
            val itemWidth = if (settings.showPrices) 365 else 455
            val quantityWidth = 60
            val priceWidth = if (settings.showPrices) 90 else 0
            val priceX: Int
            val quantityX: Int
            val itemX: Int
            if (rtl) {
                priceX = left
                quantityX = left + priceWidth
                itemX = quantityX + quantityWidth
            } else {
                itemX = left
                quantityX = left + itemWidth
                priceX = quantityX + quantityWidth
            }
            val headerHeight = 24

            ensure(headerHeight + 2)
            paint.color = teal
            canvas!!.drawRect(left.toFloat(), y.toFloat(), right.toFloat(), (y + headerHeight).toFloat(), paint)

            drawCell(
                t(lang, "Item / Specification", "المادة / المواصفات", "ماددە / تایبەتمەندی"),
                (itemX + 7).toFloat(),
                y + 5,
                itemWidth - 14,
                8,
                true,
                Color.WHITE,
                Layout.Alignment.ALIGN_NORMAL
            )
            drawCell(
                t(lang, "Qty", "الكمية", "بڕ"),
                quantityX.toFloat(),
                y + 5,
                quantityWidth,
                8,
                true,
                Color.WHITE,
                Layout.Alignment.ALIGN_CENTER
            )
            if (settings.showPrices) {
                drawCell(
                    t(lang, "Unit Price", "سعر الوحدة", "نرخی دانە"),
                    (priceX + 5).toFloat(),
                    y + 5,
                    priceWidth - 10,
                    8,
                    true,
                    Color.WHITE,
                    Layout.Alignment.ALIGN_CENTER
                )
            }

            paint.color = Color.argb(90, 255, 255, 255)
            val firstDivider = if (rtl) itemX else quantityX
            canvas!!.drawLine(firstDivider.toFloat(), y.toFloat(), firstDivider.toFloat(), (y + headerHeight).toFloat(), paint)
            if (settings.showPrices) {
                val secondDivider = if (rtl) quantityX else priceX
                canvas!!.drawLine(secondDivider.toFloat(), y.toFloat(), secondDivider.toFloat(), (y + headerHeight).toFloat(), paint)
            }
            y += headerHeight
        }

        private fun totalRow(label: String, value: String) {
            val height = 32
            ensure(height + 5)
            y += 4
            paint.color = paleTeal
            canvas!!.drawRoundRect(left.toFloat(), y.toFloat(), right.toFloat(), (y + height).toFloat(), 6f, 6f, paint)

            val labelWidth = 160
            val valueWidth = contentWidth - labelWidth - 20
            if (rtl) {
                drawCell(
                    label,
                    (right - labelWidth - 10).toFloat(),
                    y + 8,
                    labelWidth,
                    9,
                    true,
                    muted,
                    Layout.Alignment.ALIGN_NORMAL
                )
                drawCell(
                    value,
                    (left + 10).toFloat(),
                    y + 7,
                    valueWidth,
                    11,
                    true,
                    teal,
                    Layout.Alignment.ALIGN_NORMAL
                )
            } else {
                draw(label, left + 10f, y + 8, 9, true, muted, labelWidth)
                drawCell(
                    value,
                    (left + labelWidth + 10).toFloat(),
                    y + 7,
                    valueWidth,
                    11,
                    true,
                    teal,
                    Layout.Alignment.ALIGN_OPPOSITE
                )
            }
            y += height + 2
        }

        fun paragraph(text: String) {
            var remaining = text.trim()
            while (remaining.isNotEmpty()) {
                val available = bottom - y
                if (available < 42) {
                    finishPage()
                    newPage()
                }

                val layout = layout(remaining, contentWidth, 10, false, navy)
                val linesThatFit = (0 until layout.lineCount)
                    .takeWhile { layout.getLineBottom(it) <= bottom - y }
                    .size
                    .coerceAtLeast(1)
                val end = layout.getLineEnd(linesThatFit - 1)
                val chunk = remaining.substring(0, end).trimEnd()
                val chunkLayout = layout(chunk, contentWidth, 10, false, navy)

                canvas!!.save()
                canvas!!.translate(left.toFloat(), y.toFloat())
                chunkLayout.draw(canvas!!)
                canvas!!.restore()
                y += chunkLayout.height + 7

                remaining = remaining.substring(end).trimStart()
                if (remaining.isNotEmpty()) {
                    finishPage()
                    newPage()
                }
            }
        }

        fun photoPage(title: String, file: File) {
            finishPage()
            newPage()
            draw(title, if (rtl) right.toFloat() else left.toFloat(), y, 18, true, navy, contentWidth)
            y += 37

            val maxW = contentWidth.toFloat()
            val maxH = (bottom - y).toFloat()
            val bitmap = sampledBitmap(file, maxW.toInt(), maxH.toInt()) ?: run {
                paragraph(t(lang, "Photo unavailable", "الصورة غير متوفرة", "وێنە بەردەست نییە"))
                finishPage()
                return
            }
            val scale = minOf(maxW / bitmap.width, maxH / bitmap.height)
            val width = bitmap.width * scale
            val height = bitmap.height * scale
            val x = (pageWidth - width) / 2
            canvas!!.drawBitmap(bitmap, null, RectF(x, y.toFloat(), x + width, y + height), paint)
            bitmap.recycle()
            finishPage()
        }

        private fun measure(
            text: String,
            width: Int,
            size: Int,
            bold: Boolean,
            alignment: Layout.Alignment? = null
        ): Int = layout(text, width, size, bold, navy, alignment).height

        private fun draw(
            text: String,
            x: Float,
            top: Int,
            size: Int,
            bold: Boolean,
            color: Int,
            width: Int
        ) {
            val textLayout = layout(text, width, size, bold, color)
            canvas!!.save()
            canvas!!.translate(if (rtl) x - width else x, top.toFloat())
            textLayout.draw(canvas!!)
            canvas!!.restore()
        }

        private fun drawCell(
            text: String,
            x: Float,
            top: Int,
            width: Int,
            size: Int,
            bold: Boolean,
            color: Int,
            alignment: Layout.Alignment
        ) {
            val textLayout = layout(text, width, size, bold, color, alignment)
            canvas!!.save()
            canvas!!.translate(x, top.toFloat())
            textLayout.draw(canvas!!)
            canvas!!.restore()
        }

        private fun layout(
            text: String,
            width: Int,
            size: Int,
            bold: Boolean,
            color: Int,
            alignment: Layout.Alignment? = null
        ): StaticLayout {
            textPaint.textSize = size.toFloat()
            textPaint.color = color
            textPaint.typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
            // ALIGN_NORMAL follows the paragraph's start edge: right for Arabic/Kurdish, left for English.
            // FIRSTSTRONG_RTL preserves Latin model names/numbers while keeping RTL as the fallback.
            val resolvedAlignment = alignment ?: Layout.Alignment.ALIGN_NORMAL
            return StaticLayout.Builder.obtain(text, 0, text.length, textPaint, width.coerceAtLeast(1))
                .setAlignment(resolvedAlignment)
                .setTextDirection(if (rtl) TextDirectionHeuristics.FIRSTSTRONG_RTL else TextDirectionHeuristics.FIRSTSTRONG_LTR)
                .setLineSpacing(1f, 1.03f)
                .setIncludePad(false)
                .build()
        }
    }

    private fun normalizeLanguage(value: String) = when (value.lowercase(Locale.ROOT)) {
        "ar" -> "ar"
        "ckb", "ku" -> "ckb"
        else -> "en"
    }

    private fun localizedSystemType(value: String, lang: String) = when (value) {
        "Hybrid" -> t(lang, "Hybrid", "هجين", "هایبرید")
        "On-Grid" -> t(lang, "On-Grid", "متصل بالشبكة", "پەیوەست بە تۆڕ")
        "Off-Grid" -> t(lang, "Off-Grid", "منفصل عن الشبكة", "ناپەیوەست بە تۆڕ")
        else -> value
    }

    private fun localizedPhase(value: String, lang: String) = when (value) {
        "Single Phase" -> t(lang, "Single Phase", "أحادي الطور", "یەک فاز")
        "Three Phase" -> t(lang, "Three Phase", "ثلاثي الطور", "سێ فاز")
        else -> value
    }

    private fun localizedCapacity(value: String, lang: String) = when (lang) {
        "ar" -> "$value كيلوواط"
        "ckb", "ku" -> "$value کیلۆوات"
        else -> "$value kW"
    }

    private fun toExport(s: FormUiState, settings: Settings): FormExport {
        val appVersion = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { "unknown" }
        return FormExport(
            s.id,
            Instant.ofEpochMilli(s.createdAt).toString(),
            Instant.ofEpochMilli(s.updatedAt).toString(),
            s.date,
            settings.companyName,
            s.clientName,
            s.phone,
            s.location,
            s.systemType,
            s.capacity,
            s.phase,
            s.organizerName,
            s.dayFrom,
            s.dayTo,
            s.nightAmps,
            s.nightHours,
            s.emergencyAmps,
            s.emergencyDuration,
            s.customNote,
            s.items.map {
                ExportItem(
                    it.catalogId,
                    it.category,
                    it.name,
                    it.quantity.toString(),
                    it.unitPrice.ifBlank { null },
                    s.currency,
                    it.lineTotal?.toPlainString()
                )
            },
            s.estimatedTotal.toPlainString(),
            s.currency,
            s.photos.mapIndexed { i, p -> ExportPhoto(i + 1, p.included, p.displayName) },
            appVersion
        )
    }

    private fun safe(value: String) = value.trim()
        .replace(Regex("[^\\p{L}\\p{N}_-]+"), "_")
        .trim('_')
        .ifBlank { "Form" }
        .take(48)

    private fun sampledBitmap(file: File, maxWidth: Int, maxHeight: Int): Bitmap? {
        if (!file.isFile) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxWidth * 2 || bounds.outHeight / sample > maxHeight * 2) sample *= 2
        return BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample }
        )
    }

    private fun t(lang: String, en: String, ar: String, ku: String) = when (lang) {
        "ar" -> ar
        "ckb", "ku" -> ku
        else -> en
    }
}
