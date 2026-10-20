package com.example.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.R
import com.example.model.ConnectionStatus
import com.example.vpn.VpnManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FloatingVpnOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var stateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    companion object {
        fun start(context: Context) {
            if (Settings.canDrawOverlays(context)) {
                val intent = Intent(context, FloatingVpnOverlayService::class.java)
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingVpnOverlayService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createOverlayWidget()
        observeVpnState()
    }

    private fun createOverlayWidget() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 220
        }

        // Floating pill container
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))

            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#E6120E22")) // Deep dark surface
                cornerRadius = dpToPx(24).toFloat()
                setStroke(dpToPx(2), Color.parseColor("#A855F7")) // Neon Purple Border
            }
            background = bg
            elevation = dpToPx(8).toFloat()
        }

        // Flag / Game icon
        val flagTextView = TextView(this).apply {
            text = "🎮"
            textSize = 16f
            setPadding(0, 0, dpToPx(6), 0)
        }

        // Server & Status text
        val infoTextView = TextView(this).apply {
            text = "MLBB VPN"
            setTextColor(Color.WHITE)
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
        }

        // Ping / Stop pill button
        val disconnectBtn = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            val btnBg = GradientDrawable().apply {
                setColor(Color.parseColor("#EF4444")) // Crimson Red
                cornerRadius = dpToPx(12).toFloat()
            }
            background = btnBg
            val label = TextView(context).apply {
                text = "DISCONNECT"
                setTextColor(Color.WHITE)
                textSize = 10f
                setTypeface(null, Typeface.BOLD)
            }
            addView(label)
            setOnClickListener {
                VpnManager.stopVpn(this@FloatingVpnOverlayService)
                Toast.makeText(this@FloatingVpnOverlayService, "MLBB VPN Diputuskan! Kembali ke ISP lokal.", Toast.LENGTH_SHORT).show()
                stopSelf()
            }
        }

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(8), 1)
        }

        container.addView(flagTextView)
        container.addView(infoTextView)
        container.addView(spacer)
        container.addView(disconnectBtn)

        // Make floating overlay draggable
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        container.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        isDragging = true
                        params.x = initialX + deltaX
                        params.y = initialY + deltaY
                        windowManager?.updateViewLayout(overlayView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        view.performClick()
                    }
                    true
                }
                else -> false
            }
        }

        overlayView = container
        windowManager?.addView(overlayView, params)
    }

    private fun observeVpnState() {
        stateJob?.cancel()
        stateJob = scope.launch {
            VpnManager.sessionState.collect { session ->
                if (session.status == ConnectionStatus.DISCONNECTED) {
                    stopSelf()
                } else if (overlayView != null) {
                    val container = overlayView as? LinearLayout
                    val flagView = container?.getChildAt(0) as? TextView
                    val infoView = container?.getChildAt(1) as? TextView

                    session.activeServer?.let { server ->
                        flagView?.text = server.flagEmoji
                        val ping = server.pingMs ?: server.baselineMs
                        infoView?.text = "${server.country} (${ping}ms)"
                    }
                }
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroy() {
        stateJob?.cancel()
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        overlayView = null
        super.onDestroy()
    }
}
