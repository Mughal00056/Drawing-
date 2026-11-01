package com.example.ui.viewmodel

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.BrushType
import com.example.model.CanvasBackground
import com.example.model.CanvasItem
import com.example.model.DrawingPoint
import com.example.model.ShapeType
import com.example.model.StampType
import com.example.model.SymmetryMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * UI State for the Drawing Application
 */
data class DrawingUiState(
    val items: List<CanvasItem> = emptyList(),
    val redoStack: List<CanvasItem> = emptyList(),
    val currentBrush: BrushType = BrushType.PEN,
    val selectedShape: ShapeType? = null,
    val selectedStamp: StampType? = null,
    val currentColor: Color = Color(0xFF6366F1), // Modern Indigo/Purple
    val strokeWidth: Float = 14f,
    val opacity: Float = 1.0f,
    val background: CanvasBackground = CanvasBackground.CLEAN_WHITE,
    val symmetryMode: SymmetryMode = SymmetryMode.OFF,
    val isShapeFilled: Boolean = false,
    val isZenMode: Boolean = false,
    val showColorPicker: Boolean = false,
    val showBgPicker: Boolean = false,
    val showClearConfirmation: Boolean = false,
    val recentColors: List<Color> = listOf(
        Color(0xFF0F172A), // Dark slate
        Color(0xFFEF4444), // Vibrant Red
        Color(0xFFF97316), // Orange
        Color(0xFFEAB308), // Yellow
        Color(0xFF10B981), // Emerald Green
        Color(0xFF06B6D4), // Cyan
        Color(0xFF3B82F6), // Blue
        Color(0xFF6366F1), // Indigo
        Color(0xFF8B5CF6), // Violet
        Color(0xFFEC4899), // Pink
        Color(0xFFF43F5E), // Rose
        Color(0xFFFFFFFF)  // White
    )
)

sealed class DrawingEvent {
    data class ShowToast(val message: String) : DrawingEvent()
    data class ShareImage(val uri: Uri) : DrawingEvent()
}

class DrawingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DrawingUiState())
    val uiState: StateFlow<DrawingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DrawingEvent>()
    val events: SharedFlow<DrawingEvent> = _events.asSharedFlow()

    // Real-time in-progress stroke / drag
    private val _currentStrokePoints = MutableStateFlow<List<DrawingPoint>>(emptyList())
    val currentStrokePoints: StateFlow<List<DrawingPoint>> = _currentStrokePoints.asStateFlow()

    private val _liveShape = MutableStateFlow<CanvasItem.ShapeItem?>(null)
    val liveShape: StateFlow<CanvasItem.ShapeItem?> = _liveShape.asStateFlow()

    // Rainbow hue progress tracker
    private var rainbowHue = 0f

    val canUndo: Boolean
        get() = _uiState.value.items.isNotEmpty()

    val canRedo: Boolean
        get() = _uiState.value.redoStack.isNotEmpty()

    fun selectBrush(brush: BrushType) {
        _uiState.update {
            it.copy(
                currentBrush = brush,
                selectedShape = null,
                selectedStamp = null
            )
        }
    }

    fun selectShape(shape: ShapeType) {
        _uiState.update {
            it.copy(
                selectedShape = shape,
                selectedStamp = null
            )
        }
    }

    fun selectStamp(stamp: StampType) {
        _uiState.update {
            it.copy(
                selectedStamp = stamp,
                selectedShape = null
            )
        }
    }

    fun setColor(color: Color) {
        _uiState.update { state ->
            val updatedRecents = (listOf(color) + state.recentColors.filter { it != color }).take(14)
            state.copy(
                currentColor = color,
                recentColors = updatedRecents
            )
        }
    }

    fun setStrokeWidth(width: Float) {
        _uiState.update { it.copy(strokeWidth = width.coerceIn(2f, 80f)) }
    }

    fun setOpacity(alpha: Float) {
        _uiState.update { it.copy(opacity = alpha.coerceIn(0.05f, 1.0f)) }
    }

    fun setBackground(bg: CanvasBackground) {
        _uiState.update { it.copy(background = bg, showBgPicker = false) }
    }

    fun setSymmetryMode(mode: SymmetryMode) {
        _uiState.update { it.copy(symmetryMode = mode) }
    }

    fun toggleShapeFilled() {
        _uiState.update { it.copy(isShapeFilled = !it.isShapeFilled) }
    }

    fun toggleZenMode() {
        _uiState.update { it.copy(isZenMode = !it.isZenMode) }
    }

    fun showColorPickerDialog(show: Boolean) {
        _uiState.update { it.copy(showColorPicker = show) }
    }

    fun showBgPickerDialog(show: Boolean) {
        _uiState.update { it.copy(showBgPicker = show) }
    }

    fun showClearConfirmDialog(show: Boolean) {
        _uiState.update { it.copy(showClearConfirmation = show) }
    }

    /**
     * Touch down or initial tap event
     */
    fun onTouchStart(offset: Offset) {
        val state = _uiState.value
        if (state.selectedStamp != null) {
            // Instant Stamp placement on tap
            addStamp(offset, state.selectedStamp, state.currentColor, state.strokeWidth * 3.5f)
            return
        }

        if (state.selectedShape != null) {
            _liveShape.value = CanvasItem.ShapeItem(
                shapeType = state.selectedShape,
                start = offset,
                end = offset,
                color = state.currentColor,
                strokeWidth = state.strokeWidth,
                isFilled = state.isShapeFilled,
                alpha = state.opacity
            )
            return
        }

        // Freehand path: initialize point list
        val pointColor = if (state.currentBrush == BrushType.RAINBOW) {
            rainbowHue = (rainbowHue + 12f) % 360f
            Color.hsv(rainbowHue, 0.9f, 0.98f)
        } else {
            state.currentColor
        }
        _currentStrokePoints.value = listOf(DrawingPoint(offset, pointColor))
    }

    /**
     * Pointer drag move event
     */
    fun onTouchMove(offset: Offset) {
        val state = _uiState.value
        if (state.selectedStamp != null) return

        if (state.selectedShape != null) {
            _liveShape.update { current ->
                current?.copy(end = offset)
            }
            return
        }

        // Freehand stroke accumulation
        val pointColor = if (state.currentBrush == BrushType.RAINBOW) {
            rainbowHue = (rainbowHue + 5f) % 360f
            Color.hsv(rainbowHue, 0.9f, 0.98f)
        } else {
            state.currentColor
        }

        _currentStrokePoints.update { points ->
            points + DrawingPoint(offset, pointColor)
        }
    }

    /**
     * Touch up / stroke finished event
     */
    fun onTouchEnd(canvasSize: Size) {
        val state = _uiState.value

        val activeShape = _liveShape.value
        if (activeShape != null) {
            commitShape(activeShape, canvasSize, state.symmetryMode)
            _liveShape.value = null
            return
        }

        val points = _currentStrokePoints.value
        if (points.isNotEmpty()) {
            commitPath(points, canvasSize, state)
            _currentStrokePoints.value = emptyList()
        }
    }

    private fun commitPath(
        points: List<DrawingPoint>,
        canvasSize: Size,
        state: DrawingUiState
    ) {
        val newItems = mutableListOf<CanvasItem>()
        val symmetry = state.symmetryMode

        // Helper to mirror a list of points
        fun createSymmetricPath(transform: (Offset) -> Offset): CanvasItem.PathItem {
            val transformedPoints = points.map { pt ->
                pt.copy(offset = transform(pt.offset))
            }
            return CanvasItem.PathItem(
                points = transformedPoints,
                color = state.currentColor,
                strokeWidth = state.strokeWidth,
                brushType = state.currentBrush,
                alpha = if (state.currentBrush == BrushType.MARKER) 0.42f else state.opacity
            )
        }

        // Original primary path
        val primaryPath = CanvasItem.PathItem(
            points = points,
            color = state.currentColor,
            strokeWidth = state.strokeWidth,
            brushType = state.currentBrush,
            alpha = if (state.currentBrush == BrushType.MARKER) 0.42f else state.opacity
        )
        newItems.add(primaryPath)

        // Handle symmetry transformations
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            val width = canvasSize.width
            val height = canvasSize.height

            when (symmetry) {
                SymmetryMode.HORIZONTAL -> {
                    newItems.add(createSymmetricPath { Offset(width - it.x, it.y) })
                }
                SymmetryMode.VERTICAL -> {
                    newItems.add(createSymmetricPath { Offset(it.x, height - it.y) })
                }
                SymmetryMode.QUAD -> {
                    newItems.add(createSymmetricPath { Offset(width - it.x, it.y) })
                    newItems.add(createSymmetricPath { Offset(it.x, height - it.y) })
                    newItems.add(createSymmetricPath { Offset(width - it.x, height - it.y) })
                }
                SymmetryMode.OFF -> {}
            }
        }

        _uiState.update {
            it.copy(
                items = it.items + newItems,
                redoStack = emptyList()
            )
        }
    }

    private fun commitShape(
        shape: CanvasItem.ShapeItem,
        canvasSize: Size,
        symmetry: SymmetryMode
    ) {
        val newItems = mutableListOf<CanvasItem>(shape)
        if (canvasSize.width > 0 && canvasSize.height > 0 && symmetry != SymmetryMode.OFF) {
            val w = canvasSize.width
            val h = canvasSize.height
            when (symmetry) {
                SymmetryMode.HORIZONTAL -> {
                    newItems.add(shape.copy(
                        start = Offset(w - shape.start.x, shape.start.y),
                        end = Offset(w - shape.end.x, shape.end.y)
                    ))
                }
                SymmetryMode.VERTICAL -> {
                    newItems.add(shape.copy(
                        start = Offset(shape.start.x, h - shape.start.y),
                        end = Offset(shape.end.x, h - shape.end.y)
                    ))
                }
                SymmetryMode.QUAD -> {
                    newItems.add(shape.copy(
                        start = Offset(w - shape.start.x, shape.start.y),
                        end = Offset(w - shape.end.x, shape.end.y)
                    ))
                    newItems.add(shape.copy(
                        start = Offset(shape.start.x, h - shape.start.y),
                        end = Offset(shape.end.x, h - shape.end.y)
                    ))
                    newItems.add(shape.copy(
                        start = Offset(w - shape.start.x, h - shape.start.y),
                        end = Offset(w - shape.end.x, h - shape.end.y)
                    ))
                }
                SymmetryMode.OFF -> {}
            }
        }

        _uiState.update {
            it.copy(
                items = it.items + newItems,
                redoStack = emptyList()
            )
        }
    }

    private fun addStamp(center: Offset, stamp: StampType, color: Color, size: Float) {
        val item = CanvasItem.StampItem(
            stampType = stamp,
            center = center,
            size = size,
            color = color
        )
        _uiState.update {
            it.copy(
                items = it.items + item,
                redoStack = emptyList()
            )
        }
    }

    fun undo() {
        _uiState.update { state ->
            if (state.items.isEmpty()) return@update state
            val lastItem = state.items.last()
            state.copy(
                items = state.items.dropLast(1),
                redoStack = state.redoStack + lastItem
            )
        }
    }

    fun redo() {
        _uiState.update { state ->
            if (state.redoStack.isEmpty()) return@update state
            val itemToRestore = state.redoStack.last()
            state.copy(
                items = state.items + itemToRestore,
                redoStack = state.redoStack.dropLast(1)
            )
        }
    }

    fun clearCanvas() {
        _uiState.update { state ->
            state.copy(
                items = emptyList(),
                redoStack = emptyList(),
                showClearConfirmation = false
            )
        }
    }

    /**
     * Render the drawing to a full-resolution bitmap
     */
    private fun renderToBitmap(width: Int, height: Int): Bitmap {
        val state = _uiState.value
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // Draw background
        canvas.drawColor(state.background.surfaceColor.toArgb())

        val paint = Paint().apply {
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (item in state.items) {
            when (item) {
                is CanvasItem.PathItem -> {
                    if (item.points.isEmpty()) continue

                    val itemColor = item.color.toArgb()
                    val strokeW = item.strokeWidth
                    val itemAlpha = (item.alpha * 255).toInt().coerceIn(0, 255)

                    if (item.brushType == BrushType.ERASER) {
                        // Blend mode clear or background color
                        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = strokeW
                    } else if (item.brushType == BrushType.NEON) {
                        paint.xfermode = null
                        // Outer glow pass
                        paint.style = Paint.Style.STROKE
                        paint.color = itemColor
                        paint.alpha = 70
                        paint.strokeWidth = strokeW * 2.2f
                        drawAndroidPath(canvas, item.points, paint)

                        // Inner core
                        paint.color = android.graphics.Color.WHITE
                        paint.alpha = 240
                        paint.strokeWidth = strokeW * 0.6f
                        drawAndroidPath(canvas, item.points, paint)
                        continue
                    } else {
                        paint.xfermode = null
                        paint.style = Paint.Style.STROKE
                        paint.color = itemColor
                        paint.alpha = itemAlpha
                        paint.strokeWidth = strokeW
                    }

                    drawAndroidPath(canvas, item.points, paint)
                }

                is CanvasItem.ShapeItem -> {
                    paint.xfermode = null
                    paint.color = item.color.toArgb()
                    paint.alpha = (item.alpha * 255).toInt().coerceIn(0, 255)
                    paint.strokeWidth = item.strokeWidth
                    paint.style = if (item.isFilled) Paint.Style.FILL else Paint.Style.STROKE

                    when (item.shapeType) {
                        ShapeType.LINE -> {
                            canvas.drawLine(item.start.x, item.start.y, item.end.x, item.end.y, paint)
                        }
                        ShapeType.RECTANGLE -> {
                            val left = min(item.start.x, item.end.x)
                            val top = min(item.start.y, item.end.y)
                            val right = maxOf(item.start.x, item.end.x)
                            val bottom = maxOf(item.start.y, item.end.y)
                            canvas.drawRect(left, top, right, bottom, paint)
                        }
                        ShapeType.CIRCLE -> {
                            val radius = hypot(item.end.x - item.start.x, item.end.y - item.start.y)
                            canvas.drawCircle(item.start.x, item.start.y, radius, paint)
                        }
                        ShapeType.ARROW -> {
                            // Line
                            canvas.drawLine(item.start.x, item.start.y, item.end.x, item.end.y, paint)
                            // Arrowhead
                            val angle = atan2((item.end.y - item.start.y).toDouble(), (item.end.x - item.start.x).toDouble())
                            val arrowLen = item.strokeWidth * 3.5f
                            val p1x = item.end.x - arrowLen * cos(angle - PI / 6).toFloat()
                            val p1y = item.end.y - arrowLen * sin(angle - PI / 6).toFloat()
                            val p2x = item.end.x - arrowLen * cos(angle + PI / 6).toFloat()
                            val p2y = item.end.y - arrowLen * sin(angle + PI / 6).toFloat()
                            canvas.drawLine(item.end.x, item.end.y, p1x, p1y, paint)
                            canvas.drawLine(item.end.x, item.end.y, p2x, p2y, paint)
                        }
                        ShapeType.STAR -> {
                            drawStar(canvas, item.start, item.end, paint)
                        }
                    }
                }

                is CanvasItem.StampItem -> {
                    paint.xfermode = null
                    paint.textSize = item.size
                    paint.textAlign = Paint.Align.CENTER
                    val text = item.stampType.symbol
                    canvas.drawText(text, item.center.x, item.center.y + item.size / 3f, paint)
                }
            }
        }

        return bitmap
    }

    private fun drawAndroidPath(canvas: android.graphics.Canvas, points: List<DrawingPoint>, paint: Paint) {
        if (points.isEmpty()) return
        if (points.size == 1) {
            val pt = points[0].offset
            canvas.drawCircle(pt.x, pt.y, paint.strokeWidth / 2f, paint)
            return
        }
        val path = android.graphics.Path()
        path.moveTo(points[0].offset.x, points[0].offset.y)
        for (i in 1 until points.size) {
            val prev = points[i - 1].offset
            val curr = points[i].offset
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            path.quadTo(prev.x, prev.y, midX, midY)
        }
        path.lineTo(points.last().offset.x, points.last().offset.y)
        canvas.drawPath(path, paint)
    }

    private fun drawStar(canvas: android.graphics.Canvas, start: Offset, end: Offset, paint: Paint) {
        val radius = hypot(end.x - start.x, end.y - start.y)
        if (radius <= 0) return
        val innerRadius = radius * 0.45f
        val starPath = android.graphics.Path()
        val cx = start.x
        val cy = start.y
        val step = PI / 5.0
        var rot = -PI / 2.0

        starPath.moveTo(
            (cx + cos(rot) * radius).toFloat(),
            (cy + sin(rot) * radius).toFloat()
        )
        for (i in 0 until 5) {
            rot += step
            starPath.lineTo(
                (cx + cos(rot) * innerRadius).toFloat(),
                (cy + sin(rot) * innerRadius).toFloat()
            )
            rot += step
            starPath.lineTo(
                (cx + cos(rot) * radius).toFloat(),
                (cy + sin(rot) * radius).toFloat()
            )
        }
        starPath.close()
        canvas.drawPath(starPath, paint)
    }

    /**
     * Save the drawing to the device's Pictures gallery
     */
    fun saveToGallery(context: Context, width: Int, height: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val actualW = if (width > 0) width else 1080
                val actualH = if (height > 0) height else 1920
                val bitmap = renderToBitmap(actualW, actualH)

                val filename = "TapDraw_${System.currentTimeMillis()}.png"
                var fos: OutputStream? = null
                var imageUri: Uri? = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TapDraw")
                    }
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        imageUri = uri
                        fos = resolver.openOutputStream(uri)
                    }
                } else {
                    val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val appDir = File(imagesDir, "TapDraw")
                    if (!appDir.exists()) appDir.mkdirs()
                    val imageFile = File(appDir, filename)
                    fos = FileOutputStream(imageFile)
                }

                fos?.use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }

                _events.emit(DrawingEvent.ShowToast("Drawing saved to Pictures/TapDraw!"))
            } catch (e: Exception) {
                _events.emit(DrawingEvent.ShowToast("Failed to save drawing: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * Share drawing via system share sheet
     */
    fun shareDrawing(context: Context, width: Int, height: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val actualW = if (width > 0) width else 1080
                val actualH = if (height > 0) height else 1920
                val bitmap = renderToBitmap(actualW, actualH)

                val cachePath = File(context.cacheDir, "images")
                if (!cachePath.exists()) cachePath.mkdirs()
                val file = File(cachePath, "shared_drawing.png")
                val stream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()

                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                _events.emit(DrawingEvent.ShareImage(contentUri))
            } catch (e: Exception) {
                _events.emit(DrawingEvent.ShowToast("Failed to share drawing: ${e.localizedMessage}"))
            }
        }
    }
}
