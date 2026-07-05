package com.robot.controller

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Sends commands to the robot's Raspberry Pi hub over WiFi.
 * The Pi should run a small HTTP server (Flask/FastAPI) listening on port 5000
 * with a single POST endpoint: /command
 *
 * Expected JSON body examples:
 *   {"cmd":"move","x":0.8,"y":0.5}
 *   {"cmd":"arm","side":"left","action":"up"}
 *   {"cmd":"head","action":"left"}
 *   {"cmd":"blink"}
 *   {"cmd":"mode","value":"obstacle_avoidance"}
 *   {"cmd":"talk_trigger"}
 */
class RobotConnection(private var baseUrl: String) {

    interface StatusListener {
        fun onSuccess(response: String)
        fun onError(message: String)
    }

    private val executor = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun setBaseUrl(url: String) {
        baseUrl = url
    }

    fun sendCommand(json: JSONObject, listener: StatusListener? = null) {
        executor.execute {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("$baseUrl/command")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 3000
                conn.readTimeout = 3000

                val os: OutputStream = conn.outputStream
                os.write(json.toString().toByteArray(Charsets.UTF_8))
                os.flush()
                os.close()

                val code = conn.responseCode
                val response = if (code in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
                }

                mainHandler.post {
                    if (code in 200..299) {
                        listener?.onSuccess(response)
                    } else {
                        listener?.onError("Server returned $code: $response")
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    listener?.onError(e.message ?: "Connection failed")
                }
            } finally {
                conn?.disconnect()
            }
        }
    }

    // Convenience builders for each command type used by the app
    companion object {
        fun move(x: Float, y: Float) = JSONObject().apply {
            put("cmd", "move")
            put("x", x)
            put("y", y)
        }

        fun stop() = JSONObject().apply { put("cmd", "move"); put("x", 0); put("y", 0) }

        fun arm(side: String, action: String) = JSONObject().apply {
            put("cmd", "arm")
            put("side", side)
            put("action", action)
        }

        fun head(action: String) = JSONObject().apply {
            put("cmd", "head")
            put("action", action)
        }

        fun blink() = JSONObject().apply { put("cmd", "blink") }

        fun mode(value: String) = JSONObject().apply {
            put("cmd", "mode")
            put("value", value)
        }

        fun talkTrigger() = JSONObject().apply { put("cmd", "talk_trigger") }
    }

    /**
     * Sends typed text to the Pi's AI/chat endpoint. The Pi is expected to
     * run the STT->LLM->TTS pipeline server-side and return a JSON reply:
     *   {"reply": "the AI's text response"}
     * The app speaks this reply locally via TtsHelper AND the robot's own
     * speaker plays it if the Pi also triggers its own TTS — keep only one
     * source active if you don't want the reply spoken twice.
     */
    fun sendChat(text: String, listener: StatusListener) {
        val body = JSONObject().apply {
            put("cmd", "chat")
            put("text", text)
        }
        sendCommand(body, listener)
    }

    /**
     * Polls the Pi for real status (battery voltage/%, current mode, connection
     * health). Returns null fields if the Pi doesn't report them — the UI
     * should show "—" rather than inventing a number when this returns null.
     * Expected Pi response: {"battery_pct": 71, "mode": "manual", "connected": true}
     * Any field the Pi omits should be treated as unknown, not defaulted to 0/100.
     */
    fun fetchStatus(listener: StatusListener) {
        executor.execute {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("$baseUrl/status")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 2500
                conn.readTimeout = 2500

                val code = conn.responseCode
                val response = if (code in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
                }
                mainHandler.post {
                    if (code in 200..299) listener.onSuccess(response)
                    else listener.onError("Status unavailable ($code)")
                }
            } catch (e: Exception) {
                mainHandler.post { listener.onError(e.message ?: "Status unavailable") }
            } finally {
                conn?.disconnect()
            }
        }
    }
}
