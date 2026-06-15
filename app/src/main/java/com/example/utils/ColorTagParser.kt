package com.example.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping

object ColorTagParser {

    private val ANY_TAG_REGEX = Regex("\\[(/?)(red|blue|green|orange|pink|purple|yellow|bold|hyellow|hpink)\\]", RegexOption.IGNORE_CASE)

    // Map tag string to hex color for PDF (returns default since text colors are removed)
    fun getTagColorHex(tag: String): String {
        return "#000000"
    }

    // Map tag string to Jetpack Compose Color (returns default since text colors are removed)
    fun getTagColor(tag: String, defaultColor: Color = Color.Unspecified): Color {
        return defaultColor
    }

    data class ParsedStyle(
        val tagName: String,
        val start: Int,
        val end: Int
    )

    data class ParseResult(
        val plainText: String,
        val styles: List<ParsedStyle>,
        val originalToTransformed: IntArray,
        val transformedToOriginal: IntArray
    )

    /**
     * Highly robust stack-based custom tag parser.
     * Supports nested, overlapping, and malformed tags without ever crashing.
     */
    fun parseString(text: String): ParseResult {
        val rawLength = text.length
        val tagMatches = ANY_TAG_REGEX.findAll(text).toList()

        val plainTextBuilder = java.lang.StringBuilder()
        val originalToTransformed = IntArray(rawLength + 1)
        val transformedToOriginalList = ArrayList<Int>()

        data class ActiveStyle(val tagName: String, val start: Int)
        val activeStylesStack = mutableListOf<ActiveStyle>()
        val completedStyles = mutableListOf<ParsedStyle>()

        var lastEnd = 0
        var transformedIndex = 0

        for (match in tagMatches) {
            val startIdx = match.range.first
            val endIdx = match.range.last + 1

            // Process plain text before this tag
            if (startIdx > lastEnd) {
                for (k in lastEnd until startIdx) {
                    originalToTransformed[k] = transformedIndex
                    plainTextBuilder.append(text[k])
                    transformedToOriginalList.add(k)
                    transformedIndex++
                }
            }

            // Map all indices inside the tag to the current transformed index (making them invisible)
            for (k in startIdx until endIdx) {
                originalToTransformed[k] = transformedIndex
            }

            val isCloseTag = match.groupValues[1] == "/"
            val tagName = match.groupValues[2].lowercase()

            if (isCloseTag) {
                // Find matching open tag from the end of the stack
                var foundIdx = -1
                for (idx in activeStylesStack.indices.reversed()) {
                    if (activeStylesStack[idx].tagName == tagName) {
                        foundIdx = idx
                        break
                    }
                }
                if (foundIdx != -1) {
                    val openTag = activeStylesStack[foundIdx]
                    if (openTag.start < transformedIndex) {
                        completedStyles.add(ParsedStyle(tagName, openTag.start, transformedIndex))
                    }
                    activeStylesStack.removeAt(foundIdx)
                }
            } else {
                activeStylesStack.add(ActiveStyle(tagName, transformedIndex))
            }

            lastEnd = endIdx
        }

        // Process remaining plain text
        if (lastEnd < rawLength) {
            for (k in lastEnd until rawLength) {
                originalToTransformed[k] = transformedIndex
                plainTextBuilder.append(text[k])
                transformedToOriginalList.add(k)
                transformedIndex++
            }
        }

        // Map the end of string
        originalToTransformed[rawLength] = transformedIndex
        transformedToOriginalList.add(rawLength)

        // Close any dangling unclosed styles at the end
        for (openTag in activeStylesStack) {
            if (openTag.start < transformedIndex) {
                completedStyles.add(ParsedStyle(openTag.tagName, openTag.start, transformedIndex))
            }
        }

        return ParseResult(
            plainText = plainTextBuilder.toString(),
            styles = completedStyles,
            originalToTransformed = originalToTransformed,
            transformedToOriginal = transformedToOriginalList.toIntArray()
        )
    }

    /**
     * Parses a string with tags like `[red]text[/red]` into a Compose AnnotatedString.
     */
    fun parseToAnnotatedString(text: String, defaultColor: Color = Color.Unspecified): AnnotatedString {
        val parseResult = parseString(text)
        return buildAnnotatedString {
            append(parseResult.plainText)
            for (style in parseResult.styles) {
                val spanStyle = when (style.tagName) {
                    "bold" -> SpanStyle(fontWeight = FontWeight.Bold)
                    "hyellow" -> SpanStyle(background = Color(0xFFFFFF00), color = Color(0xFF000000), fontWeight = FontWeight.Bold)
                    "hpink" -> SpanStyle(background = Color(0xFFFF00B3), color = Color(0xFFFFFFFF), fontWeight = FontWeight.Bold)
                    else -> SpanStyle(color = getTagColor(style.tagName, defaultColor))
                }
                addStyle(spanStyle, style.start, style.end)
            }
        }
    }

    data class StyledSpan(
        val text: String,
        val colorHex: String,
        val isBold: Boolean = false,
        val bgHex: String? = null
    )

    /**
     * Parses a string into segments of text with their associated formatting properties for PDF or rich display.
     */
    fun parseToSpans(text: String, defaultColorHex: String = "#000000"): List<StyledSpan> {
        val parseResult = parseString(text)
        
        // Find all transition points of style boundaries
        val transitions = mutableSetOf<Int>()
        transitions.add(0)
        transitions.add(parseResult.plainText.length)
        for (style in parseResult.styles) {
            transitions.add(style.start)
            transitions.add(style.end)
        }
        
        val sortedTransitions = transitions.toList().sorted()
        val result = mutableListOf<StyledSpan>()
        
        for (idx in 0 until sortedTransitions.size - 1) {
            val start = sortedTransitions[idx]
            val end = sortedTransitions[idx + 1]
            if (start >= end) continue
            
            val segmentText = parseResult.plainText.substring(start, end)
            
            var colorHex = defaultColorHex
            var isBold = false
            var bgHex: String? = null
            
            for (style in parseResult.styles) {
                if (start >= style.start && end <= style.end) {
                    when (style.tagName) {
                        "bold" -> isBold = true
                        "hyellow" -> {
                            bgHex = "#FFFF00"
                            colorHex = "#000000"
                        }
                        "hpink" -> {
                            bgHex = "#FF00B3"
                            colorHex = "#FFFFFF"
                        }
                        else -> {
                            colorHex = getTagColorHex(style.tagName)
                        }
                    }
                }
            }
            
            result.add(StyledSpan(segmentText, colorHex, isBold, bgHex))
        }
        
        if (result.isEmpty()) {
            result.add(StyledSpan(parseResult.plainText, defaultColorHex))
        }
        
        return result
    }

    /**
     * VisualTransformation that dynamically parses tags and applies styles,
     * while completely hiding the tags from display and cursor.
     */
    class ColorTagVisualTransformation(private val defaultColor: Color) : VisualTransformation {
        override fun filter(text: AnnotatedString): TransformedText {
            val raw = text.text
            val parseResult = parseString(raw)
            
            val annotatedStringBuilder = AnnotatedString.Builder(parseResult.plainText)
            for (style in parseResult.styles) {
                val spanStyle = when (style.tagName) {
                    "bold" -> SpanStyle(fontWeight = FontWeight.Bold)
                    "hyellow" -> SpanStyle(background = Color(0xFFFFFF00), color = Color(0xFF000000), fontWeight = FontWeight.Bold)
                    "hpink" -> SpanStyle(background = Color(0xFFFF00B3), color = Color(0xFFFFFFFF), fontWeight = FontWeight.Bold)
                    else -> SpanStyle(color = getTagColor(style.tagName, defaultColor))
                }
                annotatedStringBuilder.addStyle(spanStyle, style.start, style.end)
            }
            
            val annotatedString = annotatedStringBuilder.toAnnotatedString()
            
            val offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    val clamped = offset.coerceIn(0, raw.length)
                    return parseResult.originalToTransformed[clamped]
                }

                override fun transformedToOriginal(offset: Int): Int {
                    val clamped = offset.coerceIn(0, parseResult.transformedToOriginal.size - 1)
                    return parseResult.transformedToOriginal[clamped]
                }
            }
            
            return TransformedText(annotatedString, offsetMapping)
        }
    }

    /**
     * Adjusts selection boundaries (start and end) to perfectly wrap around formatting tags,
     * protecting from splitting a tag or wrapping tags improperly.
     */
    fun adjustSelectionForTags(text: String, start: Int, end: Int): IntRange {
        val minIdx = minOf(start, end).coerceIn(0, text.length)
        val maxIdx = maxOf(start, end).coerceIn(0, text.length)
        
        var s = minIdx
        var e = maxIdx
        
        val tags = ANY_TAG_REGEX.findAll(text).toList()
        
        var shifted = true
        while (shifted) {
            shifted = false
            for (tag in tags) {
                val tagStart = tag.range.first
                val tagEnd = tag.range.last + 1
                
                if (s > tagStart && s < tagEnd) {
                    s = tagEnd
                    shifted = true
                    break
                }
                if (s == tagStart && e >= tagEnd) {
                    s = tagEnd
                    shifted = true
                    break
                }
            }
        }
        
        shifted = true
        while (shifted) {
            shifted = false
            for (tag in tags) {
                val tagStart = tag.range.first
                val tagEnd = tag.range.last + 1
                
                if (e > tagStart && e < tagEnd) {
                    e = tagStart
                    shifted = true
                    break
                }
                if (e == tagEnd && s <= tagStart) {
                    e = tagStart
                    shifted = true
                    break
                }
            }
        }
        
        s = s.coerceIn(0, text.length)
        e = e.coerceIn(s, text.length)
        
        return IntRange(s, e)
    }

    /**
     * Removes formatting tags within a selection, or from the whole string if selection start == end.
     */
    fun removeFormatting(text: String, selectionStart: Int, selectionEnd: Int): Pair<String, IntRange> {
        if (selectionStart == selectionEnd) {
            val cleanText = text.replace(ANY_TAG_REGEX, "")
            val pos = selectionStart.coerceIn(0, cleanText.length)
            return Pair(cleanText, IntRange(pos, pos))
        }

        val start = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)

        val tags = ANY_TAG_REGEX.findAll(text).toList()
        val indicesToRemove = mutableSetOf<Int>()

        for (tag in tags) {
            val tagStart = tag.range.first
            val tagEnd = tag.range.last + 1
            
            val hasOverlap = maxOf(tagStart, start) < minOf(tagEnd, end)
            if (hasOverlap) {
                for (i in tagStart until tagEnd) {
                    indicesToRemove.add(i)
                }
            }
        }

        val cleanBuilder = java.lang.StringBuilder()
        val indexMap = IntArray(text.length + 1)
        var currentNewIdx = 0
        for (i in 0..text.length) {
            indexMap[i] = currentNewIdx
            if (i < text.length && i !in indicesToRemove) {
                cleanBuilder.append(text[i])
                currentNewIdx++
            }
        }

        val newText = cleanBuilder.toString()
        val newStart = indexMap[start].coerceIn(0, newText.length)
        val newEnd = indexMap[end].coerceIn(0, newText.length)

        return Pair(newText, IntRange(newStart, newEnd))
    }
}
