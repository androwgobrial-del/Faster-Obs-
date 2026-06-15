package com.example.utils

import com.example.data.PatientEntity
import org.json.JSONArray
import org.json.JSONObject

object PatientSharing {
    private fun patientToJs(patient: PatientEntity): JSONObject {
        return JSONObject().apply {
            put("name", patient.name)
            put("abo", patient.abo)
            put("diagnosis", patient.diagnosis)
            put("lmp", patient.lmp)
            put("edd", patient.edd)
            put("us", patient.us)
            put("hb", patient.hb)
            put("hct", patient.hct)
            put("plt", patient.plt)
            put("wbc", patient.wbc)
            put("urea", patient.urea)
            put("creat", patient.creat)
            put("sgot", patient.sgot)
            put("sgpt", patient.sgpt)
            put("inr", patient.inr)
            put("rbs", patient.rbs)
            put("na", patient.na)
            put("k", patient.k)
            put("alb", patient.alb)
            put("acetone", patient.acetone)
            put("sugar", patient.sugar)
            put("pus", patient.pus)
            put("notes", patient.notes)
            put("labRecordsJson", patient.labRecordsJson)
        }
    }

    private fun jsToPatient(obj: JSONObject): PatientEntity {
        return PatientEntity(
            name = obj.optString("name", ""),
            abo = obj.optString("abo", ""),
            diagnosis = obj.optString("diagnosis", ""),
            lmp = obj.optString("lmp", ""),
            edd = obj.optString("edd", ""),
            us = obj.optString("us", ""),
            hb = obj.optString("hb", ""),
            hct = obj.optString("hct", ""),
            plt = obj.optString("plt", ""),
            wbc = obj.optString("wbc", ""),
            urea = obj.optString("urea", ""),
            creat = obj.optString("creat", ""),
            sgot = obj.optString("sgot", ""),
            sgpt = obj.optString("sgpt", ""),
            inr = obj.optString("inr", ""),
            rbs = obj.optString("rbs", ""),
            na = obj.optString("na", ""),
            k = obj.optString("k", ""),
            alb = obj.optString("alb", ""),
            acetone = obj.optString("acetone", ""),
            sugar = obj.optString("sugar", ""),
            pus = obj.optString("pus", ""),
            notes = obj.optString("notes", ""),
            labRecordsJson = obj.optString("labRecordsJson", "[]"),
            isSelectedForPrint = true
        )
    }

    fun serialize(patient: PatientEntity): String {
        return try {
            val obj = patientToJs(patient).apply {
                put("type", "FASTER_OBS_PATIENT")
                put("version", 1)
            }
            obj.toString(2)
        } catch (e: Exception) {
            ""
        }
    }

    fun serializeList(patients: List<PatientEntity>): String {
        return try {
            val root = JSONObject().apply {
                put("type", "FASTER_OBS_PATIENTS_LIST")
                put("version", 1)
                val array = JSONArray()
                patients.forEach { patient ->
                    array.put(patientToJs(patient))
                }
                put("patients", array)
            }
            root.toString(2)
        } catch (e: Exception) {
            ""
        }
    }

    fun deserialize(jsonStr: String): PatientEntity? {
        if (jsonStr.isBlank()) return null
        return try {
            val trimmed = jsonStr.trim()
            val obj = if (trimmed.startsWith("{")) {
                JSONObject(trimmed)
            } else {
                return null
            }
            
            val type = obj.optString("type", "")
            if (type != "FASTER_OBS_PATIENT" && !obj.has("name") && type != "FASTER_OBS_PATIENTS_LIST") {
                return null
            }
            
            if (type == "FASTER_OBS_PATIENTS_LIST") {
                val array = obj.optJSONArray("patients")
                if (array != null && array.length() > 0) {
                    return jsToPatient(array.getJSONObject(0))
                }
                return null
            }

            jsToPatient(obj)
        } catch (e: Exception) {
            null
        }
    }

    fun deserializeList(jsonStr: String): List<PatientEntity>? {
        if (jsonStr.isBlank()) return null
        return try {
            val trimmed = jsonStr.trim()
            val obj = if (trimmed.startsWith("{")) {
                JSONObject(trimmed)
            } else {
                return null
            }
            
            val type = obj.optString("type", "")
            
            if (type == "FASTER_OBS_PATIENTS_LIST") {
                val array = obj.optJSONArray("patients") ?: return emptyList()
                val list = mutableListOf<PatientEntity>()
                for (i in 0 until array.length()) {
                    list.add(jsToPatient(array.getJSONObject(i)))
                }
                return list
            }
            
            if (type == "FASTER_OBS_PATIENT" || obj.has("name")) {
                val p = jsToPatient(obj)
                return listOf(p)
            }
            
            null
        } catch (e: Exception) {
            null
        }
    }
}
