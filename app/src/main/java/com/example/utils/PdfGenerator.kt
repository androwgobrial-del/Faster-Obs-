package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.PatientEntity
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    /**
     * Generates an inpatient rounds PDF file from the list of patients under the app's cache directory.
     * Dimensions are based on PostScript standard A4: 595 x 842 points.
     */
    fun generatePatientRoundsPdf(
        context: Context, 
        postTitle: String,
        patients: List<PatientEntity>,
        fontSize: Float = 9.0f,
        accentColorHex: String = "#005B94"
    ): File {
        val pdfDocument = PdfDocument()

        // Page calculations: 6 patients fit on 1 page (2 columns x 3 rows)
        val selectedPatients = patients.filter { it.isSelectedForPrint }
        val patientsList = if (selectedPatients.isEmpty()) listOf(PatientEntity(name = "No Patient Selected")) else selectedPatients
        val pagesCount = (patientsList.size + 5) / 6

        // Convert hex string color safely
        val themeColor = try {
            Color.parseColor(accentColorHex)
        } catch (e: Exception) {
            Color.parseColor("#005B94") // Default clean medical blue
        }

        // Set up paint configurations
        val textPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = fontSize
            color = Color.BLACK
            isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = fontSize
            color = Color.BLACK
            isAntiAlias = true
        }
        val accentFillPaint = Paint().apply {
            color = themeColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#CCCCCC") // Clean professional light grey borders
            strokeWidth = 1.0f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        for (pageIndex in 0 until pagesCount) {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // 1. Draw "Inpatient Rounds Sheet" Accent Banner (Top)
            canvas.drawRect(15f, 15f, 580f, 38f, accentFillPaint)
            
            val titleBannerPaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 12.0f
                color = Color.WHITE
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("POST: ${postTitle.uppercase()}", 297.5f, 31f, titleBannerPaint)

            // 2. Draw Center Dividers and Surrounding Margins
            // Vertical dividing line at center
            canvas.drawLine(297.5f, 40f, 297.5f, 824.5f, linePaint)

            // Draw horizontal dividers
            // Row Height: 261.5f, starting from y=40f to y=824.5f
            canvas.drawLine(15f, 301.5f, 580f, 301.5f, linePaint)
            canvas.drawLine(15f, 563.0f, 580f, 563.0f, linePaint)

            // 3. Populate up to 6 patients on this page
            for (i in 0 until 6) {
                val patientIndex = pageIndex * 6 + i
                if (patientIndex >= patientsList.size) break

                val patient = patientsList[patientIndex]
                val col = i % 2
                val row = i / 2

                // Coordinate boundaries
                val x0 = if (col == 0) 15f else 305f
                val y0 = when (row) {
                    0 -> 40f
                    1 -> 301.5f
                    else -> 563.0f
                }

                drawPatientCard(canvas, patient, x0, y0, textPaint, boldPaint, themeColor)
            }

            pdfDocument.finishPage(page)
        }

        // Save file to application cache directory
        val file = File(context.cacheDir, "Inpatient_Rounds_${System.currentTimeMillis()}.pdf")
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        outputStream.close()

        return file
    }

    private fun drawPatientCard(
        canvas: Canvas,
        patient: PatientEntity,
        x0: Float,
        y0: Float,
        textPaint: Paint,
        boldPaint: Paint,
        themeColorValue: Int
    ) {
        val yStart = y0 + 18f
        val lineSpacing = 16f

        // Theme colored medical tag paint
        val themeTagPaint = Paint(boldPaint).apply {
            color = themeColorValue
        }

        // Name & ABO
        canvas.drawText("Name: ", x0, yStart, boldPaint)
        canvas.drawText(patient.name.take(30), x0 + 38f, yStart, textPaint)
        
        canvas.drawText("ABO: ", x0 + 200f, yStart, boldPaint)
        canvas.drawText(patient.abo, x0 + 232f, yStart, themeTagPaint) // Highlight blood group with theme color

        // Diagnosis
        canvas.drawText("Diagnosis: ", x0, yStart + lineSpacing, boldPaint)
        
        // Tokenize spans of patient.diagnosis into word/whitespace fragments
        val rawSpans = com.example.utils.ColorTagParser.parseToSpans(patient.diagnosis, "#000000")
        
        data class SpanFragment(
            val text: String,
            val colorHex: String,
            val isBold: Boolean,
            val bgHex: String?
        )
        
        val rx = mutableListOf<SpanFragment>()
        for (span in rawSpans) {
            val text = span.text
            if (text.isEmpty()) continue
            var idx = 0
            val len = text.length
            while (idx < len) {
                val isSpace = text[idx].isWhitespace()
                val start = idx
                while (idx < len && text[idx].isWhitespace() == isSpace) {
                    idx++
                }
                val part = text.substring(start, idx)
                rx.add(SpanFragment(part, span.colorHex, span.isBold, span.bgHex))
            }
        }
        
        val line1 = mutableListOf<SpanFragment>()
        val line2 = mutableListOf<SpanFragment>()
        
        var currentWidth = 0f
        var listToFill = line1
        var limit = 270f - 55f // first line limit
        
        var i = 0
        while (i < rx.size) {
            val frag = rx[i]
            val paint = Paint(textPaint).apply {
                color = try { Color.parseColor(frag.colorHex) } catch (e: Exception) { Color.BLACK }
                if (frag.isBold) {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
            }
            val w = paint.measureText(frag.text)
            
            if (currentWidth + w <= limit) {
                listToFill.add(frag)
                currentWidth += w
                i++
            } else {
                if (listToFill === line1) {
                    listToFill = line2
                    limit = 270f // second line limit
                    currentWidth = 0f
                    if (frag.text.trim().isEmpty()) {
                        i++ // skip leading whitespace on second line
                    }
                } else {
                    // Truncate line2 and add ellipsis
                    val ellipText = "..."
                    val ellipPaint = Paint(textPaint)
                    val ellipW = ellipPaint.measureText(ellipText)
                    
                    // Pop from line2 until there's room for ellipsis
                    while (line2.isNotEmpty()) {
                        var tempW = 0f
                        for (f in line2) {
                            val p = Paint(textPaint).apply {
                                color = try { Color.parseColor(f.colorHex) } catch (e: Exception) { Color.BLACK }
                                if (f.isBold) {
                                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                }
                            }
                            tempW += p.measureText(f.text)
                        }
                        if (tempW + ellipW <= limit) {
                            break
                        }
                        line2.removeAt(line2.size - 1)
                    }
                    line2.add(SpanFragment(ellipText, "#000000", false, null))
                    break // end wrapping
                }
            }
        }
        
        // Draw Line 1
        var cx1 = x0 + 55f
        val cy1 = yStart + lineSpacing
        for (frag in line1) {
            val spanPaint = Paint(textPaint).apply {
                color = try { Color.parseColor(frag.colorHex) } catch (e: Exception) { Color.BLACK }
                if (frag.isBold) {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
            }
            val w = spanPaint.measureText(frag.text)
            if (frag.bgHex != null) {
                val bgPaint = Paint().apply {
                    color = try { Color.parseColor(frag.bgHex) } catch (e: Exception) { Color.YELLOW }
                    style = Paint.Style.FILL
                }
                canvas.drawRect(
                    cx1,
                    cy1 - spanPaint.textSize * 0.85f,
                    cx1 + w,
                    cy1 + spanPaint.textSize * 0.2f,
                    bgPaint
                )
            }
            canvas.drawText(frag.text, cx1, cy1, spanPaint)
            cx1 += w
        }
        
        // Draw Line 2 (if exists)
        val hasLine2 = line2.isNotEmpty()
        if (hasLine2) {
            var cx2 = x0
            val cy2 = yStart + lineSpacing + 11f
            for (frag in line2) {
                val spanPaint = Paint(textPaint).apply {
                    color = try { Color.parseColor(frag.colorHex) } catch (e: Exception) { Color.BLACK }
                    if (frag.isBold) {
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                }
                val w = spanPaint.measureText(frag.text)
                if (frag.bgHex != null) {
                    val bgPaint = Paint().apply {
                        color = try { Color.parseColor(frag.bgHex) } catch (e: Exception) { Color.YELLOW }
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(
                        cx2,
                        cy2 - spanPaint.textSize * 0.85f,
                        cx2 + w,
                        cy2 + spanPaint.textSize * 0.2f,
                        bgPaint
                    )
                }
                canvas.drawText(frag.text, cx2, cy2, spanPaint)
                cx2 += w
            }
        }
        
        // Calculate dynamic vertical offset for lines below Diagnosis
        val shiftY = if (hasLine2) 11f else 0f
        
        // LMP & EDD
        canvas.drawText("LMP: ", x0, yStart + (lineSpacing * 2.2f) + shiftY, boldPaint)
        canvas.drawText(patient.lmp, x0 + 32f, yStart + (lineSpacing * 2.2f) + shiftY, textPaint)
        
        canvas.drawText("EDD: ", x0 + 115f, yStart + (lineSpacing * 2.2f) + shiftY, boldPaint)
        canvas.drawText(patient.edd, x0 + 143f, yStart + (lineSpacing * 2.2f) + shiftY, textPaint)

        // U/S Findings
        val usText = patient.us
        val usWords = usText.split(Regex("\\s+")).filter { it.isNotEmpty() }
        
        val usLine1 = java.lang.StringBuilder()
        val usLine2 = java.lang.StringBuilder()
        
        val prefixW = boldPaint.measureText("U/S: ")
        val limitUs1 = 270f - prefixW
        val limitUs2 = 270f
        
        var currentUsW = 0f
        var isLine2Active = false
        
        for (word in usWords) {
            val space = if (isLine2Active) {
                if (usLine2.isEmpty()) "" else " "
            } else {
                if (usLine1.isEmpty()) "" else " "
            }
            val testWord = "$space$word"
            val wordW = textPaint.measureText(testWord)
            
            if (!isLine2Active) {
                if (currentUsW + wordW <= limitUs1) {
                    usLine1.append(testWord)
                    currentUsW += wordW
                } else {
                    isLine2Active = true
                    currentUsW = 0f
                    val testWordL2 = word
                    val wordWL2 = textPaint.measureText(testWordL2)
                    if (wordWL2 <= limitUs2) {
                        usLine2.append(testWordL2)
                        currentUsW += wordWL2
                    } else {
                        val ellipsisText = "..."
                        val ellWidth = textPaint.measureText(ellipsisText)
                        var maxChars = textPaint.breakText(testWordL2, true, limitUs2 - ellWidth, null)
                        if (maxChars < 0) maxChars = 0
                        usLine2.append(testWordL2.take(maxChars)).append(ellipsisText)
                        break
                    }
                }
            } else {
                if (currentUsW + wordW <= limitUs2) {
                    usLine2.append(testWord)
                    currentUsW += wordW
                } else {
                    val ellipsisText = "..."
                    while (usLine2.isNotEmpty() && textPaint.measureText(usLine2.toString() + ellipsisText) > limitUs2) {
                        usLine2.setLength(usLine2.length - 1)
                    }
                    usLine2.append(ellipsisText)
                    break
                }
            }
        }

        canvas.drawText("U/S: ", x0, yStart + (lineSpacing * 3.2f) + shiftY, boldPaint)
        canvas.drawText(usLine1.toString(), x0 + prefixW, yStart + (lineSpacing * 3.2f) + shiftY, textPaint)

        val hasUsLine2 = usLine2.isNotEmpty()
        if (hasUsLine2) {
            canvas.drawText(usLine2.toString(), x0, yStart + (lineSpacing * 3.2f) + shiftY + 11f, textPaint)
        }

        val shiftUs = if (hasUsLine2) 11f else 0f

        // Lab parameters matrix: 4 columns x 4 rows
        // Column starts:
        val col1X = x0
        val col2X = x0 + 68f
        val col3X = x0 + 138f
        val col4X = x0 + 208f

        // Lab Row Heights:
        val labRow1Y = yStart + (lineSpacing * 4.6f) + shiftY + shiftUs
        val labRow2Y = labRow1Y + 15f
        val labRow3Y = labRow2Y + 15f
        val labRow4Y = labRow3Y + 15f

        // Matrix Column 1
        canvas.drawText("Hb: ", col1X, labRow1Y, boldPaint)
        canvas.drawText(patient.hb, col1X + 24f, labRow1Y, textPaint)
        canvas.drawText("Hct: ", col1X, labRow2Y, boldPaint)
        canvas.drawText(patient.hct, col1X + 24f, labRow2Y, textPaint)
        canvas.drawText("PLT: ", col1X, labRow3Y, boldPaint)
        canvas.drawText(patient.plt, col1X + 24f, labRow3Y, textPaint)
        canvas.drawText("WBC: ", col1X, labRow4Y, boldPaint)
        canvas.drawText(patient.wbc, col1X + 26f, labRow4Y, textPaint)

        // Matrix Column 2
        canvas.drawText("Urea: ", col2X, labRow1Y, boldPaint)
        canvas.drawText(patient.urea, col2X + 32f, labRow1Y, textPaint)
        canvas.drawText("Creat.: ", col2X, labRow2Y, boldPaint)
        canvas.drawText(patient.creat, col2X + 34f, labRow2Y, textPaint)
        canvas.drawText("SGOT: ", col2X, labRow3Y, boldPaint)
        canvas.drawText(patient.sgot, col2X + 32f, labRow3Y, textPaint)
        canvas.drawText("SGPT: ", col2X, labRow4Y, boldPaint)
        canvas.drawText(patient.sgpt, col2X + 32f, labRow4Y, textPaint)

        // Matrix Column 3
        canvas.drawText("INR: ", col3X, labRow1Y, boldPaint)
        canvas.drawText(patient.inr, col3X + 24f, labRow1Y, textPaint)
        canvas.drawText("RBS: ", col3X, labRow2Y, boldPaint)
        canvas.drawText(patient.rbs, col3X + 24f, labRow2Y, textPaint)
        canvas.drawText("Na: ", col3X, labRow3Y, boldPaint)
        canvas.drawText(patient.na, col3X + 24f, labRow3Y, textPaint)
        canvas.drawText("K: ", col3X, labRow4Y, boldPaint)
        canvas.drawText(patient.k, col3X + 24f, labRow4Y, textPaint)

        // Matrix Column 4
        canvas.drawText("Alb: ", col4X, labRow1Y, boldPaint)
        canvas.drawText(patient.alb, col4X + 32f, labRow1Y, textPaint)
        canvas.drawText("Actn: ", col4X, labRow2Y, boldPaint) // Shortened slightly to prevent bleed-over
        canvas.drawText(patient.acetone, col4X + 32f, labRow2Y, textPaint)
        canvas.drawText("Sugr: ", col4X, labRow3Y, boldPaint) // Shortened slightly
        canvas.drawText(patient.sugar, col4X + 32f, labRow3Y, textPaint)
        canvas.drawText("Pus: ", col4X, labRow4Y, boldPaint)
        canvas.drawText(patient.pus, col4X + 32f, labRow4Y, textPaint)

        // Notes section:
        val notesLabelY = labRow4Y + 18f
        canvas.drawText("Notes: ", x0, notesLabelY, themeTagPaint) // Highlight clinical notes title with theme color
        drawWrappedText(
            canvas = canvas,
            text = patient.notes,
            x = x0,
            yStart = notesLabelY + 12f,
            width = 270f, // Max width of cell minus some right margin
            paint = textPaint,
            maxLines = 5
        )
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        yStart: Float,
        width: Float,
        paint: Paint,
        maxLines: Int
    ) {
        if (text.isEmpty()) return
        val paragraphs = text.split('\n')
        var currentY = yStart
        var lineCount = 0
        val leading = paint.textSize + 2.5f

        for (pIndex in paragraphs.indices) {
            val paragraph = paragraphs[pIndex]
            if (lineCount >= maxLines) break

            // If it's a completely empty line inside paragraphs, draw an empty space and continue
            if (paragraph.isEmpty()) {
                currentY += leading
                lineCount++
                continue
            }

            // Split this paragraph's words while preserving their relative order (avoid skipping words)
            val words = paragraph.split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (words.isEmpty()) {
                currentY += leading
                lineCount++
                continue
            }

            var line = ""
            for (wIndex in words.indices) {
                val word = words[wIndex]
                val testLine = if (line.isEmpty()) word else "$line $word"
                val measure = paint.measureText(testLine)
                if (measure > width) {
                    canvas.drawText(line, x, currentY, paint)
                    line = word
                    currentY += leading
                    lineCount++
                    if (lineCount >= maxLines) {
                        // Ellipsize the last line if we cut off and there are more words or paragraphs
                        if (wIndex < words.size - 1 || pIndex < paragraphs.size - 1) {
                            canvas.drawText("...", x, currentY, paint)
                        }
                        return
                    }
                } else {
                    line = testLine
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line, x, currentY, paint)
                currentY += leading
                lineCount++
            }
        }
    }
}
