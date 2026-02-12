package com.example.echoverse

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.echoverse.presentation.service.EchoWallpaperService
import com.example.echoverse.ui.screens.*
import com.example.echoverse.ui.theme.EchoVerseTheme
//import com.google.accompanist.systemuicontroller.rememberSystemUiController // Ideally use this, but edgeToEdge handles most

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            EchoVerseTheme {
                val navController = rememberNavController()
                
                NavHost(navController = navController, startDestination = "splash") {
                    composable("splash") {
                        SplashScreen(
                            onNavigateToHome = {
                                // Check if onboarding done (mocked as false on first run)
                                val onboardingDone = false // TODO: Persist this
                                if (onboardingDone) {
                                    navController.navigate("home") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("onboarding") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                    
                    composable("onboarding") {
                        OnboardingScreen(
                            onFinish = {
                                navController.navigate("home") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        )
                    }
                    
                    composable("home") {
                        HomeScreen(
                            onWorldSelected = { world ->
                                navController.navigate("preview/${world.id}")
                            },
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            },
                            onNavigateToPremium = {
                                // TODO
                            }
                        )
                    }
                    
                    composable(
                        "preview/{worldId}",
                        arguments = listOf(navArgument("worldId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val worldId = backStackEntry.arguments?.getString("worldId") ?: "1"
                        val context = LocalContext.current
                        
                        val prefs = remember { com.example.echoverse.data.preferences.UserPreferences(context) }
                        PreviewScreen(
                            worldId = worldId,
                            onApply = {
                                prefs.selectedWorldId = worldId
                                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                    putExtra(
                                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                        ComponentName(context, EchoWallpaperService::class.java)
                                    )
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Open Settings to apply wallpaper", Toast.LENGTH_LONG).show()
                                }
                            },
                            onCustomize = {
                                navController.navigate("customize")
                            },
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                    
                    composable("customize") {
                        CustomizationScreen(onBack = { navController.popBackStack() })
                    }
                    
                    composable("settings") {
                        SettingsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}