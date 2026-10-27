package com.example.data

import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class MayaRepository(private val dao: MayaDao) {
    val messages: Flow<List<ChatMessageEntity>> = dao.observeMessages()
    val capturedNotifications: Flow<List<CapturedNotificationEntity>> = dao.observeCapturedNotifications()
    val routineTasks: Flow<List<RoutineTaskEntity>> = dao.observeRoutineTasks()
    val learnedTasks: Flow<List<LearnedTaskEntity>> = dao.observeLearnedTasks()
    val memories: Flow<List<UserMemoryEntity>> = dao.observeMemories()
    val rules: Flow<List<AssistantRuleEntity>> = dao.observeRules()
    val callNotes: Flow<List<CallNoteEntity>> = dao.observeCallNotes()
    val announcements: Flow<List<AppAnnouncementEntity>> = dao.observeAnnouncements()

    suspend fun seedDefaultsIfEmpty() {
        if (dao.getAllLearnedTasks().isEmpty()) {
            val builtIns = listOf(
                LearnedTaskEntity(
                    name = "gemini se poochho",
                    categoryTag = "sawaal",
                    stepCount = 8,
                    isBuiltIn = true,
                    stepsJson = buildStepsJson(
                        "openApp:Google Gemini",
                        "wait:1200",
                        "findAndClick:Ask Gemini",
                        "typeText:{prompt}",
                        "wait:400",
                        "findAndClick:Send",
                        "wait:3500",
                        "readScreen:summary"
                    )
                ),
                LearnedTaskEntity(
                    name = "chatgpt se poochho",
                    categoryTag = "sawaal",
                    stepCount = 8,
                    isBuiltIn = true,
                    stepsJson = buildStepsJson(
                        "openApp:ChatGPT",
                        "wait:1200",
                        "findAndClick:Message",
                        "typeText:{prompt}",
                        "wait:400",
                        "findAndClick:Send",
                        "wait:3500",
                        "readScreen:summary"
                    )
                ),
                LearnedTaskEntity(
                    name = "gemini video",
                    categoryTag = "video prompt",
                    stepCount = 16,
                    isBuiltIn = true,
                    stepsJson = buildStepsJson(
                        "openApp:Chrome",
                        "wait:1000",
                        "findAndClick:Search or type web address",
                        "typeText:https://aistudio.google.com",
                        "findAndClick:Go",
                        "wait:2000",
                        "typeText:{prompt}",
                        "readScreen:summary"
                    )
                ),
                LearnedTaskEntity(
                    name = "gemini image",
                    categoryTag = "image prompt",
                    stepCount = 17,
                    isBuiltIn = true,
                    stepsJson = buildStepsJson(
                        "openApp:Google Gemini",
                        "wait:1200",
                        "findAndClick:Ask Gemini",
                        "typeText:Create high quality image: {prompt}",
                        "findAndClick:Send",
                        "wait:4500",
                        "readScreen:summary"
                    )
                ),
                LearnedTaskEntity(
                    name = "chatgpt image",
                    categoryTag = "image prompt",
                    stepCount = 14,
                    isBuiltIn = true,
                    stepsJson = buildStepsJson(
                        "openApp:ChatGPT",
                        "wait:1200",
                        "findAndClick:Message",
                        "typeText:Generate an image of: {prompt}",
                        "findAndClick:Send",
                        "wait:4500",
                        "readScreen:summary"
                    )
                )
            )
            builtIns.forEach { dao.insertLearnedTask(it) }
        }

        if (dao.getActiveRules().isEmpty()) {
            dao.insertRule(
                AssistantRuleEntity(
                    ruleText = "Bina poochhe kisi ko call mat lagana aur na hi payment/OTP screen par tap karna.",
                    isStrictBlock = true
                )
            )
            dao.insertRule(
                AssistantRuleEntity(
                    ruleText = "Koi bhi personal file, photo ya chat delete karne se pehle pakka confirmation lena.",
                    isStrictBlock = true
                )
            )
            dao.insertRule(
                AssistantRuleEntity(
                    ruleText = "Raat 11 baje se subah 7 baje tak bina zaroorat zor se notification mat bolna.",
                    isStrictBlock = false
                )
            )
        }

        if (dao.getAllRoutineTasks().isEmpty()) {
            dao.insertRoutineTask(RoutineTaskEntity(title = "Subah Gym & Stretching", hour = 7, minute = 0, streakCount = 4))
            dao.insertRoutineTask(RoutineTaskEntity(title = "Deep Focus Kaam / Coding", hour = 9, minute = 0, streakCount = 6))
            dao.insertRoutineTask(RoutineTaskEntity(title = "Lunch & Break", hour = 13, minute = 0, streakCount = 5))
            dao.insertRoutineTask(RoutineTaskEntity(title = "YouTube / Content Review", hour = 18, minute = 30, streakCount = 3))
        }

        if (dao.countAnnouncements() == 0) {
            dao.insertAnnouncement(
                AppAnnouncementEntity(
                    title = "Maya Live Voice & Offline Hybrid Tayyar Hai",
                    body = "Ab Maya Gemini Live WebSocket (16 kHz mic in / 24 kHz audio out) ke saath real-time Hinglish, Urdu aur English me baat karti hai.\n\nAgar internet ya API key na ho, to Maya khud Android SpeechRecognizer + TTS fallback par chali jaati hai.\n\nKahan milega: Settings > Voice Engine ya Home ke center Mic Orb ko tap karo.",
                    relativeAgeLabel = "Abhi"
                )
            )
        }
    }

    private fun buildStepsJson(vararg rawSteps: String): String {
        val arr = JSONArray()
        rawSteps.forEachIndexed { idx, item ->
            val parts = item.split(":", limit = 2)
            arr.put(
                JSONObject()
                    .put("order", idx + 1)
                    .put("action", parts.getOrElse(0) { "tap" })
                    .put("target", parts.getOrElse(1) { "" })
            )
        }
        return arr.toString()
    }

    suspend fun addMessage(
        role: String,
        content: String,
        source: String = "chat",
        attachmentUri: String? = null,
        attachmentType: String? = null,
        attachmentName: String? = null
    ): Long = dao.insertMessage(
        ChatMessageEntity(
            role = role,
            content = content,
            source = source,
            attachmentUri = attachmentUri,
            attachmentType = attachmentType,
            attachmentName = attachmentName
        )
    )

    suspend fun clearChatHistory() = dao.clearMessages()

    suspend fun addCapturedNotification(
        packageName: String,
        appLabel: String,
        sender: String,
        text: String,
        wasReadAloud: Boolean
    ) = dao.insertCapturedNotification(
        CapturedNotificationEntity(
            packageName = packageName,
            appLabel = appLabel,
            sender = sender,
            text = text,
            wasReadAloud = wasReadAloud
        )
    )

    suspend fun getRecentNotifications(limit: Int = 10) = dao.getRecentNotifications(limit)

    suspend fun clearAllAnnouncements() = dao.clearAnnouncements()
    suspend fun clearCapturedNotifications() = dao.clearCapturedNotifications()

    suspend fun addRoutineTask(title: String, hour: Int, minute: Int): Long =
        dao.insertRoutineTask(RoutineTaskEntity(title = title, hour = hour.coerceIn(0, 23), minute = minute.coerceIn(0, 59)))

    suspend fun updateRoutineTask(task: RoutineTaskEntity) = dao.updateRoutineTask(task)
    suspend fun deleteRoutineTask(id: Long) = dao.deleteRoutineTask(id)
    suspend fun getAllRoutineTasks(): List<RoutineTaskEntity> = dao.getAllRoutineTasks()

    suspend fun saveMemory(key: String, value: String, category: String = "preference"): Long =
        dao.insertMemory(UserMemoryEntity(factKey = key.trim(), factValue = value.trim(), category = category))

    suspend fun deleteMemory(id: Long) = dao.deleteMemory(id)
    suspend fun getAllMemories(): List<UserMemoryEntity> = dao.getAllMemories()

    suspend fun addLearnedTask(name: String, categoryTag: String, steps: List<Pair<String, String>>): Long {
        val arr = JSONArray()
        steps.forEachIndexed { index, (act, tgt) ->
            arr.put(JSONObject().put("order", index + 1).put("action", act).put("target", tgt))
        }
        return dao.insertLearnedTask(
            LearnedTaskEntity(
                name = name.trim(),
                categoryTag = categoryTag.ifBlank { "custom" },
                stepsJson = arr.toString(),
                stepCount = steps.size,
                isBuiltIn = false
            )
        )
    }

    suspend fun updateLearnedTask(task: LearnedTaskEntity) = dao.updateLearnedTask(task)
    suspend fun deleteLearnedTask(id: Long) = dao.deleteLearnedTask(id)
    suspend fun getAllLearnedTasks() = dao.getAllLearnedTasks()

    suspend fun addRule(text: String, strict: Boolean = true) =
        dao.insertRule(AssistantRuleEntity(ruleText = text.trim(), isStrictBlock = strict))

    suspend fun toggleRule(rule: AssistantRuleEntity) =
        dao.updateRule(rule.copy(isEnabled = !rule.isEnabled))

    suspend fun deleteRule(id: Long) = dao.deleteRule(id)
    suspend fun getActiveRules() = dao.getActiveRules()

    suspend fun addCallNote(callerName: String, number: String, transcript: String, summary: String) =
        dao.insertCallNote(
            CallNoteEntity(
                callerName = callerName,
                phoneNumber = number,
                transcript = transcript,
                summary = summary
            )
        )

    suspend fun buildSystemPromptMemoryContext(): String {
        val mems = dao.getAllMemories()
        val activeRules = dao.getActiveRules()
        val sb = StringBuilder()
        if (activeRules.isNotEmpty()) {
            sb.appendLine("STRICT USER RULES (NEVER VIOLATE):")
            activeRules.forEach { sb.appendLine("- ${it.ruleText}") }
        }
        if (mems.isNotEmpty()) {
            sb.appendLine("LONG-TERM USER MEMORIES & PREFERENCES:")
            mems.take(25).forEach { sb.appendLine("- ${it.factKey}: ${it.factValue}") }
        }
        return sb.toString()
    }

    /**
     * Exports memories, learned tasks, rules, routine, and recent chats into portable JSON for moving to another phone.
     */
    suspend fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("exportedAt", System.currentTimeMillis())
        root.put("version", 1)

        val memArr = JSONArray()
        dao.getAllMemories().forEach {
            memArr.put(
                JSONObject()
                    .put("factKey", it.factKey)
                    .put("factValue", it.factValue)
                    .put("category", it.category)
            )
        }
        root.put("memories", memArr)

        val taskArr = JSONArray()
        dao.getAllLearnedTasks().filter { !it.isBuiltIn }.forEach {
            taskArr.put(
                JSONObject()
                    .put("name", it.name)
                    .put("categoryTag", it.categoryTag)
                    .put("stepsJson", it.stepsJson)
                    .put("stepCount", it.stepCount)
            )
        }
        root.put("learnedTasks", taskArr)

        val routineArr = JSONArray()
        dao.getAllRoutineTasks().forEach {
            routineArr.put(
                JSONObject()
                    .put("title", it.title)
                    .put("hour", it.hour)
                    .put("minute", it.minute)
                    .put("streakCount", it.streakCount)
            )
        }
        root.put("routines", routineArr)

        val rulesArr = JSONArray()
        dao.getActiveRules().forEach {
            rulesArr.put(JSONObject().put("ruleText", it.ruleText).put("isStrictBlock", it.isStrictBlock))
        }
        root.put("rules", rulesArr)

        val chatArr = JSONArray()
        dao.getRecentMessages(100).reversed().forEach {
            chatArr.put(
                JSONObject()
                    .put("role", it.role)
                    .put("content", it.content)
                    .put("source", it.source)
                    .put("timestamp", it.timestamp)
            )
        }
        root.put("chats", chatArr)
        return root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Int {
        val root = JSONObject(jsonString)
        var importedCount = 0

        root.optJSONArray("memories")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                dao.insertMemory(
                    UserMemoryEntity(
                        factKey = o.optString("factKey"),
                        factValue = o.optString("factValue"),
                        category = o.optString("category", "preference")
                    )
                )
                importedCount++
            }
        }
        root.optJSONArray("learnedTasks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                dao.insertLearnedTask(
                    LearnedTaskEntity(
                        name = o.optString("name"),
                        categoryTag = o.optString("categoryTag", "custom"),
                        stepsJson = o.optString("stepsJson", "[]"),
                        stepCount = o.optInt("stepCount", 1),
                        isBuiltIn = false
                    )
                )
                importedCount++
            }
        }
        root.optJSONArray("routines")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                dao.insertRoutineTask(
                    RoutineTaskEntity(
                        title = o.optString("title"),
                        hour = o.optInt("hour", 8),
                        minute = o.optInt("minute", 0),
                        streakCount = o.optInt("streakCount", 0)
                    )
                )
                importedCount++
            }
        }
        root.optJSONArray("chats")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                dao.insertMessage(
                    ChatMessageEntity(
                        role = o.optString("role", "user"),
                        content = o.optString("content"),
                        source = o.optString("source", "chat")
                    )
                )
                importedCount++
            }
        }
        return importedCount
    }
}
