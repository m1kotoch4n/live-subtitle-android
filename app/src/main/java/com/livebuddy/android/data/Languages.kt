package com.livebuddy.android.data

/**
 * Supported target languages (mirrors `settings.py:LANGUAGES`).
 *
 * `code` is the ISO-639-1 code sent to the Gemini Live API
 * `translationConfig.targetLanguageCode` field.
 */
data class TranslationLanguage(val code: String, val name: String)

object Languages {
    val ALL: List<TranslationLanguage> = listOf(
        TranslationLanguage("en", "English"),
        TranslationLanguage("es", "Spanish"),
        TranslationLanguage("fr", "French"),
        TranslationLanguage("de", "German"),
        TranslationLanguage("it", "Italian"),
        TranslationLanguage("ja", "Japanese"),
        TranslationLanguage("ko", "Korean"),
        TranslationLanguage("zh", "Chinese"),
        TranslationLanguage("vi", "Vietnamese"),
        TranslationLanguage("pt", "Portuguese"),
        TranslationLanguage("ru", "Russian"),
        TranslationLanguage("hi", "Hindi"),
        TranslationLanguage("ar", "Arabic"),
        TranslationLanguage("th", "Thai"),
        TranslationLanguage("id", "Indonesian"),
        TranslationLanguage("tr", "Turkish"),
    )

    fun nameFor(code: String): String =
        ALL.firstOrNull { it.code == code }?.name ?: code.uppercase()
}
