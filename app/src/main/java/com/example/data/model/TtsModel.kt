package com.example.data.model

data class TtsModel(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val descriptionAr: String,
    val isDefault: Boolean = false
)

object TtsModelCatalog {
    val models: List<TtsModel> = listOf(
        TtsModel(
            id = "gemini-3.8-flash-tts",
            nameAr = "Gemini 3.8 Flash TTS (موصى به)",
            nameEn = "Gemini 3.8 Flash TTS",
            descriptionAr = "أعلى جودة صوتية ودقة متميزة في مخارج الحروف واللهجات والنصوص الطويلة.",
            isDefault = true
        ),
        TtsModel(
            id = "gemini-3.8-flash-lite-tts",
            nameAr = "Gemini 3.8 Flash Lite TTS (سريع)",
            nameEn = "Gemini 3.8 Flash Lite TTS",
            descriptionAr = "فائق السرعة وأقل استهلاكاً للموارد والحصص مع جودة ممتازة.",
            isDefault = false
        ),
        TtsModel(
            id = "gemini-2.5-pro-preview-tts",
            nameAr = "Gemini 2.5 Pro TTS (احترافي)",
            nameEn = "Gemini 2.5 Pro TTS",
            descriptionAr = "معالجة متقدمة للسرد القصصي والأدبي المتعمق.",
            isDefault = false
        )
    )
}

sealed interface GenerationState {
    object Idle : GenerationState
    data class Processing(
        val currentChunk: Int,
        val totalChunks: Int,
        val percent: Float,
        val statusMessage: String
    ) : GenerationState
    object Paused : GenerationState
    data class Completed(
        val audioFilePath: String,
        val durationMs: Long,
        val totalBytes: Long,
        val chunkCount: Int
    ) : GenerationState
    data class Error(val message: String, val canRetry: Boolean = true) : GenerationState
}
