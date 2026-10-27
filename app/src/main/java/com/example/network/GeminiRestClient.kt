package com.example.network

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class GeminiTurnResult(
    val replyText: String,
    val functionCallName: String? = null,
    val functionCallArgs: JSONObject? = null,
    val error: String? = null
)

class GeminiRestClient {
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateWithTools(
        apiKey: String,
        systemInstruction: String,
        userPrompt: String,
        recentHistory: List<Pair<String, String>> = emptyList(),
        toolDeclarations: JSONArray? = null,
        inlineBytes: ByteArray? = null,
        inlineMimeType: String? = null,
        modelName: String = "gemini-2.5-flash"
    ): GeminiTurnResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiTurnResult(
                replyText = "",
                error = "API key nahi mili. Settings ya Onboarding me apni free Gemini key daalo."
            )
        }

        try {
            val root = JSONObject()
            if (systemInstruction.isNotBlank()) {
                root.put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", systemInstruction))
                    )
                )
            }

            val contents = JSONArray()
            recentHistory.takeLast(10).forEach { (role, msg) ->
                if (msg.isNotBlank()) {
                    val mappedRole = if (role == "assistant" || role == "model") "model" else "user"
                    contents.put(
                        JSONObject()
                            .put("role", mappedRole)
                            .put("parts", JSONArray().put(JSONObject().put("text", msg)))
                    )
                }
            }

            val userParts = JSONArray()
            if (inlineBytes != null && !inlineMimeType.isNullOrBlank()) {
                val b64 = Base64.encodeToString(inlineBytes, Base64.NO_WRAP)
                userParts.put(
                    JSONObject().put(
                        "inlineData",
                        JSONObject()
                            .put("mimeType", inlineMimeType)
                            .put("data", b64)
                    )
                )
            }
            userParts.put(JSONObject().put("text", userPrompt))
            contents.put(JSONObject().put("role", "user").put("parts", userParts))
            root.put("contents", contents)

            if (toolDeclarations != null && toolDeclarations.length() > 0) {
                root.put(
                    "tools",
                    JSONArray().put(JSONObject().put("functionDeclarations", toolDeclarations))
                )
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val req = Request.Builder()
                .url(url)
                .post(root.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val errMsg = runCatching {
                        JSONObject(raw).optJSONObject("error")?.optString("message")
                    }.getOrNull().orEmpty().ifBlank { "HTTP ${resp.code}" }
                    return@withContext GeminiTurnResult(replyText = "", error = errMsg)
                }

                return@withContext parseGenerateResponse(raw)
            }
        } catch (e: Exception) {
            GeminiTurnResult(replyText = "", error = e.localizedMessage ?: "Network error")
        }
    }

    suspend fun analyzeCameraBitmap(
        apiKey: String,
        bitmap: Bitmap,
        prompt: String,
        systemInstruction: String
    ): GeminiTurnResult = withContext(Dispatchers.IO) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, stream)
        generateWithTools(
            apiKey = apiKey,
            systemInstruction = systemInstruction,
            userPrompt = prompt,
            inlineBytes = stream.toByteArray(),
            inlineMimeType = "image/jpeg"
        )
    }

    companion object {
        fun parseGenerateResponse(rawJson: String): GeminiTurnResult {
            return try {
                val json = JSONObject(rawJson)
                val candidates = json.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return GeminiTurnResult(replyText = "Koi jawab nahi aaya.")
                }
                val content = candidates.getJSONObject(0).optJSONObject("content")
                val parts = content?.optJSONArray("parts") ?: JSONArray()
                val textBuilder = StringBuilder()
                var fnName: String? = null
                var fnArgs: JSONObject? = null

                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        textBuilder.append(part.optString("text"))
                    }
                    if (part.has("functionCall")) {
                        val fc = part.getJSONObject("functionCall")
                        fnName = fc.optString("name").takeIf { it.isNotBlank() }
                        fnArgs = fc.optJSONObject("args") ?: JSONObject()
                    }
                }
                GeminiTurnResult(
                    replyText = textBuilder.toString().trim(),
                    functionCallName = fnName,
                    functionCallArgs = fnArgs
                )
            } catch (e: Exception) {
                GeminiTurnResult(replyText = "", error = "JSON parse error: ${e.message}")
            }
        }
    }
}
