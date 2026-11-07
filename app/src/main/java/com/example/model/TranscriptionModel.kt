package com.example.model

/**
 * Marcação temporal individual de cada palavra devolvida pelo Whisper na Groq API.
 */
data class WordTimestamp(
    val word: String,
    val start: Double, // segundos
    val end: Double    // segundos
) {
    val startMs: Long get() = (start * 1000.0).toLong()
    val endMs: Long get() = (end * 1000.0).toLong()
}

/**
 * Resposta completa da API da Groq para o modelo whisper-large-v3-turbo.
 */
data class GroqTranscriptionResponse(
    val task: String = "transcribe",
    val language: String = "Portuguese",
    val duration: Double = 0.0,
    val text: String = "",
    val words: List<WordTimestamp> = emptyList()
)

/**
 * Sensibilidade da deteção de repetições.
 */
enum class RepetitionSensitivity(val labelPt: String, val maxWordGapSeconds: Double) {
    LOW("Baixa (Apenas repetições imediatas)", 0.6),
    MEDIUM("Média (Padrão balanceado)", 1.2),
    HIGH("Alta (Frases espaçadas e gaguez)", 2.0)
}

/**
 * Configuração da deteção de repetições e gaguez.
 */
data class RepetitionDetectionConfig(
    val ignoreShortWords: Boolean = true,
    val sensitivity: RepetitionSensitivity = RepetitionSensitivity.MEDIUM,
    val crossfadeMs: Long = 20L // Crossfade de 20ms para evitar estalos
)

/**
 * Sugestão de corte de palavra ou frase repetida para revisão do utilizador.
 */
data class RepetitionCutSuggestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val repeatedText: String,
    val contextBefore: String,
    val contextAfter: String,
    val startMs: Long,
    val endMs: Long,
    val isSelected: Boolean = true,
    val reason: String = "Repetição eliminada"
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

/**
 * Etapas do Assistente de Corte Automático.
 */
enum class AutoCutStep(val labelPt: String) {
    STEP_A_SILENCE("Etapa A: Remover Silêncios"),
    STEP_B_REPETITIONS("Etapa B: Repetições & Gaguez")
}
