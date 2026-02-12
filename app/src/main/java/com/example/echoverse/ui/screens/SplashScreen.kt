package com.example.echoverse.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.example.echoverse.R
import com.example.echoverse.ui.components.EchoScreen
import com.example.echoverse.ui.components.PulsingOrb
import com.example.echoverse.ui.theme.AccentPrimary
import com.example.echoverse.ui.theme.AccentSecondary

@Composable
fun SplashScreen(onNavigateToHome: () -> Unit) {
    var isLogoVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        isLogoVisible = true
        delay(2700)
        onNavigateToHome()
    }

    EchoScreen {
        // Center Content
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            // Pulse Backing
            PulsingOrb(
                size = 300.dp,
                color = AccentPrimary.copy(alpha = 0.2f),
                durationMillis = 1500
            )

            AnimatedVisibility(
                visible = isLogoVisible,
                enter = androidx.compose.animation.fadeIn(tween(1000)) + androidx.compose.animation.scaleIn(tween(1500))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Logo Icon (using launcher as placeholder if no SVG)
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Logo",
                        modifier = Modifier.size(120.dp),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "EchoVerse",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White
                    )
                    Text(
                        text = "See Your Sound",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
