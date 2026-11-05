package com.example.domain

object TextChunkingEngine {

    private val SENTENCE_DELIMITERS = listOf(
        ".\n", "!\n", "؟\n", "?\n",
        ". ", "! ", "؟ ", "? ",
        "؛ ", "; ", "، ", ", ",
        "\n", ".", "!", "؟", "?", "؛", "،"
    )

    /**
     * Splits arbitrary long text into coherent semantic chunks.
     * Guaranteed:
     * - Never cuts words mid-word.
     * - Respects paragraph and sentence boundaries.
     * - Each chunk stays near targetChunkSize (default 1200 characters).
     * - Verbatim text preservation (no added or omitted words).
     */
    fun splitText(text: String, targetChunkSize: Int = 1200): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (trimmed.length <= targetChunkSize) return listOf(trimmed)

        val paragraphs = trimmed.split(Regex("(\r?\n){2,}"))
        val result = mutableListOf<String>()
        var currentChunk = StringBuilder()

        for (para in paragraphs) {
            val cleanPara = para.trim()
            if (cleanPara.isEmpty()) continue

            if (currentChunk.isNotEmpty() && currentChunk.length + cleanPara.length + 2 > targetChunkSize) {
                result.add(currentChunk.toString().trim())
                currentChunk = StringBuilder()
            }

            if (cleanPara.length <= targetChunkSize) {
                if (currentChunk.isNotEmpty()) currentChunk.append("\n\n")
                currentChunk.append(cleanPara)
            } else {
                // Large paragraph: split by sentences
                val sentences = splitIntoSentences(cleanPara, targetChunkSize)
                for (sentence in sentences) {
                    if (currentChunk.isNotEmpty() && currentChunk.length + sentence.length + 1 > targetChunkSize) {
                        result.add(currentChunk.toString().trim())
                        currentChunk = StringBuilder()
                    }
                    if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                    currentChunk.append(sentence)
                }
            }
        }

        if (currentChunk.isNotEmpty()) {
            val last = currentChunk.toString().trim()
            if (last.isNotEmpty()) {
                result.add(last)
            }
        }

        return result
    }

    private fun splitIntoSentences(paragraph: String, targetChunkSize: Int): List<String> {
        val sentences = mutableListOf<String>()
        val regex = Regex("(?<=[.!?؟،؛\\n])\\s+")
        val rawTokens = paragraph.split(regex)

        var current = StringBuilder()
        for (token in rawTokens) {
            val trimmedToken = token.trim()
            if (trimmedToken.isEmpty()) continue

            if (trimmedToken.length > targetChunkSize) {
                // If a single sentence without punctuation exceeds chunk size, split by words
                if (current.isNotEmpty()) {
                    sentences.add(current.toString().trim())
                    current = StringBuilder()
                }
                sentences.addAll(splitByWords(trimmedToken, targetChunkSize))
            } else {
                if (current.isNotEmpty() && current.length + trimmedToken.length + 1 > targetChunkSize) {
                    sentences.add(current.toString().trim())
                    current = StringBuilder()
                }
                if (current.isNotEmpty()) current.append(" ")
                current.append(trimmedToken)
            }
        }

        if (current.isNotEmpty()) {
            sentences.add(current.toString().trim())
        }
        return sentences
    }

    private fun splitByWords(longSentence: String, targetChunkSize: Int): List<String> {
        val words = longSentence.split(Regex("\\s+"))
        val chunks = mutableListOf<String>()
        var current = StringBuilder()

        for (word in words) {
            if (current.isNotEmpty() && current.length + word.length + 1 > targetChunkSize) {
                chunks.add(current.toString().trim())
                current = StringBuilder()
            }
            if (current.isNotEmpty()) current.append(" ")
            current.append(word)
        }

        if (current.isNotEmpty()) {
            chunks.add(current.toString().trim())
        }
        return chunks
    }

    /**
     * Estimates word count, character count, and audio duration.
     * Average Arabic speaking rate is ~130 words per minute (about 700 chars/min).
     */
    fun estimateStats(text: String, speed: Float = 1.0f): Triple<Int, Int, Long> {
        val charCount = text.length
        val wordCount = if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
        val effectiveSpeed = if (speed <= 0f) 1.0f else speed
        // duration in milliseconds: (wordCount / 130 wpm) * 60,000 / speed
        val durationMs = if (wordCount == 0) 0L else ((wordCount.toDouble() / 130.0) * 60000.0 / effectiveSpeed).toLong()
        return Triple(charCount, wordCount, durationMs)
    }
}
