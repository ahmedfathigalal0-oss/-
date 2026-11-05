package com.example.ui

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.AudioRecord
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.ArabicDialect
import com.example.data.model.DialectCatalog
import com.example.data.model.GenerationState
import com.example.data.model.VoiceLibrary
import com.example.data.model.VoiceProfile
import com.example.data.remote.GeminiTtsService
import com.example.domain.TtsOrchestrator
import com.example.util.AudioPlayerController
import com.example.util.MediaStoreExporter
import com.example.util.PlayerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val userPrefs = UserPreferencesRepository(application)
    private val ttsService = GeminiTtsService()
    private val database = AppDatabase.getDatabase(application)
    private val orchestrator = TtsOrchestrator(application, ttsService, database)
    val playerController = AudioPlayerController(application, viewModelScope)

    // User settings flows
    val apiKey: StateFlow<String> = userPrefs.apiKeyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val selectedModelId: StateFlow<String> = userPrefs.selectedModelFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "gemini-3.8-flash-tts")

    val chunkSize: StateFlow<Int> = userPrefs.chunkSizeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1200)

    val silenceMs: StateFlow<Int> = userPrefs.silenceMsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 200)

    val applySpeedOnExport: StateFlow<Boolean> = userPrefs.applySpeedExportFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val appLanguage: StateFlow<String> = userPrefs.appLanguageFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "ar")

    // Input state
    private val _textInput = MutableStateFlow("")
    val textInput: StateFlow<String> = _textInput.asStateFlow()

    private val _selectedVoice = MutableStateFlow(VoiceLibrary.prebuiltVoices[3]) // Kore (Firm)
    val selectedVoice: StateFlow<VoiceProfile> = _selectedVoice.asStateFlow()

    private val _selectedDialect = MutableStateFlow(DialectCatalog.dialects[0]) // Auto-detect
    val selectedDialect: StateFlow<ArabicDialect> = _selectedDialect.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // Library records flow
    val allRecords: StateFlow<List<AudioRecord>> = database.audioRecordDao().getAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Voices catalog (prebuilt + extended dynamic)
    private val _availableVoices = MutableStateFlow(VoiceLibrary.prebuiltVoices)
    val availableVoices: StateFlow<List<VoiceProfile>> = _availableVoices.asStateFlow()

    // Orchestrator & player state
    val generationState: StateFlow<GenerationState> = orchestrator.generationState
    val playerState: StateFlow<PlayerState> = playerController.playerState

    // Last completed audio record for active playback
    private val _lastGeneratedRecord = MutableStateFlow<AudioRecord?>(null)
    val lastGeneratedRecord: StateFlow<AudioRecord?> = _lastGeneratedRecord.asStateFlow()

    // Auditioning state
    private val _isAuditioning = MutableStateFlow(false)
    val isAuditioning: StateFlow<Boolean> = _isAuditioning.asStateFlow()

    // UI Toast / SnackBar messages
    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()

    private var synthesisJob: Job? = null

    init {
        // Automatically attempt to fetch extended voices if API key is present
        viewModelScope.launch {
            apiKey.collect { key ->
                if (key.isNotBlank()) {
                    loadExtendedVoices(key)
                }
            }
        }
    }

    fun onTextChange(newText: String) {
        _textInput.value = newText
    }

    fun clearText() {
        _textInput.value = ""
    }

    fun pasteText(text: String) {
        _textInput.value = if (_textInput.value.isEmpty()) text else "${_textInput.value}\n$text"
    }

    fun onVoiceSelected(voice: VoiceProfile) {
        _selectedVoice.value = voice
    }

    fun onDialectSelected(dialect: ArabicDialect) {
        _selectedDialect.value = dialect
    }

    fun onSpeedChanged(speed: Float) {
        val rounded = (Math.round(speed * 10f) / 10f).coerceIn(0.25f, 3.0f)
        _playbackSpeed.value = rounded
        playerController.setSpeed(rounded)
    }

    fun startSynthesis() {
        val text = _textInput.value.trim()
        if (text.isEmpty()) {
            emitMessage("يرجى إدخال أو لصق نص أولاً.")
            return
        }

        val key = apiKey.value
        if (key.isBlank()) {
            emitMessage("يرجى إدخال مفتاح Gemini API في شاشة الإعدادات أولاً.")
            return
        }

        synthesisJob?.cancel()
        synthesisJob = viewModelScope.launch {
            try {
                val record = orchestrator.synthesizeFullText(
                    text = text,
                    voiceName = _selectedVoice.value.name,
                    dialectName = _selectedDialect.value.nameAr,
                    dialectPrompt = _selectedDialect.value.performancePrompt,
                    modelId = selectedModelId.value,
                    apiKey = key,
                    chunkSize = chunkSize.value,
                    silenceMs = silenceMs.value,
                    speed = _playbackSpeed.value
                )
                _lastGeneratedRecord.value = record
                playerController.loadAudio(record.filePath, autoPlay = true, initialSpeed = _playbackSpeed.value)
                emitMessage("اكتمل التحويل الصوتي بنجاح! جاهز للاستماع والتحميل.")
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    emitMessage(e.message ?: "فشلت عملية التحويل الصوتي.")
                }
            }
        }
    }

    fun pauseSynthesis() {
        orchestrator.pause()
    }

    fun resumeSynthesis() {
        orchestrator.resume()
    }

    fun cancelSynthesis() {
        synthesisJob?.cancel()
        orchestrator.cancel()
        emitMessage("تم إلغاء التحويل.")
    }

    fun playRecord(record: AudioRecord) {
        _lastGeneratedRecord.value = record
        playerController.loadAudio(record.filePath, autoPlay = true, initialSpeed = _playbackSpeed.value)
    }

    fun togglePlayPause() {
        playerController.togglePlayPause()
    }

    fun seekPlayer(positionMs: Long) {
        playerController.seekTo(positionMs)
    }

    fun seekRelative(offsetMs: Long) {
        playerController.seekRelative(offsetMs)
    }

    fun previewVoice(voice: VoiceProfile) {
        val key = apiKey.value
        if (key.isBlank()) {
            emitMessage("يرجى إدخال مفتاح API في الإعدادات للاستماع لعيّنة الصوت.")
            return
        }

        viewModelScope.launch {
            _isAuditioning.value = true
            try {
                val sampleFile = orchestrator.previewVoice(voice.name, key, selectedModelId.value)
                playerController.loadAudio(sampleFile.absolutePath, autoPlay = true, initialSpeed = 1.0f)
            } catch (e: Exception) {
                emitMessage("تعذر تحميل العينة: ${e.message}")
            } finally {
                _isAuditioning.value = false
            }
        }
    }

    fun exportAudio(record: AudioRecord? = null) {
        val targetRecord = record ?: _lastGeneratedRecord.value ?: return
        val sourceFile = File(targetRecord.filePath)
        if (!sourceFile.exists()) {
            emitMessage("الملف الصوتي غير موجود على الجهاز.")
            return
        }

        viewModelScope.launch {
            try {
                val uri = MediaStoreExporter.exportToDevice(
                    context = getApplication(),
                    sourceWavFile = sourceFile,
                    baseTitle = targetRecord.title,
                    speed = _playbackSpeed.value,
                    applySpeed = applySpeedOnExport.value
                )
                emitMessage("تم حفظ الملف الصوتي بنجاح في مجلد الموسيقى والتنزيلات على هاتفك!")
            } catch (e: Exception) {
                emitMessage("فشل حفظ الملف: ${e.message}")
            }
        }
    }

    fun shareAudio(record: AudioRecord? = null) {
        val targetRecord = record ?: _lastGeneratedRecord.value ?: return
        val sourceFile = File(targetRecord.filePath)
        if (!sourceFile.exists()) {
            emitMessage("الملف الصوتي غير موجود للمشاركة.")
            return
        }
        MediaStoreExporter.shareAudioFile(getApplication(), sourceFile, targetRecord.title)
    }

    fun deleteRecord(record: AudioRecord) {
        viewModelScope.launch {
            try {
                database.audioRecordDao().deleteRecord(record)
                File(record.filePath).delete()
                if (_lastGeneratedRecord.value?.id == record.id) {
                    _lastGeneratedRecord.value = null
                    playerController.pause()
                }
                emitMessage("تم حذف التسجيل.")
            } catch (e: Exception) {
                emitMessage("فشل الحذف: ${e.message}")
            }
        }
    }

    fun saveApiKey(newKey: String) {
        viewModelScope.launch {
            userPrefs.saveApiKey(newKey)
            emitMessage("تم حفظ مفتاح API.")
            if (newKey.isNotBlank()) {
                loadExtendedVoices(newKey)
            }
        }
    }

    fun testApiKey(keyToTest: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = ttsService.testApiKey(keyToTest)
            result.onSuccess { msg ->
                onComplete(true, msg)
            }.onFailure { err ->
                onComplete(false, err.message ?: "فشل التحقق من المفتاح.")
            }
        }
    }

    fun saveSelectedModel(modelId: String) {
        viewModelScope.launch {
            userPrefs.saveSelectedModel(modelId)
        }
    }

    fun saveChunkSize(size: Int) {
        viewModelScope.launch {
            userPrefs.saveChunkSize(size)
        }
    }

    fun saveSilenceMs(ms: Int) {
        viewModelScope.launch {
            userPrefs.saveSilenceMs(ms)
        }
    }

    fun saveApplySpeedExport(apply: Boolean) {
        viewModelScope.launch {
            userPrefs.saveApplySpeedExport(apply)
        }
    }

    fun saveAppLanguage(lang: String) {
        viewModelScope.launch {
            userPrefs.saveAppLanguage(lang)
        }
    }

    private fun loadExtendedVoices(key: String) {
        viewModelScope.launch {
            val extended = ttsService.fetchExtendedVoices(key)
            if (extended.isNotEmpty()) {
                val combined = (VoiceLibrary.prebuiltVoices + extended).distinctBy { it.name }
                _availableVoices.value = combined
            }
        }
    }

    private fun emitMessage(msg: String) {
        viewModelScope.launch {
            _uiEvents.emit(msg)
            Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerController.release()
    }
}
