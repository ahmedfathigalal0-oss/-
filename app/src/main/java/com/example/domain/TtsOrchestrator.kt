package com.example.domain

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.AppDatabase
import com.example.data.local.AudioRecord
import com.example.data.model.GenerationState
import com.example.data.remote.GeminiTtsService
import com.example.util.NotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TtsOrchestrator(
    private val context: Context,
    private val ttsService: GeminiTtsService = GeminiTtsService(),
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) {
    companion object {
        private const val TAG = "TtsOrchestrator"
    }

    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    @Volatile
    private var isPaused = false

    @Volatile
    private var isCancelled = false

    // Cache directory for audio
    private val audioDir: File
        get() {
            val dir = File(context.filesDir, "audio")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun pause() {
        isPaused = true
        _generationState.value = GenerationState.Paused
    }

    fun resume() {
        isPaused = false
    }

    fun cancel() {
        isCancelled = true
        isPaused = false
        _generationState.value = GenerationState.Idle
    }

    /**
     * Synthesizes full text with unlimited length by chunking and streaming to disk.
     */
    suspend fun synthesizeFullText(
        text: String,
        voiceName: String,
        dialectName: String,
        dialectPrompt: String,
        modelId: String,
        apiKey: String,
        chunkSize: Int = 1200,
        silenceMs: Int = 200,
        speed: Float = 1.0f
    ): AudioRecord = withContext(Dispatchers.IO) {
        isPaused = false
        isCancelled = false

        val chunks = TextChunkingEngine.splitText(text, chunkSize)
        if (chunks.isEmpty()) {
            throw IllegalArgumentException("النص المراد تحويله فارغ.")
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val firstWords = text.trim().split(Regex("\\s+")).take(4).joinToString("_")
            .replace(Regex("[\\\\/:*?\"<>|]"), "")
            .take(24)
        val audioFileName = "natiq_${timeStamp}_${firstWords.ifEmpty { "voice" }}.wav"
        val outputFile = File(audioDir, audioFileName)

        var fos: FileOutputStream? = null
        val notificationManager = NotificationManagerCompat.from(context)
        NotificationHelper.createNotificationChannel(context)

        try {
            fos = WavAudioProcessor.startWavFile(outputFile)
            val silenceBytes = WavAudioProcessor.createSilencePcm(silenceMs)

            val totalChunks = chunks.size
            for (index in chunks.indices) {
                while (isPaused && !isCancelled) {
                    kotlinx.coroutines.delay(300)
                }

                if (isCancelled) {
                    throw CancellationException("تم إلغاء عملية التحويل الصوتي.")
                }

                val chunkNumber = index + 1
                val progressPercent = ((chunkNumber.toFloat() / totalChunks.toFloat()) * 100).toInt()

                _generationState.value = GenerationState.Processing(
                    currentChunk = chunkNumber,
                    totalChunks = totalChunks,
                    percent = chunkNumber.toFloat() / totalChunks.toFloat(),
                    statusMessage = "معالجة المقطع $chunkNumber من $totalChunks ($progressPercent%)"
                )

                // Show/update progress notification
                try {
                    val notif = NotificationHelper.buildProgressNotification(
                        context,
                        chunkNumber,
                        totalChunks,
                        progressPercent
                    ).build()
                    notificationManager.notify(NotificationHelper.NOTIFICATION_ID, notif)
                } catch (e: SecurityException) {
                    // Notification permission not granted yet, ignore gracefully
                }

                val chunkText = chunks[index]
                Log.d(TAG, "Synthesizing chunk $chunkNumber/$totalChunks (${chunkText.length} chars)")

                val pcmData = ttsService.synthesizeChunk(
                    text = chunkText,
                    voiceName = voiceName,
                    modelId = modelId,
                    apiKey = apiKey,
                    stylePrompt = dialectPrompt
                )

                // Write raw PCM to disk immediately (zero memory overflow)
                fos.write(pcmData)

                // Append silence between chunks except after the last chunk
                if (index < chunks.size - 1 && silenceBytes.isNotEmpty()) {
                    fos.write(silenceBytes)
                }
                fos.flush()
            }

            fos.close()
            fos = null

            // Finalize RIFF WAV header with exact data length
            WavAudioProcessor.finalizeWavFile(outputFile)

            val totalBytes = outputFile.length()
            val pcmBytes = if (totalBytes >= 44) totalBytes - 44 else 0
            val durationMs = WavAudioProcessor.calculateDurationMs(pcmBytes)

            // Save record to Room database
            val recordTitle = text.trim().split(Regex("\\s+")).take(5).joinToString(" ")
            val audioRecord = AudioRecord(
                title = recordTitle.ifEmpty { "تسجيل ناطق $timeStamp" },
                textSnippet = text.take(150),
                filePath = outputFile.absolutePath,
                durationMs = durationMs,
                fileSizeBytes = totalBytes,
                voiceName = voiceName,
                dialectName = dialectName,
                speed = speed,
                timestamp = System.currentTimeMillis()
            )
            val recordId = database.audioRecordDao().insertRecord(audioRecord)
            val savedRecord = audioRecord.copy(id = recordId)

            _generationState.value = GenerationState.Completed(
                audioFilePath = outputFile.absolutePath,
                durationMs = durationMs,
                totalBytes = totalBytes,
                chunkCount = totalChunks
            )

            // Cancel progress notification and show completion notification
            try {
                notificationManager.cancel(NotificationHelper.NOTIFICATION_ID)
                NotificationHelper.showCompletionNotification(context, savedRecord.title)
            } catch (e: SecurityException) {
                // Ignore
            }

            return@withContext savedRecord
        } catch (e: Exception) {
            fos?.close()
            if (isCancelled && outputFile.exists()) {
                outputFile.delete()
            }
            try {
                notificationManager.cancel(NotificationHelper.NOTIFICATION_ID)
            } catch (se: SecurityException) {
                // Ignore
            }

            if (e !is CancellationException) {
                _generationState.value = GenerationState.Error(
                    e.message ?: "حدث خطأ غير متوقع أثناء المعالجة الصوتية."
                )
            }
            throw e
        }
    }

    /**
     * Synthesizes and caches a short audition clip for the selected voice.
     */
    suspend fun previewVoice(
        voiceName: String,
        apiKey: String,
        modelId: String
    ): File = withContext(Dispatchers.IO) {
        val sampleFile = File(context.cacheDir, "sample_${voiceName}.wav")
        if (sampleFile.exists() && sampleFile.length() > 44) {
            return@withContext sampleFile
        }

        val sampleText = "مرحباً بك! أنا أقرأ لك بصوتي هذا النموذج الذكي عبر ناطق."
        val pcm = ttsService.synthesizeChunk(
            text = sampleText,
            voiceName = voiceName,
            modelId = modelId,
            apiKey = apiKey,
            stylePrompt = "Speak in clear, friendly Modern Standard Arabic."
        )

        val fos = WavAudioProcessor.startWavFile(sampleFile)
        fos.write(pcm)
        fos.close()
        WavAudioProcessor.finalizeWavFile(sampleFile)

        return@withContext sampleFile
    }
}
