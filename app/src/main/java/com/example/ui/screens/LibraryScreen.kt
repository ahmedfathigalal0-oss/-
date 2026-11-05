package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AudioRecord
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val records by viewModel.allRecords.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var recordToDelete by remember { mutableStateOf<AudioRecord?>(null) }

    val filteredRecords = remember(records, searchQuery) {
        if (searchQuery.isBlank()) {
            records
        } else {
            records.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.textSnippet.contains(searchQuery, ignoreCase = true) ||
                        it.voiceName.contains(searchQuery, ignoreCase = true) ||
                        it.dialectName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("library_search_input"),
            placeholder = { Text("ابحث في التسجيلات الصوتية...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Total count header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "المكتبة الصوتية (${filteredRecords.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredRecords.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "لم يتم العثور على تسجيلات تطابق بحثك" else "المكتبة فارغة حتى الآن",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "قم بتحويل نصوصك إلى صوت وستظهر جميع تسجيلاتك هنا تلقائياً.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredRecords, key = { it.id }) { record ->
                    val isCurrentTrack = playerState.currentFilePath == record.filePath
                    val isPlaying = isCurrentTrack && playerState.isPlaying

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("record_card_${record.id}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentTrack) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Row 1: Title and play button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalIconButton(
                                    onClick = {
                                        if (isCurrentTrack) {
                                            viewModel.togglePlayPause()
                                        } else {
                                            viewModel.playRecord(record)
                                        }
                                    },
                                    modifier = Modifier.size(46.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل"
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = record.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = record.textSnippet,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Badges (Voice, Dialect, Duration, Size, Date)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("صوت: ${record.voiceName}", fontSize = 11.sp) },
                                    modifier = Modifier.height(26.dp)
                                )
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(record.dialectName, fontSize = 11.sp) },
                                    modifier = Modifier.height(26.dp)
                                )
                                Text(
                                    text = formatDuration(record.durationMs) + " • " + formatFileSize(record.fileSizeBytes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Actions Row (Download, Share, Delete)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                                    .format(Date(record.timestamp))
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )

                                Row {
                                    IconButton(onClick = { viewModel.exportAudio(record) }) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "تحميل على الهاتف",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(onClick = { viewModel.shareAudio(record) }) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "مشاركة",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(onClick = { recordToDelete = record }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    recordToDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل أنت متأكد من حذف هذا التسجيل الصوتي نهائياً؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecord(record)
                        recordToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalSec = millis / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}

private fun formatFileSize(bytes: Long): String {
    return if (bytes >= 1024 * 1024) {
        String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
    } else {
        String.format("%d KB", bytes / 1024)
    }
}
