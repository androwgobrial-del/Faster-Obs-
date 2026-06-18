import Foundation
import Combine

public class PatientRoundsViewModel: ObservableObject {
    @Published public var posts: [Post] = []
    @Published public var patients: [Patient] = []
    @Published public var selectedPostId: Int? = nil
    @Published public var searchQuery: String = ""
    @Published public var editingPatient: Patient? = nil
    @Published public var activePostIdForImport: Int? = nil
    
    private let postsKey = "clinical_rounds_posts"
    private let patientsKey = "clinical_rounds_patients"
    
    public init() {
        loadData()
        if posts.isEmpty {
            // Seed initial wards/posts
            let initialPosts = [
                Post(id: 1, title: "Post A (Antepartum)"),
                Post(id: 2, title: "Post B (Postpartum)"),
                Post(id: 3, title: "Post C (Labor & Delivery)")
            ]
            self.posts = initialPosts
            self.selectedPostId = 1
            savePosts()
        } else {
            self.selectedPostId = posts.first?.id
        }
    }
    
    // Core filtering
    public var filteredPatients: [Patient] {
        let query = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return patients.filter { patient in
            let matchesPost = (selectedPostId == nil || patient.postId == selectedPostId)
            let matchesSearch = query.isEmpty ||
                patient.name.lowercased().contains(query) ||
                patient.diagnosis.lowercased().contains(query) ||
                patient.abo.lowercased().contains(query)
            return matchesPost && matchesSearch
        }
    }
    
    // CRUD Operations for Wards
    public func addPost(title: String) {
        let newId = (posts.map { $0.id }.max() ?? 0) + 1
        let newPost = Post(id: newId, title: title)
        posts.append(newPost)
        savePosts()
        if selectedPostId == nil {
            selectedPostId = newId
        }
    }
    
    public func deletePost(id: Int) {
        posts.removeAll { $0.id == id }
        patients.removeAll { $0.postId == id } // Cascade delete
        savePosts()
        savePatients()
        if selectedPostId == id {
            selectedPostId = posts.first?.id
        }
    }
    
    // CRUD Operations for Patients
    public func saveOrUpdatePatient(_ patient: Patient) {
        var finalPatient = patient
        
        // Calculate GA and update diagnosis if possible
        let ga = GestationalAgeCalculator.calculateGestationalAge(lmpStr: patient.lmp, eddStr: patient.edd) ?? ""
        if !ga.isEmpty {
            finalPatient.diagnosis = GestationalAgeCalculator.updateGaInDiagnosis(diagnosis: patient.diagnosis, currentGa: ga)
        }
        
        if let idx = patients.firstIndex(where: { $0.id == patient.id && patient.id != 0 }) {
            patients[idx] = finalPatient
        } else {
            // Create New
            let newId = (patients.map { $0.id }.max() ?? 0) + 1
            finalPatient.id = newId
            patients.append(finalPatient)
        }
        savePatients()
    }
    
    public func deletePatient(id: Int) {
        patients.removeAll { $0.id == id }
        savePatients()
    }
    
    public func togglePrintSelection(patientId: Int) {
        if let idx = patients.firstIndex(where: { $0.id == patientId }) {
            patients[idx].isSelectedForPrint.toggle()
            savePatients()
        }
    }
    
    public func addLabRecord(patientId: Int, record: LabRecord) {
        if let idx = patients.firstIndex(where: { $0.id == patientId }) {
            var patient = patients[idx]
            var labs = patient.decodedLabRecords
            labs.append(record)
            
            // Re-serialize
            if let data = try? JSONEncoder().encode(labs), let jsonString = String(data: data, encoding: .utf8) {
                patient.labRecordsJson = jsonString
                
                // Keep patient static fields synced to latest lab for quick query
                patient.hb = record.hb.isEmpty ? patient.hb : record.hb
                patient.hct = record.hct.isEmpty ? patient.hct : record.hct
                patient.plt = record.plt.isEmpty ? patient.plt : record.plt
                patient.wbc = record.wbc.isEmpty ? patient.wbc : record.wbc
                patient.urea = record.urea.isEmpty ? patient.urea : record.urea
                patient.creat = record.creat.isEmpty ? patient.creat : record.creat
                patient.sgot = record.sgot.isEmpty ? patient.sgot : record.sgot
                patient.sgpt = record.sgpt.isEmpty ? patient.sgpt : record.sgpt
                patient.inr = record.inr.isEmpty ? patient.inr : record.inr
                patient.rbs = record.rbs.isEmpty ? patient.rbs : record.rbs
                patient.na = record.na.isEmpty ? patient.na : record.na
                patient.k = record.k.isEmpty ? patient.k : record.k
                patient.alb = record.alb.isEmpty ? patient.alb : record.alb
                patient.acetone = record.acetone.isEmpty ? patient.acetone : record.acetone
                patient.sugar = record.sugar.isEmpty ? patient.sugar : record.sugar
                patient.pus = record.pus.isEmpty ? patient.pus : record.pus
                
                patients[idx] = patient
                savePatients()
            }
        }
    }
    
    public func importPatients(_ imported: [Patient]) {
        let activeId = selectedPostId ?? posts.first?.id ?? 1
        for p in imported {
            var copy = p
            let newId = (patients.map { $0.id }.max() ?? 0) + 1
            copy.id = newId
            copy.postId = activeId
            patients.append(copy)
        }
        savePatients()
    }
    
    public func clearAllPatientsInSelectedPost() {
        guard let postId = selectedPostId else { return }
        patients.removeAll { $0.postId == postId }
        savePatients()
    }
    
    // Persistence Helpers
    private func savePosts() {
        if let encoded = try? JSONEncoder().encode(posts) {
            UserDefaults.standard.set(encoded, forKey: postsKey)
        }
    }
    
    private func savePatients() {
        if let encoded = try? JSONEncoder().encode(patients) {
            UserDefaults.standard.set(encoded, forKey: patientsKey)
        }
    }
    
    private func loadData() {
        if let postsData = UserDefaults.standard.data(forKey: postsKey),
           let decodedPosts = try? JSONDecoder().decode([Post].self, from: postsData) {
            self.posts = decodedPosts
        }
        
        if let patientsData = UserDefaults.standard.data(forKey: patientsKey),
           let decodedPatients = try? JSONDecoder().decode([Patient].self, from: patientsData) {
            self.patients = decodedPatients
        }
    }
}
