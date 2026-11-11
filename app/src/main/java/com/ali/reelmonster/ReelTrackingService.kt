package com.ali.reelmonster

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalDate

/** Screen pixels are sampled in memory and discarded; no frame or image is saved or uploaded. */
class ReelTrackingService : Service() {
    private val preferences by lazy { getSharedPreferences(PREFERENCES, MODE_PRIVATE) }
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var windowManager: WindowManager
    private lateinit var workerThread: HandlerThread
    private lateinit var worker: Handler
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var bubble: LinearLayout? = null
    private var bubbleCountText: TextView? = null
    private var bubbleHintText: TextView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var currentPackage: String? = null
    private var lastUsageQuery = System.currentTimeMillis() - 60_000
    private var previousGrid: IntArray? = null
    private var swipeDirection = 0
    private var swipeFrames = 0
    private var lastSwipeCountAt = 0L
    private val initializedTargetApps = mutableSetOf<String>()
    private var lastSampleAt = 0L
    private var lastCountAt = 0L
    private var lastSecondAt = 0L
    private var reelCount = 0
    private var isTargetForeground = false

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        workerThread = HandlerThread("reel-monster-local-capture").apply { start() }
        worker = Handler(workerThread.looper)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action != ACTION_START) return START_NOT_STICKY

        preferences.edit().remove(KEY_LAST_ERROR).apply()
        startTrackingForeground()
        try {
            val data = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                ?: throw IllegalArgumentException("Missing screen-capture approval")
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, data)
            startCapture()
            showBubble()
            preferences.edit().putBoolean(KEY_ACTIVE, true).apply()
            reelCount = preferences.getInt(dayCountKey(LocalDate.now()), 0)
            lastSecondAt = System.currentTimeMillis()
            worker.post(monitorRunnable)
            updateBubble()
        } catch (error: Exception) {
            Log.e(TAG, "Could not start local screen tracking", error)
            preferences.edit().putString(KEY_LAST_ERROR, error.message ?: error.javaClass.simpleName).apply()
            preferences.edit().putBoolean(KEY_ACTIVE, false).apply()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startTrackingForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startCapture() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader
        reader.setOnImageAvailableListener({ source ->
            val image = source.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                sampleImage(image)
            } finally {
                image.close()
            }
        }, worker)

        projection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                stopSelf()
            }
        }, worker)
        virtualDisplay = projection?.createVirtualDisplay(
            "Reel Monster local preview",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            worker
        ) ?: throw IllegalStateException("Screen capture could not start")
    }

    private fun showBubble() {
        if (!android.provider.Settings.canDrawOverlays(this)) throw SecurityException("Overlay permission missing")
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(7), dp(6), dp(14), dp(6))
            background = GradientDrawable().apply {
                setColor(Color.rgb(248, 251, 248))
                cornerRadius = dp(28).toFloat()
                setStroke(dp(1), Color.rgb(198, 222, 211))
            }
            elevation = dp(8).toFloat()
            setOnTouchListener(DragTouchListener())
        }
        val monster = ImageView(this).apply {
            setImageResource(R.drawable.monster_character)
            contentDescription = "Reel Monster mascot"
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        view.addView(monster, LinearLayout.LayoutParams(dp(48), dp(48)))
        val copy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, 0, 0)
        }
        copy.addView(TextView(this).apply {
            text = "REEL MONSTER"
            setTextColor(Color.rgb(75, 100, 91))
            textSize = 9f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        bubbleCountText = TextView(this).apply {
            setTextColor(Color.rgb(23, 58, 51))
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        copy.addView(bubbleCountText)
        bubbleHintText = TextView(this).apply {
            setTextColor(Color.rgb(75, 100, 91))
            textSize = 10f
        }
        copy.addView(bubbleHintText)
        view.addView(copy, LinearLayout.LayoutParams(-2, -2))
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(12)
            y = dp(150)
        }
        windowManager.addView(view, params)
        bubble = view
        bubbleParams = params
    }

    private fun sampleImage(image: Image) {
        val now = System.currentTimeMillis()
        if (now - lastSampleAt < 120L) return
        lastSampleAt = now
        val target = currentPackage
        if (!isShortVideoPackage(target)) {
            resetSwipeDetector()
            return
        }
        if (initializedTargetApps.add(target!!)) {
            incrementReelCount()
            Log.i(TAG, "counted first visible reel package=$target")
        }
        val current = sampleGrid(image) ?: return
        val previous = previousGrid
        val direction = if (previous == null) 0 else detectVerticalShift(previous, current)
        if (direction != 0) {
            if (direction == swipeDirection) swipeFrames++ else {
                swipeDirection = direction
                swipeFrames = 1
            }
            Log.d(TAG, "vertical transition direction=$direction frames=$swipeFrames package=$target")
            if (swipeFrames >= SWIPE_FRAMES_REQUIRED && now - lastSwipeCountAt >= SWIPE_COUNT_COOLDOWN_MS) {
                lastSwipeCountAt = now
                swipeFrames = 0
                incrementReelCount()
                Log.i(TAG, "counted reel after vertical transition package=$target")
            }
        } else {
            swipeDirection = 0
            swipeFrames = 0
        }
        previousGrid = current
    }

    private fun sampleGrid(image: Image): IntArray? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val start = buffer.position()
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val columns = GRID_COLUMNS
        val rows = GRID_ROWS
        val values = IntArray(columns * rows)
        for (row in 0 until rows) {
            val y = (image.height * (0.10 + row * 0.80 / (rows - 1))).toInt().coerceIn(0, image.height - 1)
            for (column in 0 until columns) {
                val x = (image.width * (0.10 + column * 0.80 / (columns - 1))).toInt().coerceIn(0, image.width - 1)
                val offset = start + y * rowStride + x * pixelStride
                if (offset + 2 >= buffer.limit()) return null
                val red = buffer.get(offset).toInt() and 0xff
                val green = buffer.get(offset + 1).toInt() and 0xff
                val blue = buffer.get(offset + 2).toInt() and 0xff
                values[row * columns + column] = (red * 299 + green * 587 + blue * 114) / 1000
            }
        }
        return values
    }

    /** Finds a coherent vertical movement between adjacent frames, as produced by a reel swipe. */
    private fun detectVerticalShift(previous: IntArray, current: IntArray): Int {
        var baseline = 0L
        var baselineSamples = 0
        for (row in 2 until GRID_ROWS - 2) {
            for (column in 1 until GRID_COLUMNS - 1) {
                val index = row * GRID_COLUMNS + column
                baseline += kotlin.math.abs(previous[index] - current[index])
                baselineSamples++
            }
        }
        if (baselineSamples == 0) return 0
        val baselineAverage = baseline / baselineSamples
        if (baselineAverage < MIN_BASELINE_DIFFERENCE) return 0

        var bestShift = 0
        var bestAverage = Long.MAX_VALUE
        for (shift in -MAX_VERTICAL_SHIFT..MAX_VERTICAL_SHIFT) {
            if (shift == 0) continue
            var difference = 0L
            var samples = 0
            for (row in 2 until GRID_ROWS - 2) {
                val shiftedRow = row + shift
                if (shiftedRow !in 0 until GRID_ROWS) continue
                for (column in 1 until GRID_COLUMNS - 1) {
                    val oldIndex = row * GRID_COLUMNS + column
                    val newIndex = shiftedRow * GRID_COLUMNS + column
                    difference += kotlin.math.abs(previous[oldIndex] - current[newIndex])
                    samples++
                }
            }
            if (samples > 0) {
                val average = difference / samples
                if (average < bestAverage) {
                    bestAverage = average
                    bestShift = shift
                }
            }
        }
        return if (bestShift != 0 && bestAverage < baselineAverage * SHIFT_IMPROVEMENT_PERCENT / 100L && bestAverage < MAX_SHIFTED_DIFFERENCE) {
            if (bestShift > 0) 1 else -1
        } else 0
    }

    private fun resetSwipeDetector() {
        isTargetForeground = false
        previousGrid = null
        swipeDirection = 0
        swipeFrames = 0
    }

    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (preferences.getBoolean(KEY_ACTIVE, false)) {
                refreshForegroundPackage()
                val now = System.currentTimeMillis()
                val elapsed = ((now - lastSecondAt) / 1000L).coerceIn(0L, 2L)
                lastSecondAt = now
                if (isShortVideoPackage(currentPackage) && elapsed > 0L) {
                    val key = dayTimeKey(LocalDate.now())
                    preferences.edit().putLong(key, preferences.getLong(key, 0L) + elapsed).apply()
                }
                updateBubble()
                worker.postDelayed(this, 1000L)
            }
        }
    }

    private fun refreshForegroundPackage() {
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(lastUsageQuery, now)
        val event = UsageEvents.Event()
        var latest = currentPackage
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED, UsageEvents.Event.MOVE_TO_FOREGROUND -> latest = event.packageName
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.MOVE_TO_BACKGROUND -> if (latest == event.packageName) latest = null
            }
        }
        lastUsageQuery = now
        if (latest != currentPackage) {
            resetSwipeDetector()
            currentPackage = latest
            isTargetForeground = isShortVideoPackage(latest)
            Log.i(TAG, "foreground package=$latest target=$isTargetForeground")
        }
    }

    private fun isShortVideoPackage(name: String?): Boolean =
        name == INSTAGRAM_PACKAGE || name == YOUTUBE_PACKAGE

    private fun incrementReelCount() {
        val key = dayCountKey(LocalDate.now())
        reelCount = preferences.getInt(key, 0) + 1
        preferences.edit().putInt(key, reelCount).apply()
        updateBubble()
    }

    private fun updateBubble() {
        val count = preferences.getInt(dayCountKey(LocalDate.now()), reelCount)
        val hint = when ((count / 5) % 4) {
            0 -> "reels counted today"
            1 -> "feed me!"
            2 -> "still scrolling?"
            else -> "tiny break?"
        }
        bubble?.post {
            bubbleCountText?.text = "$count reels"
            bubbleHintText?.text = hint
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, ReelTrackingService::class.java).setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Reel Monster is tracking")
            .setContentText("Only local screen sampling · tap Stop to end")
            .setOngoing(true)
            .addAction(Notification.Action.Builder(android.R.drawable.ic_media_pause, "Stop", stopPending).build())
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Reel Monster tracking", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private inner class DragTouchListener : View.OnTouchListener {
        private var startX = 0
        private var startY = 0
        private var downX = 0f
        private var downY = 0f

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            val params = bubbleParams ?: return false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    downX = event.rawX
                    downY = event.rawY
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX - (event.rawX - downX).toInt()
                    params.y = startY + (event.rawY - downY).toInt()
                    windowManager.updateViewLayout(view, params)
                    return true
                }
                MotionEvent.ACTION_UP -> return true
            }
            return false
        }
    }

    override fun onDestroy() {
        worker.removeCallbacks(monitorRunnable)
        preferences.edit().putBoolean(KEY_ACTIVE, false).apply()
        try {
            bubble?.let { windowManager.removeView(it) }
        } catch (_: Exception) {
        }
        bubble = null
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        projection?.stop()
        projection = null
        if (::workerThread.isInitialized) workerThread.quitSafely()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val PREFERENCES = "reel-monster"
        const val KEY_ACTIVE = "tracking_active"
        const val ACTION_START = "com.ali.reelmonster.START_TRACKING"
        const val ACTION_STOP = "com.ali.reelmonster.STOP_TRACKING"
        const val EXTRA_RESULT_CODE = "projection_result_code"
        const val EXTRA_RESULT_DATA = "projection_result_data"
        const val KEY_LAST_ERROR = "tracker_last_error"
        private const val TAG = "ReelMonster"
        private const val CHANNEL_ID = "reel_monster_tracking"
        private const val NOTIFICATION_ID = 23
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"
        private const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        private const val GRID_COLUMNS = 16
        private const val GRID_ROWS = 24
        private const val MAX_VERTICAL_SHIFT = 6
        private const val SHIFT_IMPROVEMENT_PERCENT = 72
        private const val MIN_BASELINE_DIFFERENCE = 16
        private const val MAX_SHIFTED_DIFFERENCE = 42
        // Instagram's vertical transition can be a single sampled frame at this device's capture rate.
        private const val SWIPE_FRAMES_REQUIRED = 1
        private const val SWIPE_COUNT_COOLDOWN_MS = 1200L
        private fun dayCountKey(date: LocalDate) = "count_$date"
        private fun dayTimeKey(date: LocalDate) = "time_$date"
    }
}
