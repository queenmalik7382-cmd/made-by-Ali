package com.example.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

/**
 * Local opt-in Wi-Fi HTTP bridge (`PC ⇄ Phone`) allowing the user's PC browser on the same LAN
 * to exchange clipboard notes, send voice/chat commands to Maya, or download memory backups.
 */
class PcPhoneLanServer(
    private val onRemoteCommand: (String) -> Unit,
    private val getBackupJsonProvider: suspend () -> String
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null

    private val _runningAddress = MutableStateFlow<String?>(null)
    val runningAddress: StateFlow<String?> = _runningAddress.asStateFlow()

    private val _sharedClipboard = MutableStateFlow("")
    val sharedClipboard: StateFlow<String> = _sharedClipboard.asStateFlow()

    fun start(port: Int = 8765) {
        if (serverJob?.isActive == true) return
        serverJob = scope.launch {
            runCatching {
                val ss = ServerSocket(port)
                serverSocket = ss
                val ip = detectLocalIpv4()
                _runningAddress.value = "http://$ip:$port"
                while (isActive && !ss.isClosed) {
                    val client = ss.accept()
                    launch { handleClient(client) }
                }
            }.onFailure {
                _runningAddress.value = null
            }
        }
    }

    fun stop() {
        serverJob?.cancel()
        runCatching { serverSocket?.close() }
        serverSocket = null
        _runningAddress.value = null
    }

    fun updateClipboardNote(text: String) {
        _sharedClipboard.value = text
    }

    private suspend fun handleClient(socket: Socket) {
        socket.use { s ->
            val reader = BufferedReader(InputStreamReader(s.getInputStream()))
            val reqLine = reader.readLine() ?: return
            var contentLength = 0
            var line = reader.readLine()
            while (!line.isNullOrEmpty()) {
                if (line.lowercase().startsWith("content-length:")) {
                    contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                }
                line = reader.readLine()
            }
            val bodyChars = CharArray(contentLength)
            if (contentLength > 0) reader.read(bodyChars, 0, contentLength)
            val bodyStr = String(bodyChars)

            val responseBody: String
            val contentType: String

            when {
                reqLine.startsWith("GET /backup.json") -> {
                    contentType = "application/json; charset=utf-8"
                    responseBody = getBackupJsonProvider()
                }
                reqLine.startsWith("POST /command") -> {
                    val cmd = runCatching { JSONObject(bodyStr).optString("text") }.getOrDefault(bodyStr)
                    if (cmd.isNotBlank()) {
                        _sharedClipboard.value = cmd
                        onRemoteCommand(cmd)
                    }
                    contentType = "application/json; charset=utf-8"
                    responseBody = JSONObject().put("ok", true).put("received", cmd).toString()
                }
                else -> {
                    contentType = "text/html; charset=utf-8"
                    responseBody = """
                        <!doctype html><html><head><meta charset='utf-8'><title>Maya PC ⇄ Phone</title>
                        <style>body{font-family:sans-serif;background:#EAF1FF;color:#112244;padding:28px;max-width:640px;margin:auto}
                        .card{background:#F5F9FF;padding:20px;border-radius:20px;box-shadow:6px 6px 16px #C8D6EE,-6px -6px 16px #FFFFFF}
                        input,button{padding:12px 16px;border-radius:12px;border:1px solid #BBD0F5;font-size:16px}
                        button{background:#2E8BFF;color:#fff;border:none;cursor:pointer}</style></head>
                        <body><div class='card'><h2>Maya PC ⇄ Phone Bridge</h2>
                        <p>Phone Note: <b>${_sharedClipboard.value.ifBlank { "(khali)" }}</b></p>
                        <input id='t' style='width:70%' placeholder='Maya ko command ya note bhejo...'/>
                        <button onclick="fetch('/command',{method:'POST',body:JSON.stringify({text:document.getElementById('t').value})}).then(()=>location.reload())">Send</button>
                        <p><a href='/backup.json'>Download Maya Backup JSON</a></p></div></body></html>
                    """.trimIndent()
                }
            }

            val bytes = responseBody.toByteArray(Charsets.UTF_8)
            val header = "HTTP/1.1 200 OK\r\nContent-Type: $contentType\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
            s.getOutputStream().write(header.toByteArray(Charsets.UTF_8))
            s.getOutputStream().write(bytes)
            s.getOutputStream().flush()
        }
    }

    private fun detectLocalIpv4(): String {
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                val addrs = nif.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return@runCatching addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
            "127.0.0.1"
        }.getOrDefault("127.0.0.1")
    }
}
