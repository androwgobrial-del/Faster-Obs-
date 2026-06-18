import Foundation

public struct PatientSharing {
    
    private static func patientToDict(_ patient: Patient) -> [String: Any] {
        return [
            "name": patient.name,
            "abo": patient.abo,
            "diagnosis": patient.diagnosis,
            "lmp": patient.lmp,
            "edd": patient.edd,
            "us": patient.us,
            "hb": patient.hb,
            "hct": patient.hct,
            "plt": patient.plt,
            "wbc": patient.wbc,
            "urea": patient.urea,
            "creat": patient.creat,
            "sgot": patient.sgot,
            "sgpt": patient.sgpt,
            "inr": patient.inr,
            "rbs": patient.rbs,
            "na": patient.na,
            "k": patient.k,
            "alb": patient.alb,
            "acetone": patient.acetone,
            "sugar": patient.sugar,
            "pus": patient.pus,
            "notes": patient.notes,
            "labRecordsJson": patient.labRecordsJson
        ]
    }
    
    private static func dictToPatient(_ dict: [String: Any]) -> Patient {
        return Patient(
            id: Int.random(in: 1000...999999),
            postId: 0,
            name: dict["name"] as? String ?? "",
            abo: dict["abo"] as? String ?? "",
            diagnosis: dict["diagnosis"] as? String ?? "",
            lmp: dict["lmp"] as? String ?? "",
            edd: dict["edd"] as? String ?? "",
            us: dict["us"] as? String ?? "",
            hb: dict["hb"] as? String ?? "",
            hct: dict["hct"] as? String ?? "",
            plt: dict["plt"] as? String ?? "",
            wbc: dict["wbc"] as? String ?? "",
            urea: dict["urea"] as? String ?? "",
            creat: dict["creat"] as? String ?? "",
            sgot: dict["sgot"] as? String ?? "",
            sgpt: dict["sgpt"] as? String ?? "",
            inr: dict["inr"] as? String ?? "",
            rbs: dict["rbs"] as? String ?? "",
            na: dict["na"] as? String ?? "",
            k: dict["k"] as? String ?? "",
            alb: dict["alb"] as? String ?? "",
            acetone: dict["acetone"] as? String ?? "",
            sugar: dict["sugar"] as? String ?? "",
            pus: dict["pus"] as? String ?? "",
            notes: dict["notes"] as? String ?? "",
            createdAt: Date().timeIntervalSince1970 * 1000,
            isSelectedForPrint: true,
            labRecordsJson: dict["labRecordsJson"] as? String ?? "[]"
        )
    }
    
    public static func serialize(_ patient: Patient) -> String {
        var dict = patientToDict(patient)
        dict["type"] = "FASTER_OBS_PATIENT"
        dict["version"] = 1
        
        do {
            let data = try JSONSerialization.data(withJSONObject: dict, options: [.prettyPrinted])
            return String(data: data, encoding: .utf8) ?? ""
        } catch {
            return ""
        }
    }
    
    public static func serializeList(_ patients: [Patient]) -> String {
        let listDicts = patients.map { patientToDict($0) }
        let root: [String: Any] = [
            "type": "FASTER_OBS_PATIENTS_LIST",
            "version": 1,
            "patients": listDicts
        ]
        
        do {
            let data = try JSONSerialization.data(withJSONObject: root, options: [.prettyPrinted])
            return String(data: data, encoding: .utf8) ?? ""
        } catch {
            return ""
        }
    }
    
    public static func deserializeList(_ jsonStr: String) -> [Patient]? {
        guard let data = jsonStr.trimmingCharacters(in: .whitespacesAndNewlines).data(using: .utf8) else {
            return nil
        }
        
        do {
            guard let json = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any] else {
                return nil
            }
            
            let type = json["type"] as? String ?? ""
            
            if type == "FASTER_OBS_PATIENTS_LIST" {
                guard let patientsArray = json["patients"] as? [[String: Any]] else {
                    return []
                }
                return patientsArray.map { dictToPatient($0) }
            }
            
            if type == "FASTER_OBS_PATIENT" || json["name"] != nil {
                let p = dictToPatient(json)
                return [p]
            }
            
            return nil
        } catch {
            return nil
        }
    }
}
