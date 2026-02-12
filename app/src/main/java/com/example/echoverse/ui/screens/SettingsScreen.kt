package com.example.echoverse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.echoverse.ui.components.EchoScreen
import com.example.echoverse.ui.components.GlassPanel
import com.example.echoverse.ui.theme.*

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var batterySaver by remember { mutableStateOf(false) }
    var performanceMode by remember { mutableStateOf(true) }
    var showFps by remember { mutableStateOf(false) }

    EchoScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Settings", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            }

            // General Settings
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("Performance", style = MaterialTheme.typography.titleMedium, color = AccentSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                
                SettingsSwitch(
                    title = "Battery Saver Mode",
                    subtitle = "Reduce frame rate when battery is low",
                    checked = batterySaver,
                    onCheckedChange = { batterySaver = it }
                )
                
                SettingsSwitch(
                    title = "High Performance",
                    subtitle = "Unlock 60fps rendering",
                    checked = performanceMode,
                    onCheckedChange = { performanceMode = it }
                )
                
                SettingsSwitch(
                    title = "Show FPS",
                    subtitle = "Display frame rate counter",
                    checked = showFps,
                    onCheckedChange = { showFps = it }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("About", style = MaterialTheme.typography.titleMedium, color = AccentSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "EchoVerse v1.0.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Designed for immersion. No audio is ever recorded or saved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = Color.White)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentPrimary,
                checkedTrackColor = AccentPrimary.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}
