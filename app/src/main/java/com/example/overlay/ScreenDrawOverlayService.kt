package com.example.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ScreenOverlayManager {
    val isRunningFlow = MutableStateFlow(false)
    val defaultColorFlow = MutableStateFlow(android.graphics.Color.parseColor("#FF1744"))
    val defaultStrokeWidthFlow = MutableStateFlow(14f)
}

class ScreenDrawOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayDrawView: OverlayDrawView? = null
    private var overlayControlsView: OverlayControlsView? = null

    private lateinit var canvasParams: WindowManager.LayoutParams
    private lateinit var controlsParams: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            startForegroundNotification()
            setupOverlayViews()
            isRunning = true
            ScreenOverlayManager.isRunningFlow.value = true
        } catch (e: Exception) {
            Log.e("ScreenDrawOverlayService", "Failed to start overlay service", e)
            stopSelf()
        }
    }

    private fun startForegroundNotification() {
        val channelId = "screen_draw_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Screen Draw Floating Pen",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active floating screen pen to draw over apps"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Screen Drawing Active")
            .setContentText("Tap & Draw is floating over your screen.")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun setupOverlayViews() {
        val wm = windowManager ?: return

        // 1. Setup Fullscreen Drawing Canvas View
        overlayDrawView = OverlayDrawView(this).apply {
            currentColor = ScreenOverlayManager.defaultColorFlow.value
            currentStrokeWidth = ScreenOverlayManager.defaultStrokeWidthFlow.value
        }

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Initially touches pass through (not drawing)
        val initialCanvasFlags = (
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        canvasParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            initialCanvasFlags,
            PixelFormat.TRANSLUCENT
        )

        wm.addView(overlayDrawView, canvasParams)

        // 2. Setup Floating Controls Toolbar View
        controlsParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 220
        }

        overlayControlsView = OverlayControlsView(
            context = this,
            onDragMove = { dx, dy ->
                controlsParams.x = (controlsParams.x + dx.toInt()).coerceAtLeast(0)
                controlsParams.y = (controlsParams.y + dy.toInt()).coerceAtLeast(0)
                wm.updateViewLayout(overlayControlsView, controlsParams)
            },
            onToggleDrawing = { isDrawingActive ->
                toggleCanvasTouchState(isDrawingActive)
            },
            onColorSelected = { colorInt ->
                overlayDrawView?.currentColor = colorInt
                overlayDrawView?.isEraser = false
                ScreenOverlayManager.defaultColorFlow.value = colorInt
            },
            onUndo = {
                overlayDrawView?.undo()
            },
            onClear = {
                overlayDrawView?.clearAll()
            },
            onClose = {
                stopSelf()
            }
        )

        wm.addView(overlayControlsView, controlsParams)
    }

    private fun toggleCanvasTouchState(isDrawingActive: Boolean) {
        val wm = windowManager ?: return
        val canvas = overlayDrawView ?: return

        canvas.isDrawingEnabled = isDrawingActive

        if (isDrawingActive) {
            // Intercept touches on full screen to draw
            canvasParams.flags = (
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        } else {
            // Touches pass through to whatever app is underneath
            canvasParams.flags = (
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }

        wm.updateViewLayout(canvas, canvasParams)
    }

    override fun onDestroy() {
        super.onDestroy()
        val wm = windowManager
        if (wm != null) {
            overlayDrawView?.let {
                try { wm.removeView(it) } catch (_: Exception) {}
            }
            overlayControlsView?.let {
                try { wm.removeView(it) } catch (_: Exception) {}
            }
        }
        isRunning = false
        ScreenOverlayManager.isRunningFlow.value = false
    }

    companion object {
        private const val NOTIFICATION_ID = 9021
        var isRunning: Boolean = false
            private set

        fun canDrawOverlay(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }

        fun start(context: Context) {
            val intent = Intent(context, ScreenDrawOverlayService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("ScreenDrawOverlayService", "Error starting service", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ScreenDrawOverlayService::class.java)
            context.stopService(intent)
            isRunning = false
            ScreenOverlayManager.isRunningFlow.value = false
        }
    }
}
