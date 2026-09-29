package com.unfallen.nova

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unfallen.nova.ai.MemoryUpdate
import com.unfallen.nova.ai.NovaBrain
import com.unfallen.nova.data.ChatMessage
import com.unfallen.nova.data.Memory
import com.unfallen.nova.data.NovaStatus
import com.unfallen.nova.data.Profile
import com.unfallen.nova.data.Storage
import com.unfallen.nova.voice.Speaker
import com.unfallen.nova.voice.SpeechInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class NovaViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = Storage(app)
    private val brain = NovaBrain()
    private val diskExecutor = Executors.newSingleThreadExecutor()
    private val disk = diskExecutor.asCoroutineDispatcher()
    // Scope propio para disco: no se cancela al cerrar la pantalla, así no se pierde nada
    private val diskScope = CoroutineScope(SupervisorJob() + disk)

    // ------------------------------------------------------------ estado visible
    var messages by mutableStateOf(listOf<ChatMessage>())
        private set
    var memories by mutableStateOf(listOf<Memory>())
        private set
    var profile by mutableStateOf(Profile())
        private set

    var status by mutableStateOf(NovaStatus.IDLE)
        private set
    var micLevel by mutableFloatStateOf(0f)
        private set
    var partialText by mutableStateOf("")
        private set
    var wordTick by mutableIntStateOf(0)
        private set
    var speakingId by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var learnedNotice by mutableStateOf<String?>(null)
        private set
    var isLearning by mutableStateOf(false)
        private set

    /** La pantalla "Voz" está abierta (activa el modo manos libres). */
    var voiceScreenActive by mutableStateOf(false)

    // ------------------------------------------------------------ ajustes
    var apiKey by mutableStateOf(storage.apiKey)
        private set
    var model by mutableStateOf(storage.model)
        private set
    var userName by mutableStateOf(storage.userName)
        private set
    var voiceEnabled by mutableStateOf(storage.voiceEnabled)
        private set
    var learningEnabled by mutableStateOf(storage.learningEnabled)
        private set
    var handsFree by mutableStateOf(storage.handsFree)
        private set
    var speechRate by mutableFloatStateOf(storage.speechRate)
        private set
    var speechPitch by mutableFloatStateOf(storage.speechPitch)
        private set

    private var lastInputWasVoice = false
    private var chatJob: Job? = null
    private var learnJob: Job? = null
    private var learnAgain = false
    private var noticeJob: Job? = null

    private val speaker = Speaker(
        app,
        onStart = { if (speakingId != null) status = NovaStatus.SPEAKING },
        onWord = { wordTick++ },
        onDone = { onSpeechFinished() }
    ).also {
        it.rate = speechRate
        it.pitch = speechPitch
    }

    private val speechInput = SpeechInput(
        app,
        onLevel = { micLevel = it },
        onPartial = { partialText = it },
        onResult = { text ->
            status = NovaStatus.IDLE
            partialText = ""
            send(text, viaVoice = true)
        },
        onError = { msg ->
            status = NovaStatus.IDLE
            partialText = ""
            micLevel = 0f
            if (msg != null) showError(msg)
        }
    )

    val speechAvailable: Boolean get() = speechInput.isAvailable
    val hasApiKey: Boolean get() = apiKey.isNotBlank()

    init {
        viewModelScope.launch(disk) {
            val m = storage.loadMessages()
            val mem = storage.loadMemories()
            val p = storage.loadProfile()
            launch(Dispatchers.Main) {
                messages = m
                memories = mem
                profile = p
            }
        }
    }

    // ============================================================ CONVERSACIÓN

    fun send(raw: String, viaVoice: Boolean = false) {
        val text = raw.trim()
        if (text.isEmpty() || status == NovaStatus.THINKING) return
        stopSpeaking()
        lastInputWasVoice = viaVoice
        error = null

        val userMsg = ChatMessage(role = "user", text = text, viaVoice = viaVoice)
        messages = messages + userMsg
        persistMessages()

        if (apiKey.isBlank()) {
            showError("Añade tu API key de OpenAI en Ajustes para que pueda contestarte.")
            return
        }

        status = NovaStatus.THINKING
        chatJob = viewModelScope.launch {
            try {
                val reply = brain.chat(
                    apiKey = apiKey,
                    model = model,
                    userName = userName,
                    history = messages,
                    memories = memories,
                    profile = profile,
                    voiceMode = viaVoice
                )
                val msg = ChatMessage(role = "assistant", text = reply)
                messages = messages + msg
                persistMessages()
                status = NovaStatus.IDLE
                if (voiceEnabled || voiceScreenActive) speak(msg) else maybeContinueHandsFree()
                if (learningEnabled && text.length >= 8) learn()
            } catch (e: Exception) {
                status = NovaStatus.IDLE
                showError(e.message ?: "Algo ha fallado.")
            }
        }
    }

    fun speak(msg: ChatMessage) {
        speechInput.cancel()
        speakingId = msg.id
        status = NovaStatus.SPEAKING
        speaker.speak(msg.text)
    }

    fun stopSpeaking() {
        if (speakingId != null) {
            speakingId = null
            speaker.stop()
            if (status == NovaStatus.SPEAKING) status = NovaStatus.IDLE
        }
    }

    private fun onSpeechFinished() {
        if (speakingId == null) return
        speakingId = null
        if (status == NovaStatus.SPEAKING) status = NovaStatus.IDLE
        maybeContinueHandsFree()
    }

    /** En la pantalla de Voz, tras responder, NOVA vuelve a escucharte sola. */
    private fun maybeContinueHandsFree() {
        if (voiceScreenActive && handsFree && lastInputWasVoice) {
            viewModelScope.launch {
                delay(350)
                if (status == NovaStatus.IDLE && voiceScreenActive) startListening()
            }
        }
    }

    // ============================================================ MICRO

    fun startListening() {
        stopSpeaking()
        if (status == NovaStatus.THINKING) return
        if (!speechInput.isAvailable) {
            showError("Este móvil no tiene reconocimiento de voz disponible. Instala o activa la app de Google.")
            return
        }
        error = null
        partialText = ""
        status = NovaStatus.LISTENING
        speechInput.start()
    }

    fun finishListening() = speechInput.finish()

    fun cancelListening() {
        speechInput.cancel()
        partialText = ""
        micLevel = 0f
        if (status == NovaStatus.LISTENING) status = NovaStatus.IDLE
    }

    /** Si el usuario sale de la pantalla de voz, paramos el bucle. */
    fun leaveVoiceScreen() {
        voiceScreenActive = false
        if (status == NovaStatus.LISTENING) cancelListening()
    }

    fun clearChat() {
        stopSpeaking()
        messages = emptyList()
        persistMessages()
    }

    fun dismissError() {
        error = null
    }

    private fun showError(msg: String) {
        error = msg
    }

    fun reportError(msg: String) = showError(msg)

    // ============================================================ MEMORIA

    private fun learn() {
        if (learnJob?.isActive == true) {
            learnAgain = true
            return
        }
        learnJob = viewModelScope.launch {
            isLearning = true
            try {
                do {
                    learnAgain = false
                    val update = brain.learn(apiKey, model, messages, memories, profile)
                    if (update != null) applyUpdate(update)
                } while (learnAgain)
            } finally {
                isLearning = false
            }
        }
    }

    private fun applyUpdate(u: MemoryUpdate) {
        val list = memories.toMutableList()
        // borrar (nunca borra lo que añadiste tú a mano)
        list.removeAll { it.id in u.delete && !it.manual }
        // actualizar
        u.update.forEach { (id, text) ->
            val i = list.indexOfFirst { it.id == id }
            if (i >= 0) list[i] = list[i].copy(text = text, createdAt = System.currentTimeMillis())
        }
        // añadir sin duplicados
        val added = mutableListOf<Memory>()
        u.add.forEach { m ->
            val dup = list.any { it.text.equals(m.text, ignoreCase = true) }
            if (!dup) {
                list.add(m); added.add(m)
            }
        }
        // límite: se descartan los automáticos más antiguos
        while (list.size > MAX_MEMORIES) {
            val oldest = list.filter { !it.manual }.minByOrNull { it.createdAt } ?: break
            list.remove(oldest)
        }
        memories = list
        persistMemories()

        u.profile?.let {
            profile = Profile(it, System.currentTimeMillis())
            persistProfile()
        }

        val changes = added.size + u.update.size
        if (changes > 0) {
            val first = (added.firstOrNull()?.text ?: u.update.values.firstOrNull()).orEmpty()
            notice(if (changes == 1) "🧠 He aprendido: $first" else "🧠 He aprendido $changes cosas nuevas sobre ti")
        }
    }

    private fun notice(text: String) {
        noticeJob?.cancel()
        learnedNotice = text
        noticeJob = viewModelScope.launch {
            delay(4500)
            learnedNotice = null
        }
    }

    fun addMemory(category: String, text: String) {
        if (text.isBlank()) return
        memories = memories + Memory(category = category, text = text.trim(), manual = true)
        persistMemories()
    }

    fun editMemory(id: String, category: String, text: String) {
        if (text.isBlank()) return
        memories = memories.map { if (it.id == id) it.copy(category = category, text = text.trim()) else it }
        persistMemories()
    }

    fun deleteMemory(id: String) {
        memories = memories.filterNot { it.id == id }
        persistMemories()
    }

    fun clearMemories() {
        memories = emptyList()
        profile = Profile()
        persistMemories()
        persistProfile()
    }

    fun setProfileText(text: String) {
        profile = Profile(text.trim(), System.currentTimeMillis())
        persistProfile()
    }

    // ============================================================ AJUSTES

    fun updateApiKey(v: String) { apiKey = v.trim(); storage.apiKey = v }
    fun updateModel(v: String) { model = v.trim().ifBlank { Storage.DEFAULT_MODEL }; storage.model = model }
    fun updateUserName(v: String) { userName = v; storage.userName = v }
    fun updateVoiceEnabled(v: Boolean) { voiceEnabled = v; storage.voiceEnabled = v; if (!v) stopSpeaking() }
    fun updateLearning(v: Boolean) { learningEnabled = v; storage.learningEnabled = v }
    fun updateHandsFree(v: Boolean) { handsFree = v; storage.handsFree = v }
    fun updateRate(v: Float) { speechRate = v; storage.speechRate = v; speaker.rate = v }
    fun updatePitch(v: Float) { speechPitch = v; storage.speechPitch = v; speaker.pitch = v }

    fun testVoice() {
        val name = userName.ifBlank { "" }
        val msg = ChatMessage(role = "assistant", text = "Hola${if (name.isNotBlank()) " $name" else ""}, soy NOVA. Así suena mi voz.")
        speak(msg)
    }

    // ============================================================ DISCO

    private fun persistMessages() {
        val snap = messages
        diskScope.launch { storage.saveMessages(snap) }
    }

    private fun persistMemories() {
        val snap = memories
        diskScope.launch { storage.saveMemories(snap) }
    }

    private fun persistProfile() {
        val snap = profile
        diskScope.launch { storage.saveProfile(snap) }
    }

    override fun onCleared() {
        speechInput.release()
        speaker.release()
        diskExecutor.shutdown() // termina las escrituras pendientes y se cierra
        super.onCleared()
    }

    companion object {
        const val MAX_MEMORIES = 300
    }
}
