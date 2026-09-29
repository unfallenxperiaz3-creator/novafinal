package com.unfallen.nova.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unfallen.nova.NovaViewModel
import com.unfallen.nova.R
import com.unfallen.nova.data.QaEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val qaDate = SimpleDateFormat("d MMM · HH:mm", Locale.forLanguageTag("es-ES"))

/** "Conóceme": NOVA pregunta, tú respondes, y aparece otra pregunta. Sin fin. */
@Composable
fun KnowMeScreen(vm: NovaViewModel) {
    LaunchedEffect(Unit) { vm.ensureQuestion() }
    var answer by rememberSaveable { mutableStateOf("") }
    val q = vm.currentQuestion

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(Nova.Bg)
            .imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        item {
            Column(Modifier.statusBarsPadding().padding(top = 14.dp, bottom = 10.dp)) {
                Text("Conóceme", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (vm.qa.isEmpty()) "NOVA te pregunta, tú respondes cuando quieras. Las preguntas no se acaban nunca."
                    else "Has respondido ${vm.qa.size} pregunta${if (vm.qa.size == 1) "" else "s"}. NOVA memoriza lo importante y la respuesta desaparece.",
                    color = Nova.Muted, fontSize = 14.sp, lineHeight = 19.sp
                )
            }
        }

        // ---------- Pregunta actual ----------
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1A1740), Color(0xFF0F1A2E))))
                    .border(1.dp, Color(0x557B5CFF), RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.nova_avatar),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = BiasAlignment(0f, -0.35f),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(2.dp, Nova.Purple, CircleShape)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("NOVA pregunta", color = Nova.Muted, fontSize = 12.sp)
                        if (q != null) {
                            Text(q.category, color = Nova.Violet, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.size(14.dp))
                Crossfade(targetState = q, animationSpec = tween(350), label = "question") { shown ->
                    if (shown == null) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 60.dp)) {
                            ThinkingDots()
                            Spacer(Modifier.width(10.dp))
                            Text("Pensando una pregunta para ti…", color = Nova.Muted, fontSize = 15.sp)
                        }
                    } else {
                        Text(
                            shown.text,
                            color = Color.White, fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.heightIn(min = 60.dp)
                        )
                    }
                }
                Spacer(Modifier.size(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0E1220))
                        .border(1.dp, Nova.Stroke, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    if (answer.isEmpty()) Text("Tu respuesta…", color = Nova.Muted, fontSize = 15.sp)
                    BasicTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        textStyle = TextStyle(color = Nova.Text, fontSize = 15.sp, lineHeight = 21.sp),
                        cursorBrush = SolidColor(Nova.Purple),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 82.dp)
                    )
                }
                Spacer(Modifier.size(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { vm.skipQuestion(); answer = "" },
                        enabled = q != null
                    ) { Text("Otra pregunta", color = Nova.Muted) }
                    Spacer(Modifier.weight(1f))
                    val canSend = q != null && answer.isNotBlank() && vm.hasApiKey
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (canSend) Nova.Purple else Color(0xFF2A2F45))
                            .clickable(enabled = canSend) {
                                vm.answerQuestion(answer)
                                answer = ""
                            }
                            .padding(horizontal = 20.dp, vertical = 11.dp)
                    ) { Text("Responder", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
                }
                if (!vm.hasApiKey) {
                    Text(
                        "Para memorizar tus respuestas necesito tu API key. Añádela en Ajustes.",
                        color = Nova.Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        item {
            vm.learnedNotice?.let {
                Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) { NoticeChip(it) }
            }
        }

    }
}
