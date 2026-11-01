package com.example.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * Floating draggable toolbar pill displayed over other apps.
 */
class OverlayControlsView(
    context: Context,
    private val onDragMove: (deltaX: Float, deltaY: Float) -> Unit,
    private val onToggleDrawing: (isDrawing: Boolean) -> Unit,
    private val onColorSelected: (colorInt: Int) -> Unit,
    private val onUndo: () -> Unit,
    private val onClear: () -> Unit,
    private val onClose: () -> Unit
) : LinearLayout(context) {

    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    private var isDrawingActive = false
    private var isExpanded = true

    private val expandedLayout: LinearLayout
    private val drawToggleIcon: ImageView
    private val drawToggleLabel: TextView
    private val miniIcon: ImageView

    private val colorOptions = listOf(
        Color.parseColor("#FF1744"), // Neon Red
        Color.parseColor("#FFD600"), // Neon Yellow
        Color.parseColor("#00E676"), // Neon Green
        Color.parseColor("#00E5FF"), // Neon Cyan
        Color.parseColor("#FFFFFF")  // Pure White
    )

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        // Card background shape
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 48f
            setColor(Color.parseColor("#EE1E1B4B")) // Deep Indigo with slight transparency
            setStroke(3, Color.parseColor("#55818CF8"))
        }
        background = bgDrawable
        setPadding(16, 12, 16, 12)
        elevation = 20f

        // 1. Draggable Pill Handle / Minimized Icon
        miniIcon = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_edit)
            setColorFilter(Color.WHITE)
            layoutParams = LayoutParams(64, 64).apply {
                marginEnd = 12
            }
        }
        addView(miniIcon)

        // Expanded content container
        expandedLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        addView(expandedLayout)

        // Draw / Pass-through Mode Toggle Button
        val toggleContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 8, 16, 8)
            background = GradientDrawable().apply {
                cornerRadius = 32f
                setColor(Color.parseColor("#334F46E5"))
            }
            setOnClickListener {
                isDrawingActive = !isDrawingActive
                updateDrawToggleState()
                onToggleDrawing(isDrawingActive)
            }
        }

        drawToggleIcon = ImageView(context).apply {
            setImageResource(android.R.drawable.checkbox_off_background)
            setColorFilter(Color.parseColor("#CBD5E1"))
            layoutParams = LayoutParams(42, 42)
        }
        toggleContainer.addView(drawToggleIcon)

        drawToggleLabel = TextView(context).apply {
            text = "Draw: OFF"
            textSize = 12f
            setTextColor(Color.parseColor("#CBD5E1"))
            setPadding(10, 0, 0, 0)
        }
        toggleContainer.addView(drawToggleLabel)

        expandedLayout.addView(toggleContainer)

        // Color Swatches
        val colorsLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 0, 12, 0)
        }
        colorOptions.forEach { col ->
            val dot = View(context).apply {
                layoutParams = LayoutParams(42, 42).apply {
                    setMargins(6, 0, 6, 0)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(col)
                    setStroke(2, Color.parseColor("#88000000"))
                }
                setOnClickListener {
                    onColorSelected(col)
                    if (!isDrawingActive) {
                        isDrawingActive = true
                        updateDrawToggleState()
                        onToggleDrawing(true)
                    }
                }
            }
            colorsLayout.addView(dot)
        }
        expandedLayout.addView(colorsLayout)

        // Undo Button
        val undoBtn = createIconButton(android.R.drawable.ic_menu_revert, "Undo") {
            onUndo()
        }
        expandedLayout.addView(undoBtn)

        // Clear Screen Button
        val clearBtn = createIconButton(android.R.drawable.ic_menu_delete, "Clear") {
            onClear()
        }
        expandedLayout.addView(clearBtn)

        // Minimize / Expand Toggle
        val minMaxBtn = createIconButton(android.R.drawable.arrow_down_float, "Minimize") {
            toggleMinimize()
        }
        expandedLayout.addView(minMaxBtn)

        // Close Button
        val closeBtn = createIconButton(android.R.drawable.ic_menu_close_clear_cancel, "Close") {
            onClose()
        }
        expandedLayout.addView(closeBtn)

        // Dragging listener on miniIcon
        miniIcon.setOnTouchListener { _, event ->
            handleDragTouch(event)
        }
    }

    private fun createIconButton(iconRes: Int, desc: String, onClick: () -> Unit): ImageView {
        return ImageView(context).apply {
            setImageResource(iconRes)
            contentDescription = desc
            setColorFilter(Color.WHITE)
            setPadding(12, 12, 12, 12)
            layoutParams = LayoutParams(64, 64).apply {
                setMargins(4, 0, 4, 0)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#22FFFFFF"))
            }
            setOnClickListener { onClick() }
        }
    }

    private fun updateDrawToggleState() {
        if (isDrawingActive) {
            drawToggleLabel.text = "Draw: ON"
            drawToggleLabel.setTextColor(Color.parseColor("#4ADE80")) // Green
            drawToggleIcon.setColorFilter(Color.parseColor("#4ADE80"))
        } else {
            drawToggleLabel.text = "Draw: OFF"
            drawToggleLabel.setTextColor(Color.parseColor("#CBD5E1"))
            drawToggleIcon.setColorFilter(Color.parseColor("#CBD5E1"))
        }
    }

    private fun toggleMinimize() {
        isExpanded = !isExpanded
        expandedLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
    }

    private fun handleDragTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY
                if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) {
                    isDragging = true
                }
                if (isDragging) {
                    onDragMove(dx, dy)
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    toggleMinimize()
                }
                return true
            }
        }
        return false
    }

    fun setDrawingState(active: Boolean) {
        isDrawingActive = active
        updateDrawToggleState()
    }
}
