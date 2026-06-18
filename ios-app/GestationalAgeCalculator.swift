import Foundation

public struct GestationalAgeCalculator {
    
    private static func parseDateCandidates(_ dateStr: String) -> [Date] {
        let cleaned = dateStr.trimmingCharacters(in: .whitespacesAndNewlines)
        if cleaned.isEmpty { return [] }
        
        var candidates: [Date] = []
        
        // Try common date formats
        let patterns = [
            "dd/MM/yyyy", "d/M/yyyy", "dd/MM/yy", "d/M/yy",
            "dd.MM.yyyy", "d.M.yyyy", "dd-MM-yyyy", "d-M-yyyy",
            "MM/dd/yyyy", "M/d/yyyy", "MM/dd/yy", "M/d/yy",
            "yyyy-MM-dd", "yyyy.MM.dd", "yyyy/MM/dd"
        ]
        
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US")
        formatter.calendar = Calendar(identifier: .gregorian)
        
        for p in patterns {
            formatter.dateFormat = p
            if let date = formatter.date(from: cleaned) {
                candidates.append(date)
            }
        }
        
        // Normalized replacement
        let normalized = cleaned.replacingOccurrences(of: "[-.\\s]+", with: "/", options: .regularExpression)
        let normalizedPatterns = [
            "dd/MM/yyyy", "d/M/yyyy", "dd/MM/yy", "d/M/yy",
            "dd/MMM/yyyy", "d/MMM/yyyy", "dd/MMMM/yyyy", "d/MMMM/yyyy",
            "MM/dd/yyyy", "M/d/yyyy", "MM/dd/yy", "M/d/yy",
            "yyyy/MM/dd", "yyyy/M/d",
            "MMM/dd/yyyy", "MMMM/dd/yyyy", "yyyy/MMM/dd"
        ]
        
        for p in normalizedPatterns {
            formatter.dateFormat = p
            formatter.locale = Locale(identifier: "en_US")
            if let date = formatter.date(from: normalized) {
                candidates.append(date)
            }
            formatter.locale = Locale.current
            if let date = formatter.date(from: normalized) {
                candidates.append(date)
            }
        }
        
        // Remove duplicates while keeping order
        var uniqueCandidates: [Date] = []
        for c in candidates {
            if !uniqueCandidates.contains(c) {
                uniqueCandidates.append(c)
            }
        }
        return uniqueCandidates
    }
    
    public static func calculateFromLmp(_ lmpStr: String, referenceDate: Date = Date()) -> (weeks: Int, days: Int)? {
        let candidates = parseDateCandidates(lmpStr)
        if candidates.isEmpty { return nil }
        
        let calendar = Calendar.current
        var bestWeeks = -1
        var bestDays = -1
        var found = false
        
        for c in candidates {
            if referenceDate >= c {
                let diff = calendar.dateComponents([.day], from: c, to: referenceDate)
                if let diffDays = diff.day {
                    let weeks = diffDays / 7
                    let days = diffDays % 7
                    if weeks >= 0 && weeks <= 45 {
                        bestWeeks = weeks
                        bestDays = days
                        found = true
                        break
                    }
                }
            }
        }
        
        if !found {
            guard let first = candidates.first else { return nil }
            if referenceDate < first { return nil }
            let diff = calendar.dateComponents([.day], from: first, to: referenceDate)
            if let diffDays = diff.day {
                let weeks = diffDays / 7
                let days = diffDays % 7
                if weeks > 50 { return nil }
                return (weeks, days)
            }
            return nil
        }
        
        return (bestWeeks, bestDays)
    }
    
    public static func calculateFromEdd(_ eddStr: String, referenceDate: Date = Date()) -> (weeks: Int, days: Int)? {
        let candidates = parseDateCandidates(eddStr)
        if candidates.isEmpty { return nil }
        
        let calendar = Calendar.current
        var bestWeeks = -1
        var bestDays = -1
        var found = false
        
        for eddDate in candidates {
            // Estimated LMP = EDD - 280 days
            if let estLmp = calendar.date(byAdding: .day, value: -280, to: eddDate) {
                if referenceDate >= estLmp {
                    let diff = calendar.dateComponents([.day], from: estLmp, to: referenceDate)
                    if let diffDays = diff.day {
                        let weeks = diffDays / 7
                        let days = diffDays % 7
                        if weeks >= 0 && weeks <= 45 {
                            bestWeeks = weeks
                            bestDays = days
                            found = true
                            break
                        }
                    }
                }
            }
        }
        
        if !found {
            guard let firstEdd = candidates.first else { return nil }
            if let estLmp = calendar.date(byAdding: .day, value: -280, to: firstEdd) {
                if referenceDate < estLmp { return nil }
                let diff = calendar.dateComponents([.day], from: estLmp, to: referenceDate)
                if let diffDays = diff.day {
                    let weeks = diffDays / 7
                    let days = diffDays % 7
                    if weeks < 0 || weeks > 50 { return nil }
                    return (weeks, days)
                }
            }
            return nil
        }
        
        return (bestWeeks, bestDays)
    }
    
    public static func calculateGestationalAge(lmpStr: String, eddStr: String, referenceDate: Date = Date()) -> String? {
        if let lmpResult = calculateFromLmp(lmpStr, referenceDate: referenceDate) {
            return lmpResult.days == 0 ? "\(lmpResult.weeks)wks" : "\(lmpResult.weeks)wks+\(lmpResult.days)D"
        }
        if let eddResult = calculateFromEdd(eddStr, referenceDate: referenceDate) {
            return eddResult.days == 0 ? "\(eddResult.weeks)wks" : "\(eddResult.weeks)wks+\(eddResult.days)D"
        }
        return nil
    }
    
    public static func calculateGestationalAgeLong(lmpStr: String, eddStr: String, referenceDate: Date = Date()) -> String? {
        if let lmpResult = calculateFromLmp(lmpStr, referenceDate: referenceDate) {
            return lmpResult.days == 0 ? "\(lmpResult.weeks) Weeks (by LMP)" : "\(lmpResult.weeks) Weeks + \(lmpResult.days) Days (by LMP)"
        }
        if let eddResult = calculateFromEdd(eddStr, referenceDate: referenceDate) {
            return eddResult.days == 0 ? "\(eddResult.weeks) Weeks (by EDD)" : "\(eddResult.weeks) Weeks + \(eddResult.days) Days (by EDD)"
        }
        return nil
    }
    
    // Pattern equivalent to: \b\d+\s*(?:wks?|w|weeks?)\s*\+\s*\d+\s*(?:[Dd]|days?)\b|\b\d+\s*(?:wks?|weeks?)\b
    private static let regexPattern = "\\b\\d+\\s*(?:wks?|w|weeks?)\\s*\\+\\s*\\d+\\s*(?:[Dd]|days?)\\b|\\b\\d+\\s*(?:wks?|weeks?)\\b"
    
    public static func updateGaInDiagnosis(diagnosis: String, currentGa: String, forceAppend: Bool = false) -> String {
        if currentGa.isEmpty { return diagnosis }
        
        guard let regex = try? NSRegularExpression(pattern: regexPattern, options: [.caseInsensitive]) else {
            return diagnosis
        }
        
        let range = NSRange(location: 0, length: diagnosis.utf16.count)
        let hasExisting = regex.firstMatch(in: diagnosis, options: [], range: range) != nil
        
        if hasExisting {
            let mutableString = NSMutableString(string: diagnosis)
            regex.replaceMatches(in: mutableString, options: [], range: range, withTemplate: currentGa)
            return mutableString as String
        } else if forceAppend {
            let trimmed = diagnosis.trimmingCharacters(in: .whitespacesAndNewlines)
            if trimmed.isEmpty {
                return currentGa
            } else {
                return "\(trimmed), \(currentGa)"
            }
        } else {
            return diagnosis
        }
    }
}
