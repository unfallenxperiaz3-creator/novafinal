package com.unfallen.nova

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.sp
import com.unfallen.nova.data.NovaStatus
import com.unfallen.nova.ui.ChatScreen
import com.unfallen.nova.ui.MemoryScreen
import com.unfallen.nova.ui.Nova
import com.unfallen.nova.ui.NovaTheme
import com.unfallen.nova.ui.PinLockScreen
import com.unfallen.nova.ui.SettingsScreen
import com.unfallen.nova.ui.DiaryScreen
import com.unfallen.nova.ui.CapsuleScreen
import com.unfallen.nova.capsule.CapsuleScheduler
import com.unfallen.nova.ui.KnowMeScreen

enum class Tab(val label: String, val icon: ImageVector, val needsPin: Boolean = false) {
    CHAT("Chat", Icons.Filled.ChatBubble),
    KNOWME("Conóceme", Icons.Filled.QuestionAnswer),
    DIARY("Diario", Icons.Filled.Book, needsPin = true),
    MEMORY("Memoria", Icons.Filled.Psychology, needsPin = true),
    SETTINGS("Ajustes", Icons.Filled.Settings)
}

class MainActivity : ComponentActivity() {
    private val vm: NovaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            NovaTheme { NovaRoot(vm) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Al tocar la notificación de una cápsula: ir a Diario → Cápsulas (pidiendo PIN si toca). */
    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(CapsuleScheduler.EXTRA_OPEN_CAPSULES, false)) {
            vm.showCapsules = true
            vm.openCapsulesRequest = true
            intent.removeExtra(CapsuleScheduler.EXTRA_OPEN_CAPSULES)
        }
    }

    override fun onStop() {
        // Al salir de la app (o apagar pantalla) la Memoria vuelve a pedir el PIN
        vm.lockMemory()
        super.onStop()
    }
}

@Composable
fun NovaRoot(vm: NovaViewModel) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(Tab.CHAT) }
    LaunchedEffect(vm.openCapsulesRequest) {
        if (vm.openCapsulesRequest) {
            tab = Tab.DIARY
            vm.openCapsulesRequest = false
        }
    }

    // Plan B: si el móvil no deja usar el reconocimiento "dentro" de la app, abrimos el de Google
    val systemVoice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val text = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!text.isNullOrBlank()) vm.send(text, viaVoice = true)
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.startListening()
        else vm.reportError("Sin permiso de micrófono no puedo escucharte. Actívalo en Ajustes del móvil → Apps → NOVA → Permisos.")
    }

    val onMic: () -> Unit = {
        when {
            vm.status == NovaStatus.LISTENING -> vm.finishListening()
            !vm.speechAvailable -> {
                vm.stopSpeaking()
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla con NOVA")
                }
                try {
                    systemVoice.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    vm.reportError("Este móvil no tiene reconocimiento de voz. Instala la app de Google.")
                }
            }
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> vm.startListening()
            else -> micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Scaffold(
        containerColor = Nova.Bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!keyboardOpen) {
                NovaBottomBar(tab) { new ->
                    // Memoria y Diario comparten PIN: se vuelve a bloquear al salir de ambos
                    if (tab.needsPin && !new.needsPin) vm.lockMemory()
                    if (new != Tab.DIARY) vm.showCapsules = false
                    tab = new
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                Tab.CHAT -> ChatScreen(vm, onMic = onMic, onOpenSettings = { tab = Tab.SETTINGS })
                Tab.MEMORY -> if (vm.memoryUnlocked) MemoryScreen(vm) else PinLockScreen(vm)
                Tab.KNOWME -> KnowMeScreen(vm)
                Tab.DIARY -> if (vm.memoryUnlocked) {
                    if (vm.showCapsules) CapsuleScreen(vm, onBack = { vm.showCapsules = false })
                    else DiaryScreen(vm, onOpenCapsules = { vm.showCapsules = true })
                } else {
                    PinLockScreen(vm, lockedTitle = "Diario protegido", what = "tu diario")
                }
                Tab.SETTINGS -> SettingsScreen(vm)
            }
        }
    }
}

@Composable
private fun NovaBottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    NavigationBar(containerColor = Color(0xFF0A0D16), tonalElevation = androidx.compose.ui.unit.Dp(0f)) {
        Tab.entries.forEach { t ->
            NavigationBarItem(
                selected = current == t,
                onClick = { onSelect(t) },
                icon = { Icon(t.icon, contentDescription = t.label) },
                label = { Text(t.label, fontSize = 11.sp, maxLines = 1, softWrap = false) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = Nova.PurpleDeep,
                    unselectedIconColor = Nova.Muted,
                    unselectedTextColor = Nova.Muted
                )
            )
        }
    }
}
