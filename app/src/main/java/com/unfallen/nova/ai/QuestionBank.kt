package com.unfallen.nova.ai

import com.unfallen.nova.data.Question

/**
 * Preguntas de reserva: se usan si no hay API key o si falla internet.
 * Con API key, NOVA inventa preguntas nuevas sin fin, adaptadas a lo que ya sabe de ti.
 */
object QuestionBank {
    val categories = listOf(
        "Infancia", "Recuerdos", "Gustos", "Valores", "Sueños", "Miedos", "Relaciones",
        "Trabajo", "Hábitos", "¿Qué harías si…?", "Filosofía", "Humor", "Emociones", "Futuro", "Curiosidades"
    )

    private val bank: List<Question> = listOf(
        "Infancia" to "¿Qué querías ser de mayor cuando eras pequeño?",
        "Infancia" to "¿Cuál es el primer recuerdo que tienes?",
        "Infancia" to "¿A qué jugabas más cuando eras niño?",
        "Infancia" to "¿Había algún adulto al que admiraras de pequeño? ¿Por qué?",
        "Recuerdos" to "¿Cuál ha sido el mejor viaje de tu vida?",
        "Recuerdos" to "¿Qué día de tu vida repetirías si pudieras?",
        "Recuerdos" to "¿Cuándo fue la última vez que te reíste hasta llorar?",
        "Recuerdos" to "¿Qué momento te hizo sentir más orgulloso de ti mismo?",
        "Gustos" to "¿Qué canción pondrías para describir cómo eres?",
        "Gustos" to "¿Cuál es tu comida favorita y cuál no soportas?",
        "Gustos" to "¿Qué película o serie podrías ver mil veces?",
        "Gustos" to "¿Prefieres la montaña o la playa? ¿Por qué?",
        "Gustos" to "¿Qué haces cuando tienes un día entero libre para ti?",
        "Gustos" to "¿Qué coche tendrías si el dinero no importara?",
        "Valores" to "¿Qué es lo que más valoras en una persona?",
        "Valores" to "¿Qué cosa nunca harías, aunque te pagaran mucho dinero?",
        "Valores" to "¿Qué significa para ti tener éxito?",
        "Valores" to "¿Hay alguna regla de la sociedad que te parezca absurda?",
        "Sueños" to "Si mañana pudieras dedicarte a lo que quisieras, ¿qué harías?",
        "Sueños" to "¿Qué te gustaría haber conseguido dentro de cinco años?",
        "Sueños" to "¿Qué lugar del mundo quieres visitar antes de morir?",
        "Sueños" to "¿Tienes algún proyecto que nunca te has atrevido a empezar?",
        "Miedos" to "¿Qué es lo que más te preocupa últimamente?",
        "Miedos" to "¿A qué le tenías miedo de pequeño y ya no?",
        "Miedos" to "¿Qué te impide a veces hacer lo que de verdad quieres?",
        "Relaciones" to "¿Quién es la persona que mejor te conoce?",
        "Relaciones" to "¿Qué aprendiste de tu padre o de tu madre?",
        "Relaciones" to "¿Cómo eres cuando estás con tus amigos de confianza?",
        "Relaciones" to "¿Hay alguien con quien te gustaría arreglar las cosas?",
        "Relaciones" to "¿Qué te hace sentir querido?",
        "Trabajo" to "¿Qué parte de tu trabajo o de tus proyectos te hace disfrutar de verdad?",
        "Trabajo" to "¿Qué harías distinto si empezaras tu carrera de cero?",
        "Trabajo" to "¿Qué habilidad te gustaría dominar este año?",
        "Hábitos" to "¿Cómo es un día normal tuyo, desde que te levantas?",
        "Hábitos" to "¿Qué hábito te gustaría tener y todavía no tienes?",
        "Hábitos" to "¿Eres más de mañanas o de noches?",
        "Hábitos" to "¿Qué haces para desconectar cuando estás agobiado?",
        "¿Qué harías si…?" to "¿Qué harías si te tocara la lotería mañana?",
        "¿Qué harías si…?" to "Si pudieras viajar en el tiempo una sola vez, ¿a qué momento irías?",
        "¿Qué harías si…?" to "Si pudieras tener un superpoder, ¿cuál elegirías y para qué lo usarías?",
        "¿Qué harías si…?" to "Si tuvieras que vivir en otro país, ¿cuál elegirías?",
        "¿Qué harías si…?" to "Si pudieras cenar con cualquier persona, viva o muerta, ¿con quién sería?",
        "Filosofía" to "¿Crees que las personas pueden cambiar de verdad?",
        "Filosofía" to "¿Qué crees que pasa después de la muerte?",
        "Filosofía" to "¿Crees en el destino o en que cada uno se lo busca?",
        "Filosofía" to "¿Crees que hay vida en otros planetas? (Pregunto por un amigo 👽)",
        "Humor" to "¿Cuál es la cosa más absurda que has hecho?",
        "Humor" to "¿Qué manía rara tienes que poca gente sabe?",
        "Humor" to "¿Cuál es tu peor habilidad, esa que te sale fatal?",
        "Emociones" to "¿Qué te pone de buen humor al instante?",
        "Emociones" to "¿Qué te enfada más de lo que debería?",
        "Emociones" to "¿Cómo sabes cuándo estás empezando a estresarte?",
        "Emociones" to "¿Te cuesta pedir ayuda? ¿Por qué crees que es?",
        "Futuro" to "¿Cómo te imaginas con 70 años?",
        "Futuro" to "¿Qué consejo te darías a ti mismo dentro de diez años?",
        "Futuro" to "¿Qué te gustaría que la gente recordara de ti?",
        "Curiosidades" to "¿Qué tema podrías explicar durante horas sin aburrirte?",
        "Curiosidades" to "¿Qué es algo que aprendiste hace poco y te sorprendió?",
        "Curiosidades" to "¿Qué objeto de tu casa tiene más historia?"
    ).map { (c, q) -> Question(q, c) }

    /** Una pregunta de reserva que aún no se haya hecho (o cualquiera si ya se hicieron todas). */
    fun pick(alreadyAsked: Set<String>): Question {
        val fresh = bank.filter { it.text !in alreadyAsked }
        return (fresh.ifEmpty { bank }).random()
    }
}
