package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.domain.WavAudioProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MediaStoreExporter {

    /**
     * Saves the audio file to device public storage (Music / Downloads) via MediaStore.
     * Returns the Uri of the saved file or throws an error.
     */
    suspend fun exportToDevice(
        context: Context,
        sourceWavFile: File,
        baseTitle: String,
        speed: Float = 1.0f,
        applySpeed: Boolean = true
    ): Uri = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanTitle = sanitizeFileName(baseTitle).take(30).trim()
        val fileName = if (cleanTitle.isNotEmpty()) "ناطق_${cleanTitle}_$timeStamp.wav" else "ناطق_$timeStamp.wav"

        // Prepare source or speed-adjusted temp file
        val fileToExport: File
        val tempAdjustedFile: File?
        if (applySpeed && speed !in 0.98f..1.02f) {
            tempAdjustedFile = File(context.cacheDir, "export_temp_$timeStamp.wav")
            WavAudioProcessor.applySpeedToWavFile(sourceWavFile, tempAdjustedFile, speed)
            fileToExport = tempAdjustedFile
        } else {
            fileToExport = sourceWavFile
            tempAdjustedFile = null
        }

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                put(MediaStore.Audio.Media.TITLE, cleanTitle.ifEmpty { "تسجيل صوتي ناطق" })
                put(MediaStore.Audio.Media.ARTIST, "ناطق TTS")
                put(MediaStore.Audio.Media.ALBUM, "ناطق - Gemini TTS")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Natiq")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val itemUri = resolver.insert(collectionUri, contentValues)
                ?: throw IllegalStateException("فشل إنشاء مدخل في وسائط التخزين.")

            resolver.openOutputStream(itemUri).use { outStream ->
                if (outStream == null) throw IllegalStateException("تعذر فتح ملف الوسائط للكتابة.")
                FileInputStream(fileToExport).use { inStream ->
                    inStream.copyTo(outStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
            }

            return@withContext itemUri
        } finally {
            tempAdjustedFile?.delete()
        }
    }

    /**
     * Shares audio file via standard Android share sheet with FileProvider.
     */
    fun shareAudioFile(context: Context, audioFile: File, title: String) {
        if (!audioFile.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            audioFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "استمع إلى هذا التسجيل الصوتي عبر تطبيق ناطق")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "مشاركة الملف الصوتي")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|\\r\\n]"), "_")
    }
}
