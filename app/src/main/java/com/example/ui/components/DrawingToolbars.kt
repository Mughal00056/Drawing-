package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrushType
import com.example.model.CanvasBackground
import com.example.model.SymmetryMode
import com.example.ui.viewmodel.DrawingUiState

@Composable
fun DrawingTopBar(
    uiState: DrawingUiState,
    canUndo: Boolean,
    canRedo: Boolean,
    isOverlayActive: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    onCycleSymmetry: () -> Unit,
    onOpenBgPicker: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onToggleScreenOverlay: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleZenMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Branding Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF6366F1), Color(0xFFEC4899))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Brush,
                        contentDescription = "Tap & Draw",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tap & Draw",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Undo
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.testTag("undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                // Redo
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.testTag("redo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                // Symmetry Toggle
                IconButton(
                    onClick = onCycleSymmetry,
                    modifier = Modifier.testTag("symmetry_button")
                ) {
                    val isActive = uiState.symmetryMode != SymmetryMode.OFF
                    Icon(
                        imageVector = Icons.Default.Flip,
                        contentDescription = "Symmetry: ${uiState.symmetryMode.title}",
                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Background Theme Picker
                IconButton(
                    onClick = onOpenBgPicker,
                    modifier = Modifier.testTag("background_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Canvas Background",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Clear
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.testTag("clear_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear Canvas",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                // Save
                IconButton(
                    onClick = onSave,
                    modifier = Modifier.testTag("save_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SaveAlt,
                        contentDescription = "Save Drawing",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Share
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.testTag("share_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                // Floating Screen Pen (Draw Over Any App)
                IconButton(
                    onClick = onToggleScreenOverlay,
                    modifier = Modifier.testTag("floating_pen_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartDisplay,
                        contentDescription = if (isOverlayActive) "Stop Screen Draw" else "Draw Over Other Apps",
                        tint = if (isOverlayActive) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                    )
                }

                // Zen Full Screen
                IconButton(
                    onClick = onToggleZenMode,
                    modifier = Modifier.testTag("zen_mode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FitScreen,
                        contentDescription = "Zen Full Screen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Screen Draw & Floating Settings
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("settings_screen_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Screen Draw Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DrawingBottomDock(
    uiState: DrawingUiState,
    onSelectBrush: (BrushType) -> Unit,
    onOpenShapeSelector: () -> Unit,
    onOpenStampSelector: () -> Unit,
    onSelectColor: (Color) -> Unit,
    onOpenColorPicker: () -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSliders by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Expandable Brush Width & Opacity Controls
            AnimatedVisibility(visible = showSliders) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    // Stroke size slider with live preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Size",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(52.dp)
                        )
                        Slider(
                            value = uiState.strokeWidth,
                            onValueChange = onStrokeWidthChange,
                            valueRange = 2f..70f,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stroke_slider")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Preview circle
                        Box(
                            modifier = Modifier
                                .size(34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(uiState.strokeWidth.coerceIn(4f, 32f).dp)
                                    .clip(CircleShape)
                                    .background(uiState.currentColor)
                            )
                        }
                    }

                    // Opacity slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Opacity",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(52.dp)
                        )
                        Slider(
                            value = uiState.opacity,
                            onValueChange = onOpacityChange,
                            valueRange = 0.1f..1f,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("opacity_slider")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(uiState.opacity * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(34.dp)
                        )
                    }
                }
            }

            // Quick Color Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sliders toggle button
                IconButton(
                    onClick = { showSliders = !showSliders },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("slider_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Adjust brush size and opacity",
                        tint = if (showSliders) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Scrollable Color Chips
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.recentColors.forEach { color ->
                        val isSelected = color == uiState.currentColor && uiState.currentBrush != BrushType.ERASER
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0x33000000),
                                    shape = CircleShape
                                )
                                .clickable {
                                    onSelectColor(color)
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Custom Color Picker Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onOpenColorPicker() }
                        .testTag("open_color_picker_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ColorLens,
                        contentDescription = "Pick custom color",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tool Switcher Row: Pen, Neon, Marker, Rainbow, Shapes, Stamp, Eraser
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pen
                ToolChip(
                    label = "Pen",
                    icon = Icons.Default.Edit,
                    isSelected = uiState.currentBrush == BrushType.PEN && uiState.selectedShape == null && uiState.selectedStamp == null,
                    onClick = { onSelectBrush(BrushType.PEN) }
                )

                // Neon / Glow
                ToolChip(
                    label = "Neon Glow",
                    icon = Icons.Default.AutoAwesome,
                    isSelected = uiState.currentBrush == BrushType.NEON && uiState.selectedShape == null && uiState.selectedStamp == null,
                    accentColor = Color(0xFF00F5FF),
                    onClick = { onSelectBrush(BrushType.NEON) }
                )

                // Marker
                ToolChip(
                    label = "Marker",
                    icon = Icons.Default.Highlight,
                    isSelected = uiState.currentBrush == BrushType.MARKER && uiState.selectedShape == null && uiState.selectedStamp == null,
                    onClick = { onSelectBrush(BrushType.MARKER) }
                )

                // Rainbow
                ToolChip(
                    label = "Rainbow",
                    icon = Icons.Default.Brush,
                    isSelected = uiState.currentBrush == BrushType.RAINBOW && uiState.selectedShape == null && uiState.selectedStamp == null,
                    gradient = Brush.linearGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Magenta)),
                    onClick = { onSelectBrush(BrushType.RAINBOW) }
                )

                // Shapes
                ToolChip(
                    label = if (uiState.selectedShape != null) uiState.selectedShape.label else "Shapes",
                    icon = Icons.Outlined.Category,
                    isSelected = uiState.selectedShape != null,
                    onClick = onOpenShapeSelector
                )

                // Stamps
                ToolChip(
                    label = if (uiState.selectedStamp != null) uiState.selectedStamp.symbol else "Stamps",
                    icon = Icons.Default.Mood,
                    isSelected = uiState.selectedStamp != null,
                    onClick = onOpenStampSelector
                )

                // Eraser
                ToolChip(
                    label = "Eraser",
                    icon = Icons.Default.DeleteOutline,
                    isSelected = uiState.currentBrush == BrushType.ERASER && uiState.selectedShape == null && uiState.selectedStamp == null,
                    accentColor = MaterialTheme.colorScheme.error,
                    onClick = { onSelectBrush(BrushType.ERASER) }
                )
            }
        }
    }
}

@Composable
private fun ToolChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color? = null,
    gradient: Brush? = null
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (gradient != null) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(gradient)
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor ?: contentColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
