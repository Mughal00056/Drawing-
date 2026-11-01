package com.example.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.MotionEvent
import android.view.View

/**
 * Fullscreen transparent view that captures touch events and renders
 * drawing strokes directly over any open app or the home screen.
 */
class OverlayDrawView(context: Context) : View(context) {

    data class OverlayStroke(
        val path: Path,
        val color: Int,
        val strokeWidth: Float,
        val isEraser: Boolean
    )

    private val strokes = mutableListOf<OverlayStroke>()
    private var currentPath = Path()
    private var lastX = 0f
    private var lastY = 0f

    var currentColor: Int = Color.parseColor("#FF0055") // Bright Pink/Red default
    var currentStrokeWidth: Float = 14f
    var isEraser: Boolean = false

    var isDrawingEnabled: Boolean = false

    private val strokePaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val glowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    init {
        // Transparent background
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isDrawingEnabled) return false

        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path()
                currentPath.moveTo(x, y)
                lastX = x
                lastY = y
                // Tiny line for instant tap dots
                currentPath.lineTo(x + 0.1f, y + 0.1f)
                strokes.add(
                    OverlayStroke(
                        path = currentPath,
                        color = currentColor,
                        strokeWidth = currentStrokeWidth,
                        isEraser = isEraser
                    )
                )
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = kotlin.math.abs(x - lastX)
                val dy = kotlin.math.abs(y - lastY)
                if (dx >= 2 || dy >= 2) {
                    val midX = (x + lastX) / 2f
                    val midY = (y + lastY) / 2f
                    currentPath.quadTo(lastX, lastY, midX, midY)
                    lastX = x
                    lastY = y
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                currentPath.lineTo(x, y)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (stroke in strokes) {
            if (stroke.isEraser) {
                strokePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                strokePaint.strokeWidth = stroke.strokeWidth * 1.8f
                canvas.drawPath(stroke.path, strokePaint)
            } else {
                strokePaint.xfermode = null

                // Outer subtle glow for high visibility over any dark or light app background
                glowPaint.xfermode = null
                glowPaint.color = Color.BLACK
                glowPaint.alpha = 110
                glowPaint.strokeWidth = stroke.strokeWidth + 6f
                canvas.drawPath(stroke.path, glowPaint)

                // Main stroke
                strokePaint.color = stroke.color
                strokePaint.alpha = 255
                strokePaint.strokeWidth = stroke.strokeWidth
                canvas.drawPath(stroke.path, strokePaint)
            }
        }
    }

    fun undo() {
        if (strokes.isNotEmpty()) {
            strokes.removeAt(strokes.lastIndex)
            invalidate()
        }
    }

    fun clearAll() {
        strokes.clear()
        currentPath.reset()
        invalidate()
    }

    val canUndo: Boolean
        get() = strokes.isNotEmpty()
}
