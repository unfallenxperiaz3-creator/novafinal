# NOVA — Tu IA personal 👽

App Android (Kotlin + Jetpack Compose) con un alien que te escucha, te habla y te va conociendo.

## Qué hace
- 👽 **Avatar arriba** que respira en reposo, se acerca cuando le hablas, inclina la cabeza cuando piensa y **asiente con cada palabra** cuando contesta.
- 💬 **Chat justo debajo del avatar** (el avatar se puede agrandar o encoger).
- 🎙️ **Voz dentro de la app**: onda que sigue tu voz, texto en directo y botón Cancelar.
- 🔊 **Respuestas habladas** (se pueden silenciar y ajustar la velocidad y el tono).
- 🗣️ **Pestaña Voz**: conversación continua. Terminas de hablar, NOVA responde y vuelve a escucharte.
- 🧠 **Memoria tipo psicólogo**: después de cada respuesta, NOVA analiza lo que le has contado y guarda gustos, personalidad, emociones (con fecha), metas, relaciones, rutinas, bienestar y proyectos. También mantiene un **retrato** de cómo eres.
- 🗑️ **Pestaña Memoria**: ver por categorías, editar, borrar, añadir cosas a mano y borrarlo todo.
- 💾 Conversación, recuerdos y ajustes se guardan en el móvil.

## Compilar en GitHub
1. Sube **todo el contenido** de esta carpeta a un repositorio (incluida la carpeta oculta `.github`).
2. Ve a **Actions → Build NOVA APK → Run workflow**. También se ejecuta solo con cada push a `main`.
3. Cuando termine, descarga el artefacto **NOVA-debug-apk** y dentro tendrás `app-debug.apk`.

Si sale algún error, copia el log del paso *Build debug APK*.

## Primer uso
1. Abre **Ajustes**, pon tu nombre y tu **API key de OpenAI** y pulsa Guardar.
2. El modelo por defecto es `gpt-5.6-luna` (barato y rápido). Puedes cambiarlo en Ajustes.
3. La primera vez que pulses el micro, acepta el permiso de micrófono.
4. Si la voz suena rara: Ajustes del móvil → Idioma → Salida de texto a voz → instala la voz en español de Google.

## Estructura
```
app/src/main/java/com/unfallen/nova/
├── MainActivity.kt        pestañas + permiso de micro
├── NovaViewModel.kt       cerebro de la app (estado, voz, memoria)
├── ai/NovaBrain.kt        OpenAI Responses API + extracción de recuerdos
├── data/                  modelos y guardado en disco
├── voice/                 SpeechInput (micro) y Speaker (voz)
└── ui/                    Avatar, Chat, Voz, Memoria, Ajustes
```

## Privacidad
La API key y los recuerdos se guardan solo en tu móvil. Para contestarte, NOVA envía a OpenAI tu mensaje, la conversación reciente y tus recuerdos (con `store: false`). Si algún día publicas la app, la API key debe ir en un servidor tuyo, no dentro del APK.
