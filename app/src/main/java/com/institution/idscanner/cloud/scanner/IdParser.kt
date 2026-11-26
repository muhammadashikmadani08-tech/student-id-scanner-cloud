package com.institution.idscanner.cloud.scanner

/** Extracts a likely Student ID from raw OCR text.
 *  Prefers an explicit pattern like STD-1001 / ID: 1001, otherwise the
 *  longest run of digits, otherwise the first token that looks like an ID. */
object IdParser {
    private val explicit = Regex("""(?i)(?:STD|ID|ROLL)[-:\s]*([A-Z0-9][A-Z0-9\-]{2,})""")
    private val digits = Regex("""\b\d{3,}\b""")

    fun extractId(text: String): String? {
        val cleaned = text.replace('\n', ' ').trim()
        explicit.find(cleaned)?.groupValues?.get(1)?.let { return it.uppercase() }
        digits.findAll(cleaned).maxByOrNull { it.value.length }?.let { return it.value }
        return cleaned.split(" ").firstOrNull { it.length >= 4 && it.any(Char::isDigit) }
            ?.uppercase()
    }

    /** Best-effort name/class hints from OCR text, shown on the Not-Found screen. */
    fun guessName(text: String): String {
        val lines = text.lines().map { it.trim() }.filter { it.length > 2 }
        val named = lines.firstOrNull { it.contains("name", ignoreCase = true) }
        if (named != null) {
            return named.substringAfter(":").trim()
        }
        return lines.getOrNull(0).orEmpty()
    }
}
