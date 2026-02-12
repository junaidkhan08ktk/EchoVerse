package com.example.echoverse.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.echoverse.domain.model.WorldCategory
import com.example.echoverse.ui.components.*
import com.example.echoverse.ui.models.*
import com.example.echoverse.ui.theme.*
import java.util.Locale

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onWorldSelected: (WorldUiModel) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPremium: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf<WorldCategory?>(null) }
    val scrollState = rememberLazyGridState()

    EchoScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "EchoVerse", style = MaterialTheme.typography.displaySmall)
                    Text(text = "Choose your world", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.5f))
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }

            // Filter Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp).horizontalScroll(rememberScrollState())
            ) {
                // "All" chip
                val isAllSelected = selectedFilter == null
                FilterChip(
                    selected = isAllSelected,
                    onClick = { selectedFilter = null },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SurfaceGlass,
                        labelColor = Color.White,
                        selectedContainerColor = AccentPrimary,
                        selectedLabelColor = Color.Black
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isAllSelected,
                        borderColor = Color.White.copy(alpha = 0.2f),
                        selectedBorderColor = AccentPrimary
                    )
                )

                WorldCategory.entries.forEach { category ->
                    val isSelected = selectedFilter == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = category },
                        label = { Text(category.name.lowercase()
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SurfaceGlass,
                            labelColor = Color.White,
                            selectedContainerColor = AccentPrimary,
                            selectedLabelColor = Color.Black
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color.White.copy(alpha = 0.2f),
                            selectedBorderColor = AccentPrimary
                        )
                    )
                }
            }

            // World Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                state = scrollState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(SampleWorlds.filter { 
                    selectedFilter == null || it.category == selectedFilter
                }) { world ->
                    WorldCard(
                        world = world,
                        onClick = { onWorldSelected(world) }
                    )
                }
                // Spacer at bottom
                item { Spacer(modifier = Modifier.height(64.dp)) }
            }
        }
    }
}

@Composable
fun WorldCard(
    world: WorldUiModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(0.8f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        border = androidx.compose.foundation.BorderStroke(1.dp, Brush.verticalGradient(listOf(Color(0x20FFFFFF), Color(0x05FFFFFF))))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Live Preview Placeholder (Simulated)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(world.thumbnailColor.copy(alpha=0.6f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset.Unspecified, 
                            radius = Float.POSITIVE_INFINITY
                        )
                    )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha=0.8f))))
                    .fillMaxWidth()
            ) {
                Text(text = world.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(text = world.category.name, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha=0.7f))
            }

            // Premium Badge
            if (world.isPremium) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(AccentSecondary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("PRO", style = MaterialTheme.typography.labelSmall, color = Color.Black)
                }
            }
        }
    }
}
