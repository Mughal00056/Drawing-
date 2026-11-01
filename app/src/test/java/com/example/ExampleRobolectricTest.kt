package com.example

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.model.BrushType
import com.example.model.CanvasItem
import com.example.model.SymmetryMode
import com.example.ui.viewmodel.DrawingViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Tap & Draw", appName)
    }

    @Test
    fun `tap and drag adds stroke and supports undo redo`() {
        val viewModel = DrawingViewModel()
        assertFalse(viewModel.canUndo)
        assertFalse(viewModel.canRedo)

        // Simulate touch down and drag
        viewModel.onTouchStart(Offset(100f, 150f))
        viewModel.onTouchMove(Offset(110f, 160f))
        viewModel.onTouchEnd(Size(1080f, 1920f))

        val stateAfterDraw = viewModel.uiState.value
        assertEquals(1, stateAfterDraw.items.size)
        assertTrue(viewModel.canUndo)
        assertFalse(viewModel.canRedo)

        // Undo
        viewModel.undo()
        assertEquals(0, viewModel.uiState.value.items.size)
        assertTrue(viewModel.canRedo)

        // Redo
        viewModel.redo()
        assertEquals(1, viewModel.uiState.value.items.size)
    }

    @Test
    fun `symmetry mode generates mirrored strokes`() {
        val viewModel = DrawingViewModel()
        viewModel.setSymmetryMode(SymmetryMode.HORIZONTAL)

        viewModel.onTouchStart(Offset(100f, 200f))
        viewModel.onTouchEnd(Size(1000f, 2000f))

        // Horizontal symmetry creates primary + 1 mirrored path = 2 paths
        assertEquals(2, viewModel.uiState.value.items.size)
    }
}
