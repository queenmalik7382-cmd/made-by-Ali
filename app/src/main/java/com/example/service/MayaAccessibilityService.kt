package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.core.FlavorConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class RecordedActionStep(
    val action: String,
    val target: String,
    val timestamp: Long = System.currentTimeMillis()
)

class MayaAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (!FlavorConfig.supportsAccessibilityAutomation) {
            disableSelf()
            return
        }
        instance = this
        _isConnectedFlow.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!FlavorConfig.supportsAccessibilityAutomation || event == null) return

        // Record sequence of actions when user activates Yaadein Task Recorder
        if (_isRecordingTask.value && event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val label = event.text?.joinToString(" ")?.trim().orEmpty()
                .ifBlank { event.contentDescription?.toString()?.trim().orEmpty() }
            val pkg = event.packageName?.toString().orEmpty()
            if (label.isNotBlank() && !pkg.contains("myassistant")) {
                val current = _recordedSteps.value.toMutableList()
                current.add(RecordedActionStep(action = "findAndClick", target = label))
                _recordedSteps.value = current
            }
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance === this) instance = null
        _isConnectedFlow.value = false
        return super.onUnbind(intent)
    }

    fun readScreenSummary(maxNodes: Int = 45): String {
        val root = rootInActiveWindow ?: return "Screen par abhi koi active window nahi mili."
        val items = mutableListOf<String>()
        collectNodeDescriptions(root, items, maxNodes)
        if (items.isEmpty()) return "Screen khali hai ya koi readable text nahi mila."
        return "Package: ${root.packageName}\nElements:\n" + items.joinToString("\n") { "• $it" }
    }

    private fun collectNodeDescriptions(
        node: AccessibilityNodeInfo?,
        out: MutableList<String>,
        limit: Int
    ) {
        if (node == null || out.size >= limit) return
        val txt = node.text?.toString()?.trim().orEmpty()
        val desc = node.contentDescription?.toString()?.trim().orEmpty()
        val label = txt.ifBlank { desc }
        if (label.isNotBlank()) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            val clickableTag = if (node.isClickable) "[tap]" else ""
            val editableTag = if (node.isEditable) "[input]" else ""
            out.add("$label $clickableTag$editableTag (${bounds.centerX()},${bounds.centerY()})".trim())
        }
        for (i in 0 until node.childCount) {
            if (out.size >= limit) break
            collectNodeDescriptions(node.getChild(i), out, limit)
        }
    }

    fun findAndClick(queryLabel: String, longClick: Boolean = false): Boolean {
        val root = rootInActiveWindow ?: return false
        val target = findMatchingNode(root, queryLabel.trim()) ?: return false
        var clickable: AccessibilityNodeInfo? = target
        while (clickable != null && !clickable.isClickable && !clickable.isLongClickable) {
            clickable = clickable.parent
        }
        val actionCode = if (longClick) AccessibilityNodeInfo.ACTION_LONG_CLICK else AccessibilityNodeInfo.ACTION_CLICK
        if (clickable != null && clickable.performAction(actionCode)) {
            return true
        }
        val rect = Rect()
        target.getBoundsInScreen(rect)
        return tapCoordinates(rect.centerX().toFloat(), rect.centerY().toFloat(), if (longClick) 650L else 85L)
    }

    private fun findMatchingNode(node: AccessibilityNodeInfo?, query: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val q = query.lowercase()
        val txt = node.text?.toString()?.lowercase().orEmpty()
        val desc = node.contentDescription?.toString()?.lowercase().orEmpty()
        if (txt.contains(q) || desc.contains(q)) {
            return node
        }
        for (i in 0 until node.childCount) {
            findMatchingNode(node.getChild(i), query)?.let { return it }
        }
        return null
    }

    fun tapCoordinates(x: Float, y: Float, durationMs: Long = 80L): Boolean {
        val path = Path().apply { moveTo(x.coerceAtLeast(1f), y.coerceAtLeast(1f)) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, durationMs))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    fun swipeScreen(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 320L): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, durationMs))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    fun scrollDirection(forward: Boolean = true): Boolean {
        val root = rootInActiveWindow
        if (root != null) {
            val scrollable = findFirstScrollable(root)
            if (scrollable != null) {
                val act = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                if (scrollable.performAction(act)) return true
            }
        }
        val dm = resources.displayMetrics
        val cx = dm.widthPixels * 0.5f
        return if (forward) {
            swipeScreen(cx, dm.heightPixels * 0.76f, cx, dm.heightPixels * 0.24f)
        } else {
            swipeScreen(cx, dm.heightPixels * 0.26f, cx, dm.heightPixels * 0.76f)
        }
    }

    private fun findFirstScrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            findFirstScrollable(node.getChild(i))?.let { return it }
        }
        return null
    }

    fun typeTextIntoFocusedOrFirstInput(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val target = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: findFirstEditable(root) ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun findFirstEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            findFirstEditable(node.getChild(i))?.let { return it }
        }
        return null
    }

    fun performNavGlobal(target: String): Boolean = when (target.lowercase()) {
        "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
        "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
        "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
        "notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        "screenshot" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else false
        "lock" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else false
        else -> false
    }

    /**
     * Automates enabling Developer Options (7x Build Number taps across OEM variants) + Wireless Debugging.
     */
    suspend fun automateDeveloperOptionsAndWirelessDebugging(): String {
        abortAutomationRequested = false
        val buildLabels = listOf("Build number", "MIUI version", "OS version", "Software information", "Version")

        // Step 1: Open About Phone
        val aboutIntent = Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(aboutIntent)
        delay(1200)

        // Samsung / OneUI puts Build Number inside Software information
        findAndClick("Software information")
        delay(700)

        var foundBuild = false
        for (attempt in 0 until 5) {
            if (abortAutomationRequested) return "Ruko bola gaya — automation rok di gayi."
            for (lbl in buildLabels) {
                val root = rootInActiveWindow
                if (root != null && findMatchingNode(root, lbl) != null) {
                    repeat(7) {
                        if (abortAutomationRequested) return "Automation rok di gayi."
                        findAndClick(lbl)
                        delay(180)
                    }
                    foundBuild = true
                    break
                }
            }
            if (foundBuild) break
            scrollDirection(forward = true)
            delay(650)
        }

        // Step 2: Open Developer Options directly and find Wireless debugging
        val devIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(devIntent) }
        delay(1300)

        val wirelessLabels = listOf("Wireless debugging", "Wireless ADB debugging", "Wi-Fi debugging")
        for (scrollStep in 0 until 8) {
            if (abortAutomationRequested) return "Automation rok di gayi."
            for (wLabel in wirelessLabels) {
                if (findAndClick(wLabel)) {
                    delay(800)
                    findAndClick("Allow")
                    return "Developer options khol kar Wireless debugging dhoond liya aur tap kar diya."
                }
            }
            scrollDirection(forward = true)
            delay(650)
        }

        return if (foundBuild) {
            "Build number 7 baar tap ho gaya (agar PIN maanga hai to daalo), aur Developer Options screen khol di hai."
        } else {
            "About Phone aur Developer Options screen khol di hai; OEM menu me Wireless debugging par tap karo."
        }
    }

    /**
     * Replays a Yaadein saved task JSON sequence with parameter substitution and emergency stop check.
     */
    suspend fun replayLearnedTaskSteps(
        stepsJson: String,
        promptValue: String,
        openAppCallback: (String) -> Boolean
    ): String {
        abortAutomationRequested = false
        val arr = runCatching { JSONArray(stepsJson) }.getOrNull() ?: return "Task steps invalid hain."
        var completed = 0
        for (i in 0 until arr.length()) {
            if (abortAutomationRequested) return "Ruko command mili — $completed steps ke baad rok diya."
            val step = arr.getJSONObject(i)
            val act = step.optString("action")
            val rawTarget = step.optString("target").replace("{prompt}", promptValue)
            when (act) {
                "openApp" -> openAppCallback(rawTarget)
                "wait" -> delay(rawTarget.toLongOrNull()?.coerceIn(100L, 6000L) ?: 900L)
                "findAndClick" -> findAndClick(rawTarget)
                "typeText" -> typeTextIntoFocusedOrFirstInput(rawTarget)
                "scroll" -> scrollDirection(forward = rawTarget != "up")
                "back" -> performNavGlobal("back")
                "home" -> performNavGlobal("home")
                "readScreen" -> {
                    val summary = readScreenSummary(15)
                    return "Task poora hua ($completed steps). Screen summary: ${summary.take(240)}"
                }
            }
            completed++
            delay(550)
        }
        return "Sikha hua task safalta se poora chala ($completed steps)."
    }

    companion object {
        @Volatile
        var instance: MayaAccessibilityService? = null
            private set

        @Volatile
        var abortAutomationRequested: Boolean = false

        private val _isConnectedFlow = MutableStateFlow(false)
        val isConnectedFlow: StateFlow<Boolean> = _isConnectedFlow.asStateFlow()

        private val _isRecordingTask = MutableStateFlow(false)
        val isRecordingTask: StateFlow<Boolean> = _isRecordingTask.asStateFlow()

        private val _recordedSteps = MutableStateFlow<List<RecordedActionStep>>(emptyList())
        val recordedSteps: StateFlow<List<RecordedActionStep>> = _recordedSteps.asStateFlow()

        fun startTaskRecording() {
            _recordedSteps.value = emptyList()
            _isRecordingTask.value = true
        }

        fun stopTaskRecording(): List<RecordedActionStep> {
            _isRecordingTask.value = false
            return _recordedSteps.value
        }

        fun triggerEmergencyStop() {
            abortAutomationRequested = true
            _isRecordingTask.value = false
        }
    }
}
