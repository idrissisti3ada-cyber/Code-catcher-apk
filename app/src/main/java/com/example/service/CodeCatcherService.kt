package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.CodeCatcherApp
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppSettings
import com.example.util.ClipboardHelper
import com.example.util.FeedbackHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

class CodeCatcherService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var windowManager: WindowManager? = null
    private var bubbleContainer: FrameLayout? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var compactOrbView: FrameLayout? = null
    private var compactOrbIcon: ImageView? = null
    private var expandedPillView: LinearLayout? = null
    private var expandedTitleView: TextView? = null
    private var expandedCodeView: TextView? = null

    private var currentSettings: AppSettings = AppSettings()
    private var isForegroundStarted = false
    private var isExpanded = false

    private val collapseRunnable = Runnable {
        isExpanded = false
        renderBubbleState()
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        createNotificationChannel()

        val app = application as CodeCatcherApp
        serviceScope.launch {
            app.container.settingsRepository.settingsFlow.collectLatest { settings ->
                currentSettings = settings
                updateForegroundNotification()
                if (settings.hasAnyAppSelected) {
                    ensureOverlayBubble()
                    renderBubbleState()
                } else {
                    removeOverlayBubble()
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureForegroundStarted()

        val app = application as CodeCatcherApp
        when (intent?.action) {
            ACTION_TOGGLE_MONITORING -> {
                serviceScope.launch {
                    app.container.settingsRepository.toggleMonitoringActive()
                }
            }
            ACTION_STOP_SERVICE -> {
                serviceScope.launch {
                    app.container.settingsRepository.setMonitoringActive(false)
                    removeOverlayBubble()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
            ACTION_CODE_DETECTED -> {
                val code = intent.getStringExtra(EXTRA_CODE)
                val sourceApp = intent.getStringExtra(EXTRA_SOURCE_APP) ?: ""
                if (!code.isNullOrBlank()) {
                    handleDetectedCode(code, sourceApp)
                }
            }
            ACTION_REFRESH_OVERLAY -> {
                serviceScope.launch {
                    val snapshot = app.container.settingsRepository.getSnapshot()
                    currentSettings = snapshot
                    if (snapshot.hasAnyAppSelected) {
                        ensureOverlayBubble()
                        renderBubbleState()
                    }
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(collapseRunnable)
        removeOverlayBubble()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureForegroundStarted() {
        if (isForegroundStarted) return
        val notification = buildNotification(currentSettings)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        isForegroundStarted = true
    }

    private fun updateForegroundNotification() {
        if (!isForegroundStarted) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        nm.notify(NOTIFICATION_ID, buildNotification(currentSettings))
    }

    private fun handleDetectedCode(code: String, sourceApp: String) {
        if (currentSettings.hasAnyAppSelected) {
            ensureOverlayBubble()
        }
        ClipboardHelper.copyFromOverlayContext(
            context = this,
            windowManager = windowManager,
            text = code
        ) {
            FeedbackHelper.triggerCopyFeedback(
                context = this,
                vibrate = currentSettings.vibrateOnCopy,
                sound = currentSettings.soundOnCopy
            )
            showExpandedCopiedState(code, sourceApp)
        }
    }

    private fun showExpandedCopiedState(code: String, sourceApp: String) {
        mainHandler.removeCallbacks(collapseRunnable)
        isExpanded = true
        val titleText = if (sourceApp.isNotBlank()) {
            "${getString(R.string.bubble_copied_label)} • $sourceApp"
        } else {
            getString(R.string.bubble_copied_label)
        }
        expandedTitleView?.text = titleText
        expandedCodeView?.text = code
        renderBubbleState()
        mainHandler.postDelayed(collapseRunnable, EXPAND_DURATION_MS)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun ensureOverlayBubble() {
        if (!Settings.canDrawOverlays(this)) return
        if (bubbleContainer != null) return
        val wm = windowManager ?: return

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = currentSettings.bubbleX
            y = currentSettings.bubbleY
        }

        val container = FrameLayout(this).apply {
            contentDescription = getString(R.string.bubble_cd)
        }

        val orbSizePx = dp(52)
        val orb = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(orbSizePx, orbSizePx)
            elevation = dp(6).toFloat()
        }
        val orbIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_notification_catch)
            setColorFilter(Color.WHITE)
            val iconSize = dp(24)
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER)
        }
        orb.addView(orbIcon)

        val pill = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            elevation = dp(8).toFloat()
            visibility = View.GONE
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(18).toFloat()
                setColor(Color.parseColor("#FF0D1B2A"))
                setStroke(dp(1.5f), Color.parseColor("#FF00E5A3"))
            }
        }
        val titleTv = TextView(this).apply {
            setTextColor(Color.parseColor("#FF00E5A3"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val codeTv = TextView(this).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.06f
        }
        pill.addView(titleTv)
        pill.addView(codeTv)

        container.addView(orb)
        container.addView(pill)

        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        container.setOnTouchListener { _, event ->
            val currentParams = bubbleParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = currentParams.x
                    initialY = currentParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                        isDragging = true
                    }
                    if (isDragging) {
                        val metrics = resources.displayMetrics
                        val maxX = (metrics.widthPixels - container.width).coerceAtLeast(0)
                        val maxY = (metrics.heightPixels - container.height).coerceAtLeast(0)
                        currentParams.x = (initialX + dx).coerceIn(0, maxX)
                        currentParams.y = (initialY + dy).coerceIn(dp(24), maxY)
                        runCatching { wm.updateViewLayout(container, currentParams) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        onBubbleTapped()
                    } else {
                        val finalX = currentParams.x
                        val finalY = currentParams.y
                        serviceScope.launch {
                            (application as CodeCatcherApp).container.settingsRepository
                                .saveBubblePosition(finalX, finalY)
                        }
                    }
                    true
                }
                else -> false
            }
        }

        val added = runCatching {
            wm.addView(container, params)
            true
        }.getOrDefault(false)

        if (added) {
            bubbleContainer = container
            bubbleParams = params
            compactOrbView = orb
            compactOrbIcon = orbIcon
            expandedPillView = pill
            expandedTitleView = titleTv
            expandedCodeView = codeTv
            renderBubbleState()
        }
    }

    private fun onBubbleTapped() {
        if (isExpanded) {
            mainHandler.removeCallbacks(collapseRunnable)
            isExpanded = false
            renderBubbleState()
            return
        }
        serviceScope.launch {
            (application as CodeCatcherApp).container.settingsRepository.toggleMonitoringActive()
        }
    }

    private fun renderBubbleState() {
        val orb = compactOrbView ?: return
        val icon = compactOrbIcon ?: return
        val pill = expandedPillView ?: return
        val container = bubbleContainer ?: return
        val params = bubbleParams ?: return

        if (isExpanded) {
            orb.visibility = View.GONE
            pill.visibility = View.VISIBLE
        } else {
            pill.visibility = View.GONE
            orb.visibility = View.VISIBLE
            val isActive = currentSettings.isMonitoringActive
            val bgColor = if (isActive) {
                Color.parseColor("#FF00B884") // Active Green
            } else {
                Color.parseColor("#FF64748B") // Inactive Grey
            }
            val borderColor = if (isActive) {
                Color.parseColor("#FF80F5D0")
            } else {
                Color.parseColor("#FFCBD5E1")
            }
            orb.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(bgColor)
                setStroke(dp(2), borderColor)
            }
            icon.alpha = if (isActive) 1.0f else 0.78f
        }

        runCatching {
            windowManager?.updateViewLayout(container, params)
        }
    }

    private fun removeOverlayBubble() {
        val wm = windowManager ?: return
        val container = bubbleContainer ?: return
        runCatching {
            if (container.isAttachedToWindow) {
                wm.removeViewImmediate(container)
            }
        }
        bubbleContainer = null
        bubbleParams = null
        compactOrbView = null
        compactOrbIcon = null
        expandedPillView = null
        expandedTitleView = null
        expandedCodeView = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_desc)
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(settings: AppSettings): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, CodeCatcherService::class.java).apply { action = ACTION_TOGGLE_MONITORING },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val activeApps = buildList {
            if (settings.whatsAppEnabled) add(getString(R.string.whatsapp_title))
            if (settings.instagramEnabled) add(getString(R.string.instagram_title))
        }.joinToString(" & ").ifEmpty { "No apps" }

        val title = if (settings.isMonitoringActive) {
            getString(R.string.notif_title_active)
        } else {
            getString(R.string.notif_title_paused)
        }
        val toggleLabel = if (settings.isMonitoringActive) {
            getString(R.string.notif_action_pause)
        } else {
            getString(R.string.notif_action_resume)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_catch)
            .setContentTitle(title)
            .setContentText("Watching: $activeApps")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openAppIntent)
            .addAction(0, toggleLabel, toggleIntent)
            .build()
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun dp(value: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        ).toInt()
    }

    companion object {
        private const val CHANNEL_ID = "code_catcher_monitor_channel"
        private const val NOTIFICATION_ID = 1001
        private const val EXPAND_DURATION_MS = 3500L

        const val ACTION_START = "com.example.action.START"
        const val ACTION_TOGGLE_MONITORING = "com.example.action.TOGGLE_MONITORING"
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_SERVICE"
        const val ACTION_CODE_DETECTED = "com.example.action.CODE_DETECTED"
        const val ACTION_REFRESH_OVERLAY = "com.example.action.REFRESH_OVERLAY"
        const val EXTRA_CODE = "extra_code"
        const val EXTRA_SOURCE_APP = "extra_source_app"

        fun startOrSync(context: Context) {
            val intent = Intent(context, CodeCatcherService::class.java).apply {
                action = ACTION_REFRESH_OVERLAY
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, CodeCatcherService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            runCatching { context.startService(intent) }
        }

        fun notifyCodeDetected(context: Context, code: String, sourceApp: String) {
            val intent = Intent(context, CodeCatcherService::class.java).apply {
                action = ACTION_CODE_DETECTED
                putExtra(EXTRA_CODE, code)
                putExtra(EXTRA_SOURCE_APP, sourceApp)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
