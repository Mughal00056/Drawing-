package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.SymmetryMode
import com.example.overlay.ScreenDrawOverlayService
import com.example.ui.components.BackgroundSelectorDialog
import com.example.ui.components.ColorPickerDialog
import com.example.ui.components.DrawingBottomDock
import com.example.ui.components.DrawingCanvas
import com.example.ui.components.DrawingTopBar
import com.example.ui.components.ShapeSelectorDialog
import com.example.ui.components.StampSelectorDialog
import com.example.ui.viewmodel.DrawingEvent
import com.example.ui.viewmodel.DrawingViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DrawingScreen(
    viewModel: DrawingViewModel = viewModel(),
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val livePoints by viewModel.currentStrokePoints.collectAsState()
    val liveShape by viewModel.liveShape.collectAsState()

    var showShapeDialog by remember { mutableStateOf(false) }
    var showStampDialog by remember { mutableStateOf(false) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    val isOverlayActive by com.example.overlay.ScreenOverlayManager.isRunningFlow.collectAsState()

    val view = LocalView.current

    // Listen to ViewModel events (Toasts, Share intents)
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is DrawingEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is DrawingEvent.ShareImage -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Drawing"))
                }
            }
        }
    }

    // Toggle screen draw overlay
    fun toggleScreenOverlay() {
        if (ScreenDrawOverlayService.isRunning) {
            ScreenDrawOverlayService.stop(context)
            Toast.makeText(context, "Floating Screen Pen stopped", Toast.LENGTH_SHORT).show()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                showOverlayPermissionDialog = true
            } else {
                ScreenDrawOverlayService.start(context)
                Toast.makeText(
                    context,
                    "Floating Pen Active! Minimize to draw over any app or home screen.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // BackHandler: If in Zen mode, back exits Zen mode; if has drawings, can undo
    BackHandler(enabled = uiState.isZenMode || viewModel.canUndo) {
        if (uiState.isZenMode) {
            viewModel.toggleZenMode()
        } else if (viewModel.canUndo) {
            viewModel.undo()
        }
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("drawing_screen_root")
    ) {
        // Main Drawing Canvas (underneath all toolbars, full-bleed)
        DrawingCanvas(
            uiState = uiState,
            currentPoints = livePoints,
            liveShape = liveShape,
            onTouchStart = { offset -> viewModel.onTouchStart(offset) },
            onTouchMove = { offset -> viewModel.onTouchMove(offset) },
            onTouchEnd = { size -> viewModel.onTouchEnd(size) },
            modifier = Modifier.fillMaxSize()
        )

        // Top Bar (Animated hide in Zen mode)
        AnimatedVisibility(
            visible = !uiState.isZenMode,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(modifier = Modifier.padding(top = statusBarPadding.calculateTopPadding())) {
                DrawingTopBar(
                    uiState = uiState,
                    canUndo = viewModel.canUndo,
                    canRedo = viewModel.canRedo,
                    isOverlayActive = isOverlayActive,
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    onClear = { viewModel.showClearConfirmDialog(true) },
                    onCycleSymmetry = {
                        val nextMode = when (uiState.symmetryMode) {
                            SymmetryMode.OFF -> SymmetryMode.HORIZONTAL
                            SymmetryMode.HORIZONTAL -> SymmetryMode.VERTICAL
                            SymmetryMode.VERTICAL -> SymmetryMode.QUAD
                            SymmetryMode.QUAD -> SymmetryMode.OFF
                        }
                        viewModel.setSymmetryMode(nextMode)
                        Toast.makeText(context, "Symmetry: ${nextMode.title}", Toast.LENGTH_SHORT).show()
                    },
                    onOpenBgPicker = { viewModel.showBgPickerDialog(true) },
                    onSave = {
                        val w = view.width.takeIf { it > 0 } ?: 1080
                        val h = view.height.takeIf { it > 0 } ?: 1920
                        viewModel.saveToGallery(context, w, h)
                    },
                    onShare = {
                        val w = view.width.takeIf { it > 0 } ?: 1080
                        val h = view.height.takeIf { it > 0 } ?: 1920
                        viewModel.shareDrawing(context, w, h)
                    },
                    onToggleScreenOverlay = { toggleScreenOverlay() },
                    onOpenSettings = onOpenSettings,
                    onToggleZenMode = { viewModel.toggleZenMode() }
                )
            }
        }

        // Floating Bottom Dock (Animated hide in Zen mode)
        AnimatedVisibility(
            visible = !uiState.isZenMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(modifier = Modifier.padding(bottom = navBarPadding.calculateBottomPadding() + 4.dp)) {
                DrawingBottomDock(
                    uiState = uiState,
                    onSelectBrush = { brush -> viewModel.selectBrush(brush) },
                    onOpenShapeSelector = { showShapeDialog = true },
                    onOpenStampSelector = { showStampDialog = true },
                    onSelectColor = { color -> viewModel.setColor(color) },
                    onOpenColorPicker = { viewModel.showColorPickerDialog(true) },
                    onStrokeWidthChange = { width -> viewModel.setStrokeWidth(width) },
                    onOpacityChange = { alpha -> viewModel.setOpacity(alpha) }
                )
            }
        }

        // Zen Mode Floating Restore Button
        AnimatedVisibility(
            visible = uiState.isZenMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = statusBarPadding.calculateTopPadding() + 12.dp,
                    end = 16.dp
                )
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { viewModel.toggleZenMode() }
                    .shadow(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloseFullscreen,
                        contentDescription = "Exit Fullscreen",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Show Controls",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    // Dialogs
    if (uiState.showColorPicker) {
        ColorPickerDialog(
            currentColor = uiState.currentColor,
            onColorSelected = { color -> viewModel.setColor(color) },
            onDismiss = { viewModel.showColorPickerDialog(false) }
        )
    }

    if (uiState.showBgPicker) {
        BackgroundSelectorDialog(
            currentBackground = uiState.background,
            onSelectBackground = { bg -> viewModel.setBackground(bg) },
            onDismiss = { viewModel.showBgPickerDialog(false) }
        )
    }

    if (showShapeDialog) {
        ShapeSelectorDialog(
            selectedShape = uiState.selectedShape,
            isFilled = uiState.isShapeFilled,
            onSelectShape = { shape ->
                viewModel.selectShape(shape)
                showShapeDialog = false
            },
            onToggleFilled = { viewModel.toggleShapeFilled() },
            onDismiss = { showShapeDialog = false }
        )
    }

    if (showStampDialog) {
        StampSelectorDialog(
            selectedStamp = uiState.selectedStamp,
            onSelectStamp = { stamp ->
                viewModel.selectStamp(stamp)
                showStampDialog = false
            },
            onDismiss = { showStampDialog = false }
        )
    }

    if (uiState.showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.showClearConfirmDialog(false) },
            title = { Text("Clear Canvas?") },
            text = { Text("This will erase all strokes and shapes currently drawn.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearCanvas() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showClearConfirmDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Overlay Permission Explanation Dialog
    if (showOverlayPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayPermissionDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SmartDisplay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Screen Draw Permission")
                }
            },
            text = {
                Text(
                    text = "Phone ki screen par aur doosri apps (WhatsApp, YouTube, etc.) ke upar draw karne ke liye 'Display over other apps' permission zaroori hai.\n\n'Open Settings' par tap karke allow karein."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverlayPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                context.startActivity(intent)
                            }
                        }
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
