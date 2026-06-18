import Foundation
import UIKit

public struct PdfGenerator {
    
    public static func generateRoundsPdf(patients: [Patient], postTitle: String) -> URL? {
        let pdfMetaData = [
            kCGImagePropertyGPSDictionary: nil,
            kCGPDFContextCreator: "Obstetrics Clinical Rounds",
            kCGPDFContextAuthor: "Clinical Rounds Planner"
        ] as [CFString : Any]
        
        let format = UIGraphicsPDFRendererFormat()
        format.documentInfo = pdfMetaData as [String: Any]
        
        // A4 Page dimensions: 595.2 x 841.8 points
        let pageWidth = 595.2
        let pageHeight = 841.8
        let bounds = CGRect(x: 0, y: 0, width: pageWidth, height: pageHeight)
        
        let renderer = UIGraphicsPDFRenderer(bounds: bounds, format: format)
        
        let tempDir = FileManager.default.temporaryDirectory
        let fileURL = tempDir.appendingPathComponent("Clinical_Rounds_\(Date().timeIntervalSince1970).pdf")
        
        do {
            try renderer.writePDF(to: fileURL) { context in
                context.beginPage()
                
                var currentY: CGFloat = 40.0
                let margin: CGFloat = 36.0
                let contentWidth = CGFloat(pageWidth) - (margin * 2)
                
                // --- HEADER ---
                let titleFont = UIFont.boldSystemFont(ofSize: 20)
                let subtitleFont = UIFont.systemFont(ofSize: 12)
                let regularFont = UIFont.systemFont(ofSize: 10)
                let boldFont = UIFont.boldSystemFont(ofSize: 10)
                
                let titleText = "CLINICAL ROUNDS SHEET"
                let dateText = DateFormatter.localizedString(from: Date(), dateStyle: .medium, timeStyle: .short)
                let wardText = "Wards/Post: \(postTitle)"
                
                // Draw Title
                let titleAttributes: [NSAttributedString.Key: Any] = [
                    .font: titleFont,
                    .foregroundColor: UIColor.label
                ]
                titleText.draw(at: CGPoint(x: margin, y: currentY), withAttributes: titleAttributes)
                currentY += 26
                
                // Draw Subtitles
                let subAttributes: [NSAttributedString.Key: Any] = [
                    .font: subtitleFont,
                    .foregroundColor: UIColor.secondaryLabel
                ]
                wardText.draw(at: CGPoint(x: margin, y: currentY), withAttributes: subAttributes)
                
                let dateWidth = dateText.size(withAttributes: subAttributes).width
                dateText.draw(at: CGPoint(x: CGFloat(pageWidth) - margin - dateWidth, y: currentY), withAttributes: subAttributes)
                currentY += 24
                
                // Draw Horizontal separator line
                let borderPath = UIBezierPath()
                borderPath.move(to: CGPoint(x: margin, y: currentY))
                borderPath.addLine(to: CGPoint(x: CGFloat(pageWidth) - margin, y: currentY))
                borderPath.lineWidth = 1.0
                UIColor.separator.setStroke()
                borderPath.stroke()
                
                currentY += 15
                
                if patients.isEmpty {
                    let emptyText = "No active records selected for print."
                    emptyText.draw(at: CGPoint(x: margin, y: currentY), withAttributes: [.font: subtitleFont])
                    return
                }
                
                // --- DRAW PATIENTS ---
                for patient in patients {
                    // Check if page overflow
                    if currentY > CGFloat(pageHeight) - 80 {
                        context.beginPage()
                        currentY = 40.0
                    }
                    
                    // Card background for each patient
                    let cardHeight: CGFloat = 130.0
                    let cardRect = CGRect(x: margin, y: currentY, width: contentWidth, height: cardHeight)
                    let cardPath = UIBezierPath(roundedRect: cardRect, cornerRadius: 6.0)
                    UIColor.systemGray6.setFill()
                    cardPath.fill()
                    
                    // Patient Header: Name and BLOOD GROUP
                    let pName = patient.name.isEmpty ? "Unnamed Patient" : patient.name
                    let bloodStr = patient.abo.isEmpty ? "" : "(\(patient.abo))"
                    let headerText = "\(pName) \(bloodStr)"
                    
                    headerText.draw(at: CGPoint(x: margin + 10, y: currentY + 10), withAttributes: [
                        .font: UIFont.boldSystemFont(ofSize: 12),
                        .foregroundColor: UIColor.label
                    ])
                    
                    // Gestational Age Callout
                    let ga = GestationalAgeCalculator.calculateGestationalAgeLong(lmpStr: patient.lmp, eddStr: patient.edd) ?? "Pregnancy Dates: Unknown"
                    ga.draw(at: CGPoint(x: margin + 10, y: currentY + 28), withAttributes: [
                        .font: regularFont,
                        .foregroundColor: UIColor.systemIndigo
                    ])
                    
                    // Diagnosis block
                    let diagText = "Diagnosis: \(patient.diagnosis.isEmpty ? "No active diagnosis" : patient.diagnosis)"
                    diagText.draw(in: CGRect(x: margin + 10, y: currentY + 44, width: contentWidth - 20, height: 32), withAttributes: [
                        .font: regularFont,
                        .foregroundColor: UIColor.label
                    ])
                    
                    // Vital Labs Sub-table (compact)
                    let labsRowY = currentY + 80
                    let cols = [
                        ("Hb", patient.hb),
                        ("Hct", patient.hct),
                        ("Plt", patient.plt),
                        ("Wbc", patient.wbc),
                        ("Creat", patient.creat),
                        ("Sugar", patient.sugar)
                    ]
                    
                    let colWidth = (contentWidth - 20) / CGFloat(cols.count)
                    for (i, col) in cols.enumerated() {
                        let colX = margin + 10 + (CGFloat(i) * colWidth)
                        
                        // Header label
                        col.0.draw(at: CGPoint(x: colX, y: labsRowY), withAttributes: [
                            .font: UIFont.boldSystemFont(ofSize: 8),
                            .foregroundColor: UIColor.secondaryLabel
                        ])
                        
                        // Value
                        let val = col.1.isEmpty ? "—" : col.1
                        val.draw(at: CGPoint(x: colX, y: labsRowY + 10), withAttributes: [
                            .font: boldFont,
                            .foregroundColor: UIColor.label
                        ])
                    }
                    
                    // Notes Footer
                    if !patient.notes.isEmpty {
                        let notesStr = "Notes: \(patient.notes)"
                        notesStr.draw(in: CGRect(x: margin + 10, y: currentY + 105, width: contentWidth - 20, height: 20), withAttributes: [
                            .font: UIFont.italicSystemFont(ofSize: 8),
                            .foregroundColor: UIColor.secondaryLabel
                        ])
                    }
                    
                    currentY += cardHeight + 10
                }
            }
            return fileURL
        } catch {
            return nil
        }
    }
}
