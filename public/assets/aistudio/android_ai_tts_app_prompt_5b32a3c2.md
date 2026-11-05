# برومت شامل لبناء تطبيق أندرويد لتحويل النصوص إلى كلام (AI Agent TTS) باستخدام أصوات Google AI Studio

يحتوي هذا المستند على **برومت هندسي متكامل (Master Prompt)** باللغتين العربية والإنجليزية، مصمم خصيصاً لإعطائه لوكلاء البرمجة بالذكاء الاصطناعي (مثل Android Studio Gemini Agent أو Cursor أو Windsurf أو Claude Code) لبناء تطبيق أندرويد احترافي يحول النصوص إلى كلام مسموع بدون أي قيود على عدد الحروف أو المدة الزمنية، مع دعم كامل لجميع أصوات [Google AI Studio Speech Generation](https://aistudio.google.com/docs/speech-generation) وجميع اللهجات العربية.

## 1. البرومت الشامل باللغة العربية (جاهز للنسخ واللصق للـ AI Coding Agent)

انسخ النص التالي بالكامل وقدمه لوكيل البرمجة الذكي لبناء التطبيق:

> **الدور والمهمة الأساسية:**
> تصرف كمهندس برمجيات أندرويد خبير (Senior Android Architect) ومهندس ذكاء اصطناعي. مهمتك هي كتابة الكود الكامل لتطبيق أندرويد أصلي (Native Android App) باستخدام لغة **Kotlin** وواجهة **Jetpack Compose (Material Design 3)** مع بنية **MVVM + Clean Architecture**.
>
> **فكرة التطبيق:**
> تطبيق أندرويد يعتمد على وكيل ذكاء اصطناعي (AI Agent) لتحويل النصوص المكتوبة إلى كلام مسموع (Text-to-Speech) باستخدام واجهة برمجة تطبيقات [Gemini API Speech Generation](https://ai.google.dev/gemini-api/docs/speech-generation) ونماذج الصوت الرسمية (`gemini-3.8-flash-tts` و`gemini-2.5-pro-preview-tts`). يضع المستخدم النص فقط مهما كان حجمه، ويتولى الـ Agent داخلياً تقسيم النص ومعالجته وتوليد الصوت ودمجه وتشغيله وإتاحة تحميله على الهاتف بصيغة WAV أو MP3.
>
> **المتطلبات الأساسية والقيود الصارمة:**
> 1. **إدخال نص غير محدود إطلاقاً (Unlimited Characters & Words):**
>    - لا تضع أي حد أقصى (`maxLength`) لحقل إدخال النص في واجهة المستخدم، مع دعم لصق الكتب والمقالات الطويلة أو استيراد ملفات نصية (`.txt`).
>    - يقوم **AI Chunking Agent** داخلي بتقسيم النصوص الضخمة تلقائياً إلى فقرات وجمل مترابطة دلالياً (Semantic Sentence Chunking) عند علامات الترقيم ونهايات الجمل، بحيث لا ينقطع السياق اللغوي أو النبرة الصوتية.
> 2. **مدة صوتية غير محدودة (Unlimited Audio Duration):**
>    - لا تضع أي قيود زمنية على طول الملف الصوتي الناتج (سواء كان ثواني أو عدة ساعات).
>    - يعالج الـ Agent القطع النصية بالتوازي أو التسلسل عبر طلبات `generateContent` أو البث المستمر `streamGenerateContent` (بصيغة PCM 16-bit, 24kHz, Mono)، ثم يقوم بدمج جميع المقاطع الصوتية برمجياً في ملف صوتي واحد متصل ومتجانس مع ضبط ترويسة ملف الـ WAV (`RIFF Header`) ديناميكياً ليتطابق مع الحجم الكلي الفعلي.
> 3. **استخدام جميع أصوات Google AI Studio الرسمية (30 Prebuilt Studio Voices):**
>    - يجب تضمين القائمة الكاملة للأصوات الـ 30 المتوفرة في [توثيق Google Cloud Gemini TTS](https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/text-to-speech/overview) مع عرض اسم الصوت وطابعه الصوتي للمستخدم، وتمرير الاسم البرمجي الدقيق في حقل `speechConfig.voiceConfig.prebuiltVoiceConfig.voiceName`:
>      - `Zephyr` (Bright - مشرق)، `Puck` (Upbeat - حيوي)، `Charon` (Informative - إخباري/معلوماتي)، `Kore` (Firm - حازم)، `Fenrir` (Excitable - حماسي)، `Leda` (Youthful - شبابي)، `Orus` (Firm - رصين)، `Aoede` (Breezy - هادئ/منعش)، `Callirrhoe` (Easy-going - عفوي)، `Autonoe` (Bright - واضح)، `Enceladus` (Breathy - دافئ/هامس)، `Iapetus` (Clear - نقي)، `Umbriel` (Easy-going - سلس)، `Algieba` (Smooth - ناعم)، `Despina` (Smooth - انسيابي)، `Erinome` (Clear - جلي)، `Algenib` (Gravelly - رخيم/عميق)، `Rasalgethi` (Informative - وثائقي)، `Laomedeia` (Upbeat - متفائل)، `Achernar` (Soft - لطيف)، `Alnilam` (Firm - متزن)، `Schedar` (Even - متناسق)، `Gacrux` (Mature - ناضج)، `Pulcherrima` (Forward - مباشر)، `Achird` (Friendly - ودود)، `Zubenelgenubi` (Casual - غير رسمي)، `Vindemiatrix` (Gentle - هادئ)، `Sadachbia` (Lively - مفعم بالحيوية)، `Sadaltager` (Knowledgeable - خبير/معرفي)، `Sulafat` (Warm - دافئ).
>    - إضافة دعم استعراض المكتبة الموسعة (`GET /v1beta/voices`) من [واجهة Gemini Voices API](https://aistudio.google.com/api-reference/voices).
> 4. **دعم كامل للغة العربية بجميع لهجاتها (Full Arabic & Multi-Dialect Support):**
>    - دعم اتجاه القراءة والكتابة من اليمين إلى اليسار (RTL) بالكامل.
>    - توفير قائمة اختيار للهجة العربية المطلوبة، حيث يقوم الـ Agent بتمرير رمز اللغة (`language_code` وفق معيار BCP-47) وتوجيه النبرة الصوتية (`speech_metadata.style` أو تعليمات الأداء) للنطق الدقيق باللهجة المختارة:
>      - العربية الفصحى الحديثة (`ar-XA` / MSA - مع ضبط التشكيل التلقائي عند الحاجة).
>      - اللهجة المصرية (`ar-EG` - Egyptian Arabic).
>      - اللهجة السعودية (`ar-SA` - Najdi / Hijazi / Khaleeji).
>      - اللهجة الإماراتية والخليجية (`ar-AE` / `ar-KW` / `ar-QA` / `ar-BH` / `ar-OM`).
>      - اللهجة الشامية: السورية، اللبنانية، الأردنية، الفلسطينية (`ar-SY` / `ar-LB` / `ar-JO` / `ar-PS`).
>      - اللهجة العراقية (`ar-IQ` - Iraqi Arabic).
>      - اللهجات المغاربية: المغربية (الدارجة)، الجزائرية، التونسية، الليبية (`ar-MA` / `ar-DZ` / `ar-TN` / `ar-LY`).
>      - اللهجة السودانية واليمنية (`ar-SD` / `ar-YE`).
> 5. **التحكم الدقيق في سرعة القراءة (Reading Speed Control):**
>    - توفير شريط تمرير (Slider) وأزرار سريعة للتحكم في سرعة القراءة من `0.25x` إلى `3.0x` (بخطوات `0.05x`).
>    - يعمل التحكم بالسرعة على مستويين:
>      1. **أثناء التوليد (AI Pace Steering):** توجيه نموذج Gemini TTS عبر وصف السرعة والإيقاع في `speech_metadata` (مثل: بطيء، معتدل، سريع).
>      2. **أثناء التشغيل والتحميل (DSP Time-Stretching):** استخدام مشغل `androidx.media3.exoplayer.ExoPlayer` (`PlaybackParameters`) لتغيير السرعة في الوقت الفعلي دون تشويه طبقة الصوت (Pitch-Preserving Speed Control)، مع خيار تطبيق السرعة المختارة عند تصدير وتحميل الملف الصوتي النهائي.
> 6. **التحميل الكامل على الهاتف والعمل في الخلفية (Full Local Download & Background Service):**
>    - تنفيذ عمليات التوليد الطويلة داخل `WorkManager` مع `ForegroundService` وإشعار تقدم حي (Progress Notification) يوضح النسبة المئوية للقطع المنجزة، حتى لا يتوقف التحويل عند إغلاق الشاشة أو الخروج من التطبيق.
>    - زر **"تحميل الصوت (Download Audio)"** يحفظ الملف الصوتي الكامل مباشرة في مجلد التنزيلات أو الصوتيات العام على الهاتف (`Environment.DIRECTORY_DOWNLOADS` أو `MediaStore.Audio.Media`) بصيغة `WAV` (جودة أصلية 24kHz) أو `MP3/AAC`، مع إظهار إشعار بنجاح التحميل وإمكانية مشاركة الملف.
>
> **المطلوب في المخرجات البرمجية:**
> 1. ملف `build.gradle.kts` شاملاً جميع المكتبات اللازمة (`OkHttp`/`Retrofit`، `Kotlin Coroutines & Flow`، `Media3 ExoPlayer`، `WorkManager`، `Jetpack Compose Material3`).
> 2. كود `TextChunkingAgent.kt` المسؤول عن تقسيم النصوص غير المحدودة ذكياً وتجهيز تعليمات اللهجة العربية والسرعة.
> 3. كود `GeminiTtsRepository.kt` للاتصال بـ Gemini API ودمج مقاطع الـ PCM/WAV بدون حدود زمنية.
> 4. كود `AudioExportManager.kt` لحفظ الملف الصوتي النهائي على ذاكرة الهاتف عبر `MediaStore`.
> 5. كود `TtsViewModel.kt` وواجهة المستخدم الكاملة `TtsMainScreen.kt` بتصميم عصري يدعم العربية (RTL) والوضع الليلي.

---

## 2. جدول الأصوات الـ 30 الرسمية في Google AI Studio

يوضح الجدول التالي جميع الأصوات المدمجة في [Google AI Studio Speech Generation](https://aistudio.google.com/docs/speech-generation) التي يجب إدراجها في قائمة اختيار الأصوات داخل التطبيق:

| الاسم البرمجي (`voiceName`) | الطابع الصوتي (Official Characteristic) | الوصف بالعربية للاستخدام في واجهة التطبيق | الاستخدام المقترح |
| :--- | :--- | :--- | :--- |
| `Zephyr` | Bright | مشرق وواضح | السرد الإيجابي والمحتوى العام |
| `Puck` | Upbeat | حيوي ونشيط | الإعلانات والمحتوى التفاعلي |
| `Charon` | Informative | إخباري ومعلوماتي | النشرات الإخبارية والمقالات العلمية |
| `Kore` | Firm | حازم ومتزن | الكتب الصوتية والعروض الرسمية |
| `Fenrir` | Excitable | حماسي ومتفاعل | القصص المشوقة والمحتوى الترفيهي |
| `Leda` | Youthful | شبابي وعفوي | المحتوى التعليمي والبودكاست الشبابي |
| `Orus` | Firm | رصين وواثق | الوثائقيات والتقارير الاحترافية |
| `Aoede` | Breezy | هادئ ومنعش | القراءة المريحة والتأمل والسرد الأدبي |
| `Callirrhoe` | Easy-going | سلس ومريح | المحادثات اليومية والقصص الخفيفة |
| `Autonoe` | Bright | ساطع وبارز | الشروحات التعليمية والتوجيهات |
| `Enceladus` | Breathy | دافئ وهامس | السرد الدرامي والخواطر |
| `Iapetus` | Clear | نقي وواضح المخارج | القراءة الفصحى والنصوص الطويلة |
| `Umbriel` | Easy-going | تلقائي ومرن | المحتوى الحواري والاجتماعي |
| `Algieba` | Smooth | ناعم وانسيابي | الروايات والكتب الصوتية الطويلة |
| `Despina` | Smooth | هادئ ومتناغم | المقالات الثقافية والسرد الهادئ |
| `Erinome` | Clear | جلي ودقيق | المحتوى التقني والتعليمي |
| `Algenib` | Gravelly | عميق ورخيم | السرد السينمائي والتعليق الصوتي الفخم |
| `Rasalgethi` | Informative | معرفي ووثائقي | التاريخ والعلوم والأبحاث |
| `Laomedeia` | Upbeat | مشرق ومتفائل | المحتوى التحفيزي والقصص |
| `Achernar` | Soft | لطيف وهادئ | قصص ما قبل النوم والمحتوى المريح |
| `Alnilam` | Firm | ثابت وقوي | البيانات الرسمية والخطابات |
| `Schedar` | Even | متوازن ومعتدل | قراءة الكتب والمقالات الطويلة |
| `Gacrux` | Mature | ناضج ووقور | السرد التاريخي والأدبي الكلاسيكي |
| `Pulcherrima` | Forward | مباشر وحاضر | البودكاست والتقديم الإذاعي |
| `Achird` | Friendly | ودود وقريب | المساعدات الصوتية والشروحات المبسطة |
| `Zubenelgenubi` | Casual | عفوي وغير رسمي | اللهجات العامية والمحتوى اليومي |
| `Vindemiatrix` | Gentle | رقيق ولطيف | الشعر والنثر الفني |
| `Sadachbia` | Lively | مفعم بالحيوية | القصص التفاعلية والحوارات |
| `Sadaltager` | Knowledgeable | خبير ومثقف | المحاضرات والملخصات المعرفية |
| `Sulafat` | Warm | دافئ وحميمي | السرد القصصي والروايات الإنسانية |

---

## 3. جدول اللهجات العربية المدعومة وتوجيه الـ Agent

لضمان نطق النص باللهجة العربية التي يختارها المستخدم، يدمج الـ Agent الداخلي رمز اللغة مع وصف الأداء الصوتي الموجه لنموذج [Gemini TTS](https://docs.cloud.google.com/text-to-speech/docs/gemini-tts):

| اللهجة العربية | رمز اللغة (BCP-47) | توجيه الأداء الصوتي للـ Agent (`Style & Accent Prompt`) |
| :--- | :--- | :--- |
| العربية الفصحى (MSA) | `ar-XA` | `Speak in clear, eloquent Modern Standard Arabic (Fusha) with accurate grammar and vowel pronunciation.` |
| المصرية (Egyptian) | `ar-EG` | `Speak in a natural, authentic Egyptian Arabic accent (Cairene dialect) with native intonation.` |
| السعودية (Saudi) | `ar-SA` | `Speak in a natural, authentic Saudi Arabic accent (Najdi/Hijazi dialect) with native Gulf cadence.` |
| الإماراتية والخليجية (Gulf) | `ar-AE` | `Speak in a natural, warm Emirati and Khaleeji Gulf Arabic accent.` |
| الشامية - سوري/لبناني (Levantine North) | `ar-LB` / `ar-SY` | `Speak in a natural, expressive Levantine Arabic accent (Syrian/Lebanese dialect).` |
| الشامية - أردني/فلسطيني (Levantine South) | `ar-JO` / `ar-PS` | `Speak in a natural, authentic Jordanian/Palestinian Levantine Arabic accent.` |
| العراقية (Iraqi) | `ar-IQ` | `Speak in a natural, authentic Iraqi Mesopotamian Arabic accent.` |
| المغربية - الدارجة (Moroccan) | `ar-MA` | `Speak in a natural Moroccan Darija Arabic accent with authentic rhythm.` |
| الجزائرية والتونسية (Maghrebi) | `ar-DZ` / `ar-TN` | `Speak in a natural Maghrebi (Algerian/Tunisian) Arabic accent.` |
| السودانية (Sudanese) | `ar-SD` | `Speak in a warm, natural Sudanese Arabic accent.` |
| اليمنية (Yemeni) | `ar-YE` | `Speak in an authentic, clear Yemeni Arabic accent.` |

---

## 4. البرومت التقني باللغة الإنجليزية (English Technical Prompt for AI Coding Agents)

إذا كنت تستخدم أداة برمجة بالذكاء الاصطناعي تفضل التعليمات باللغة الإنجليزية لإنتاج كود خالٍ من الأخطاء، انسخ البرومت التالي:

```text
Act as a Principal Android Engineer & AI Systems Architect. Build a complete, production-ready Native Android Application in Kotlin using Jetpack Compose (Material 3) and MVVM Clean Architecture that converts unlimited text into speech using an autonomous AI Agent powered by Google AI Studio's Gemini TTS API.

### 1. Core Functional Requirements
1. Zero Character or Word Limits (Unlimited Text Input):
   - The UI text input field must have NO character or word limit (`maxLines = Int.MAX_VALUE`, scrollable container, plus support for importing `.txt` files).
   - Implement an autonomous `TextChunkingAgent` that accepts arbitrarily large texts (from a single sentence to an entire book) and splits them into semantically coherent chunks (~1,500–2,500 characters per chunk) exclusively at sentence/paragraph boundaries (`\n`, `.`, `؟`, `!`, `،`) so speech prosody is never broken mid-sentence.

2. Zero Audio Duration Limits (Unlimited Audio Synthesis & Merging):
   - There must be NO time limit on the generated audio output.
   - The `AudioSynthesisOrchestrator` processes all text chunks sequentially or via a bounded coroutine channel (`Dispatchers.IO`), collecting raw 16-bit PCM audio at 24kHz Mono returned by Gemini API (`audio/wav` or `audio/l16`).
   - Implement a zero-memory-overflow `WavStreamMerger` that writes incoming PCM audio bytes directly to a temporary file on disk and dynamically calculates and writes the final 44-byte RIFF/WAV header upon completion, allowing multi-hour audio files without `OutOfMemoryError`.

3. Official Google AI Studio TTS Voices (All 30 Prebuilt Studio Voices):
   - Integrate the Gemini API `generateContent` endpoint with `responseModalities = ["AUDIO"]` and `speechConfig.voiceConfig.prebuiltVoiceConfig.voiceName`.
   - Populate the Voice Selector UI with all 30 official Google AI Studio prebuilt voices and their tonal tags:
     1. `Zephyr` (Bright), 2. `Puck` (Upbeat), 3. `Charon` (Informative), 4. `Kore` (Firm), 5. `Fenrir` (Excitable),
     6. `Leda` (Youthful), 7. `Orus` (Firm), 8. `Aoede` (Breezy), 9. `Callirrhoe` (Easy-going), 10. `Autonoe` (Bright),
     11. `Enceladus` (Breathy), 12. `Iapetus` (Clear), 13. `Umbriel` (Easy-going), 14. `Algieba` (Smooth), 15. `Despina` (Smooth),
     16. `Erinome` (Clear), 17. `Algenib` (Gravelly), 18. `Rasalgethi` (Informative), 19. `Laomedeia` (Upbeat), 20. `Achernar` (Soft),
     21. `Alnilam` (Firm), 22. `Schedar` (Even), 23. `Gacrux` (Mature), 24. `Pulcherrima` (Forward), 25. `Achird` (Friendly),
     26. `Zubenelgenubi` (Casual), 27. `Vindemiatrix` (Gentle), 28. `Sadachbia` (Lively), 29. `Sadaltager` (Knowledgeable), 30. `Sulafat` (Warm).
   - Also include a dynamic loader for Google AI Studio's Extended Voice Library (`GET https://generativelanguage.googleapis.com/v1beta/voices`).

4. Complete Arabic Language & All Regional Dialects Support:
   - Full RTL UI layout support.
   - Provide an Arabic Dialect Selector allowing the user to pick:
     - Auto-Detect from Text
     - Modern Standard Arabic - الفصحى (`ar-XA`)
     - Egyptian - مصري (`ar-EG`)
     - Saudi - سعودي نجدي/حجازي (`ar-SA`)
     - Emirati / Gulf - إماراتي/خليجي (`ar-AE`, `ar-KW`, `ar-QA`)
     - Levantine - شامي سوري/لبناني/أردني/فلسطيني (`ar-SY`, `ar-LB`, `ar-JO`, `ar-PS`)
     - Iraqi - عراقي (`ar-IQ`)
     - Maghrebi - مغربي/جزائري/تونسي/ليبي (`ar-MA`, `ar-DZ`, `ar-TN`, `ar-LY`)
     - Sudanese & Yemeni - سوداني/يمني (`ar-SD`, `ar-YE`)
   - The AI Agent automatically injects the selected dialect instruction and pace directive into the TTS request metadata/prompt steering while ensuring only the user's verbatim text is spoken aloud.

5. Reading Speed Control (0.25x to 3.0x):
   - Provide a fine-grained speed slider (`0.25f` to `3.0f`) and preset chips (`0.5x`, `0.75x`, `1.0x`, `1.25x`, `1.5x`, `2.0x`).
   - Apply speed control both during AI synthesis (via pace steering in `speech_metadata`) and instantly during playback via `androidx.media3.exoplayer.ExoPlayer` (`PlaybackParameters(speed, 1.0f)`) so pitch remains natural.
   - Support exporting the downloaded file with the selected speed applied.

6. Full Device Audio Download & Background Processing:
   - Run long-form synthesis inside a `CoroutineWorker` backed by a `ForegroundInfo` notification showing real-time chunk progress (`Chunk X of Y - Z%`).
   - Provide a "Download to Phone" button that saves the complete merged audio file into the public `Download/` or `Music/` directory using Android `MediaStore` (`Scoped Storage` compliant for Android 10–15+) and offers a system Share sheet.

### 2. Required Deliverables
Generate the complete, compilable source code for:
1. `AndroidManifest.xml` & `build.gradle.kts`
2. `domain/model/VoiceProfile.kt` & `ArabicDialect.kt` (containing all 30 voices and all Arabic dialects)
3. `domain/agent/TextChunkingAgent.kt` (unlimited text smart splitter & dialect prompt builder)
4. `data/remote/GeminiTtsApiService.kt` & `WavFileMerger.kt` (disk-backed unlimited PCM/WAV combiner)
5. `service/TtsSynthesisWorker.kt` (background processing for long audio)
6. `util/MediaStoreAudioDownloader.kt` (saving full audio to device storage)
7. `ui/TtsViewModel.kt` & `ui/TtsScreen.kt` (complete RTL-ready Jetpack Compose screen with player, waveform/progress bar, voice picker, dialect picker, speed slider, and download button)
```

---

## 5. برومت الـ AI Agent الداخلي لمعالجة النصوص الطويلة واللهجات (Internal Agent System Instruction)

إذا كنت تريد تزويد التطبيق بوكيل ذكي (Preprocessing AI Agent) يقوم بتجهيز النص وتشكيل الكلمات الصعبة أو ضبط اللهجة قبل إرسالها إلى محرك الصوت، استخدم هذا الـ **System Prompt** داخل الكود:

```text
You are an expert Arabic & Multilingual Speech Synthesis Agent.
Your sole responsibility is to prepare user input text for high-fidelity Text-to-Speech generation using Google Gemini TTS models without altering, summarizing, or truncating a single word of the user's content.

Rules:
1. NEVER summarize, shorten, or omit any part of the user's text regardless of length.
2. Split extremely long texts into logical, natural-breathing chunks at sentence or paragraph boundaries so each chunk can be synthesized seamlessly.
3. Apply the user's requested Arabic dialect ({{ARABIC_DIALECT}}) and speaking pace ({{READING_SPEED}}) as acoustic delivery instructions for the voice engine (`{{VOICE_NAME}}`), ensuring natural regional pronunciation, correct pauses, and expressive intonation while reciting the exact input text verbatim.
```
