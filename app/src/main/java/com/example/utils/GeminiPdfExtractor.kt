package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import com.example.data.PatientEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiPdfExtractor {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private fun renderPdfPageToBitmap(context: Context, uri: Uri, pageIndex: Int): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            renderer = PdfRenderer(pfd)
            if (pageIndex >= renderer.pageCount) return null
            page = renderer.openPage(pageIndex)

            val maxDim = 1024
            val originalWidth = page.width
            val originalHeight = page.height
            val scale = maxDim.toFloat() / maxOf(originalWidth, originalHeight).coerceAtLeast(1)
            val dstWidth = if (scale < 1.0f) (originalWidth * scale).toInt() else originalWidth
            val dstHeight = if (scale < 1.0f) (originalHeight * scale).toInt() else originalHeight

            val bitmap = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE) // Solves transparency rendering on dark frames

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            try { page?.close() } catch (ignored: Exception) {}
            try { renderer?.close() } catch (ignored: Exception) {}
            try { pfd?.close() } catch (ignored: Exception) {}
        }
    }

    private fun getPdfPageCount(context: Context, uri: Uri): Int {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return 0
            renderer = PdfRenderer(pfd)
            return renderer.pageCount
        } catch (e: Exception) {
            return 0
        } finally {
            try { renderer?.close() } catch (ignored: Exception) {}
            try { pfd?.close() } catch (ignored: Exception) {}
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    suspend fun extractPatientFromPdf(
        context: Context,
        uri: Uri,
        postId: Int,
        apiKey: String
    ): Result<List<PatientEntity>> = withContext(Dispatchers.IO) {
        try {
            val pageCount = getPdfPageCount(context, uri)
            if (pageCount == 0) {
                return@withContext Result.failure(Exception("Could not open the selected PDF file, or it has no pages."))
            }

            // Convert up to the first 4 pages to base64 images
            val base64Images = mutableListOf<String>()
            val maxPagesToProcess = minOf(pageCount, 4)
            for (i in 0 until maxPagesToProcess) {
                val bitmap = renderPdfPageToBitmap(context, uri, i)
                if (bitmap != null) {
                    base64Images.add(bitmapToBase64(bitmap))
                }
            }

            if (base64Images.isEmpty()) {
                return@withContext Result.failure(Exception("Failed to render pages of the PDF as images."))
            }

            val prompt = """
                You are high-precision clinical scanning system. Analyze these patient rounds or check-in PDF sheets and extract the current clinical data for the patient(s).
                The PDF could contain one or multiple patients. Extract all patients you find.
                Return ONLY a JSON Array containing patient objects representing the extracted fields. Keep clinical values exactly as written.
                If any field or metric is missing, use an empty string "" as its value.

                CRITICAL VISUAL FORMATTING & HIGHLIGHT PRESERVATION:
                In the "Admission / General Diagnosis" section of the patient sheet, certain words or phrases have visual styling. You MUST look carefully at the visual output and enclose those styled words/phrases under the "diagnosis" field value with their exact custom styling tags:
                - If a word or phrase is highlighted with a yellow background, wrap it in [hyellow]word[/hyellow] (for example: [hyellow]35wks[/hyellow] or [hyellow]PIV[/hyellow]).
                - If a word or phrase is highlighted with a pink background, wrap it in [hpink]word[/hpink].
                - If a word or phrase is bold, wrap it in [bold]word[/bold].
                - If a word has a specific text color (e.g., Red, Blue, Green, Orange, Pink, Purple, Yellow), wrap it with the lowercase tag, e.g. [red]word[/red], [green]word[/green], etc.
                Apply these tags precisely to preserve the exact highlighting shown in the PDF image. Do not change or remove these tags for the highlighted clinical keywords.

                Map standard parameters accurately to these short names:
                - Hemoglobin -> "hb"
                - Hematocrit -> "hct"
                - Platelets -> "plt"
                - White Blood Cells -> "wbc"
                - Urea -> "urea"
                - Creatinine -> "creat"
                - SGOT / AST -> "sgot"
                - SGPT / ALT -> "sgpt"
                - INR -> "inr"
                - Random Blood Sugar -> "rbs"
                - Sodium (Na) -> "na"
                - Potassium (K) -> "k"
                - Albumin -> "alb"
                - Acetone -> "acetone"
                - Sugar -> "sugar"
                - Pus (Pus cells, urinalysis) -> "pus"

                Format of the JSON response:
                [
                  {
                    "name": "Patient full name",
                    "abo": "ABO blood group e.g. A+, O-",
                    "diagnosis": "Admission / General Diagnosis containing any styling tags if found like [hyellow]PIV[/hyellow]",
                    "lmp": "LMP Date",
                    "edd": "EDD Milestone Date",
                    "us": "ultrasound findings or gestational age by US",
                    "hb": "Value",
                    "hct": "Value",
                    "plt": "Value",
                    "wbc": "Value",
                    "urea": "Value",
                    "creat": "Value",
                    "sgot": "Value",
                    "sgpt": "Value",
                    "inr": "Value",
                    "rbs": "Value",
                    "na": "Value",
                    "k": "Value",
                    "alb": "Value",
                    "acetone": "Value",
                    "sugar": "Value",
                    "pus": "Value",
                    "notes": "Any other rounds, notes, plan or comments if found"
                  }
                ]
                Please output ONLY a JSON array, even if there is only 1 patient.
            """.trimIndent()

            // Construct JSON request body
            val requestJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            // 1. Prompt Part
            partsArray.put(JSONObject().put("text", prompt))

            // 2. Multimodal Inline Image Parts
            for (b64 in base64Images) {
                partsArray.put(JSONObject().put("inlineData", JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", b64)
                }))
            }

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            // Force JSON output
            val generationConfig = JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            }
            requestJson.put("generationConfig", generationConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)

            // Use 'gemini-3.5-flash' as recommended for basic/multimodal text extraction tasks
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gemini API request failed with code: ${response.code}"))
            }

            val responseString = response.body?.string() ?: ""
            if (responseString.isEmpty()) {
                return@withContext Result.failure(Exception("Gemini API returned an empty response."))
            }

            // Parse response json structure
            val responseJson = JSONObject(responseString)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("No extraction candidates returned by Gemini."))
            }

            val content = candidates.getJSONObject(0).optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts == null || parts.length() == 0) {
                return@withContext Result.failure(Exception("No output text parts returned by Gemini."))
            }

            val parsedText = parts.getJSONObject(0).optString("text", "")
            if (parsedText.isEmpty()) {
                return@withContext Result.failure(Exception("Zero content returned from Gemini scanner."))
            }

            // Extract the json block (Gemini with responseMimeType usually returns clean JSON, but if formatted in ```json we unwrap it)
            var cleanJson = parsedText.trim()
            if (cleanJson.startsWith("```")) {
                val lines = cleanJson.lines()
                val targetLines = if (lines.first().startsWith("```")) {
                    lines.drop(1).dropLast(1)
                } else {
                    lines
                }
                cleanJson = targetLines.joinToString("\n").trim()
            }

            val resultList = mutableListOf<PatientEntity>()

            if (cleanJson.startsWith("[")) {
                val array = JSONArray(cleanJson)
                for (i in 0 until array.length()) {
                    val pObj = array.getJSONObject(i)
                    resultList.add(parseObject(pObj, postId))
                }
            } else {
                val pObj = JSONObject(cleanJson)
                resultList.add(parseObject(pObj, postId))
            }

            return@withContext Result.success(resultList)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(e)
        }
    }

    private fun parseObject(patientJson: JSONObject, postId: Int): PatientEntity {
        return PatientEntity(
            postId = postId,
            name = patientJson.optString("name", ""),
            abo = patientJson.optString("abo", ""),
            diagnosis = patientJson.optString("diagnosis", ""),
            lmp = patientJson.optString("lmp", ""),
            edd = patientJson.optString("edd", ""),
            us = patientJson.optString("us", ""),
            hb = patientJson.optString("hb", ""),
            hct = patientJson.optString("hct", ""),
            plt = patientJson.optString("plt", ""),
            wbc = patientJson.optString("wbc", ""),
            urea = patientJson.optString("urea", ""),
            creat = patientJson.optString("creat", ""),
            sgot = patientJson.optString("sgot", ""),
            sgpt = patientJson.optString("sgpt", ""),
            inr = patientJson.optString("inr", ""),
            rbs = patientJson.optString("rbs", ""),
            na = patientJson.optString("na", ""),
            k = patientJson.optString("k", ""),
            alb = patientJson.optString("alb", ""),
            acetone = patientJson.optString("acetone", ""),
            sugar = patientJson.optString("sugar", ""),
            pus = patientJson.optString("pus", ""),
            notes = patientJson.optString("notes", ""),
            createdAt = System.currentTimeMillis(),
            isSelectedForPrint = true
        )
    }
}
