package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int = 0,
    val name: String = "",
    val abo: String = "",
    val diagnosis: String = "",
    val lmp: String = "",
    val edd: String = "",
    val us: String = "",
    
    // Lab parameters (Stored as String to allow qualitative inputs like "+", "Negative", etc.)
    val hb: String = "",
    val hct: String = "",
    val plt: String = "",
    val wbc: String = "",
    val urea: String = "",
    val creat: String = "",
    val sgot: String = "",
    val sgpt: String = "",
    val inr: String = "",
    val rbs: String = "",
    val na: String = "",
    val k: String = "",
    val alb: String = "",
    val acetone: String = "",
    val sugar: String = "",
    val pus: String = "",
    
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isSelectedForPrint: Boolean = true,
    val labRecordsJson: String = ""
)

data class LabRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val hb: String = "",
    val hct: String = "",
    val plt: String = "",
    val wbc: String = "",
    val urea: String = "",
    val creat: String = "",
    val sgot: String = "",
    val sgpt: String = "",
    val inr: String = "",
    val rbs: String = "",
    val na: String = "",
    val k: String = "",
    val alb: String = "",
    val acetone: String = "",
    val sugar: String = "",
    val pus: String = ""
)

fun List<LabRecord>.consolidated(): LabRecord {
    val sorted = this.sortedByDescending { it.createdAt }
    return LabRecord(
        id = "consolidated",
        createdAt = sorted.firstOrNull()?.createdAt ?: System.currentTimeMillis(),
        hb = sorted.firstOrNull { it.hb.isNotBlank() }?.hb ?: "",
        hct = sorted.firstOrNull { it.hct.isNotBlank() }?.hct ?: "",
        plt = sorted.firstOrNull { it.plt.isNotBlank() }?.plt ?: "",
        wbc = sorted.firstOrNull { it.wbc.isNotBlank() }?.wbc ?: "",
        urea = sorted.firstOrNull { it.urea.isNotBlank() }?.urea ?: "",
        creat = sorted.firstOrNull { it.creat.isNotBlank() }?.creat ?: "",
        sgot = sorted.firstOrNull { it.sgot.isNotBlank() }?.sgot ?: "",
        sgpt = sorted.firstOrNull { it.sgpt.isNotBlank() }?.sgpt ?: "",
        inr = sorted.firstOrNull { it.inr.isNotBlank() }?.inr ?: "",
        rbs = sorted.firstOrNull { it.rbs.isNotBlank() }?.rbs ?: "",
        na = sorted.firstOrNull { it.na.isNotBlank() }?.na ?: "",
        k = sorted.firstOrNull { it.k.isNotBlank() }?.k ?: "",
        alb = sorted.firstOrNull { it.alb.isNotBlank() }?.alb ?: "",
        acetone = sorted.firstOrNull { it.acetone.isNotBlank() }?.acetone ?: "",
        sugar = sorted.firstOrNull { it.sugar.isNotBlank() }?.sugar ?: "",
        pus = sorted.firstOrNull { it.pus.isNotBlank() }?.pus ?: ""
    )
}

object LabRecordSerializer {
    fun toJson(records: List<LabRecord>): String {
        return try {
            val jsonArray = org.json.JSONArray()
            for (rec in records) {
                val jsonObj = org.json.JSONObject().apply {
                    put("id", rec.id)
                    put("createdAt", rec.createdAt)
                    put("hb", rec.hb)
                    put("hct", rec.hct)
                    put("plt", rec.plt)
                    put("wbc", rec.wbc)
                    put("urea", rec.urea)
                    put("creat", rec.creat)
                    put("sgot", rec.sgot)
                    put("sgpt", rec.sgpt)
                    put("inr", rec.inr)
                    put("rbs", rec.rbs)
                    put("na", rec.na)
                    put("k", rec.k)
                    put("alb", rec.alb)
                    put("acetone", rec.acetone)
                    put("sugar", rec.sugar)
                    put("pus", rec.pus)
                }
                jsonArray.put(jsonObj)
            }
            jsonArray.toString()
        } catch (e: Exception) {
            "[]"
        }
    }

    fun fromJson(json: String): List<LabRecord> {
        if (json.isBlank()) return emptyList()
        return try {
            val list = mutableListOf<LabRecord>()
            val jsonArray = org.json.JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val jsonObj = jsonArray.getJSONObject(i)
                list.add(
                    LabRecord(
                        id = jsonObj.optString("id", java.util.UUID.randomUUID().toString()),
                        createdAt = jsonObj.optLong("createdAt", System.currentTimeMillis()),
                        hb = jsonObj.optString("hb", ""),
                        hct = jsonObj.optString("hct", ""),
                        plt = jsonObj.optString("plt", ""),
                        wbc = jsonObj.optString("wbc", ""),
                        urea = jsonObj.optString("urea", ""),
                        creat = jsonObj.optString("creat", ""),
                        sgot = jsonObj.optString("sgot", ""),
                        sgpt = jsonObj.optString("sgpt", ""),
                        inr = jsonObj.optString("inr", ""),
                        rbs = jsonObj.optString("rbs", ""),
                        na = jsonObj.optString("na", ""),
                        k = jsonObj.optString("k", ""),
                        alb = jsonObj.optString("alb", ""),
                        acetone = jsonObj.optString("acetone", ""),
                        sugar = jsonObj.optString("sugar", ""),
                        pus = jsonObj.optString("pus", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}

