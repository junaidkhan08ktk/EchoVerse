package com.example.echoverse.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.echoverse.data.preferences.UserPreferences
import com.example.echoverse.ui.components.*
import com.example.echoverse.ui.theme.*

@Composable
fun CustomizationScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    
    // UI State
    var sensitivity by remember { mutableFloatStateOf(prefs.sensitivity) }
    var calmFactor by remember { mutableFloatStateOf(prefs.calmFactor) }
    var isMicEnabled by remember { mutableStateOf(prefs.isMicEnabled) }
    var colorPalette by remember { mutableIntStateOf(prefs.colorPalette) }

    // Permission
    val hasAudioPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasAudioPermission.value = isGranted
    }

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
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Customize World", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            }

            // Controls
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                // Mic Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Audio Reactivity", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(if (isMicEnabled) "Reacting to sound" else "Simulated movement", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha=0.6f))
                    }
                    Switch(
                        checked = isMicEnabled,
                        onCheckedChange = { 
                            if (it && !hasAudioPermission.value) {
                                launcher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                isMicEnabled = it
                                prefs.isMicEnabled = it
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentPrimary,
                            checkedTrackColor = AccentPrimary.copy(alpha=0.5f)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Sensitivity
                Text("Sensitivity", style = MaterialTheme.typography.bodyMedium, color = AccentSecondary)
                Slider(
                    value = sensitivity,
                    onValueChange = { 
                        sensitivity = it
                        prefs.sensitivity = it
                    },
                    valueRange = 0.1f..3.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentSecondary,
                        activeTrackColor = AccentSecondary,
                        inactiveTrackColor = Color.White.copy(alpha=0.2f)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Calm/Energy
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                     Text("Calm", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha=0.7f))
                     Text("Energetic", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha=0.7f))
                }
                Slider(
                    value = calmFactor,
                    onValueChange = { 
                        calmFactor = it
                        prefs.calmFactor = it
                    },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentPrimary,
                        activeTrackColor = AccentPrimary,
                        inactiveTrackColor = Color.White.copy(alpha=0.2f)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Color Palette
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("Color Core", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("Deep", "Neon", "Pastel").forEachIndexed { index, name ->
                         val isSelected = colorPalette == index
                         FilterChip(
                            selected = isSelected,
                            onClick = { 
                                colorPalette = index 
                                prefs.colorPalette = index
                            },
                            label = { Text(name) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White.copy(alpha=0.05f),
                                labelColor = Color.White,
                                selectedContainerColor = AccentPrimary,
                                selectedLabelColor = Color.Black
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color.White.copy(alpha=0.3f),
                                selectedBorderColor = AccentPrimary,
                            )
                        )
                    }
                }
            }
        }
    }
}
