package com.example.network

import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gemini Multimodal Live API WebSocket client (BidiGenerateContent).
 * Streams 16 kHz 16-bit mono PCM microphone chunks in, and receives 24 kHz 16-bit mono PCM audio + tool calls out.
 * Supports instant barge-in (interrupting assistant playback when user speaks).
 */
class GeminiLiveWebSocketClient(
    private val httpClient: OkHttpClient,
    private val Listener: ListenerCallbacks
) {
    interface ListenerCallbacks {
        fun onConnected()
        fun onAudioPcm24kReceived(pcmBytes: ByteArray)
        fun onTranscriptText(text: String, isUser: Boolean)
        fun onToolCall(callId: String, name: String, args: JSONObject)
        fun onInterrupted()
        fun onDisconnected(reason: String)
    }

    private var webSocket: WebSocket? = null
    @Volatile
    var isConnected: Boolean = false
        private set

    fun connect(
        apiKey: String,
        systemInstruction: String,
        toolDeclarations: JSONArray?,
        voiceName: String = "Aoede"
    ) {
        disconnect()
        if (apiKey.isBlank()) {
            Listener.onDisconnected("Missing Gemini API Key")
            return
        }

        val wsUrl =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(wsUrl).build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                sendSetupMessage(webSocket, systemInstruction, toolDeclarations, voiceName)
                Listener.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerPayload(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                handleServerPayload(bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                webSocket.close(1000, null)
                Listener.onDisconnected(reason.ifBlank { "Closed ($code)" })
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Listener.onDisconnected(t.localizedMessage ?: "Live WebSocket connection failed")
            }
        })
    }

    private fun sendSetupMessage(
        ws: WebSocket,
        systemInstruction: String,
        toolDeclarations: JSONArray?,
        voiceName: String
    ) {
        val setup = JSONObject()
        setup.put("model", "models/gemini-2.0-flash-live-001")

        val genConfig = JSONObject()
        genConfig.put("responseModalities", JSONArray().put("AUDIO"))
        genConfig.put(
            "speechConfig",
            JSONObject().put(
                "voiceConfig",
                JSONObject().put(
                    "prebuiltVoiceConfig",
                    JSONObject().put("voiceName", voiceName)
                )
            )
        )
        setup.put("generationConfig", genConfig)

        if (systemInstruction.isNotBlank()) {
            setup.put(
                "systemInstruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", systemInstruction))
                )
            )
        }

        if (toolDeclarations != null && toolDeclarations.length() > 0) {
            setup.put(
                "tools",
                JSONArray().put(JSONObject().put("functionDeclarations", toolDeclarations))
            )
        }

        val wrapper = JSONObject().put("setup", setup)
        ws.send(wrapper.toString())
    }

    /**
     * Sends raw 16 kHz, 16-bit, mono little-endian PCM chunk to Gemini Live API.
     */
    fun sendAudioChunk16k(pcmBytes: ByteArray, length: Int) {
        val ws = webSocket ?: return
        if (!isConnected || length <= 0) return

        val slice = if (length == pcmBytes.size) pcmBytes else pcmBytes.copyOf(length)
        val b64 = Base64.encodeToString(slice, Base64.NO_WRAP)

        val mediaChunk = JSONObject()
            .put("mimeType", "audio/pcm;rate=16000")
            .put("data", b64)

        val realtimeInput = JSONObject()
            .put("mediaChunks", JSONArray().put(mediaChunk))

        ws.send(JSONObject().put("realtimeInput", realtimeInput).toString())
    }

    fun sendUserTextTurn(text: String) {
        val ws = webSocket ?: return
        if (!isConnected) return
        val turn = JSONObject()
            .put(
                "turns",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", text)))
                )
            )
            .put("turnComplete", true)
        ws.send(JSONObject().put("clientContent", turn).toString())
    }

    fun sendToolResponse(callId: String, name: String, resultObj: JSONObject) {
        val ws = webSocket ?: return
        if (!isConnected) return
        val fnResp = JSONObject()
            .put("id", callId)
            .put("name", name)
            .put("response", resultObj)

        val payload = JSONObject().put(
            "toolResponse",
            JSONObject().put("functionResponses", JSONArray().put(fnResp))
        )
        ws.send(payload.toString())
    }

    private fun handleServerPayload(raw: String) {
        try {
            val json = JSONObject(raw)

            json.optJSONObject("serverContent")?.let { serverContent ->
                if (serverContent.optBoolean("interrupted", false)) {
                    Listener.onInterrupted()
                }
                val modelTurn = serverContent.optJSONObject("modelTurn")
                val parts = modelTurn?.optJSONArray("parts")
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        if (part.has("text")) {
                            val txt = part.optString("text")
                            if (txt.isNotBlank()) Listener.onTranscriptText(txt, false)
                        }
                        part.optJSONObject("inlineData")?.let { inline ->
                            val mime = inline.optString("mimeType")
                            if (mime.startsWith("audio/pcm")) {
                                val dataB64 = inline.optString("data")
                                if (dataB64.isNotBlank()) {
                                    val pcm = Base64.decode(dataB64, Base64.DEFAULT)
                                    Listener.onAudioPcm24kReceived(pcm)
                                }
                            }
                        }
                    }
                }
            }

            json.optJSONObject("toolCall")?.let { tc ->
                val calls = tc.optJSONArray("functionCalls")
                if (calls != null) {
                    for (i in 0 until calls.length()) {
                        val fc = calls.getJSONObject(i)
                        val id = fc.optString("id", "call_$i")
                        val name = fc.optString("name")
                        val args = fc.optJSONObject("args") ?: JSONObject()
                        if (name.isNotBlank()) {
                            Listener.onToolCall(id, name, args)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore malformed keepalive frames
        }
    }

    fun disconnect() {
        isConnected = false
        runCatching { webSocket?.close(1000, "Client disconnect") }
        webSocket = null
    }
}
