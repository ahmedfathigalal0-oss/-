package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_records")
data class AudioRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val textSnippet: String,
    val filePath: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val voiceName: String,
    val dialectName: String,
    val speed: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
)
