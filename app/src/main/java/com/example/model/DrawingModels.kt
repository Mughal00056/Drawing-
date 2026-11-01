package com.example.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Brush types available for drawing
 */
enum class BrushType(val label: String, val description: String) {
    PEN("Pen", "Smooth solid line"),
    NEON("Glow", "Vibrant glowing neon stroke"),
    MARKER("Marker", "Translucent highlighter"),
    RAINBOW("Rainbow", "Color-shifting magic stroke"),
    ERASER("Eraser", "Erase existing strokes")
}

/**
 * Shape types that can be drawn on canvas
 */
enum class ShapeType(val label: String) {
    LINE("Line"),
    RECTANGLE("Rectangle"),
    CIRCLE("Circle"),
    ARROW("Arrow"),
    STAR("Star")
}

/**
 * Stamps that can be tapped onto the canvas
 */
enum class StampType(val label: String, val symbol: String) {
    STAR("Star", "⭐"),
    HEART("Heart", "❤️"),
    SPARKLE("Sparkle", "✨"),
    FIRE("Fire", "🔥"),
    SMILEY("Smiley", "😊"),
    FLOWER("Flower", "🌸"),
    CROWN("Crown", "👑"),
    MUSIC("Music", "🎵")
}

/**
 * Background textures/themes for the drawing pad
 */
enum class CanvasBackground(
    val title: String,
    val surfaceColor: Color,
    val patternColor: Color,
    val isDark: Boolean
) {
    CLEAN_WHITE("White Paper", Color(0xFFFFFFFF), Color(0xFFE2E8F0), false),
    CHALKBOARD("Chalkboard", Color(0xFF1E293B), Color(0xFF334155), true),
    MIDNIGHT("Midnight Glow", Color(0xFF0F172A), Color(0xFF1E293B), true),
    GRAPH_PAPER("Grid Paper", Color(0xFFF8FAFC), Color(0xFFCBD5E1), false),
    DOT_GRID("Dot Grid", Color(0xFFFDFBF7), Color(0xFFD4D4D8), false),
    NOTEBOOK_LINED("Lined Paper", Color(0xFFFFFDF5), Color(0xFF93C5FD), false),
    PARCHMENT("Parchment", Color(0xFFF5EBE0), Color(0xFFD5BDAF), false)
}

/**
 * Mirror symmetry modes
 */
enum class SymmetryMode(val title: String) {
    OFF("Normal"),
    HORIZONTAL("Left-Right"),
    VERTICAL("Top-Bottom"),
    QUAD("4-Way Kaleidoscope")
}

/**
 * Represents an individual drawn point in a path
 */
data class DrawingPoint(
    val offset: Offset,
    val color: Color? = null
)

/**
 * Base sealed class for all drawing actions on the canvas stack
 */
sealed class CanvasItem {
    /**
     * Freehand or tapped stroke path
     */
    data class PathItem(
        val points: List<DrawingPoint>,
        val color: Color,
        val strokeWidth: Float,
        val brushType: BrushType,
        val alpha: Float = 1.0f
    ) : CanvasItem() {
        fun buildComposePath(): Path {
            val path = Path()
            if (points.isEmpty()) return path
            path.moveTo(points.first().offset.x, points.first().offset.y)
            if (points.size == 1) {
                // Single point tap: tiny segment so it renders as a round dot
                path.lineTo(points.first().offset.x + 0.1f, points.first().offset.y + 0.1f)
                return path
            }
            for (i in 1 until points.size) {
                val prev = points[i - 1].offset
                val curr = points[i].offset
                val midX = (prev.x + curr.x) / 2f
                val midY = (prev.y + curr.y) / 2f
                path.quadraticTo(prev.x, prev.y, midX, midY)
            }
            path.lineTo(points.last().offset.x, points.last().offset.y)
            return path
        }
    }

    /**
     * Geometric shape item
     */
    data class ShapeItem(
        val shapeType: ShapeType,
        val start: Offset,
        val end: Offset,
        val color: Color,
        val strokeWidth: Float,
        val isFilled: Boolean,
        val alpha: Float = 1.0f
    ) : CanvasItem()

    /**
     * Placed stamp item
     */
    data class StampItem(
        val stampType: StampType,
        val center: Offset,
        val size: Float,
        val color: Color
    ) : CanvasItem()
}
