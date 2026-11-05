package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioRecordDao {
    @Query("SELECT * FROM audio_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<AudioRecord>>

    @Query("SELECT * FROM audio_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): AudioRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AudioRecord): Long

    @Delete
    suspend fun deleteRecord(record: AudioRecord)

    @Query("DELETE FROM audio_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
