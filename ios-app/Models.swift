import Foundation

public struct Post: Identifiable, Codable, Hashable {
    public var id: Int
    public var title: String
    public var createdAt: Date
    
    public init(id: Int, title: String, createdAt: Date = Date()) {
        self.id = id
        self.title = title
        self.createdAt = createdAt
    }
}

public struct LabRecord: Identifiable, Codable, Hashable {
    public var id: String
    public var createdAt: Double
    public var hb: String
    public var hct: String
    public var plt: String
    public var wbc: String
    public var urea: String
    public var creat: String
    public var sgot: String
    public var sgpt: String
    public var inr: String
    public var rbs: String
    public var na: String
    public var k: String
    public var alb: String
    public var acetone: String
    public var sugar: String
    public var pus: String
    
    public init(
        id: String = UUID().uuidString,
        createdAt: Double = Date().timeIntervalSince1970 * 1000,
        hb: String = "",
        hct: String = "",
        plt: String = "",
        wbc: String = "",
        urea: String = "",
        creat: String = "",
        sgot: String = "",
        sgpt: String = "",
        inr: String = "",
        rbs: String = "",
        na: String = "",
        k: String = "",
        alb: String = "",
        acetone: String = "",
        sugar: String = "",
        pus: String = ""
    ) {
        self.id = id
        self.createdAt = createdAt
        self.hb = hb
        self.hct = hct
        self.plt = plt
        self.wbc = wbc
        self.urea = urea
        self.creat = creat
        self.sgot = sgot
        self.sgpt = sgpt
        self.inr = inr
        self.rbs = rbs
        self.na = na
        self.k = k
        self.alb = alb
        self.acetone = acetone
        self.sugar = sugar
        self.pus = pus
    }
}

public struct Patient: Identifiable, Codable, Hashable {
    public var id: Int
    public var postId: Int
    public var name: String
    public var abo: String
    public var diagnosis: String
    public var lmp: String
    public var edd: String
    public var us: String
    
    // Labs
    public var hb: String
    public var hct: String
    public var plt: String
    public var wbc: String
    public var urea: String
    public var creat: String
    public var sgot: String
    public var sgpt: String
    public var inr: String
    public var rbs: String
    public var na: String
    public var k: String
    public var alb: String
    public var acetone: String
    public var sugar: String
    public var pus: String
    
    public var notes: String
    public var createdAt: Double
    public var isSelectedForPrint: Bool
    public var labRecordsJson: String // Serialized array of labs
    
    public init(
        id: Int = 0,
        postId: Int = 0,
        name: String = "",
        abo: String = "",
        diagnosis: String = "",
        lmp: String = "",
        edd: String = "",
        us: String = "",
        hb: String = "",
        hct: String = "",
        plt: String = "",
        wbc: String = "",
        urea: String = "",
        creat: String = "",
        sgot: String = "",
        sgpt: String = "",
        inr: String = "",
        rbs: String = "",
        na: String = "",
        k: String = "",
        alb: String = "",
        acetone: String = "",
        sugar: String = "",
        pus: String = "",
        notes: String = "",
        createdAt: Double = Date().timeIntervalSince1970 * 1000,
        isSelectedForPrint: Bool = true,
        labRecordsJson: String = "[]"
    ) {
        self.id = id
        self.postId = postId
        self.name = name
        self.abo = abo
        self.diagnosis = diagnosis
        self.lmp = lmp
        self.edd = edd
        self.us = us
        self.hb = hb
        self.hct = hct
        self.plt = plt
        self.wbc = wbc
        self.urea = urea
        self.creat = creat
        self.sgot = sgot
        self.sgpt = sgpt
        self.inr = inr
        self.rbs = rbs
        self.na = na
        self.k = k
        self.alb = alb
        self.acetone = acetone
        self.sugar = sugar
        self.pus = pus
        self.notes = notes
        self.createdAt = createdAt
        self.isSelectedForPrint = isSelectedForPrint
        self.labRecordsJson = labRecordsJson
    }
    
    // Parse helper
    public var decodedLabRecords: [LabRecord] {
        guard let data = labRecordsJson.data(using: .utf8) else { return [] }
        return (try? JSONDecoder().decode([LabRecord].self, from: data)) ?? []
    }
}
