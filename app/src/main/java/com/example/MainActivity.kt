package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.screens.ConvertScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.NatiqTheme

enum class Screen(val title: String) {
    CONVERT("التحويل"),
    LIBRARY("المكتبة"),
    SETTINGS("الإعدادات")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NatiqTheme {
                val appLanguage by viewModel.appLanguage.collectAsState()
                val layoutDirection = if (appLanguage == "en") LayoutDirection.Ltr else LayoutDirection.Rtl

                // Request notification permission on Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { /* Handled gracefully */ }

                    LaunchedEffect(Unit) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    var currentScreen by remember { mutableStateOf(Screen.CONVERT) }

                    // Hardware back button navigation
                    BackHandler(enabled = currentScreen != Screen.CONVERT) {
                        currentScreen = Screen.CONVERT
                    }

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("main_scaffold"),
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RecordVoiceOver,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "ناطق",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        SuggestionChip(
                                            onClick = { currentScreen = Screen.SETTINGS },
                                            label = { Text("Gemini TTS", fontSize = 10.sp) },
                                            modifier = Modifier.height(24.dp)
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp,
                                modifier = Modifier.testTag("bottom_nav_bar")
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen == Screen.CONVERT,
                                    onClick = { currentScreen = Screen.CONVERT },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.RecordVoiceOver,
                                            contentDescription = "التحويل"
                                        )
                                    },
                                    label = { Text("التحويل") },
                                    modifier = Modifier.testTag("nav_convert_tab")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == Screen.LIBRARY,
                                    onClick = { currentScreen = Screen.LIBRARY },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.LibraryMusic,
                                            contentDescription = "المكتبة"
                                        )
                                    },
                                    label = { Text("المكتبة") },
                                    modifier = Modifier.testTag("nav_library_tab")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == Screen.SETTINGS,
                                    onClick = { currentScreen = Screen.SETTINGS },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "الإعدادات"
                                        )
                                    },
                                    label = { Text("الإعدادات") },
                                    modifier = Modifier.testTag("nav_settings_tab")
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentScreen) {
                                Screen.CONVERT -> ConvertScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Screen.LIBRARY -> LibraryScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Screen.SETTINGS -> SettingsScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
