package com.example.tools

import org.json.JSONArray
import org.json.JSONObject

data class ToolResult(
    val ok: Boolean,
    val spokenFeedback: String,
    val payload: JSONObject = JSONObject(),
    val requiresConfirmation: Boolean = false,
    val confirmationPrompt: String = ""
) {
    fun toJson(): JSONObject = JSONObject()
        .put("ok", ok)
        .put("spokenFeedback", spokenFeedback)
        .put("data", payload)
}

interface Tool {
    val name: String
    val description: String
    val isDangerous: Boolean
    fun parametersSchema(): JSONObject
    suspend fun execute(args: JSONObject, confirmed: Boolean = false): ToolResult

    fun toGeminiDeclaration(): JSONObject = JSONObject()
        .put("name", name)
        .put("description", description)
        .put("parameters", parametersSchema())
}

data class PendingToolConfirmation(
    val callId: String,
    val toolName: String,
    val args: JSONObject,
    val promptSpoken: String
)

class ToolRegistry {
    private val tools = linkedMapOf<String, Tool>()

    fun register(tool: Tool) {
        tools[tool.name] = tool
    }

    fun get(name: String): Tool? = tools[name]

    fun allTools(): List<Tool> = tools.values.toList()

    fun geminiFunctionDeclarations(): JSONArray {
        val arr = JSONArray()
        tools.values.forEach { arr.put(it.toGeminiDeclaration()) }
        return arr
    }

    suspend fun invokeTool(name: String, args: JSONObject, confirmed: Boolean = false): ToolResult {
        val tool = tools[name]
            ?: return ToolResult(false, "Ye tool ($name) abhi Maya me registered nahi hai.")
        if (tool.isDangerous && !confirmed) {
            val summary = buildActionSummary(name, args)
            return ToolResult(
                ok = true,
                spokenFeedback = "$summary — Pakka bhej doon ya kar doon?",
                requiresConfirmation = true,
                confirmationPrompt = "$summary — Pakka bhej doon?"
            )
        }
        return runCatching { tool.execute(args, confirmed = true) }
            .getOrElse { ToolResult(false, "Tool chalane me dikkat aayi: ${it.localizedMessage}") }
    }

    /**
     * Parses local/offline Hinglish voice commands into structured tool invocations when offline or in fallback mode.
     */
    fun matchLocalHinglishIntent(utterance: String): Pair<String, JSONObject>? {
        val text = utterance.trim().lowercase()
        if (text.isBlank()) return null

        // Emergency stop handled prior, check Developer Options / Wireless Debugging
        if (text.contains("developer option") || text.contains("wireless debugging")) {
            return "enable_wireless_debugging" to JSONObject()
        }
        // Call answering / hanging up
        if (text.contains("call utha") || text.contains("phone utha") || text == "answer call") {
            return "manage_active_call" to JSONObject().put("action", "answer")
        }
        if (text.contains("call kaat") || text.contains("phone kaat") || text == "end call") {
            return "manage_active_call" to JSONObject().put("action", "end")
        }
        // Call someone: "ali ko call lagao"
        val callRegex = Regex("""([a-z0-9 ]+?)\s+ko\s+call""")
        callRegex.find(text)?.let { m ->
            val target = m.groupValues[1].trim()
            if (target.isNotBlank()) {
                return "make_phone_call" to JSONObject().put("contactName", target)
            }
        }
        // Open app: "youtube kholo", "instagram kholo", "tiktok kholo"
        val openRegex = Regex("""([a-z0-9 .]+?)\s+(kholo|open karo|chalu karo)""")
        openRegex.find(text)?.let { m ->
            val app = m.groupValues[1].replace("app", "").trim()
            if (app.isNotBlank()) {
                return "open_app" to JSONObject().put("appName", app)
            }
        }
        if (text.startsWith("open ")) {
            val app = text.removePrefix("open ").trim()
            if (app.isNotBlank()) return "open_app" to JSONObject().put("appName", app)
        }
        // Routine queries
        if (text.contains("abhi mera kya kaam") || text.contains("aaj ki routine")) {
            return "query_routine" to JSONObject().put("mode", if (text.contains("abhi")) "now" else "today")
        }
        // Notifications readback
        if (text.contains("pichli notification") || text.contains("notifications sunao")) {
            return "read_recent_notifications" to JSONObject().put("count", 5)
        }
        // YouTube Channel analysis
        if (text.contains("channel ka analysis") || text.contains("youtube analysis")) {
            return "analyze_youtube_channel" to JSONObject()
        }
        // Flashlight
        if (text.contains("torch") || text.contains("flashlight")) {
            val turnOn = !text.contains("band") && !text.contains("off")
            return "device_setting_control" to JSONObject()
                .put("setting", "flashlight")
                .put("value", if (turnOn) "on" else "off")
        }
        // Volume
        if (text.contains("volume") || text.contains("awaaz")) {
            val dir = when {
                text.contains("badha") || text.contains("up") || text.contains("zyada") -> "up"
                text.contains("kam") || text.contains("down") -> "down"
                else -> "up"
            }
            return "device_setting_control" to JSONObject().put("setting", "volume").put("value", dir)
        }
        // Screen actions: scroll / next video / like
        if (text.contains("next video") || text.contains("agli video") || text.contains("upar swipe")) {
            return "screen_automation" to JSONObject().put("action", "swipe_up")
        }
        if (text.contains("like karo")) {
            return "screen_automation" to JSONObject().put("action", "find_and_click").put("label", "Like")
        }
        return null
    }

    companion object {
        fun buildActionSummary(name: String, args: JSONObject): String = when (name) {
            "make_phone_call" -> "${args.optString("contactName", args.optString("phoneNumber", "Contact"))} ko phone call lagana"
            "compose_sms" -> "${args.optString("recipient", "number")} ke liye SMS compose kholna: \"${args.optString("body")}\""
            "delete_memory_or_task" -> "${args.optString("itemType", "item")} hatana"
            else -> "$name chalana"
        }

        fun schemaObject(vararg props: Pair<String, Pair<String, String>>, required: List<String> = emptyList()): JSONObject {
            val propObj = JSONObject()
            props.forEach { (key, typeAndDesc) ->
                propObj.put(
                    key,
                    JSONObject()
                        .put("type", typeAndDesc.first)
                        .put("description", typeAndDesc.second)
                )
            }
            val root = JSONObject()
                .put("type", "OBJECT")
                .put("properties", propObj)
            if (required.isNotEmpty()) {
                val reqArr = JSONArray()
                required.forEach { reqArr.put(it) }
                root.put("required", reqArr)
            }
            return root
        }
    }
}
