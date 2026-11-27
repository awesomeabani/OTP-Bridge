package com.example.otpbridge

object OtpExtractor {
    private val digitWords = mapOf(
        "zero" to "0", "oh" to "0", "one" to "1", "two" to "2", "three" to "3",
        "four" to "4", "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9"
    )
    private const val WORD = "zero|oh|one|two|three|four|five|six|seven|eight|nine"
    private val digitRegex = Regex("(?<!\\d)(\\d{6})(?!\\d)")
    private val wordSequence = Regex("(?i)(?<![a-z])($WORD)(?:[\\s-]+($WORD)){5}(?![a-z])")
    private val singleWord = Regex("(?i)$WORD")

    /**
     * Returns every distinct six-digit candidate in the SMS. Accepts "123456" and
     * spelled-out digits such as "one-two-three-four-five-six".
     */
    fun extract(message: String): Set<String> {
        val found = linkedSetOf<String>()
        digitRegex.findAll(message).forEach { found.add(it.groupValues[1]) }
        wordSequence.findAll(message).forEach { match ->
            val digits = singleWord.findAll(match.value)
                .mapNotNull { digitWords[it.value.lowercase()] }
                .joinToString("")
            if (digits.length == 6) found.add(digits)
        }
        return found
    }
}
