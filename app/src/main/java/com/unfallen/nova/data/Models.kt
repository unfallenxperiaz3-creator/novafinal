package com.unfallen.nova.data

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String,              // "user" | "assistant"
    val text: String,
    val time: Long = System.currentTimeMillis(),
    val viaVoice: Boolean = false
)

data class Memory(
    val id: String = UUID.randomUUID().toString().take(8),
    val category: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val manual: Boolean = false
)

/** Una entrada del diario: lo que hiciste o una reflexión de ese día. */
data class DiaryEntry(
    val id: String = UUID.randomUUID().toString().take(8),
    val time: Long = System.currentTimeMillis(),
    val mood: String = "",          // emoji + palabra, p. ej. "🙂 Bien" (opcional)
    val text: String
)

/** Una pregunta de "Conóceme" ya respondida. */
data class QaEntry(
    val id: String = UUID.randomUUID().toString().take(8),
    val question: String,
    val category: String,
    val answer: String,
    val time: Long = System.currentTimeMillis()
)

/** Pregunta pendiente de responder. */
data class Question(val text: String, val category: String)

object Moods {
    val all = listOf("😄 Genial", "🙂 Bien", "😐 Normal", "😔 Bajo", "😣 Mal")
}

data class Profile(
    val text: String = "",
    val updatedAt: Long = 0L
)

enum class NovaStatus { IDLE, LISTENING, THINKING, SPEAKING }

object Categories {
    const val SOBRE_MI = "Sobre mí"
    const val GUSTOS = "Gustos"
    const val PERSONALIDAD = "Personalidad"
    const val EMOCIONES = "Emociones"
    const val METAS = "Metas"
    const val RELACIONES = "Relaciones"
    const val RUTINAS = "Rutinas"
    const val BIENESTAR = "Bienestar"
    const val PROYECTOS = "Trabajo y proyectos"

    val all = listOf(SOBRE_MI, GUSTOS, PERSONALIDAD, EMOCIONES, METAS, RELACIONES, RUTINAS, BIENESTAR, PROYECTOS)

    fun emoji(category: String): String = when (category) {
        SOBRE_MI -> "🪪"
        GUSTOS -> "❤️"
        PERSONALIDAD -> "🧩"
        EMOCIONES -> "🌗"
        METAS -> "🎯"
        RELACIONES -> "👥"
        RUTINAS -> "🔁"
        BIENESTAR -> "🌿"
        PROYECTOS -> "🛠️"
        else -> "✨"
    }

    /** Normaliza lo que devuelva la IA a una de nuestras categorías. */
    fun normalize(raw: String): String {
        val r = raw.trim().lowercase()
        return all.firstOrNull { it.lowercase() == r }
            ?: when {
                "gust" in r || "prefer" in r -> GUSTOS
                "personal" in r || "carácter" in r || "caracter" in r -> PERSONALIDAD
                "emoc" in r || "ánimo" in r || "animo" in r || "sentim" in r -> EMOCIONES
                "meta" in r || "objetiv" in r || "sueño" in r -> METAS
                "relac" in r || "famil" in r || "amig" in r || "pareja" in r -> RELACIONES
                "rutin" in r || "hábit" in r || "habit" in r -> RUTINAS
                "salud" in r || "bienest" in r -> BIENESTAR
                "trabaj" in r || "proyect" in r -> PROYECTOS
                else -> SOBRE_MI
            }
    }
}
