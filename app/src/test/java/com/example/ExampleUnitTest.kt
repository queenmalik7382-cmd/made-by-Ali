package com.example

import com.example.network.GeminiRestClient
import com.example.tools.Tool
import com.example.tools.ToolRegistry
import com.example.tools.ToolResult
import com.example.ui.screens.parseNaturalHinglishRoutine
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {

    @Test
    fun `matchLocalHinglishIntent parses open app, call, routine, and wireless debugging`() {
        val registry = ToolRegistry()

        val yt = registry.matchLocalHinglishIntent("YouTube kholo")
        assertNotNull(yt)
        assertEquals("open_app", yt!!.first)
        assertEquals("youtube", yt.second.optString("appName"))

        val call = registry.matchLocalHinglishIntent("Ali ko call lagao")
        assertNotNull(call)
        assertEquals("make_phone_call", call!!.first)
        assertEquals("ali", call.second.optString("contactName"))

        val routine = registry.matchLocalHinglishIntent("aaj ki routine batao")
        assertNotNull(routine)
        assertEquals("query_routine", routine!!.first)
        assertEquals("today", routine.second.optString("mode"))

        val wireless = registry.matchLocalHinglishIntent("Developer options on karo, wireless debugging dhoondo")
        assertNotNull(wireless)
        assertEquals("enable_wireless_debugging", wireless!!.first)
    }

    @Test
    fun `dangerous tool requires voice confirmation before executing`() = runTest {
        val registry = ToolRegistry()
        var executedCount = 0
        registry.register(object : Tool {
            override val name = "make_phone_call"
            override val description = "Dial contact"
            override val isDangerous = true
            override fun parametersSchema(): JSONObject = ToolRegistry.schemaObject()
            override suspend fun execute(args: JSONObject, confirmed: Boolean): ToolResult {
                executedCount++
                return ToolResult(true, "Call laga di.")
            }
        })

        val unconfirmed = registry.invokeTool("make_phone_call", JSONObject().put("contactName", "Ali"), confirmed = false)
        assertTrue(unconfirmed.requiresConfirmation)
        assertTrue(unconfirmed.spokenFeedback.contains("Pakka"))
        assertEquals(0, executedCount)

        val confirmed = registry.invokeTool("make_phone_call", JSONObject().put("contactName", "Ali"), confirmed = true)
        assertFalse(confirmed.requiresConfirmation)
        assertEquals(1, executedCount)
        assertEquals("Call laga di.", confirmed.spokenFeedback)
    }

    @Test
    fun `parseGenerateResponse extracts Gemini functionCall name and args`() {
        val sampleJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "functionCall": {
                          "name": "open_app",
                          "args": { "appName": "Instagram" }
                        }
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = GeminiRestClient.parseGenerateResponse(sampleJson)
        assertEquals("open_app", parsed.functionCallName)
        assertEquals("Instagram", parsed.functionCallArgs?.optString("appName"))
    }

    @Test
    fun `parseNaturalHinglishRoutine extracts morning and afternoon tasks`() {
        val tasks = parseNaturalHinglishRoutine("subah 7 baje gym, 9 baje kaam, 1 baje lunch")
        assertEquals(3, tasks.size)
        assertEquals(Triple("Gym", 7, 0), tasks[0])
        assertEquals(Triple("Kaam", 9, 0), tasks[1])
        assertEquals(Triple("Lunch", 13, 0), tasks[2])
    }
}
