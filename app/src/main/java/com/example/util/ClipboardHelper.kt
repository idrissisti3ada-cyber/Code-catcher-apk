package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager

object ClipboardHelper {

    fun copyDirect(context: Context, text: String): Boolean {
        return runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return false
            val clip = ClipData.newPlainText("Code Catcher", text)
            clipboard.setPrimaryClip(clip)
            true
        }.getOrDefault(false)
    }

    /**
     * Copies [text] to the system clipboard reliably on Android 10+ (API 29+) from a foreground
     * service/overlay context by temporarily attaching a 1x1 transparent focusable overlay window
     * so Android grants window focus for `setPrimaryClip`, then immediately removing it.
     */
    fun copyFromOverlayContext(
        context: Context,
        windowManager: WindowManager?,
        text: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                windowManager == null ||
                !Settings.canDrawOverlays(context)
            ) {
                val ok = copyDirect(context, text)
                onComplete(ok)
                return@post
            }

            var completed = false
            val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                1,
                1,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }

            lateinit var focusHelperView: View
            val finishAndCleanup = {
                if (!completed) {
                    completed = true
                    val ok = copyDirect(context, text)
                    runCatching {
                        if (focusHelperView.isAttachedToWindow) {
                            windowManager.removeViewImmediate(focusHelperView)
                        }
                    }
                    onComplete(ok)
                }
            }

            val timeoutRunnable = Runnable { finishAndCleanup() }

            focusHelperView = object : View(context) {
                override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
                    super.onWindowFocusChanged(hasWindowFocus)
                    if (hasWindowFocus && !completed) {
                        mainHandler.removeCallbacks(timeoutRunnable)
                        finishAndCleanup()
                    }
                }
            }.apply {
                isFocusable = true
                isFocusableInTouchMode = true
            }

            val attached = runCatching {
                windowManager.addView(focusHelperView, params)
                focusHelperView.requestFocus()
                true
            }.getOrDefault(false)

            if (attached) {
                mainHandler.postDelayed(timeoutRunnable, 180L)
            } else {
                val ok = copyDirect(context, text)
                onComplete(ok)
            }
        }
    }
}
