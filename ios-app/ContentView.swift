import SwiftUI

public struct ContentView: View {
    @StateObject private var viewModel = PatientRoundsViewModel()
    
    // UI Sheets State
    @State private var showingAddEditPatient = false
    @State private var showingAddPost = false
    @State private var showingImportData = false
    @State private var showingShareAll = false
    @State private var selectedPatientForLabs: Patient? = nil
    @State private var newPostTitle = ""
    @State private var importJsonText = ""
    @State private var isExportingPdf = false
    @State private var pdfUrl: URL? = nil
    
    public init() {}
    
    public var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Ward/Post Selectors (Horizontal Bar)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        // All Wards button
                        Button(action: {
                            viewModel.selectedPostId = nil
                        }) {
                            Text("All Posts")
                                .font(.system(size: 13, weight: .bold))
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(viewModel.selectedPostId == nil ? Color.accentColor : Color(.systemGray5))
                                .foregroundColor(viewModel.selectedPostId == nil ? .white : .primary)
                                .cornerRadius(20)
                        }
                        
                        ForEach(viewModel.posts) { post in
                            Button(action: {
                                viewModel.selectedPostId = post.id
                            }) {
                                HStack(spacing: 4) {
                                    Text(post.title)
                                        .font(.system(size: 13, weight: .bold))
                                    
                                    if viewModel.selectedPostId == post.id {
                                        Button(action: {
                                            viewModel.deletePost(id: post.id)
                                        }) {
                                            Image(systemName: "xmark.circle.fill")
                                                .foregroundColor(.white.opacity(0.8))
                                        }
                                    }
                                }
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(viewModel.selectedPostId == post.id ? Color.accentColor : Color(.systemGray5))
                                .foregroundColor(viewModel.selectedPostId == post.id ? .white : .primary)
                                .cornerRadius(20)
                            }
                        }
                        
                        Button(action: {
                            newPostTitle = ""
                            showingAddPost = true
                        }) {
                            Image(systemName: "plus")
                                .padding(8)
                                .background(Color(.systemGray5))
                                .foregroundColor(.primary)
                                .clipShape(Circle())
                        }
                    }
                    .padding(.horizontal)
                    .padding(.vertical, 10)
                }
                .background(Color(.systemBackground))
                
                // Search bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)
                    TextField("Search patients, diagnosis, blood group...", text: $viewModel.searchQuery)
                        .textFieldStyle(PlainTextFieldStyle())
                }
                .padding(10)
                .background(Color(.systemGray6))
                .cornerRadius(10)
                .padding(.horizontal)
                .padding(.bottom, 8)
                
                // Active Patients List
                if viewModel.filteredPatients.isEmpty {
                    Spacer()
                    VStack(spacing: 12) {
                        Image(systemName: "doc.plaintext.fill")
                            .font(.system(size: 48))
                            .foregroundColor(.secondary)
                        Text("No Patients Found")
                            .font(.headline)
                        Text("Add or import clinical data to get started.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 40)
                    }
                    Spacer()
                } else {
                    List {
                        ForEach(viewModel.filteredPatients) { p in
                            PatientCard(patient: p, onToggleSelect: {
                                viewModel.togglePrintSelection(patientId: p.id)
                            }, onAddLab: {
                                selectedPatientForLabs = p
                            }, onEdit: {
                                viewModel.editingPatient = p
                                showingAddEditPatient = true
                            }, onDelete: {
                                viewModel.deletePatient(id: p.id)
                            })
                            .listRowInsets(EdgeInsets())
                            .listRowSeparator(.hidden)
                            .padding(.vertical, 4)
                            .padding(.horizontal)
                        }
                    }
                    .listStyle(.plain)
                    .background(Color(.systemGroupedBackground))
                }
                
                // Bottom Quick Action: Create New Patient Button
                Button(action: {
                    viewModel.editingPatient = nil
                    showingAddEditPatient = true
                }) {
                    HStack {
                        Image(systemName: "person.badge.plus")
                        Text("Add Patient Profile")
                            .fontWeight(.semibold)
                    }
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.accentColor)
                    .cornerRadius(12)
                    .padding()
                }
                .background(Color(.systemBackground))
            }
            .navigationTitle("Clinical Rounds Planner")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Menu {
                        Button(action: {
                            showingImportData = true
                        }) {
                            Label("Import Patient Data (JSON)", systemName: "square.and.arrow.down")
                        }
                        
                        Button(action: {
                            shareAllPatients()
                        }) {
                            Label("Share All Saved Profiles", systemName: "square.and.arrow.up")
                        }
                        
                        Button(role: .destructive, action: {
                            viewModel.clearAllPatientsInSelectedPost()
                        }) {
                            Label("Clear Post Patients", systemName: "trash")
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        exportRoundsPdf()
                    }) {
                        HStack(spacing: 4) {
                            Image(systemName: "printer")
                            Text("Rounds Sheet")
                        }
                    }
                }
            }
            // Dialog / Sheet Triggers
            .sheet(isPresented: $showingAddEditPatient) {
                AddEditPatientSheet(viewModel: viewModel, patientToEdit: viewModel.editingPatient)
            }
            .sheet(item: $selectedPatientForLabs) { patient in
                LabHistorySheet(viewModel: viewModel, patient: patient)
            }
            .sheet(isPresented: $showingImportData) {
                ImportDataSheet(viewModel: viewModel)
            }
            .alert("Add Clinical Post/Ward", isPresented: $showingAddPost) {
                TextField("Post Name (e.g. Ward A)", text: $newPostTitle)
                Button("Add") {
                    if !newPostTitle.isEmpty {
                        viewModel.addPost(title: newPostTitle)
                    }
                }
                Button("Cancel", role: .cancel) {}
            }
            .sheet(isPresented: $isExportingPdf, onDismiss: { pdfUrl = nil }) {
                if let url = pdfUrl {
                    ShareSheet(activityItems: [url])
                }
            }
        }
    }
    
    private func exportRoundsPdf() {
        let printList = viewModel.filteredPatients.filter { $0.isSelectedForPrint }
        let currentPostTitle = viewModel.posts.first(where: { $0.id == viewModel.selectedPostId })?.title ?? "All Ward Posts"
        
        if let url = PdfGenerator.generateRoundsPdf(patients: printList, postTitle: currentPostTitle) {
            self.pdfUrl = url
            self.isExportingPdf = true
        }
    }
    
    private func shareAllPatients() {
        let jsonStr = PatientSharing.serializeList(viewModel.patients)
        if !jsonStr.isEmpty {
            let av = UIActivityViewController(activityItems: [jsonStr], applicationActivities: nil)
            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let rootVC = windowScene.windows.first?.rootViewController {
                rootVC.present(av, animated: true)
            }
        }
    }
}

// MARK: - Patient Card Component
struct PatientCard: View {
    let patient: Patient
    let onToggleSelect: () -> Void
    let onAddLab: () -> Void
    let onEdit: () -> Void
    let onDelete: () -> Void
    
    @State private var isExpanded = false
    
    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .top, spacing: 12) {
                // Clickable custom checkbox for PDF print state
                Button(action: onToggleSelect) {
                    Image(systemName: patient.isSelectedForPrint ? "checkmark.circle.fill" : "circle")
                        .font(.system(size: 22))
                        .foregroundColor(patient.isSelectedForPrint ? .green : .secondary)
                }
                .padding(.top, 4)
                
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(patient.name.isEmpty ? "Unnamed Patient" : patient.name)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.primary)
                        
                        if !patient.abo.isEmpty {
                            Text(patient.abo)
                                .font(.system(size: 11, weight: .bold))
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Color.red.opacity(0.15))
                                .foregroundColor(.red)
                                .cornerRadius(4)
                        }
                        
                        Spacer()
                        
                        Button(action: {
                            withAnimation { isExpanded.toggle() }
                        }) {
                            Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                                .foregroundColor(.secondary)
                        }
                    }
                    
                    // Gestational Age Callout
                    if let ga = GestationalAgeCalculator.calculateGestationalAgeLong(lmpStr: patient.lmp, eddStr: patient.edd) {
                        Text(ga)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(.indigo)
                    }
                    
                    // Diagnosis Banner
                    if !patient.diagnosis.isEmpty {
                        Text(patient.diagnosis)
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)
                            .lineLimit(2)
                    }
                }
            }
            .padding(12)
            
            if isExpanded {
                Divider()
                
                VStack(alignment: .leading, spacing: 10) {
                    // Labs Summary Grid
                    Text("LATEST LAB CLINICALS")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(.secondary)
                    
                    let columns = [
                        GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())
                    ]
                    
                    LazyVGrid(columns: columns, spacing: 8) {
                        LabMiniCell(title: "Hb", value: patient.hb)
                        LabMiniCell(title: "Hct", value: patient.hct)
                        LabMiniCell(title: "Plt", value: patient.plt)
                        LabMiniCell(title: "WBC", value: patient.wbc)
                        LabMiniCell(title: "Creat", value: patient.creat)
                        LabMiniCell(title: "Sugar", value: patient.sugar)
                    }
                    
                    if !patient.notes.isEmpty {
                        Text("Notes: \(patient.notes)")
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(.secondary)
                            .padding(.top, 4)
                    }
                    
                    // Card Bottom Utilities
                    HStack {
                        Button(action: onAddLab) {
                            Label("Vitals & Labs", systemName: "chart.bar.doc.horizontal")
                                .font(.system(size: 12, weight: .semibold))
                                .padding(.vertical, 6)
                                .padding(.horizontal, 10)
                                .background(Color.accentColor.opacity(0.12))
                                .cornerRadius(6)
                        }
                        
                        Spacer()
                        
                        Button(action: onEdit) {
                            Image(systemName: "pencil")
                                .padding(8)
                                .background(Color(.systemGray5))
                                .cornerRadius(6)
                        }
                        
                        Button(action: onDelete) {
                            Image(systemName: "trash")
                                .padding(8)
                                .foregroundColor(.red)
                                .background(Color.red.opacity(0.12))
                                .cornerRadius(6)
                        }
                    }
                }
                .padding(12)
                .background(Color(.systemGray6).opacity(0.4))
            }
        }
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.04), radius: 4, x: 0, y: 2)
    }
}

struct LabMiniCell: View {
    let title: String
    let value: String
    
    var body: some View {
        HStack {
            Text("\(title):")
                .font(.system(size: 10, weight: .bold))
                .foregroundColor(.secondary)
            Text(value.isEmpty ? "—" : value)
                .font(.system(size: 10, weight: .semibold))
                .foregroundColor(.primary)
                .lineLimit(1)
            Spacer()
        }
        .padding(4)
        .background(Color(.systemGray5).opacity(0.5))
        .cornerRadius(4)
    }
}

// MARK: - Lab Records & History Management View
struct LabHistorySheet: View {
    @ObservedObject var viewModel: PatientRoundsViewModel
    let patient: Patient
    @Environment(\.dismiss) var dismiss
    
    @State private var showingAddLabForm = false
    // Lab form values
    @State private var hb = ""
    @State private var hct = ""
    @State private var plt = ""
    @State private var wbc = ""
    @State private var urea = ""
    @State private var creat = ""
    @State private var sgot = ""
    @State private var sgpt = ""
    @State private var inr = ""
    @State private var rbs = ""
    @State private var na = ""
    @State private var k = ""
    @State private var alb = ""
    @State private var acetone = ""
    @State private var sugar = ""
    @State private var pus = ""
    
    var body: some View {
        NavigationStack {
            VStack {
                if patient.decodedLabRecords.isEmpty {
                    Spacer()
                    Text("No lab entries recorded yet.")
                        .foregroundColor(.secondary)
                    Spacer()
                } else {
                    List {
                        ForEach(patient.decodedLabRecords.sorted(by: { $0.createdAt > $1.createdAt })) { lab in
                            VStack(alignment: .leading, spacing: 6) {
                                Text(DateFormatter.localizedString(from: Date(timeIntervalSince1970: lab.createdAt / 1000), dateStyle: .short, timeStyle: .short))
                                    .font(.system(size: 11, weight: .semibold))
                                    .foregroundColor(.accentColor)
                                
                                let columns = [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())]
                                LazyVGrid(columns: columns, spacing: 4) {
                                    LabValRow(label: "Hb", val: lab.hb)
                                    LabValRow(label: "Hct", val: lab.hct)
                                    LabValRow(label: "Plt", val: lab.plt)
                                    LabValRow(label: "WBC", val: lab.wbc)
                                    LabValRow(label: "Creat", val: lab.creat)
                                    LabValRow(label: "Urea", val: lab.urea)
                                    LabValRow(label: "RBS", val: lab.rbs)
                                    LabValRow(label: "SGOT", val: lab.sgot)
                                    LabValRow(label: "SGPT", val: lab.sgpt)
                                    LabValRow(label: "Na", val: lab.na)
                                    LabValRow(label: "K", val: lab.k)
                                    LabValRow(label: "Alb", val: lab.alb)
                                    LabValRow(label: "INR", val: lab.inr)
                                    LabValRow(label: "U.Sugar", val: lab.sugar)
                                    LabValRow(label: "U.Pus", val: lab.pus)
                                }
                            }
                            .padding(.vertical, 6)
                        }
                    }
                }
                
                Button(action: {
                    showingAddLabForm = true
                }) {
                    Text("Enter New Lab Data")
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.accentColor)
                        .cornerRadius(10)
                        .padding()
                }
            }
            .navigationTitle("Lab & Vitals Registry")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
            .sheet(isPresented: $showingAddLabForm) {
                NavigationStack {
                    Form {
                        Section("COMPLETE VITALS PANEL") {
                            HStack { Text("Hemoglobin"); Spacer(); TextField("Hb (g/dL)", text: $hb) }
                            HStack { Text("Hematocrit"); Spacer(); TextField("Hct (%)", text: $hct) }
                            HStack { Text("Platelets"); Spacer(); TextField("Plt (k/uL)", text: $plt) }
                            HStack { Text("WBC Count"); Spacer(); TextField("Wbc", text: $wbc) }
                        }
                        Section("KIDNEY & METABOLIC") {
                            HStack { Text("Creatinine"); Spacer(); TextField("Cr", text: $creat) }
                            HStack { Text("Blood Urea"); Spacer(); TextField("Urea", text: $urea) }
                            HStack { Text("Blood Glucose"); Spacer(); TextField("RBS", text: $rbs) }
                        }
                        Section("LIVER & ELECTROLYTES") {
                            HStack { Text("SGOT"); Spacer(); TextField("Sgot", text: $sgot) }
                            HStack { Text("SGPT"); Spacer(); TextField("Sgpt", text: $sgpt) }
                            HStack { Text("Sodium (Na)"); Spacer(); TextField("Na", text: $na) }
                            HStack { Text("Potassium (K)"); Spacer(); TextField("K", text: $k) }
                            HStack { Text("Albumin"); Spacer(); TextField("Alb", text: $alb) }
                            HStack { Text("INR"); Spacer(); TextField("Inr", text: $inr) }
                        }
                        Section("URINALYSIS PANEL") {
                            HStack { Text("U. Acetone"); Spacer(); TextField("Acetone", text: $acetone) }
                            HStack { Text("U. Sugar"); Spacer(); TextField("Sugar", text: $sugar) }
                            HStack { Text("U. Pus cells"); Spacer(); TextField("Pus", text: $pus) }
                        }
                    }
                    .navigationTitle("New Vitals Log")
                    .toolbar {
                        ToolbarItem(placement: .navigationBarLeading) {
                            Button("Cancel") { showingAddLabForm = false }
                        }
                        ToolbarItem(placement: .navigationBarTrailing) {
                            Button("Save") {
                                let newRec = LabRecord(
                                    hb: hb, hct: hct, plt: plt, wbc: wbc, urea: urea, creat: creat,
                                    sgot: sgot, sgpt: sgpt, inr: inr, rbs: rbs, na: na, k: k, alb: alb,
                                    acetone: acetone, sugar: sugar, pus: pus
                                )
                                viewModel.addLabRecord(patientId: patient.id, record: newRec)
                                showingAddLabForm = false
                            }
                        }
                    }
                }
            }
        }
    }
}

struct LabValRow: View {
    let label: String
    let val: String
    
    var body: some View {
        HStack {
            if !val.isEmpty {
                Text("\(label):")
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    .fontWeight(.bold)
                Text(val)
                    .font(.caption2)
                    .lineLimit(1)
            }
        }
    }
}

// MARK: - Add / Edit Patient View
struct AddEditPatientSheet: View {
    @ObservedObject var viewModel: PatientRoundsViewModel
    let patientToEdit: Patient?
    @Environment(\.dismiss) var dismiss
    
    @State private var name = ""
    @State private var abo = ""
    @State private var diagnosis = ""
    @State private var lmp = ""
    @State private var edd = ""
    @State private var us = ""
    @State private var notes = ""
    
    var body: some View {
        NavigationStack {
            Form {
                Section("PATIENT ESSENTIALS") {
                    TextField("Full Name", text: $name)
                    Picker("Blood Group (ABO/Rh)", selection: $abo) {
                        Text("None").tag("")
                        Text("A+").tag("A+")
                        Text("A-").tag("A-")
                        Text("B+").tag("B+")
                        Text("B-").tag("B-")
                        Text("AB+").tag("AB+")
                        Text("AB-").tag("AB-")
                        Text("O+").tag("O+")
                        Text("O-").tag("O-")
                    }
                }
                
                Section("DATES & DATING (LMP / EDD)") {
                    TextField("LMP (e.g. 21/09/2025)", text: $lmp)
                    TextField("EDD (e.g. 28/06/2026)", text: $edd)
                    TextField("US Gestation (e.g. 12wks+2d)", text: $us)
                    
                    // Live Dating Preview calculations
                    if let ga = GestationalAgeCalculator.calculateGestationalAgeLong(lmpStr: lmp, eddStr: edd) {
                        HStack {
                            Image(systemName: "calendar.badge.clock")
                                .foregroundColor(.indigo)
                            Text("Calculated: \(ga)")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(.indigo)
                        }
                    }
                }
                
                Section("DIAGNOSIS PROFILE") {
                    TextField("Diagnosis & Tags", text: $diagnosis)
                }
                
                Section("CLINICAL CASE NOTES") {
                    TextEditor(text: $notes)
                        .frame(height: 80)
                }
            }
            .navigationTitle(patientToEdit == nil ? "Create Profile" : "Edit Profile")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Save") {
                        let basePatient = patientToEdit ?? Patient()
                        let activePostId = viewModel.selectedPostId ?? viewModel.posts.first?.id ?? 1
                        
                        let updated = Patient(
                            id: basePatient.id,
                            postId: basePatient.id == 0 ? activePostId : basePatient.postId,
                            name: name,
                            abo: abo,
                            diagnosis: diagnosis,
                            lmp: lmp,
                            edd: edd,
                            us: us,
                            hb: basePatient.hb,
                            hct: basePatient.hct,
                            plt: basePatient.plt,
                            wbc: basePatient.wbc,
                            urea: basePatient.urea,
                            creat: basePatient.creat,
                            sgot: basePatient.sgot,
                            sgpt: basePatient.sgpt,
                            inr: basePatient.inr,
                            rbs: basePatient.rbs,
                            na: basePatient.na,
                            k: basePatient.k,
                            alb: basePatient.alb,
                            acetone: basePatient.acetone,
                            sugar: basePatient.sugar,
                            pus: basePatient.pus,
                            notes: notes,
                            createdAt: basePatient.createdAt,
                            isSelectedForPrint: basePatient.isSelectedForPrint,
                            labRecordsJson: basePatient.labRecordsJson
                        )
                        viewModel.saveOrUpdatePatient(updated)
                        dismiss()
                    }
                }
            }
            .onAppear {
                if let p = patientToEdit {
                    name = p.name
                    abo = p.abo
                    diagnosis = p.diagnosis
                    lmp = p.lmp
                    edd = p.edd
                    us = p.us
                    notes = p.notes
                }
            }
        }
    }
}

// MARK: - Import Shared JSON Parser View
struct ImportDataSheet: View {
    @ObservedObject var viewModel: PatientRoundsViewModel
    @Environment(\.dismiss) var dismiss
    @State private var jsonStringInput = ""
    @State private var parsedPatients: [Patient]? = nil
    
    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 14) {
                Text("Paste the shared OBD Patient JSON profile string down below to parse and import their profile details.")
                    .font(.system(size: 13))
                    .foregroundColor(.secondary)
                    .padding(.horizontal)
                    .padding(.top)
                
                TextEditor(text: $jsonStringInput)
                    .padding(8)
                    .background(Color(.systemGray6))
                    .cornerRadius(8)
                    .frame(height: 180)
                    .padding(.horizontal)
                    .onChange(of: jsonStringInput) { _ in
                        parsedPatients = PatientSharing.deserializeList(jsonStringInput)
                    }
                
                if let parsed = parsedPatients, !parsed.isEmpty {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("✔ Extracted \(parsed.count) Clinical Profile(s):")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(.green)
                        
                        ForEach(parsed.prefix(3)) { p in
                            Text("• \(p.name.isEmpty ? "Unnamed Patient" : p.name) (\(p.diagnosis.isEmpty ? "No active diagnosis" : p.diagnosis))")
                                .font(.system(size: 12))
                                .foregroundColor(.primary)
                        }
                        
                        if parsed.count > 3 {
                            Text("• ... And \(parsed.count - 3) more profiles.")
                                .font(.system(size: 12))
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding()
                    .background(Color.green.opacity(0.1))
                    .cornerRadius(8)
                    .padding(.horizontal)
                } else if !jsonStringInput.isEmpty {
                    Text("❌ Invalid client profile package format.")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(.red)
                        .padding(.horizontal)
                }
                
                Spacer()
                
                Button(action: {
                    if let parsed = parsedPatients {
                        viewModel.importPatients(parsed)
                        dismiss()
                    }
                }) {
                    Text("Import Patients (\(parsedPatients?.count ?? 0))")
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(parsedPatients == nil || parsedPatients!.isEmpty ? Color.gray : Color.accentColor)
                        .cornerRadius(10)
                        .padding()
                }
                .disabled(parsedPatients == nil || parsedPatients!.isEmpty)
            }
            .navigationTitle("Import Shared Data")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}

// Helper: iOS Activity View Sheet wrapper
struct ShareSheet: UIViewControllerRepresentable {
    var activityItems: [Any]
    var applicationActivities: [UIActivity]? = nil
    
    func makeUIViewController(context: Context) -> UIActivityViewController {
        let controller = UIActivityViewController(activityItems: activityItems, applicationActivities: applicationActivities)
        return controller
    }
    
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
