package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ArabicDialect
import com.example.data.model.DialectCatalog
import com.example.data.model.GenerationState
import com.example.data.model.VoiceProfile
import com.example.domain.TextChunkingEngine
import com.example.ui.MainViewModel
import com.example.ui.components.AudioPlayerBar
import com.example.ui.components.VoicePickerBottomSheet
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val textInput by viewModel.textInput.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()
    val selectedDialect by viewModel.selectedDialect.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()
    val availableVoices by viewModel.availableVoices.collectAsState()
    val generationState by viewModel.generationState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val lastRecord by viewModel.lastGeneratedRecord.collectAsState()
    val isAuditioning by viewModel.isAuditioning.collectAsState()

    var showVoicePicker by remember { mutableStateOf(false) }

    // File picker for .txt files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val fileContent = reader.readText()
                    viewModel.onTextChange(fileContent)
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // Informational stats (characters, words, estimated audio duration)
    val stats = remember(textInput, speed) {
        TextChunkingEngine.estimateStats(textInput, speed)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Text Input Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("text_input_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Action row (Import, Paste, Clear)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "النص المراد تحويله (غير محدود)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { filePickerLauncher.launch("text/plain") },
                            modifier = Modifier.testTag("import_file_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = "استيراد ملف نصي",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text ?: ""
                                if (clip.isNotBlank()) {
                                    viewModel.pasteText(clip)
                                }
                            },
                            modifier = Modifier.testTag("paste_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "لصق من الحافظة",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }

                        if (textInput.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.clearText() },
                                modifier = Modifier.testTag("clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "مسح النص",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Big Multiline Text Editor
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { viewModel.onTextChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp, max = 280.dp)
                        .testTag("text_input_field"),
                    placeholder = {
                        Text(
                            text = "اكتب أو الصق أي نص بأي طول (مقالة، قصة، كتاب كامل)...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 22.sp
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Informational Counter Bar (Non-blocking)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الحروف: ${stats.first}  •  الكلمات: ${stats.second}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val estimatedSecs = stats.third / 1000
                        val estMin = estimatedSecs / 60
                        val estSec = estimatedSecs % 60
                        val timeStr = if (estMin > 0) "~$estMin دقيقة و $estSec ثانية" else "~$estSec ثانية"

                        Text(
                            text = "المدة التقديرية: $timeStr",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // --- 2. Arabic Dialects Chips ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "اللهجة العربية المستهدفة:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(DialectCatalog.dialects, key = { it.id }) { dialect ->
                        val isSelected = dialect.id == selectedDialect.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onDialectSelected(dialect) },
                            label = {
                                Text(
                                    text = "${dialect.flag} ${dialect.nameAr}",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // --- 3. Voice Selection Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showVoicePicker = true }
                .testTag("voice_select_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الصوت المختار",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = selectedVoice.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text(selectedVoice.toneAr, fontSize = 11.sp) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { showVoicePicker = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تغيير", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        // --- 4. Speed Slider ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "سرعة القراءة",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = "${speed}x",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = speed,
                    onValueChange = { viewModel.onSpeedChanged(it) },
                    valueRange = 0.5f..2.0f,
                    steps = 14, // 0.1 increments
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("speed_slider")
                )

                // Quick speed presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val presets = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    presets.forEach { preset ->
                        val isSelected = Math.abs(speed - preset) < 0.05f
                        OutlinedButton(
                            onClick = { viewModel.onSpeedChanged(preset) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "${preset}x",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // --- 5. Conversion Progress Card (when generating) ---
        AnimatedVisibility(visible = generationState is GenerationState.Processing || generationState is GenerationState.Paused) {
            val isPaused = generationState is GenerationState.Paused
            val progress = (generationState as? GenerationState.Processing)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("conversion_progress_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPaused) "التحويل متوقف مؤقتاً" else "جارٍ التحويل الصوتي عبر Gemini...",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${((progress?.percent ?: 0f) * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { progress?.percent ?: 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = progress?.statusMessage ?: "معالجة المقاطع الصوتية بالترتيب...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPaused) {
                            TextButton(
                                onClick = { viewModel.resumeSynthesis() },
                                modifier = Modifier.testTag("resume_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استئناف")
                            }
                        } else {
                            TextButton(
                                onClick = { viewModel.pauseSynthesis() },
                                modifier = Modifier.testTag("pause_button")
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إيقاف مؤقت")
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        TextButton(
                            onClick = { viewModel.cancelSynthesis() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("cancel_button")
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إلغاء")
                        }
                    }
                }
            }
        }

        // --- 6. Primary Action Button ---
        val isProcessing = generationState is GenerationState.Processing || generationState is GenerationState.Paused
        Button(
            onClick = { viewModel.startSynthesis() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("convert_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = !isProcessing && textInput.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.Default.RecordVoiceOver,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "حوّل إلى صوت الآن",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        // --- 7. Audio Player Bar (when audio is loaded/completed) ---
        AnimatedVisibility(visible = playerState.isLoaded) {
            AudioPlayerBar(
                playerState = playerState,
                title = lastRecord?.title ?: "الملف الصوتي المولد",
                voiceName = lastRecord?.voiceName ?: selectedVoice.name,
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSeekTo = { viewModel.seekPlayer(it) },
                onSeekRelative = { viewModel.seekRelative(it) },
                onSpeedChanged = { viewModel.onSpeedChanged(it) },
                onDownload = { viewModel.exportAudio() },
                onShare = { viewModel.shareAudio() },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Voice Picker Bottom Sheet Modal
    if (showVoicePicker) {
        VoicePickerBottomSheet(
            voices = availableVoices,
            selectedVoice = selectedVoice,
            isAuditioning = isAuditioning,
            onVoiceSelected = { viewModel.onVoiceSelected(it) },
            onPreviewVoice = { viewModel.previewVoice(it) },
            onDismiss = { showVoicePicker = false }
        )
    }
}
