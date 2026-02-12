package com.example.echoverse.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.echoverse.ui.components.EchoButton
import com.example.echoverse.ui.components.EchoScreen
import com.example.echoverse.ui.components.GlassPanel
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: androidx.compose.material.icons.Icons.Filled? = null // Placeholder for assets in future
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            "See Your Sound",
            "Transform your music into a living, breathing visual world."
        ),
        OnboardingPage(
            "Touch. Play. Feel.",
            "Interact with your wallpaper. Every touch creates a ripple in the universe."
        ),
        OnboardingPage(
            "Make Your Phone Alive",
            "A premium audio-visual experience that evolves with your day."
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    EchoScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Carousel
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(4f)
            ) { pageIndex ->
                val page = pages[pageIndex]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Placeholder for big illustration
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                         Text(
                             text = "${pageIndex + 1}",
                             style = MaterialTheme.typography.displayLarge,
                             color = Color.White.copy(alpha = 0.2f)
                         )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = page.title,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = page.description,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Indicators
            Row(
                Modifier
                    .height(50.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.2f)
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(if (pagerState.currentPage == iteration) 12.dp else 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(onClick = onFinish) {
                        Text("Skip", color = Color.White.copy(alpha = 0.5f))
                    }
                    EchoButton(
                        text = "Next",
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier.width(140.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.width(10.dp)) // spacer for alignment
                    EchoButton(
                        text = "Get Started",
                        onClick = onFinish,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
