package com.example.util

import com.example.model.RepetitionCutSuggestion
import com.example.model.RepetitionDetectionConfig
import com.example.model.WordTimestamp
import java.text.Normalizer
import java.util.Locale

/**
 * Algoritmo determinístico de deteção de repetições de palavras, frases gaguejadas
 * e gaguez com hífen na transcrição do áudio.
 */
object RepetitionDetector {

    private val SHORT_WORDS_TO_IGNORE = setOf("a", "o", "e", "de", "do", "da", "em", "um", "na", "no", "se")

    /**
     * Remove acentos, pontuação e converte para minúsculas para comparação textual fidedigna.
     */
    fun normalize(rawWord: String): String {
        val withoutAccents = Normalizer.normalize(rawWord, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return withoutAccents
            .lowercase(Locale.ROOT)
            .replace("[^a-z0-9-]".toRegex(), "")
            .trim()
    }

    /**
     * Corrige sobreposições temporais nas palavras recebidas do modelo de transcrição.
     * Garante que o fim de uma palavra nunca ultrapassa o início da seguinte.
     */
    fun sanitizeWordTimestamps(words: List<WordTimestamp>): List<WordTimestamp> {
        if (words.size <= 1) return words
        val sanitized = mutableListOf<WordTimestamp>()

        for (i in words.indices) {
            val current = words[i]
            var end = current.end

            if (i < words.size - 1) {
                val nextStart = words[i + 1].start
                if (end > nextStart) {
                    end = nextStart
                }
            }
            if (end <= current.start) {
                end = current.start + 0.05
            }
            sanitized.add(current.copy(end = end))
        }
        return sanitized
    }

    /**
     * Analisa as palavras transcritas e localiza repetições de palavras e frases.
     * Regra: Preserva sempre a ÚLTIMA ocorrência fluente e marca a anterior para corte.
     */
    fun findRepetitions(
        words: List<WordTimestamp>,
        config: RepetitionDetectionConfig
    ): List<RepetitionCutSuggestion> {
        val sanitized = sanitizeWordTimestamps(words)
        if (sanitized.size < 2) return emptyList()

        val suggestions = mutableListOf<RepetitionCutSuggestion>()
        val wordsCount = sanitized.size
        val markedIndices = BooleanArray(wordsCount) // Evita marcar a mesma palavra em múltiplos cortes

        // 1. Deteção de Frases Repetidas (de 4 palavras decrescendo até 2 palavras)
        for (phraseLen in 4 downTo 2) {
            var i = 0
            while (i + 2 * phraseLen <= wordsCount) {
                if ((i until (i + 2 * phraseLen)).any { markedIndices[it] }) {
                    i++
                    continue
                }

                val phrase1 = (0 until phraseLen).map { normalize(sanitized[i + it].word) }
                val phrase2 = (0 until phraseLen).map { normalize(sanitized[i + phraseLen + it].word) }

                val timeGap = sanitized[i + phraseLen].start - sanitized[i + phraseLen - 1].end
                val maxGap = config.sensitivity.maxWordGapSeconds

                if (phrase1 == phrase2 && timeGap <= maxGap) {
                    // Frase 1 é a repetição (a cortar); Frase 2 é a mantida
                    val startCutMs = sanitized[i].startMs
                    val endCutMs = sanitized[i + phraseLen - 1].endMs
                    val repeatedPhraseText = (0 until phraseLen).joinToString(" ") { sanitized[i + it].word }

                    val beforeWords = (maxOf(0, i - 3) until i).map { sanitized[it].word }.joinToString(" ")
                    val afterWords = ((i + phraseLen) until minOf(wordsCount, i + 2 * phraseLen + 2)).map { sanitized[it].word }.joinToString(" ")

                    suggestions.add(
                        RepetitionCutSuggestion(
                            repeatedText = repeatedPhraseText,
                            contextBefore = if (beforeWords.isNotBlank()) "...$beforeWords" else "",
                            contextAfter = if (afterWords.isNotBlank()) "$afterWords..." else "",
                            startMs = startCutMs,
                            endMs = endCutMs,
                            reason = "Frase repetida ($phraseLen palavras)"
                        )
                    )

                    for (k in i until (i + phraseLen)) {
                        markedIndices[k] = true
                    }
                    i += phraseLen
                } else {
                    i++
                }
            }
        }

        // 2. Deteção de Palavras Individuais Repetidas ("eu eu", "que que")
        var j = 0
        while (j < wordsCount - 1) {
            if (markedIndices[j] || markedIndices[j + 1]) {
                j++
                continue
            }

            val w1 = normalize(sanitized[j].word)
            val w2 = normalize(sanitized[j + 1].word)
            val timeGap = sanitized[j + 1].start - sanitized[j].end

            val isShortWord = w1.length <= 2 && SHORT_WORDS_TO_IGNORE.contains(w1)
            val shouldIgnore = config.ignoreShortWords && isShortWord

            // Também verifica palavras partidas com hífen ou gaguez (ex: "ca-casa")
            val isStutterWord = w1.contains("-") && w1.split("-").any { part -> part.length >= 2 && w2.startsWith(part) }

            if (!shouldIgnore && (w1 == w2 || isStutterWord) && timeGap <= config.sensitivity.maxWordGapSeconds) {
                val startCutMs = sanitized[j].startMs
                val endCutMs = sanitized[j].endMs
                val repeatedWordText = sanitized[j].word

                val beforeWords = (maxOf(0, j - 2) until j).map { sanitized[it].word }.joinToString(" ")
                val afterWords = ((j + 1) until minOf(wordsCount, j + 4)).map { sanitized[it].word }.joinToString(" ")

                suggestions.add(
                    RepetitionCutSuggestion(
                        repeatedText = repeatedWordText,
                        contextBefore = if (beforeWords.isNotBlank()) "...$beforeWords" else "",
                        contextAfter = if (afterWords.isNotBlank()) "$afterWords..." else "",
                        startMs = startCutMs,
                        endMs = endCutMs,
                        reason = if (isStutterWord) "Gaguez / Hesitação" else "Palavra repetida"
                    )
                )

                markedIndices[j] = true
            }
            j++
        }

        return suggestions.sortedBy { it.startMs }
    }
}
