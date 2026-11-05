package com.example.data.model

data class VoiceProfile(
    val name: String,
    val toneEn: String,
    val toneAr: String,
    val gender: String, // "Male", "Female", "Neutral"
    val descriptionAr: String,
    val isPrebuilt: Boolean = true
)

object VoiceLibrary {
    val prebuiltVoices: List<VoiceProfile> = listOf(
        VoiceProfile("Zephyr", "Bright", "مشرق", "Male", "صوت مشرق وواضح، ممتاز للسرد والمحتوى العام"),
        VoiceProfile("Puck", "Upbeat", "حيوي", "Male", "صوت حيوي ونشيط، ملائم للإعلانات والتفاعل"),
        VoiceProfile("Charon", "Informative", "إخباري", "Male", "صوت إخباري ومعلوماتي للمقالات والأخبار"),
        VoiceProfile("Kore", "Firm", "حازمة", "Female", "صوت نسائي حازم ومتزن، مناسب للكتب والعروض"),
        VoiceProfile("Fenrir", "Excitable", "حماسي", "Male", "صوت حماسي وجذاب للقصص المشوقة"),
        VoiceProfile("Leda", "Youthful", "شبابي", "Female", "صوت شبابي وعفوي للمحتوى التعليمي والبودكاست"),
        VoiceProfile("Orus", "Firm", "رصين", "Male", "صوت رصين وواثق للوثائقيات والتقارير"),
        VoiceProfile("Aoede", "Breezy", "هادئ ومنعش", "Female", "صوت هادئ ومريح للسرد الأدبي والتأمل"),
        VoiceProfile("Callirrhoe", "Easy-going", "عفوي", "Female", "صوت عفوي وسلس للمحادثات اليومية"),
        VoiceProfile("Autonoe", "Bright", "واضح", "Female", "صوت ناصع ومشرق للشروحات والتوجيهات"),
        VoiceProfile("Enceladus", "Breathy", "دافئ هامس", "Male", "صوت دافئ وشاعري للخواطر والدراما"),
        VoiceProfile("Iapetus", "Clear", "نقي", "Male", "صوت نقي وواضح المخارج للفصحى والنصوص الطويلة"),
        VoiceProfile("Umbriel", "Easy-going", "سلس", "Male", "صوت مرن للمحتوى الحواري والاجتماعي"),
        VoiceProfile("Algieba", "Smooth", "ناعم", "Male", "صوت ناعم وانسيابي للروايات والكتب"),
        VoiceProfile("Despina", "Smooth", "انسيابي", "Female", "صوت متناغم للمقالات الثقافية"),
        VoiceProfile("Erinome", "Clear", "جلي", "Female", "صوت دقيق وجلي للمحتوى العلمي والتقني"),
        VoiceProfile("Algenib", "Gravelly", "رخيم وعميق", "Male", "صوت سينمائي فخم وعميق"),
        VoiceProfile("Rasalgethi", "Informative", "وثائقي", "Male", "صوت وثائقي ومعرفي للأبحاث والتاريخ"),
        VoiceProfile("Laomedeia", "Upbeat", "متفائل", "Female", "صوت متفائل ومشرق للقصص التحفيزية"),
        VoiceProfile("Achernar", "Soft", "لطيف", "Female", "صوت لطيف وهادئ لقصص ما قبل النوم"),
        VoiceProfile("Alnilam", "Firm", "متزن", "Male", "صوت قوي وثابت للبيانات والخطابات"),
        VoiceProfile("Schedar", "Even", "متناسق", "Male", "صوت متوازن ومعتدل لقراءة الكتب الطويلة"),
        VoiceProfile("Gacrux", "Mature", "ناضج ووقور", "Male", "صوت وقور للسرد التاريخي والكلاسيكي"),
        VoiceProfile("Pulcherrima", "Forward", "مباشر", "Female", "صوت إذاعي مباشر وحاضر للبودكاست"),
        VoiceProfile("Achird", "Friendly", "ودود", "Male", "صوت قريب وودود للشروحات المبسطة"),
        VoiceProfile("Zubenelgenubi", "Casual", "غير رسمي", "Male", "صوت تلقائي ومناسب للهجات العامية"),
        VoiceProfile("Vindemiatrix", "Gentle", "رقيق", "Female", "صوت رقيق ولطيف للشعر والنثر الأدبي"),
        VoiceProfile("Sadachbia", "Lively", "مفعم بالحيوية", "Male", "صوت تفاعلي مفعم بالحياة للحوارات"),
        VoiceProfile("Sadaltager", "Knowledgeable", "خبير معرفي", "Male", "صوت خبير ومثقف للمحاضرات والملخصات"),
        VoiceProfile("Sulafat", "Warm", "دافئ وحميمي", "Female", "صوت إنساني دافئ للروايات الإنسانية")
    )
}
