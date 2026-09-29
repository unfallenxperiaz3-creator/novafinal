package com.unfallen.nova.ai

import com.unfallen.nova.data.Categories
import com.unfallen.nova.data.ChatMessage
import com.unfallen.nova.data.DiaryEntry
import com.unfallen.nova.data.Memory
import com.unfallen.nova.data.Profile
import com.unfallen.nova.data.QaEntry
import com.unfallen.nova.data.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NovaException(message: String) : Exception(message)

/** Resultado del análisis de memoria tras cada conversación. */
data class MemoryUpdate(
    val add: List<Memory>,
    val update: Map<String, String>,
    val delete: Set<String>,
    val profile: String?
)

/**
 * Habla con la OpenAI Responses API.
 * - chat(): respuesta de NOVA
 * - learn(): analiza lo último que has dicho y actualiza recuerdos + retrato psicológico
 */
class NovaBrain {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val es = Locale.forLanguageTag("es-ES")

    // ------------------------------------------------------------------ CHAT

    suspend fun chat(
        apiKey: String,
        model: String,
        userName: String,
        history: List<ChatMessage>,
        memories: List<Memory>,
        profile: Profile,
        voiceMode: Boolean,
        diary: List<DiaryEntry> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val input = JSONArray()
        history.takeLast(24).forEach { m ->
            input.put(JSONObject().put("role", if (m.role == "user") "user" else "assistant").put("content", m.text))
        }
        val body = JSONObject()
            .put("model", model)
            .put("instructions", buildInstructions(userName, memories, profile, voiceMode, diary))
            .put("input", input)
            .put("max_output_tokens", 3000)
            .put("store", false)
        val raw = post(apiKey, body, allowReasoningFallback = true)
        parseText(raw).ifBlank { throw NovaException("La IA no ha devuelto texto. Prueba otra vez.") }
    }

    private fun buildInstructions(
        userName: String,
        memories: List<Memory>,
        profile: Profile,
        voiceMode: Boolean,
        diary: List<DiaryEntry>
    ): String {
        val now = SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy, HH:mm", es).format(Date())
        val name = userName.ifBlank { "(todavía no me ha dicho su nombre)" }
        val memText = if (memories.isEmpty()) "(Todavía no sé nada. Estoy empezando a conocerle.)"
        else Categories.all.mapNotNull { cat ->
            val items = memories.filter { it.category == cat }
            if (items.isEmpty()) null
            else "## $cat\n" + items.joinToString("\n") { "- [${shortDate(it.createdAt)}] ${it.text}" }
        }.joinToString("\n\n")
        val profileText = profile.text.ifBlank { "(Aún no hay retrato. Se irá formando con las conversaciones.)" }
        val style = if (voiceMode)
            "AHORA TE HABLA POR VOZ: responde como en una conversación hablada, en 1-4 frases naturales, sin listas, sin emojis, sin símbolos ni formato."
        else
            "Responde de forma natural y cercana. Normalmente 2-6 frases; si te pide algo técnico o detallado, extiéndete lo necesario. Evita emojis en exceso."

        return """
Eres NOVA, una inteligencia extraterrestre amable que vive en el móvil de $name y se ha convertido en su compañera personal.
Tu misión: conversar, acompañar y conocer de verdad a esta persona con el tiempo, como lo haría un buen psicólogo que además es un amigo de confianza.

CÓMO ERES
- Cálida, curiosa, honesta y directa. Nada de frases vacías ni peloteo; si algo no te parece buena idea, lo dices con tacto.
- Escuchas activamente: reflejas lo que la persona siente, validas sin exagerar y, cuando encaja, haces UNA buena pregunta para entender mejor (no interrogues).
- Usas lo que sabes de ella para personalizar: recuerdas sus gustos, metas, preocupaciones y lo que te contó otros días, y conectas ideas ("la semana pasada me dijiste que...") solo cuando aporta.
- Detectas patrones (estrés que se repite, hábitos, motivaciones) y los comentas con delicadeza cuando es útil.
- No diagnosticas enfermedades ni sustituyes a un profesional. Si notas malestar serio y mantenido, sugiere con naturalidad hablar con un psicólogo o médico.
- Si la persona expresa ideas de hacerse daño o de suicidio, respóndele con mucho cuidado, quédate con ella en la conversación y anímala a llamar ya al 024 (línea de atención a la conducta suicida en España, gratuita, 24h) o al 112 si hay peligro inmediato.
- Nunca inventes recuerdos. Si no sabes algo de ella, pregúntalo.
- Si un recuerdo contradice lo que acaba de decir, lo nuevo manda.
- Idioma: español de España, salvo que te hablen en otro idioma.
- $style

FECHA Y HORA ACTUAL: $now

RETRATO DE LA PERSONA (lo que has ido entendiendo de cómo es):
$profileText

RECUERDOS (fecha en que lo aprendiste):
$memText

DIARIO RECIENTE (lo que ha escrito sobre sus días; úsalo con tacto, sin recitarlo):
${diaryText(diary)}
""".trim()
    }

    // ------------------------------------------------------------------ MEMORIA

    suspend fun learn(
        apiKey: String,
        model: String,
        recent: List<ChatMessage>,
        memories: List<Memory>,
        profile: Profile
    ): MemoryUpdate? = withContext(Dispatchers.IO) {
        val convo = recent.takeLast(8).joinToString("\n") {
            (if (it.role == "user") "PERSONA: " else "NOVA: ") + it.text
        }
        val current = if (memories.isEmpty()) "(vacío)" else memories.joinToString("\n") {
            "${it.id} | ${it.category} | ${shortDate(it.createdAt)} | ${it.text}"
        }
        val today = SimpleDateFormat("d/M/yyyy", es).format(Date())
        val prompt = """
Eres el módulo de memoria de NOVA, una IA que quiere conocer a su usuario como lo haría un psicólogo atento.
Analiza SOLO lo que la PERSONA ha dicho en la conversación reciente y decide qué merece recordarse a largo plazo.

Categorías permitidas: ${Categories.all.joinToString(", ")}.

Qué guardar:
- Datos personales estables (nombre, edad aproximada, dónde vive a nivel de ciudad, trabajo, coche, mascotas...).
- Gustos, preferencias y cosas que no le gustan.
- Rasgos de personalidad y forma de pensar que ÉL/ELLA muestre o diga de sí mismo.
- Estados emocionales relevantes y su causa, con fecha (hoy es $today). Ej: "($today) Se siente estresado por el trabajo".
- Metas, sueños, preocupaciones, relaciones importantes, rutinas y hábitos.
Qué NO guardar:
- Preguntas genéricas o peticiones puntuales ("explícame X").
- Contraseñas, claves, números de tarjeta o cuenta, documentos de identidad.
- Cosas que dijo NOVA y la persona no confirmó.
- Duplicados de recuerdos que ya existen.

Si algo nuevo corrige o actualiza un recuerdo existente, usa "update" con su id. Si un recuerdo ya no es cierto, ponlo en "delete".
Escribe cada recuerdo como una frase corta en tercera persona ("Le encanta conducir por puertos de montaña").

Además mantén un RETRATO breve (máx. 120 palabras) de cómo es la persona: carácter, qué le motiva, qué le preocupa, cómo prefiere que le hablen.
Devuelve "profile" solo si ha cambiado algo significativo; si no, devuelve "".

RETRATO ACTUAL:
${profile.text.ifBlank { "(vacío)" }}

RECUERDOS ACTUALES (id | categoría | fecha | texto):
$current

CONVERSACIÓN RECIENTE:
$convo

Responde ÚNICAMENTE con JSON válido, sin texto extra, con este formato:
{"add":[{"category":"...","text":"..."}],"update":[{"id":"...","text":"..."}],"delete":["id"],"profile":""}
""".trim()

        val body = JSONObject()
            .put("model", model)
            .put("input", prompt)
            .put("max_output_tokens", 2500)
            .put("store", false)
        val raw = try {
            post(apiKey, body, allowReasoningFallback = true)
        } catch (e: Exception) {
            return@withContext null
        }
        parseMemoryUpdate(parseText(raw))
    }

    // ------------------------------------------------------------------ CÁPSULAS

    /** Lo que NOVA le dice a la persona al abrir una cápsula del tiempo. */
    suspend fun capsuleNote(
        apiKey: String,
        model: String,
        userName: String,
        capsuleText: String,
        writtenAt: Long,
        profile: Profile,
        memories: List<Memory>,
        diary: List<DiaryEntry>
    ): String? = withContext(Dispatchers.IO) {
        val fmt = SimpleDateFormat("d 'de' MMMM 'de' yyyy", es)
        val since = memories.filter { it.createdAt >= writtenAt }
            .joinToString("\n") { "- ${it.category}: ${it.text}" }.ifBlank { "(pocos datos nuevos desde entonces)" }
        val instructions = """
Eres NOVA, la compañera extraterrestre de ${userName.ifBlank { "esta persona" }}. Hoy (${fmt.format(Date())}) abre una cápsula del tiempo que se escribió a sí mismo el ${fmt.format(Date(writtenAt))}.
Escríbele un mensaje breve (80-150 palabras), cálido y honesto, en español de España, tuteando, sin listas ni emojis:
- Comenta qué ha cambiado en él/ella desde entonces y qué sigue igual, usando lo que sabes. No inventes nada.
- Si en la cápsula se ponía metas o hacía preguntas a su yo futuro, haz referencia a ellas con tacto.
- Termina con una pregunta que le invite a reflexionar.

RETRATO ACTUAL:
${profile.text.ifBlank { "(aún no hay)" }}

LO QUE HAS APRENDIDO DE ÉL/ELLA DESDE QUE ESCRIBIÓ LA CÁPSULA:
$since

DIARIO RECIENTE:
${diaryText(diary)}
""".trim()
        val body = JSONObject()
            .put("model", model)
            .put("instructions", instructions)
            .put("input", "Mi cápsula del tiempo dice:\n\n$capsuleText")
            .put("max_output_tokens", 1500)
            .put("store", false)
        try {
            parseText(post(apiKey, body, allowReasoningFallback = true)).ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }

    // ------------------------------------------------------------------ CONÓCEME

    /**
     * Inventa la siguiente pregunta para conocer mejor a la persona.
     * Nunca repite, va variando de tema y a veces profundiza en una respuesta anterior.
     */
    suspend fun nextQuestion(
        apiKey: String,
        model: String,
        userName: String,
        profile: Profile,
        memories: List<Memory>,
        recentQa: List<QaEntry>,
        avoid: List<String>
    ): Question? = withContext(Dispatchers.IO) {
        val known = memories.takeLast(60).joinToString("\n") { "- ${it.category}: ${it.text}" }.ifBlank { "(casi nada todavía)" }
        val qa = recentQa.takeLast(12).joinToString("\n") { "- ${it.question}" }.ifBlank { "(ninguna todavía)" }
        val lastCats = recentQa.takeLast(4).joinToString(", ") { it.category }.ifBlank { "ninguna" }
        val deepen = recentQa.isNotEmpty() && Math.random() < 0.3
        val prompt = """
Eres NOVA, una IA extraterrestre curiosa y cálida que quiere conocer a fondo a ${userName.ifBlank { "su usuario" }}, como un buen psicólogo que además es su amigo.
Escribe UNA sola pregunta nueva para hacerle ahora.

Reglas:
- En español de España, tuteando, natural y cercana. Máximo 25 palabras.
- Varía muchísimo los temas: infancia, recuerdos, gustos (música, cine, comida, coches, viajes), valores, sueños, miedos, relaciones, familia, amistad, trabajo y proyectos, hábitos, salud y deporte, dinero y metas, dilemas "¿qué harías si…?", filosofía, humor, emociones, futuro, curiosidades raras.
- No uses la misma categoría que las últimas preguntas ($lastCats).
- ${if (deepen) "Esta vez PROFUNDIZA en algo concreto que ya sabes de él/ella (sus recuerdos o su retrato) con una pregunta de seguimiento." else "Esta vez abre un tema nuevo del que aún sepas poco."}
- No repitas ni reformules ninguna de estas preguntas ya hechas:
${avoid.takeLast(80).joinToString("\n") { "  · $it" }.ifBlank { "  (ninguna)" }}
- Alterna preguntas ligeras y divertidas con otras más profundas. Nada de preguntas incómodas sobre sexo, dinero exacto o datos privados como direcciones o contraseñas.

LO QUE YA SABES DE ÉL/ELLA:
$known

RETRATO:
${profile.text.ifBlank { "(aún no hay)" }}

ÚLTIMAS PREGUNTAS QUE YA LE HICISTE:
$qa

Responde SOLO con JSON: {"category":"una o dos palabras","question":"..."}
""".trim()
        val body = JSONObject()
            .put("model", model)
            .put("input", prompt)
            .put("max_output_tokens", 800)
            .put("store", false)
        val raw = try {
            post(apiKey, body, allowReasoningFallback = true)
        } catch (e: Exception) {
            return@withContext null
        }
        val text = parseText(raw)
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return@withContext null
        try {
            val o = JSONObject(text.substring(start, end + 1))
            val q = o.optString("question").trim()
            if (q.length < 5) null else Question(q, o.optString("category").trim().ifBlank { "Conóceme" })
        } catch (e: Exception) {
            null
        }
    }

    private fun parseMemoryUpdate(text: String): MemoryUpdate? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return try {
            val o = JSONObject(text.substring(start, end + 1))
            val add = mutableListOf<Memory>()
            o.optJSONArray("add")?.let { a ->
                for (i in 0 until a.length()) {
                    val item = a.optJSONObject(i) ?: continue
                    val t = item.optString("text").trim()
                    if (t.length >= 3) add.add(Memory(category = Categories.normalize(item.optString("category")), text = t))
                }
            }
            val update = mutableMapOf<String, String>()
            o.optJSONArray("update")?.let { a ->
                for (i in 0 until a.length()) {
                    val item = a.optJSONObject(i) ?: continue
                    val id = item.optString("id").trim()
                    val t = item.optString("text").trim()
                    if (id.isNotBlank() && t.isNotBlank()) update[id] = t
                }
            }
            val delete = mutableSetOf<String>()
            o.optJSONArray("delete")?.let { a ->
                for (i in 0 until a.length()) a.optString(i).trim().takeIf { it.isNotBlank() }?.let { delete.add(it) }
            }
            val profile = o.optString("profile").trim().takeIf { it.isNotBlank() }
            MemoryUpdate(add, update, delete, profile)
        } catch (e: Exception) {
            null
        }
    }

    // ------------------------------------------------------------------ HTTP

    private fun post(apiKey: String, body: JSONObject, allowReasoningFallback: Boolean): String {
        // Primero intentamos con razonamiento "low" (más rápido). Si el modelo no lo admite, reintenta sin él.
        if (allowReasoningFallback) {
            val withReasoning = JSONObject(body.toString()).put("reasoning", JSONObject().put("effort", "low"))
            try {
                return execute(apiKey, withReasoning)
            } catch (e: HttpError) {
                if (e.code != 400) throw NovaException(e.friendly)
            }
        }
        return try {
            execute(apiKey, body)
        } catch (e: HttpError) {
            throw NovaException(e.friendly)
        }
    }

    private class HttpError(val code: Int, val friendly: String) : Exception(friendly)

    private fun execute(apiKey: String, body: JSONObject): String {
        val request = Request.Builder()
            .url("https://api.openai.com/v1/responses")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val (code, raw) = try {
            client.newCall(request).execute().use { r -> Pair(r.code, r.body?.string().orEmpty()) }
        } catch (e: java.net.SocketTimeoutException) {
            throw NovaException("La IA ha tardado demasiado en responder. Prueba otra vez.")
        } catch (e: java.io.IOException) {
            throw NovaException("Sin conexión a internet (${e.message ?: "error de red"}).")
        }
        if (code !in 200..299) {
            val apiMsg = try {
                JSONObject(raw).optJSONObject("error")?.optString("message").orEmpty()
            } catch (e: Exception) {
                ""
            }
            val friendly = when (code) {
                401 -> "La API key no es válida. Revísala en Ajustes."
                403 -> "Tu cuenta no tiene acceso a este modelo. ($apiMsg)"
                404 -> "No encuentro el modelo. Revisa el nombre en Ajustes. ($apiMsg)"
                429 -> "Sin saldo o demasiadas peticiones seguidas. Revisa tu cuenta de OpenAI. ($apiMsg)"
                in 500..599 -> "Los servidores de OpenAI están fallando ahora mismo. Prueba en un momento."
                else -> "Error $code: ${apiMsg.ifBlank { "respuesta inesperada" }}"
            }
            throw HttpError(code, friendly)
        }
        return raw
    }

    private fun parseText(raw: String): String {
        val root = try {
            JSONObject(raw)
        } catch (e: Exception) {
            return ""
        }
        val direct = root.optString("output_text")
        if (direct.isNotBlank()) return direct.trim()
        val output = root.optJSONArray("output") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until output.length()) {
            val item = output.optJSONObject(i) ?: continue
            if (item.optString("type") != "message") continue
            val content = item.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                val part = content.optJSONObject(j) ?: continue
                val type = part.optString("type")
                if (type == "output_text" || type == "text") sb.append(part.optString("text"))
            }
        }
        return sb.toString().trim()
    }

    private fun diaryText(diary: List<DiaryEntry>): String =
        if (diary.isEmpty()) "(Todavía no ha escrito en su diario.)"
        else diary.sortedByDescending { it.time }.take(7).joinToString("\n") { e ->
            val day = SimpleDateFormat("EEEE d/M", es).format(Date(e.time))
            val mood = if (e.mood.isNotBlank()) " [ánimo: ${e.mood}]" else ""
            "- $day$mood: ${e.text.take(600)}"
        }

    private fun shortDate(t: Long): String = SimpleDateFormat("d/M/yy", es).format(Date(t))
}
