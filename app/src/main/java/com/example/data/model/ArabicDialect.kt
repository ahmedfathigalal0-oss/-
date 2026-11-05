package com.example.data.model

data class ArabicDialect(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val code: String,
    val flag: String,
    val performancePrompt: String
)

object DialectCatalog {
    val dialects: List<ArabicDialect> = listOf(
        ArabicDialect(
            id = "auto",
            nameAr = "تلقائي من النص",
            nameEn = "Auto Detect",
            code = "ar-001",
            flag = "🌐",
            performancePrompt = ""
        ),
        ArabicDialect(
            id = "fusha",
            nameAr = "العربية الفصحى",
            nameEn = "Modern Standard Arabic",
            code = "ar-XA",
            flag = "📖",
            performancePrompt = "Speak in clear, eloquent Modern Standard Arabic (Fusha) with accurate grammar and natural pronunciation."
        ),
        ArabicDialect(
            id = "egyptian",
            nameAr = "اللهجة المصرية",
            nameEn = "Egyptian Arabic",
            code = "ar-EG",
            flag = "🇪🇬",
            performancePrompt = "Speak in a natural, authentic Egyptian Arabic accent (Cairene dialect) with native intonation."
        ),
        ArabicDialect(
            id = "saudi",
            nameAr = "اللهجة السعودية",
            nameEn = "Saudi Arabic",
            code = "ar-SA",
            flag = "🇸🇦",
            performancePrompt = "Speak in a natural, authentic Saudi Arabic accent with native Gulf cadence."
        ),
        ArabicDialect(
            id = "emirati",
            nameAr = "الإماراتية والخليجية",
            nameEn = "Gulf Arabic",
            code = "ar-AE",
            flag = "🇦🇪",
            performancePrompt = "Speak in a natural, warm Emirati and Khaleeji Gulf Arabic accent."
        ),
        ArabicDialect(
            id = "levantine",
            nameAr = "اللهجة الشامية",
            nameEn = "Levantine Arabic",
            code = "ar-SY",
            flag = "🇱🇧",
            performancePrompt = "Speak in a natural, expressive Levantine Arabic accent (Syrian/Lebanese dialect)."
        ),
        ArabicDialect(
            id = "iraqi",
            nameAr = "اللهجة العراقية",
            nameEn = "Iraqi Arabic",
            code = "ar-IQ",
            flag = "🇮🇶",
            performancePrompt = "Speak in a natural, authentic Iraqi Mesopotamian Arabic accent."
        ),
        ArabicDialect(
            id = "moroccan",
            nameAr = "المغربية (الدارجة)",
            nameEn = "Moroccan Darija",
            code = "ar-MA",
            flag = "🇲🇦",
            performancePrompt = "Speak in a natural Moroccan Darija Arabic accent with authentic rhythm."
        ),
        ArabicDialect(
            id = "maghrebi",
            nameAr = "الجزائرية والتونسية",
            nameEn = "Maghrebi Arabic",
            code = "ar-DZ",
            flag = "🇩🇿",
            performancePrompt = "Speak in a natural Maghrebi (Algerian/Tunisian) Arabic accent."
        ),
        ArabicDialect(
            id = "sudanese_yemeni",
            nameAr = "السودانية واليمنية",
            nameEn = "Sudanese & Yemeni",
            code = "ar-SD",
            flag = "🇸🇩",
            performancePrompt = "Speak in a warm, natural Sudanese and Yemeni Arabic accent."
        ),
        ArabicDialect(
            id = "english",
            nameAr = "الإنجليزية",
            nameEn = "English",
            code = "en-US",
            flag = "🌐",
            performancePrompt = "Speak in natural, clear English pronunciation."
        )
    )
}
