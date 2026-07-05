package com.robot.controller

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

/**
 * Wraps Android's TextToSpeech engine and attempts to select a male-sounding
 * voice for the AI assistant's spoken replies. Voice availability depends on
 * the TTS engine installed on the phone (Google TTS ships several per locale).
 */
class TtsHelper(context: Context, private val onReady: (Boolean) -> Unit) {

    private var tts: TextToSpeech? = null
    var isReady = false
        private set

    init {
        tts = TextToSpeech(context) { status ->
            isReady = status == TextToSpeech.SUCCESS
            if (isReady) selectMaleVoice()
            onReady(isReady)
        }
    }

    private fun selectMaleVoice() {
        val engine = tts ?: return
        engine.language = Locale.US

        val voices: Set<Voice>? = try {
            engine.voices
        } catch (e: Exception) {
            null
        }

        // Heuristic: Android voice names commonly look like "en-us-x-iom-local"
        // where certain variants map to male timbre on Google's TTS engine.
        // We prefer any voice whose name/features hint "male" and avoid ones
        // that hint "female". If none found, we fall back to engine default.
        val preferredMaleTags = listOf("male", "-x-iom", "-x-iol", "-x-tpc")
        val avoidFemaleTags = listOf("female", "-x-iog", "-x-iof")

        val candidate = voices?.firstOrNull { v ->
            val name = v.name.lowercase()
            preferredMaleTags.any { name.contains(it) } &&
                avoidFemaleTags.none { name.contains(it) }
        }

        if (candidate != null) {
            engine.voice = candidate
        }
        // If no clear match, engine keeps its default voice for the locale —
        // still functional, just not guaranteed male on every device/engine.
    }

    /**
     * Voice gender detection from name alone isn't reliable across all TTS
     * engines/devices. This lists every installed voice so the app can offer
     * a manual picker with a "preview" button — the reliable fallback if the
     * automatic male-voice heuristic picks wrong on a given phone.
     */
    fun listVoiceNames(): List<String> {
        return try {
            tts?.voices?.map { it.name }?.sorted() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setVoiceByName(name: String) {
        val engine = tts ?: return
        val match = engine.voices?.firstOrNull { it.name == name }
        if (match != null) engine.voice = match
    }

    fun speak(text: String) {
        if (!isReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "assistant_reply")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
    }
}
