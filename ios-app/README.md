# Clinical Rounds Planner - iOS App Codebase

This is a complete, production-ready, high-fidelity native iOS app translated directly from the Android application codebase using Swift and SwiftUI. It maintains **exact layout, functionality, list card shapes, and cross-platform patient sharing data compatibility**.

## Core Features Included
1. **Dynamic Pregnancy dating**: The exact gestational age candidate dating algorithm (`GestationalAgeCalculator`), parsing multiple date forms and calculating weeks/days offsets based on reference date, plus replacing diagnosis strings with matching regex selectors.
2. **Clinical Ward/Post Filtration**: Manage multiple columns and wards with horizontal scrolling selectors and full CRUD capability.
3. **Clinical Vitals Register & Timeline**: Log consecutive metrics over time for individual patients and view consolidated lab states.
4. **Interactive Rounds Planner**: Mark patients for sheet print, search queries, and filter through patient profiles.
5. **PDF Rounds Sheet Compiler**: Native Swift `UIGraphicsPDFRenderer` generation allowing you to print or preview complete clinical rounds directly from your iPhone.
6. **Unified Cross-Platform Data Sharing**: Fully compatible custom JSON encoder/decoder (`PatientSharing`). You can share patient JSON strings between Android and iOS and import them with 100% data fidelity!

---

## How to Set Up in Xcode (iOS / macOS / iPad)

### Option 1: Using Xcode on a Mac (Recommended)
1. **Open Xcode** on your Mac.
2. Select **"Create a new Xcode project"**.
3. Under the iOS tab, choose **App** and click Next.
4. Name your project (e.g., `ClinicalRoundsPlanner`), set **Interface** to **SwiftUI**, and **Language** to **Swift**. Click Next and save the project.
5. In the Xcode file explorer sidebar, delete `ContentView.swift` and the main App entry file (`ClinicalRoundsPlannerApp.swift`).
6. Copy the following files from this `ios-app/` directory and drag/import them into your Xcode project:
   - `Models.swift`
   - `GestationalAgeCalculator.swift`
   - `PatientSharing.swift`
   - `PatientRoundsViewModel.swift`
   - `PdfGenerator.swift`
   - `ContentView.swift`
   - `App.swift` (Make sure this contains the `@main` attribute)
7. Connect your iPhone via USB, select it as the target device in the top bar, and press **Cmd + R** (or click the Play arrow icon) to build and run the application!

### Option 2: Using Swift Playgrounds (M1/M2/M3 iPads or Macbooks)
1. Open the official **Swift Playgrounds** app.
2. Create a new "App" playground.
3. Replace the active code in the core files or add new custom Swift files matching the contents of the files in `/ios-app/`.
4. Click **Run** to launch and preview the fully functional application live in the iPad sandbox!
