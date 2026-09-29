package com.unfallen.nova.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Todo se guarda en el almacenamiento privado de la app (solo en este móvil).
 * - messages.json : conversación
 * - memories.json : recuerdos
 * - profile.json  : retrato de la persona que NOVA va construyendo
 * - SharedPreferences "nova_settings": ajustes
 */
class Storage(context: Context) {
    private val dir: File = context.filesDir
    private val prefs = context.getSharedPreferences("nova_settings", Context.MODE_PRIVATE)

    // ---------- Ajustes ----------
    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(v) = prefs.edit().putString("api_key", v.trim()).apply()

    var model: String
        get() = prefs.getString("model", DEFAULT_MODEL)?.ifBlank { DEFAULT_MODEL } ?: DEFAULT_MODEL
        set(v) = prefs.edit().putString("model", v.trim()).apply()

    var userName: String
        get() = prefs.getString("user_name", "") ?: ""
        set(v) = prefs.edit().putString("user_name", v.trim()).apply()

    var voiceEnabled: Boolean
        get() = prefs.getBoolean("voice_enabled", true)
        set(v) = prefs.edit().putBoolean("voice_enabled", v).apply()

    var learningEnabled: Boolean
        get() = prefs.getBoolean("learning_enabled", true)
        set(v) = prefs.edit().putBoolean("learning_enabled", v).apply()

    var handsFree: Boolean
        get() = prefs.getBoolean("hands_free", true)
        set(v) = prefs.edit().putBoolean("hands_free", v).apply()

    var speechRate: Float
        get() = prefs.getFloat("speech_rate", 1.0f)
        set(v) = prefs.edit().putFloat("speech_rate", v).apply()

    var speechPitch: Float
        get() = prefs.getFloat("speech_pitch", 0.9f)
        set(v) = prefs.edit().putFloat("speech_pitch", v).apply()

    // ---------- PIN de la Memoria ----------
    // Se guarda solo el hash (SHA-256 con sal), nunca el PIN en claro.
    var pinHash: String
        get() = prefs.getString("pin_hash", "") ?: ""
        set(v) = prefs.edit().putString("pin_hash", v).apply()

    var pinSalt: String
        get() = prefs.getString("pin_salt", "") ?: ""
        set(v) = prefs.edit().putString("pin_salt", v).apply()

    var pinFails: Int
        get() = prefs.getInt("pin_fails", 0)
        set(v) = prefs.edit().putInt("pin_fails", v).apply()

    var pinLockUntil: Long
        get() = prefs.getLong("pin_lock_until", 0L)
        set(v) = prefs.edit().putLong("pin_lock_until", v).apply()

    // ---------- Mensajes ----------
    fun loadMessages(): List<ChatMessage> = readArray("messages.json").mapObjects { o ->
        ChatMessage(
            id = o.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
            role = o.optString("role", "user"),
            text = o.optString("text"),
            time = o.optLong("time", System.currentTimeMillis()),
            viaVoice = o.optBoolean("voice", false)
        )
    }

    fun saveMessages(list: List<ChatMessage>) {
        val arr = JSONArray()
        list.takeLast(MAX_MESSAGES).forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id).put("role", it.role).put("text", it.text)
                    .put("time", it.time).put("voice", it.viaVoice)
            )
        }
        write("messages.json", arr.toString())
    }

    // ---------- Recuerdos ----------
    fun loadMemories(): List<Memory> = readArray("memories.json").mapObjects { o ->
        Memory(
            id = o.optString("id"),
            category = o.optString("category", Categories.SOBRE_MI),
            text = o.optString("text"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            manual = o.optBoolean("manual", false)
        )
    }.filter { it.text.isNotBlank() && it.id.isNotBlank() }

    fun saveMemories(list: List<Memory>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id).put("category", it.category).put("text", it.text)
                    .put("createdAt", it.createdAt).put("manual", it.manual)
            )
        }
        write("memories.json", arr.toString())
    }

    // ---------- Diario ----------
    fun loadDiary(): List<DiaryEntry> = readArray("diary.json").mapObjects { o ->
        DiaryEntry(
            id = o.optString("id"),
            time = o.optLong("time", System.currentTimeMillis()),
            mood = o.optString("mood"),
            text = o.optString("text")
        )
    }.filter { it.text.isNotBlank() && it.id.isNotBlank() }

    fun saveDiary(list: List<DiaryEntry>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("time", it.time).put("mood", it.mood).put("text", it.text))
        }
        write("diary.json", arr.toString())
    }

    // ---------- Conóceme ----------
    fun loadQa(): List<QaEntry> = readArray("qa.json").mapObjects { o ->
        QaEntry(
            id = o.optString("id"),
            question = o.optString("q"),
            category = o.optString("c"),
            answer = o.optString("a"),
            time = o.optLong("time", System.currentTimeMillis())
        )
    }.filter { it.id.isNotBlank() && it.question.isNotBlank() }

    fun saveQa(list: List<QaEntry>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("q", it.question).put("c", it.category).put("a", it.answer).put("time", it.time))
        }
        write("qa.json", arr.toString())
    }

    /** Preguntas saltadas (para no repetirlas). */
    fun loadSkipped(): List<String> {
        val a = readArray("qa_skipped.json")
        return (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() }
    }

    fun saveSkipped(list: List<String>) {
        val arr = JSONArray()
        list.takeLast(300).forEach { arr.put(it) }
        write("qa_skipped.json", arr.toString())
    }

    /** La pregunta que tienes delante (y la siguiente ya preparada), para que no cambie al cerrar la app. */
    fun loadQuestion(key: String): Question? {
        val raw = prefs.getString(key, null) ?: return null
        return try {
            val o = JSONObject(raw)
            Question(o.getString("q"), o.optString("c"))
        } catch (e: Exception) {
            null
        }
    }

    fun saveQuestion(key: String, q: Question?) {
        if (q == null) prefs.edit().remove(key).apply()
        else prefs.edit().putString(key, JSONObject().put("q", q.text).put("c", q.category).toString()).apply()
    }

    // ---------- Cápsulas del tiempo ----------
    fun loadCapsules(): List<TimeCapsule> = readArray("capsules.json").mapObjects { o ->
        TimeCapsule(
            id = o.optString("id"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            openAt = o.optLong("openAt", 0L),
            text = o.optString("text"),
            opened = o.optBoolean("opened", false),
            novaNote = o.optString("note")
        )
    }.filter { it.id.isNotBlank() && it.text.isNotBlank() }

    fun saveCapsules(list: List<TimeCapsule>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("createdAt", it.createdAt).put("openAt", it.openAt)
                    .put("text", it.text).put("opened", it.opened).put("note", it.novaNote)
            )
        }
        write("capsules.json", arr.toString())
    }

    // ---------- Perfil ----------
    fun loadProfile(): Profile {
        val raw = read("profile.json") ?: return Profile()
        return try {
            val o = JSONObject(raw)
            Profile(o.optString("text"), o.optLong("updatedAt"))
        } catch (e: Exception) {
            Profile()
        }
    }

    fun saveProfile(p: Profile) {
        write("profile.json", JSONObject().put("text", p.text).put("updatedAt", p.updatedAt).toString())
    }

    // ---------- utilidades ----------
    private fun read(name: String): String? {
        val f = File(dir, name)
        return if (f.exists()) f.readText() else null
    }

    private fun write(name: String, content: String) {
        // Escritura atómica: primero a un temporal y luego renombrar
        val tmp = File(dir, "$name.tmp")
        tmp.writeText(content)
        val target = File(dir, name)
        if (!tmp.renameTo(target)) {
            target.writeText(content)
            tmp.delete()
        }
    }

    private fun readArray(name: String): JSONArray {
        val raw = read(name) ?: return JSONArray()
        return try {
            JSONArray(raw)
        } catch (e: Exception) {
            JSONArray()
        }
    }

    private fun <T> JSONArray.mapObjects(block: (JSONObject) -> T): List<T> {
        val out = ArrayList<T>(length())
        for (i in 0 until length()) {
            val o = optJSONObject(i) ?: continue
            out.add(block(o))
        }
        return out
    }

    companion object {
        const val DEFAULT_MODEL = "gpt-5.6-luna"
        const val MAX_MESSAGES = 400
    }
}
