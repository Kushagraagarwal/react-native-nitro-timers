package com.margelo.nitro.nitrotimer

import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.facebook.proguard.annotations.DoNotStrip
import com.facebook.react.uimanager.ThemedReactContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@DoNotStrip
class HybridNitroTimer(val context: ThemedReactContext) : HybridNitroTimerSpec() {

    // View with TextView for timer display
    override val view: FrameLayout = FrameLayout(context)
    private val timerTextView = TextView(context)

    // Timer properties
    private var endDate: Date? = null
    private var isRunning = false
    private var isPaused = false
    private var wasExpired = false
    private var wasCritical = false
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    // Props
    private var _endTime = ""
    override var endTime: String
        get() = _endTime
        set(value) {
            _endTime = value
            setEndTimeFromString(value)
        }

    private var _autoStart = true
    override var autoStart: Boolean
        get() = _autoStart
        set(value) {
            _autoStart = value
            if (value && !isRunning) {
                start()
            }
        }

    private var _criticalThreshold = 300.0 // 5 minutes default
    override var criticalThreshold: Double
        get() = _criticalThreshold
        set(value) {
            _criticalThreshold = value
        }

    // Display format
    private var _format = "HH:MM:SS"
    override var format: String
        get() = _format
        set(value) {
            _format = value
        }

    private var _showDays = true
    override var showDays: Boolean
        get() = _showDays
        set(value) {
            _showDays = value
        }

    private var _hideZeroHours = false
    override var hideZeroHours: Boolean
        get() = _hideZeroHours
        set(value) {
            _hideZeroHours = value
        }

    // Styling
    private var _fontSize = 16.0
    override var fontSize: Double
        get() = _fontSize
        set(value) {
            _fontSize = value
            updateTextViewStyle()
        }

    private var _textColor = "#000000"
    override var textColor: String
        get() = _textColor
        set(value) {
            _textColor = value
            updateTextViewStyle()
        }

    private var _criticalColor = "#FF0000"
    override var criticalColor: String
        get() = _criticalColor
        set(value) {
            _criticalColor = value
        }

    private var _fontWeight = "normal"
    override var fontWeight: String
        get() = _fontWeight
        set(value) {
            _fontWeight = value
            updateTextViewStyle()
        }

    private var _textAlign = "left"
    override var textAlign: String
        get() = _textAlign
        set(value) {
            _textAlign = value
            updateTextViewStyle()
        }

    private var _backgroundColor: String? = null
    override var backgroundColor: String?
        get() = _backgroundColor
        set(value) {
            _backgroundColor = value
            updateViewStyle()
        }

    private var _cornerRadius: Double? = null
    override var cornerRadius: Double?
        get() = _cornerRadius
        set(value) {
            _cornerRadius = value
            updateViewStyle()
        }

    private var _padding: Double? = null
    override var padding: Double?
        get() = _padding
        set(value) {
            _padding = value
            updateTextViewLayout()
        }

    // Behavior
    private var _pauseWhenNotVisible = false
    override var pauseWhenNotVisible: Boolean
        get() = _pauseWhenNotVisible
        set(value) {
            _pauseWhenNotVisible = value
        }

    private var _continueInBackground = true
    override var continueInBackground: Boolean
        get() = _continueInBackground
        set(value) {
            _continueInBackground = value
        }

    // Events
    override var onExpired: (() -> Unit)? = null
    override var onCritical: (() -> Unit)? = null

    init {
        setupTimerTextView()
        TimerManager.addTimer(this)
    }

    fun destroy() {
        TimerManager.removeTimer(this)
    }

    private fun setupTimerTextView() {
        view.addView(timerTextView)
        updateTextViewLayout()
        updateTextViewStyle()
        updateViewStyle()

        timerTextView.text = formatTime(0, 0, 0, 0)
    }

    private fun updateTextViewLayout() {
        val paddingPx = (_padding ?: 0.0).dpToPx().roundToInt()
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        layoutParams.setMargins(paddingPx, paddingPx, paddingPx, paddingPx)
        timerTextView.layoutParams = layoutParams
        timerTextView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
    }

    private fun updateTextViewStyle() {
        timerTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, _fontSize.toFloat())
        timerTextView.setTextColor(_textColor.toColorInt())

        timerTextView.typeface = if (_fontWeight == "bold") {
            Typeface.DEFAULT_BOLD
        } else {
            Typeface.DEFAULT
        }

        timerTextView.gravity = when (_textAlign) {
            "center" -> Gravity.CENTER
            "right" -> Gravity.END or Gravity.CENTER_VERTICAL
            else -> Gravity.START or Gravity.CENTER_VERTICAL
        }
    }

    private fun updateViewStyle() {
        _backgroundColor?.let { bgColor ->
            view.setBackgroundColor(bgColor.toColorInt())
        }

        _cornerRadius?.let { radius ->
            view.background?.let { drawable ->
                // Note: For proper corner radius, you'd want to use a GradientDrawable
                // This is a simplified version
            }
        }
    }

    private fun setEndTimeFromString(timeString: String) {
        try {
            endDate = dateFormat.parse(timeString)
            updateDisplay()
        } catch (e: Exception) {
            // Handle parsing error
            endDate = null
        }
    }

    fun updateDisplay() {
        val endDate = this.endDate ?: run {
            timerTextView.text = formatTime(0, 0, 0, 0)
            return
        }

        val now = Date()
        val timeInterval = (endDate.time - now.time) / 1000.0

        if (timeInterval <= 0) {
            // Timer expired
            timerTextView.text = formatTime(0, 0, 0, 0)
            if (!wasExpired) {
                wasExpired = true
                onExpired?.invoke()
            }
            return
        }

        wasExpired = false

        // Check critical threshold
        val isCritical = timeInterval <= _criticalThreshold
        if (isCritical && !wasCritical) {
            wasCritical = true
            onCritical?.invoke()
        } else if (!isCritical) {
            wasCritical = false
        }

        // Update text color based on critical state
        val currentColor = if (isCritical) _criticalColor else _textColor
        timerTextView.setTextColor(currentColor.toColorInt())

        // Calculate time components
        val totalSeconds = timeInterval.toInt()
        val days = totalSeconds / 86400
        val hours = (totalSeconds % 86400) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        timerTextView.text = formatTime(days, hours, minutes, seconds)
    }

    private fun formatTime(days: Int, hours: Int, minutes: Int, seconds: Int): String {
        return when (_format) {
            "HH:MM:SS" -> {
                when {
                    _showDays && days > 0 -> String.format("%d:%02d:%02d:%02d", days, hours, minutes, seconds)
                    _hideZeroHours && hours == 0 -> String.format("%02d:%02d", minutes, seconds)
                    else -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
                }
            }
            "H:M:S" -> {
                when {
                    _showDays && days > 0 -> String.format("%d:%d:%d:%d", days, hours, minutes, seconds)
                    _hideZeroHours && hours == 0 -> String.format("%d:%d", minutes, seconds)
                    else -> String.format("%d:%d:%d", hours, minutes, seconds)
                }
            }
            "Hh Mm Ss" -> {
                when {
                    _showDays && days > 0 -> String.format("%dd %dh %dm %ds", days, hours, minutes, seconds)
                    _hideZeroHours && hours == 0 -> String.format("%dm %ds", minutes, seconds)
                    else -> String.format("%dh %dm %ds", hours, minutes, seconds)
                }
            }
            "H hours M minutes" -> {
                when {
                    _showDays && days > 0 -> String.format("%d days %d hours %d minutes", days, hours, minutes)
                    _hideZeroHours && hours == 0 -> String.format("%d minutes", minutes)
                    else -> String.format("%d hours %d minutes", hours, minutes)
                }
            }
            else -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    // MARK: - Timer Methods

    override fun start() {
        isRunning = true
        isPaused = false
        TimerManager.startTimer()
    }

    override fun pause() {
        isPaused = true
    }

    override fun resume() {
        isPaused = false
    }

    override fun reset() {
        wasExpired = false
        wasCritical = false
        updateDisplay()
    }

    override fun addSeconds(seconds: Double) {
        endDate?.let { currentEndDate ->
            endDate = Date(currentEndDate.time + (seconds * 1000).toLong())
            updateDisplay()
        }
    }

    override fun setEndTime(endTime: String) {
        setEndTimeFromString(endTime)
    }

    override fun getCurrentTime(): Map<String, Any> {
        val endDate = this.endDate ?: return mapOf(
            "days" to 0,
            "hours" to 0,
            "minutes" to 0,
            "seconds" to 0,
            "totalSeconds" to 0,
            "isExpired" to true,
            "isCritical" to false
        )

        val now = Date()
        val timeInterval = (endDate.time - now.time) / 1000.0

        if (timeInterval <= 0) {
            return mapOf(
                "days" to 0,
                "hours" to 0,
                "minutes" to 0,
                "seconds" to 0,
                "totalSeconds" to 0,
                "isExpired" to true,
                "isCritical" to false
            )
        }

        val totalSeconds = timeInterval.toInt()
        val days = totalSeconds / 86400
        val hours = (totalSeconds % 86400) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return mapOf(
            "days" to days,
            "hours" to hours,
            "minutes" to minutes,
            "seconds" to seconds,
            "totalSeconds" to totalSeconds,
            "isExpired" to false,
            "isCritical" to timeInterval <= _criticalThreshold
        )
    }

    private fun Double.dpToPx(): Double {
        val density = context.resources.displayMetrics.density
        return this * density
    }
}

// MARK: - Timer Manager Singleton

object TimerManager {
    private val handler = Handler(Looper.getMainLooper())
    private val timers = mutableListOf<HybridNitroTimer>()
    private var updateRunnable: Runnable? = null
    private var isRunning = false
    private var isAppInBackground = false

    fun addTimer(timer: HybridNitroTimer) {
        timers.add(timer)
    }

    fun removeTimer(timer: HybridNitroTimer) {
        timers.remove(timer)
        if (timers.isEmpty()) {
            stopTimer()
        }
    }

    fun startTimer() {
        if (!isRunning) {
            isRunning = true
            scheduleUpdate()
        }
    }

    fun onAppEnterBackground() {
        isAppInBackground = true
        // Pause timers that shouldn't continue in background
        for (timer in timers.toList()) {
            if (!timer._continueInBackground) {
                timer.pause()
            }
        }
    }

    fun onAppEnterForeground() {
        isAppInBackground = false
        // Resume timers that were paused due to backgrounding
        for (timer in timers.toList()) {
            if (!timer._continueInBackground && timer._autoStart) {
                timer.resume()
            }
        }
        // Force update all timers for accuracy
        updateTimers()
    }

    private fun stopTimer() {
        isRunning = false
        updateRunnable?.let { handler.removeCallbacks(it) }
        updateRunnable = null
    }

    private fun scheduleUpdate() {
        updateRunnable?.let { handler.removeCallbacks(it) }

        updateRunnable = Runnable {
            updateTimers()
            if (isRunning) {
                scheduleUpdate()
            }
        }

        handler.postDelayed(updateRunnable!!, 16) // ~60 FPS
    }

    private fun updateTimers() {
        for (timer in timers.toList()) {
            val shouldUpdate = timer.isRunning && !timer.isPaused

            // Handle visibility-based pausing
            if (timer._pauseWhenNotVisible) {
                // In a real implementation, you'd check if the timer's view is visible
                // For now, we'll assume timers are visible when app is in foreground
                val isVisible = !isAppInBackground
                if (!isVisible && shouldUpdate) {
                    continue // Skip update for invisible timers
                }
            }

            if (shouldUpdate) {
                timer.updateDisplay()
            }
        }
    }
}
