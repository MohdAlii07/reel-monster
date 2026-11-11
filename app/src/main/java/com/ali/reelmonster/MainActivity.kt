package com.ali.reelmonster

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : Activity() {
    private val preferences by lazy { getSharedPreferences(ReelTrackingService.PREFERENCES, MODE_PRIVATE) }
    private var level = 1
    private lateinit var countText: TextView
    private lateinit var todayText: TextView
    private lateinit var timeText: TextView
    private lateinit var historyList: LinearLayout
    private lateinit var modeText: TextView
    private lateinit var levelOneButton: Button
    private lateinit var levelTwoButton: Button
    private lateinit var trackingButton: Button
    private lateinit var manualButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = PAGE
        window.navigationBarColor = PAGE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        level = preferences.getInt(KEY_LEVEL, 1)
        renderScreen()
    }

    override fun onResume() {
        super.onResume()
        if (::countText.isInitialized) {
            refreshDashboard()
            updateLevelControls()
        }
    }

    private fun renderScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(28))
            setBackgroundColor(PAGE)
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = true
            addView(page)
            setOnApplyWindowInsetsListener { view, insets ->
                val topInset = if (Build.VERSION.SDK_INT >= 30) {
                    maxOf(
                        insets.getInsets(WindowInsets.Type.statusBars()).top,
                        insets.getInsets(WindowInsets.Type.displayCutout()).top
                    )
                } else {
                    @Suppress("DEPRECATION")
                    maxOf(insets.systemWindowInsetTop, if (Build.VERSION.SDK_INT >= 28) insets.displayCutout?.safeInsetTop ?: 0 else 0)
                }
                val bottomInset = if (Build.VERSION.SDK_INT >= 30) {
                    maxOf(
                        insets.getInsets(WindowInsets.Type.navigationBars()).bottom,
                        insets.getInsets(WindowInsets.Type.ime()).bottom
                    )
                } else {
                    @Suppress("DEPRECATION")
                    insets.systemWindowInsetBottom
                }
                view.setPadding(0, maxOf(topInset, dp(24)), 0, maxOf(bottomInset, dp(44)))
                insets
            }
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val appIcon = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            contentDescription = "Reel Monster app icon"
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        header.addView(appIcon, LinearLayout.LayoutParams(dp(48), dp(48)))
        val titleBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        titleBlock.addView(label("Reel Monster", 21, INK, true))
        titleBlock.addView(label("Your scroll companion", 13, MUTED, top = 2))
        header.addView(titleBlock)
        page.addView(header)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(16), dp(10), dp(16))
            background = rounded(TEAL, 24)
        }
        val heroCopy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        heroCopy.addView(label("Reels today", 14, Color.rgb(213, 239, 232), true))
        countText = label(todayCount().toString(), 48, Color.WHITE, true, top = 3)
        heroCopy.addView(countText)
        heroCopy.addView(label("counted on this phone", 12, Color.rgb(213, 239, 232), top = 1))
        hero.addView(heroCopy, LinearLayout.LayoutParams(0, -2, 1f))
        val monster = ImageView(this).apply {
            setImageResource(R.drawable.monster_character)
            contentDescription = "Your blue Reel Monster mascot"
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        hero.addView(monster, LinearLayout.LayoutParams(dp(126), dp(126)))
        page.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })

        val stats = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        todayText = label("", 14, INK, true, top = 5)
        timeText = label("", 14, INK, true, top = 5)
        stats.addView(statCard("TODAY", todayText), LinearLayout.LayoutParams(0, -2, 1f))
        stats.addView(statCard("WATCH TIME", timeText), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(10) })
        page.addView(stats, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        page.addView(label("Tracking", 20, INK, true, top = 23))
        page.addView(label("Choose how Reel Monster counts.", 14, MUTED, top = 3))

        val modeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        levelOneButton = Button(this).apply {
            text = "Level 1 · Manual"
            setOnClickListener { chooseLevel(1) }
        }
        levelTwoButton = Button(this).apply {
            text = "Level 2 · Auto"
            setOnClickListener { chooseLevel(2) }
        }
        modeRow.addView(levelOneButton, LinearLayout.LayoutParams(0, dp(50), 1f))
        modeRow.addView(levelTwoButton, LinearLayout.LayoutParams(0, dp(50), 1f).apply { leftMargin = dp(8) })
        page.addView(modeRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        modeText = label("", 13, MUTED, top = 9)
        page.addView(modeText)

        trackingButton = Button(this).apply {
            setOnClickListener {
                if (preferences.getBoolean(ReelTrackingService.KEY_ACTIVE, false)) stopTracking()
                else {
                    level = 2
                    preferences.edit().putInt(KEY_LEVEL, level).apply()
                    updateLevelControls()
                    prepareAutomaticTracking()
                }
            }
        }
        page.addView(trackingButton, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(12) })

        manualButton = Button(this).apply {
            text = "Count one reel"
            setOnClickListener {
                if (level == 1 && !preferences.getBoolean(ReelTrackingService.KEY_ACTIVE, false)) {
                    incrementToday()
                    refreshDashboard()
                }
            }
        }
        page.addView(manualButton, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(8) })

        page.addView(label("Last 7 days", 20, INK, true, top = 24))
        historyList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val historyHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(label("DAY", 11, MUTED, true), LinearLayout.LayoutParams(0, dp(32), 1f))
            addView(label("REELS", 11, MUTED, true).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            }, LinearLayout.LayoutParams(dp(96), dp(32)))
        }
        historyList.addView(historyHeader)
        val history = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
            background = rounded(Color.WHITE, 18)
            addView(historyList)
        }
        page.addView(history, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        page.addView(label("Your counts and watch time stay on this phone.", 12, MUTED, top = 17))
        updateLevelControls()
        refreshDashboard()
        setContentView(scroll)
        scroll.requestApplyInsets()
    }

    private fun updateLevelControls() {
        val active = preferences.getBoolean(ReelTrackingService.KEY_ACTIVE, false)
        levelOneButton.isSelected = level == 1
        levelTwoButton.isSelected = level == 2
        styleModeButton(levelOneButton, level == 1)
        styleModeButton(levelTwoButton, level == 2)
        levelOneButton.isEnabled = !active
        levelTwoButton.isEnabled = !active
        trackingButton.text = if (active) "Stop automatic tracking" else "Start automatic tracking"
        trackingButton.isEnabled = active || level == 2
        manualButton.isEnabled = !active && level == 1
        styleActionButton(trackingButton, primary = true)
        styleActionButton(manualButton, primary = false)
        modeText.text = when {
            active -> "Tracking session active. Screen analysis stays on this phone."
            !preferences.getString(ReelTrackingService.KEY_LAST_ERROR, null).isNullOrBlank() ->
                "Tracker could not start: ${preferences.getString(ReelTrackingService.KEY_LAST_ERROR, "")}. Tap Start to try again."
            level == 2 -> "Android will ask you to approve screen capture and display the monster bubble."
            else -> "Manual mode is ready. Tap once for each reel."
        }
    }

    private fun chooseLevel(selectedLevel: Int) {
        if (preferences.getBoolean(ReelTrackingService.KEY_ACTIVE, false)) return
        level = selectedLevel
        preferences.edit().putInt(KEY_LEVEL, level).apply()
        updateLevelControls()
    }

    private fun prepareAutomaticTracking() {
        if (!Settings.canDrawOverlays(this)) {
            modeText.text = "Allow Reel Monster to display over other apps to show the bubble."
            startActivityForResult(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                REQUEST_OVERLAY
            )
            return
        }
        if (!hasUsageAccess()) {
            modeText.text = "Allow usage access so the counter only watches Instagram and YouTube."
            startActivityForResult(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), REQUEST_USAGE)
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
            return
        }
        requestScreenCapture()
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
    }

    private fun requestScreenCapture() {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_OVERLAY, REQUEST_USAGE -> prepareAutomaticTracking()
            REQUEST_CAPTURE -> {
                if (resultCode == RESULT_OK && data != null) {
                    val service = Intent(this, ReelTrackingService::class.java)
                        .setAction(ReelTrackingService.ACTION_START)
                        .putExtra(ReelTrackingService.EXTRA_RESULT_CODE, resultCode)
                        .putExtra(ReelTrackingService.EXTRA_RESULT_DATA, data)
                    if (Build.VERSION.SDK_INT >= 26) startForegroundService(service) else startService(service)
                    preferences.edit().putBoolean(ReelTrackingService.KEY_ACTIVE, true).apply()
                    updateLevelControls()
                } else {
                    modeText.text = "Screen capture was not approved. Tracking has not started."
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) requestScreenCapture()
            else modeText.text = "Allow notifications to see when tracking is active, then try again."
        }
    }

    private fun stopTracking() {
        startService(Intent(this, ReelTrackingService::class.java).setAction(ReelTrackingService.ACTION_STOP))
        preferences.edit().putBoolean(ReelTrackingService.KEY_ACTIVE, false).apply()
        updateLevelControls()
    }

    private fun todayCount(): Int = preferences.getInt(dayCountKey(LocalDate.now()), 0)

    private fun incrementToday() {
        val key = dayCountKey(LocalDate.now())
        preferences.edit().putInt(key, preferences.getInt(key, 0) + 1).apply()
    }

    private fun refreshDashboard() {
        val today = LocalDate.now()
        todayText.text = "${todayCount()} reels · Instagram + YouTube"
        val seconds = preferences.getLong(dayTimeKey(today), 0L)
        timeText.text = if (seconds == 0L) "No tracked time yet" else formatDuration(seconds)
        historyList.removeViews(1, historyList.childCount - 1)
        (0..6).forEach { offset ->
            val date = today.minusDays(offset.toLong())
            val name = if (offset == 0) "Today" else date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(3), 0, dp(3))
            }
            row.addView(label(name, 14, INK), LinearLayout.LayoutParams(0, dp(34), 1f))
            row.addView(label("${preferences.getInt(dayCountKey(date), 0)}", 14, INK, true).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            }, LinearLayout.LayoutParams(dp(96), dp(34)))
            historyList.addView(row)
        }
        countText.text = todayCount().toString()
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (hours > 0) "$hours hr $minutes min on Instagram + YouTube" else "$minutes min on Instagram + YouTube"
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean = false, top: Int = 0) = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER_VERTICAL
        if (top > 0) setPadding(0, dp(top), 0, 0)
    }

    private fun statCard(title: String, value: TextView) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(12), dp(15), dp(13))
        background = rounded(Color.WHITE, 18)
        addView(label(title, 11, MUTED, true))
        addView(value)
    }

    private fun rounded(color: Int, radius: Int, strokeColor: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        strokeColor?.let { setStroke(dp(1), it) }
    }

    private fun styleModeButton(button: Button, selected: Boolean) {
        button.textSize = 13f
        button.isAllCaps = false
        button.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        button.setTextColor(if (selected) Color.WHITE else INK)
        button.background = rounded(if (selected) TEAL else Color.WHITE, 16, if (selected) TEAL else LINE)
        button.setPadding(dp(6), 0, dp(6), 0)
        button.minimumHeight = dp(48)
        button.stateListAnimator = null
    }

    private fun styleActionButton(button: Button, primary: Boolean) {
        button.textSize = 15f
        button.isAllCaps = false
        button.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        button.setTextColor(if (primary) Color.WHITE else TEAL)
        button.background = rounded(if (primary) TEAL else Color.WHITE, 16, if (primary) null else LINE)
        button.setPadding(dp(12), 0, dp(12), 0)
        button.minimumHeight = dp(48)
        button.stateListAnimator = null
        button.alpha = if (button.isEnabled) 1f else 0.45f
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private val PAGE = Color.rgb(246, 248, 244)
        private val INK = Color.rgb(24, 48, 45)
        private val MUTED = Color.rgb(82, 105, 98)
        private val TEAL = Color.rgb(24, 111, 98)
        private val LINE = Color.rgb(213, 224, 216)
        private const val KEY_LEVEL = "tracking_level"
        private const val REQUEST_OVERLAY = 201
        private const val REQUEST_USAGE = 202
        private const val REQUEST_CAPTURE = 203
        private const val REQUEST_NOTIFICATIONS = 204
        private fun dayCountKey(date: LocalDate) = "count_$date"
        private fun dayTimeKey(date: LocalDate) = "time_$date"
    }
}
