package com.example.data.remote

import android.util.Base64
import android.util.Log
import com.example.data.model.VoiceProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiTtsService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val TAG = "GeminiTtsService"
        private const val INTERACTIONS_URL = "https://generativelanguage.googleapis.com/v1beta/interactions"
        private const val BASE_MODELS_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val VOICES_URL = "https://generativelanguage.googleapis.com/v1beta/voices"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    /**
     * Synthesizes audio for a single text chunk.
     * Returns raw 16-bit PCM bytes (24kHz Mono).
     */
    suspend fun synthesizeChunk(
        text: String,
        voiceName: String,
        modelId: String,
        apiKey: String,
        stylePrompt: String = "",
        maxRetries: Int = 3
    ): ByteArray = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("يرجى إدخال مفتاح Gemini API في الإعدادات أولاً.")
        }
        if (text.isBlank()) {
            throw IllegalArgumentException("النص فارغ.")
        }

        var attempt = 0
        var lastException: Exception? = null

        while (attempt <= maxRetries) {
            try {
                // Try Interactions API first as requested in official specification
                return@withContext executeInteractionsRequest(text, voiceName, modelId, apiKey, stylePrompt)
            } catch (e: Exception) {
                lastException = e
                val errorMsg = e.message ?: ""
                Log.w(TAG, "Interactions attempt $attempt failed: $errorMsg, trying generateContent fallback...")

                // If interactions endpoint fails (e.g. 404, 400 schema discrepancy), try generateContent
                try {
                    return@withContext executeGenerateContentRequest(text, voiceName, modelId, apiKey, stylePrompt)
                } catch (fallbackException: Exception) {
                    lastException = fallbackException
                    Log.w(TAG, "Fallback attempt $attempt failed: ${fallbackException.message}")
                }

                // Check if rate limited (429) or transient error
                val isRateLimited = errorMsg.contains("429") || errorMsg.contains("RESOURCE_EXHAUSTED")
                val isServerError = errorMsg.contains("500") || errorMsg.contains("503") || errorMsg.contains("504")

                if (attempt < maxRetries && (isRateLimited || isServerError || e is IOException)) {
                    val backoffMs = (2000L * (1 shl attempt)) + (0..500).random()
                    Log.i(TAG, "Retrying in ${backoffMs}ms (attempt ${attempt + 1}/$maxRetries)...")
                    delay(backoffMs)
                    attempt++
                } else {
                    break
                }
            }
        }

        throw lastException ?: IOException("فشل الاتصال بـ Gemini API.")
    }

    /**
     * Executes official Interactions API request.
     */
    private fun executeInteractionsRequest(
        text: String,
        voiceName: String,
        modelId: String,
        apiKey: String,
        stylePrompt: String
    ): ByteArray {
        val root = JSONObject()
        root.put("model", modelId)

        val inputArr = JSONArray()
        val userInputObj = JSONObject()
        userInputObj.put("type", "user_input")

        val contentArr = JSONArray()
        val textObj = JSONObject()
        textObj.put("type", "text")
        textObj.put("text", text)

        if (stylePrompt.isNotBlank()) {
            val annotArr = JSONArray()
            val metaObj = JSONObject()
            metaObj.put("type", "speech_metadata")
            metaObj.put("style", stylePrompt)
            annotArr.put(metaObj)
            textObj.put("annotations", annotArr)
        }
        contentArr.put(textObj)
        userInputObj.put("content", contentArr)
        inputArr.put(userInputObj)
        root.put("input", inputArr)

        val responseFormat = JSONObject()
        responseFormat.put("type", "audio")
        responseFormat.put("mime_type", "audio/l16")
        responseFormat.put("sample_rate", 24000)
        root.put("response_format", responseFormat)

        val generationConfig = JSONObject()
        val speechConfigArr = JSONArray()
        val voiceObj = JSONObject()
        voiceObj.put("voice", voiceName)
        speechConfigArr.put(voiceObj)
        generationConfig.put("speech_config", speechConfigArr)
        root.put("generation_config", generationConfig)

        val request = Request.Builder()
            .url(INTERACTIONS_URL)
            .addHeader("x-goog-api-key", apiKey)
            .post(root.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val parsedError = parseErrorMessage(bodyString, response.code)
                throw IOException(parsedError)
            }

            return extractPcmFromInteractionsResponse(bodyString)
        }
    }

    /**
     * Fallback to generateContent audio synthesis endpoint.
     */
    private fun executeGenerateContentRequest(
        text: String,
        voiceName: String,
        modelId: String,
        apiKey: String,
        stylePrompt: String
    ): ByteArray {
        val root = JSONObject()
        val contentsArr = JSONArray()
        val contentObj = JSONObject()
        val partsArr = JSONArray()
        val partObj = JSONObject()

        val fullPrompt = if (stylePrompt.isNotBlank()) {
            // Note: prompt instruction requests verbatim text reading
            text
        } else {
            text
        }
        partObj.put("text", fullPrompt)
        partsArr.put(partObj)
        contentObj.put("parts", partsArr)
        contentsArr.put(contentObj)
        root.put("contents", contentsArr)

        val genConfig = JSONObject()
        val modalities = JSONArray()
        modalities.put("AUDIO")
        genConfig.put("responseModalities", modalities)

        val speechConfig = JSONObject()
        val voiceConfig = JSONObject()
        val prebuiltVoiceConfig = JSONObject()
        prebuiltVoiceConfig.put("voiceName", voiceName)
        voiceConfig.put("prebuiltVoiceConfig", prebuiltVoiceConfig)
        speechConfig.put("voiceConfig", voiceConfig)
        genConfig.put("speechConfig", speechConfig)
        root.put("generationConfig", genConfig)

        val effectiveModel = if (modelId.startsWith("models/")) modelId else "models/$modelId"
        val url = "$BASE_MODELS_URL/$effectiveModel:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .post(root.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val parsedError = parseErrorMessage(bodyString, response.code)
                throw IOException(parsedError)
            }

            return extractPcmFromGenerateContentResponse(bodyString)
        }
    }

    private fun extractPcmFromInteractionsResponse(jsonString: String): ByteArray {
        val root = JSONObject(jsonString)

        // Find last audio content in steps or outputs
        if (root.has("steps")) {
            val steps = root.getJSONArray("steps")
            for (i in steps.length() - 1 downTo 0) {
                val step = steps.getJSONObject(i)
                if (step.has("content")) {
                    val contentArr = step.getJSONArray("content")
                    for (j in contentArr.length() - 1 downTo 0) {
                        val item = contentArr.getJSONObject(j)
                        if (item.optString("type") == "audio" && item.has("data")) {
                            return cleanPcmBytes(Base64.decode(item.getString("data"), Base64.DEFAULT))
                        }
                    }
                }
            }
        }

        // Alternative outputs or candidates
        if (root.has("candidates")) {
            return extractPcmFromGenerateContentResponse(jsonString)
        }

        throw IOException("لم يتم العثور على بيانات صوتية في رد النموذج.")
    }

    private fun extractPcmFromGenerateContentResponse(jsonString: String): ByteArray {
        val root = JSONObject(jsonString)
        val candidates = root.optJSONArray("candidates")
            ?: throw IOException("لا توجد مخرجات في الرد.")
        if (candidates.length() == 0) throw IOException("مخرجات النموذج فارغة.")

        val candidate = candidates.getJSONObject(0)
        val content = candidate.getJSONObject("content")
        val parts = content.getJSONArray("parts")

        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.has("inlineData")) {
                val inlineData = part.getJSONObject("inlineData")
                val base64Data = inlineData.getString("data")
                val rawBytes = Base64.decode(base64Data, Base64.DEFAULT)
                return cleanPcmBytes(rawBytes)
            }
        }

        throw IOException("لم يُرجع النموذج أي ملف صوتي صالح.")
    }

    /**
     * If the returned bytes contain a 44-byte RIFF/WAV header, strip it to get pure raw PCM.
     */
    private fun cleanPcmBytes(bytes: ByteArray): ByteArray {
        if (bytes.size > 44 &&
            bytes[0] == 'R'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == 'F'.code.toByte()
        ) {
            // Strip 44-byte RIFF header to yield pure PCM
            return bytes.copyOfRange(44, bytes.size)
        }
        return bytes
    }

    /**
     * Tests the provided Gemini API key with a minimal request.
     */
    suspend fun testApiKey(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("المفتاح فارغ."))
        try {
            val url = "$BASE_MODELS_URL?key=$apiKey"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("تم التحقق من المفتاح بنجاح! الاتصال يعمل بشكل ممتاز.")
                } else {
                    val body = response.body?.string() ?: ""
                    Result.failure(IOException(parseErrorMessage(body, response.code)))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Loads dynamic voice library from GET /v1beta/voices
     */
    suspend fun fetchExtendedVoices(apiKey: String): List<VoiceProfile> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext emptyList()
        try {
            val url = "$VOICES_URL?key=$apiKey&page_size=100"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(body)
                val voicesArr = root.optJSONArray("voices") ?: return@withContext emptyList()

                val result = mutableListOf<VoiceProfile>()
                for (i in 0 until voicesArr.length()) {
                    val obj = voicesArr.getJSONObject(i)
                    val name = obj.optString("name", "").ifEmpty { obj.optString("voiceName", "") }
                    if (name.isBlank()) continue
                    val gender = obj.optString("gender", "Neutral")
                    val tone = obj.optString("pitch", "") + " " + obj.optString("persona", "")
                    val description = obj.optString("description", "صوت إضافي من مكتبة أصوات Google AI")
                    result.add(
                        VoiceProfile(
                            name = name,
                            toneEn = tone.trim().ifEmpty { "Dynamic" },
                            toneAr = "مكتبة موسعة",
                            gender = if (gender.contains("female", ignoreCase = true)) "Female" else if (gender.contains("male", ignoreCase = true)) "Male" else "Neutral",
                            descriptionAr = description,
                            isPrebuilt = false
                        )
                    )
                }
                return@withContext result
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load extended voices: ${e.message}")
            return@withContext emptyList()
        }
    }

    private fun parseErrorMessage(responseBody: String, statusCode: Int): String {
        return try {
            val root = JSONObject(responseBody)
            val error = root.optJSONObject("error")
            val message = error?.optString("message") ?: ""
            when {
                statusCode == 429 -> "تم تجاوز حد استخدام الـ API مؤقتاً (429). يُرجى المحاولة بعد قليل."
                statusCode == 403 || statusCode == 401 -> "مفتاح API غير صالح أو غير مصرح له. يرجى التحقق من المفتاح في الإعدادات."
                message.isNotBlank() -> message
                else -> "خطأ في الاتصال بالخدمة ($statusCode)."
            }
        } catch (e: Exception) {
            when (statusCode) {
                429 -> "تم تجاوز حد استخدام الـ API مؤقتاً (429). يُرجى المحاولة بعد قليل."
                401, 403 -> "مفتاح API غير صالح أو منتهي الصلاحية."
                500, 503 -> "خوادم Gemini مشغولة حالياً، يرجى المحاولة مجدداً."
                else -> "خطأ غير متوقع في الاتصال ($statusCode)."
            }
        }
    }
}
