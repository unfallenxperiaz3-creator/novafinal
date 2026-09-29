package com.unfallen.nova.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Voz de NOVA (Text-to-Speech del sistema).
 * Avisa de cada palabra pronunciada para que el avatar "hable" al ritmo de la voz.
 */
class Speaker(
    context: Context,
    private val onStart: () -> Unit,
    private val onWord: () -> Unit,
    private val onDone: () -> Unit
) : TextToSpeech.OnInitListener {

    private val main = Handler(Looper.getMainLooper())
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var pending: String? = null
    private var session = 0
    @Volatile
    private var lastId: String? = null

    var rate = 1.0f
    var pitch = 0.9f

    /** true si el móvil no tiene motor de voz: así nunca nos quedamos "hablando" para siempre */
    var failed = false
        private set

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            failed = true
            if (pending != null) {
                pending = null
                main.post { onDone() }
            }
            return
        }
        val spanish = Locale.forLanguageTag("es-ES")
        val r = tts.setLanguage(spanish)
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.forLanguageTag("es"))
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                main.post { onStart() }
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId != null && utteranceId == lastId) main.post { onDone() }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (utteranceId != null && utteranceId == lastId) main.post { onDone() }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                if (utteranceId != null && utteranceId == lastId) main.post { onDone() }
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                // Solo si se corta la frase final sin que la hayamos parado nosotros
                if (utteranceId != null && utteranceId == lastId) main.post { onDone() }
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                main.post { onWord() }
            }
        })
        ready = true
        pending?.let { speak(it) }
        pending = null
    }

    fun speak(text: String) {
        val clean = cleanForSpeech(text)
        if (clean.isBlank()) {
            onDone(); return
        }
        if (failed) {
            onDone(); return
        }
        if (!ready) {
            pending = text; return
        }
        tts.stop()
        tts.setSpeechRate(rate)
        tts.setPitch(pitch)
        session++
        val chunks = splitChunks(clean, 380)
        chunks.forEachIndexed { i, chunk ->
            val id = "nova-$session-$i"
            if (i == chunks.lastIndex) lastId = id
            tts.speak(chunk, if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, id)
        }
    }

    fun stop() {
        pending = null
        lastId = null
        if (ready) tts.stop()
    }

    fun release() {
        tts.stop()
        tts.shutdown()
    }

    companion object {
        private val emoji = Regex("[\\x{1F000}-\\x{1FAFF}\\x{2600}-\\x{27BF}\\x{2B00}-\\x{2BFF}\\x{FE0F}\\x{200D}]")

        /** Quita markdown, enlaces y emojis para que la voz no lea símbolos. */
        fun cleanForSpeech(text: String): String = text
            .replace(Regex("```[\\s\\S]*?```"), " ")
            .replace(Regex("\\[([^\\]]+)\\]\\(([^)]*)\\)"), "$1")
            .replace(Regex("https?://\\S+"), "enlace")
            .replace(Regex("(?m)^\\s{0,3}#{1,6}\\s*"), "")
            .replace(Regex("(?m)^\\s*[-*•]\\s+"), "")
            .replace(Regex("[*_`~>|]"), "")
            .replace(emoji, "")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{2,}"), ".\n")
            .trim()

        private fun splitChunks(text: String, max: Int): List<String> {
            if (text.length <= max) return listOf(text)
            val sentences = text.split(Regex("(?<=[.!?¿¡:;\\n])\\s+"))
            val out = mutableListOf<String>()
            val sb = StringBuilder()
            for (s in sentences) {
                if (sb.length + s.length + 1 > max && sb.isNotEmpty()) {
                    out.add(sb.toString()); sb.clear()
                }
                if (s.length > max) {
                    s.chunked(max).forEach { out.add(it) }
                } else {
                    if (sb.isNotEmpty()) sb.append(' ')
                    sb.append(s)
                }
            }
            if (sb.isNotEmpty()) out.add(sb.toString())
            return out
        }
    }
}
