package com.example.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.example.model.BrushType
import com.example.model.CanvasBackground
import com.example.model.CanvasItem
import com.example.model.DrawingPoint
import com.example.model.ShapeType
import com.example.model.SymmetryMode
import com.example.ui.viewmodel.DrawingUiState
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

@Composable
fun DrawingCanvas(
    uiState: DrawingUiState,
    currentPoints: List<DrawingPoint>,
    liveShape: CanvasItem.ShapeItem?,
    onTouchStart: (Offset) -> Unit,
    onTouchMove: (Offset) -> Unit,
    onTouchEnd: (Size) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("drawing_canvas")
            .background(uiState.background.surfaceColor)
            .onSizeChanged { intSize ->
                canvasSize = intSize.toSize()
            }
            .pointerInput(uiState.selectedShape, uiState.selectedStamp, uiState.currentBrush) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onTouchStart(down.position)
                    down.consume()

                    val pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (change.changedToUp()) {
                            change.consume()
                            onTouchEnd(canvasSize)
                            break
                        } else if (change.positionChanged()) {
                            change.consume()
                            onTouchMove(change.position)
                        }
                    }
                }
            }
    ) {
        // 1. Draw canvas background texture/grid/lines
        drawBackgroundTexture(uiState.background)

        // 2. Draw symmetry guide lines if enabled
        if (uiState.symmetryMode != SymmetryMode.OFF) {
            drawSymmetryGuides(uiState.symmetryMode, size, uiState.background.isDark)
        }

        // 3. Draw committed items (strokes, shapes, stamps)
        for (item in uiState.items) {
            drawCanvasItem(item, uiState.background.surfaceColor)
        }

        // 4. Draw active live shape (preview during drag)
        if (liveShape != null) {
            drawShapeItem(liveShape)
            // Live symmetry preview for shapes
            if (uiState.symmetryMode != SymmetryMode.OFF) {
                drawSymmetricLiveShape(liveShape, size, uiState.symmetryMode)
            }
        }

        // 5. Draw active in-progress stroke
        if (currentPoints.isNotEmpty()) {
            drawLiveStroke(
                points = currentPoints,
                brushType = uiState.currentBrush,
                color = uiState.currentColor,
                strokeWidth = uiState.strokeWidth,
                opacity = uiState.opacity,
                surfaceColor = uiState.background.surfaceColor
            )

            // Live symmetry preview for strokes
            if (uiState.symmetryMode != SymmetryMode.OFF) {
                drawSymmetricLiveStroke(
                    points = currentPoints,
                    brushType = uiState.currentBrush,
                    color = uiState.currentColor,
                    strokeWidth = uiState.strokeWidth,
                    opacity = uiState.opacity,
                    surfaceColor = uiState.background.surfaceColor,
                    symmetryMode = uiState.symmetryMode,
                    canvasSize = size
                )
            }
        }
    }
}

/**
 * Draws background guidelines such as grid, dots, or notebook lines
 */
private fun DrawScope.drawBackgroundTexture(background: CanvasBackground) {
    when (background) {
        CanvasBackground.GRAPH_PAPER -> {
            val step = 32.dp.toPx()
            val lineColor = background.patternColor
            var x = step
            while (x < size.width) {
                drawLine(
                    color = lineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += step
            }
            var y = step
            while (y < size.height) {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += step
            }
        }

        CanvasBackground.DOT_GRID -> {
            val step = 28.dp.toPx()
            val dotColor = background.patternColor
            val dotRadius = 1.5.dp.toPx()
            var x = step
            while (x < size.width) {
                var y = step
                while (y < size.height) {
                    drawCircle(color = dotColor, radius = dotRadius, center = Offset(x, y))
                    y += step
                }
                x += step
            }
        }

        CanvasBackground.NOTEBOOK_LINED -> {
            val step = 36.dp.toPx()
            val lineBlue = Color(0xFF93C5FD)
            val marginRed = Color(0xFFFCA5A5)
            // Left margin line
            drawLine(
                color = marginRed,
                start = Offset(52.dp.toPx(), 0f),
                end = Offset(52.dp.toPx(), size.height),
                strokeWidth = 1.5f
            )
            // Horizontal lines
            var y = 60.dp.toPx()
            while (y < size.height) {
                drawLine(
                    color = lineBlue,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += step
            }
        }

        CanvasBackground.CHALKBOARD -> {
            // Subtle border glow for chalkboard
            drawRect(
                color = Color(0x1AFFFFFF),
                size = size,
                style = Stroke(width = 4.dp.toPx())
            )
        }

        else -> {}
    }
}

/**
 * Visual guidelines for symmetry mode
 */
private fun DrawScope.drawSymmetryGuides(mode: SymmetryMode, size: Size, isDark: Boolean) {
    val guideColor = if (isDark) Color(0x33FFFFFF) else Color(0x33000000)
    val stroke = Stroke(width = 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f)))

    val midX = size.width / 2f
    val midY = size.height / 2f

    when (mode) {
        SymmetryMode.HORIZONTAL -> {
            drawLine(guideColor, Offset(midX, 0f), Offset(midX, size.height), strokeWidth = 1f)
        }
        SymmetryMode.VERTICAL -> {
            drawLine(guideColor, Offset(0f, midY), Offset(size.width, midY), strokeWidth = 1f)
        }
        SymmetryMode.QUAD -> {
            drawLine(guideColor, Offset(midX, 0f), Offset(midX, size.height), strokeWidth = 1f)
            drawLine(guideColor, Offset(0f, midY), Offset(size.width, midY), strokeWidth = 1f)
        }
        SymmetryMode.OFF -> {}
    }
}

private fun DrawScope.drawCanvasItem(item: CanvasItem, surfaceColor: Color) {
    when (item) {
        is CanvasItem.PathItem -> {
            drawPathItem(item, surfaceColor)
        }
        is CanvasItem.ShapeItem -> {
            drawShapeItem(item)
        }
        is CanvasItem.StampItem -> {
            drawStampItem(item)
        }
    }
}

private fun DrawScope.drawPathItem(item: CanvasItem.PathItem, surfaceColor: Color) {
    if (item.points.isEmpty()) return

    val strokeWidth = item.strokeWidth
    val strokeColor = if (item.brushType == BrushType.ERASER) surfaceColor else item.color.copy(alpha = item.alpha)

    if (item.brushType == BrushType.RAINBOW) {
        drawRainbowPath(item.points, strokeWidth)
        return
    }

    if (item.brushType == BrushType.NEON) {
        val path = item.buildComposePath()
        // Outer blur/glow simulation
        drawPath(
            path = path,
            color = item.color.copy(alpha = 0.35f),
            style = Stroke(width = strokeWidth * 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Mid glow
        drawPath(
            path = path,
            color = item.color.copy(alpha = 0.75f),
            style = Stroke(width = strokeWidth * 1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Inner white light core
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.95f),
            style = Stroke(width = strokeWidth * 0.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        return
    }

    val path = item.buildComposePath()
    drawPath(
        path = path,
        color = strokeColor,
        style = Stroke(
            width = strokeWidth,
            cap = if (item.brushType == BrushType.MARKER) StrokeCap.Square else StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawRainbowPath(points: List<DrawingPoint>, strokeWidth: Float) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        val pt = points.first()
        drawCircle(
            color = pt.color ?: Color.Red,
            radius = strokeWidth / 2f,
            center = pt.offset
        )
        return
    }

    for (i in 1 until points.size) {
        val p0 = points[i - 1]
        val p1 = points[i]
        val c = p1.color ?: Color.Red
        drawLine(
            color = c,
            start = p0.offset,
            end = p1.offset,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawLiveStroke(
    points: List<DrawingPoint>,
    brushType: BrushType,
    color: Color,
    strokeWidth: Float,
    opacity: Float,
    surfaceColor: Color
) {
    val tempPathItem = CanvasItem.PathItem(
        points = points,
        color = color,
        strokeWidth = strokeWidth,
        brushType = brushType,
        alpha = if (brushType == BrushType.MARKER) 0.42f else opacity
    )
    drawPathItem(tempPathItem, surfaceColor)
}

private fun DrawScope.drawSymmetricLiveStroke(
    points: List<DrawingPoint>,
    brushType: BrushType,
    color: Color,
    strokeWidth: Float,
    opacity: Float,
    surfaceColor: Color,
    symmetryMode: SymmetryMode,
    canvasSize: Size
) {
    val w = canvasSize.width
    val h = canvasSize.height

    fun mirror(transform: (Offset) -> Offset) {
        val mirroredPoints = points.map { it.copy(offset = transform(it.offset)) }
        drawLiveStroke(mirroredPoints, brushType, color, strokeWidth, opacity, surfaceColor)
    }

    when (symmetryMode) {
        SymmetryMode.HORIZONTAL -> {
            mirror { Offset(w - it.x, it.y) }
        }
        SymmetryMode.VERTICAL -> {
            mirror { Offset(it.x, h - it.y) }
        }
        SymmetryMode.QUAD -> {
            mirror { Offset(w - it.x, it.y) }
            mirror { Offset(it.x, h - it.y) }
            mirror { Offset(w - it.x, h - it.y) }
        }
        SymmetryMode.OFF -> {}
    }
}

private fun DrawScope.drawShapeItem(item: CanvasItem.ShapeItem) {
    val drawColor = item.color.copy(alpha = item.alpha)
    val strokeStyle = Stroke(width = item.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val fillStyle = Fill

    when (item.shapeType) {
        ShapeType.LINE -> {
            drawLine(
                color = drawColor,
                start = item.start,
                end = item.end,
                strokeWidth = item.strokeWidth,
                cap = StrokeCap.Round
            )
        }

        ShapeType.RECTANGLE -> {
            val topLeft = Offset(
                min(item.start.x, item.end.x),
                min(item.start.y, item.end.y)
            )
            val rectSize = Size(
                kotlin.math.abs(item.end.x - item.start.x),
                kotlin.math.abs(item.end.y - item.start.y)
            )
            if (item.isFilled) {
                drawRect(color = drawColor, topLeft = topLeft, size = rectSize, style = fillStyle)
            } else {
                drawRect(color = drawColor, topLeft = topLeft, size = rectSize, style = strokeStyle)
            }
        }

        ShapeType.CIRCLE -> {
            val radius = hypot(item.end.x - item.start.x, item.end.y - item.start.y)
            if (item.isFilled) {
                drawCircle(color = drawColor, radius = radius, center = item.start, style = fillStyle)
            } else {
                drawCircle(color = drawColor, radius = radius, center = item.start, style = strokeStyle)
            }
        }

        ShapeType.ARROW -> {
            drawLine(
                color = drawColor,
                start = item.start,
                end = item.end,
                strokeWidth = item.strokeWidth,
                cap = StrokeCap.Round
            )
            val angle = atan2((item.end.y - item.start.y).toDouble(), (item.end.x - item.start.x).toDouble())
            val arrowLength = item.strokeWidth * 3.5f
            val p1 = Offset(
                (item.end.x - arrowLength * cos(angle - PI / 6)).toFloat(),
                (item.end.y - arrowLength * sin(angle - PI / 6)).toFloat()
            )
            val p2 = Offset(
                (item.end.x - arrowLength * cos(angle + PI / 6)).toFloat(),
                (item.end.y - arrowLength * sin(angle + PI / 6)).toFloat()
            )
            drawLine(drawColor, item.end, p1, strokeWidth = item.strokeWidth, cap = StrokeCap.Round)
            drawLine(drawColor, item.end, p2, strokeWidth = item.strokeWidth, cap = StrokeCap.Round)
        }

        ShapeType.STAR -> {
            val starPath = Path()
            val radius = hypot(item.end.x - item.start.x, item.end.y - item.start.y)
            if (radius > 0) {
                val innerRadius = radius * 0.45f
                val cx = item.start.x
                val cy = item.start.y
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

                if (item.isFilled) {
                    drawPath(starPath, drawColor, style = fillStyle)
                } else {
                    drawPath(starPath, drawColor, style = strokeStyle)
                }
            }
        }
    }
}

private fun DrawScope.drawSymmetricLiveShape(shape: CanvasItem.ShapeItem, size: Size, mode: SymmetryMode) {
    val w = size.width
    val h = size.height

    fun mirror(t: (Offset) -> Offset) {
        val s = shape.copy(start = t(shape.start), end = t(shape.end))
        drawShapeItem(s)
    }

    when (mode) {
        SymmetryMode.HORIZONTAL -> mirror { Offset(w - it.x, it.y) }
        SymmetryMode.VERTICAL -> mirror { Offset(it.x, h - it.y) }
        SymmetryMode.QUAD -> {
            mirror { Offset(w - it.x, it.y) }
            mirror { Offset(it.x, h - it.y) }
            mirror { Offset(w - it.x, h - it.y) }
        }
        SymmetryMode.OFF -> {}
    }
}

private fun DrawScope.drawStampItem(item: CanvasItem.StampItem) {
    drawContext.canvas.nativeCanvas.apply {
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = item.size
            textAlign = Paint.Align.CENTER
        }
        drawText(item.stampType.symbol, item.center.x, item.center.y + item.size / 3f, paint)
    }
}
