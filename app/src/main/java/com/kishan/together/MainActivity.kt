package com.kishan.together

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val TAGS = listOf("📚 Studying", "📱 Shorting", "📸 Reeling", "🎮 Gaming", "💼 Working", "🍽 Eating", "😴 Sleeping", "💬 Chatting")
private val MOODS = listOf("😊 Happy", "😡 Mad", "😢 Sad", "😞 Depressed", "😤 Hate you")
private val BODY = listOf("🩸 Period", "😣 PMSing")

class MainActivity : ComponentActivity() {
    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) { App() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Repo.configured) PresenceService.start(this)
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    var configured by remember { mutableStateOf(Repo.configured) }
    if (configured) Home()
    else Setup { configured = true; PresenceService.start(ctx) }
}

@Composable
fun Setup(onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("Together", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("Make a code on one phone and type the same code on the other.")
        OutlinedTextField(name, { name = it }, label = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(room, { room = it }, label = { Text("Shared code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = {
            room = (1..10).map { "abcdefghjkmnpqrstuvwxyz23456789".random() }.joinToString("")
        }) { Text("Make a new code") }
        Button(
            onClick = { Repo.saveSetup(name, room); onDone() },
            enabled = name.isNotBlank() && room.trim().length >= 6,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Start") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Home() {
    val users by Repo.users.collectAsState()
    val nicks by Repo.nicks.collectAsState()
    var now by remember { mutableLongStateOf(Repo.now()) }
    LaunchedEffect(Unit) { while (true) { now = Repo.now(); delay(1000) } }
    var editNick by remember { mutableStateOf<Person?>(null) }

    val me = users[Repo.uid] ?: Person(Repo.uid, Repo.name)
    val partner = users.values.firstOrNull { it.uid != Repo.uid }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).systemBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BatteryHint()

        if (partner == null) {
            Card(Modifier.fillMaxWidth()) {
                Text("Waiting for your partner. Code: ${Repo.room}", Modifier.padding(20.dp))
            }
        } else {
            PartnerCard(partner, nicks, now) { editNick = partner }
            Button(onClick = { Repo.talk() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("💬 I want to talk")
            }
        }

        HorizontalDivider()

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Me: ${nicks[me.uid]?.takeIf { it.isNotBlank() } ?: me.name}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = { editNick = me }) { Text("✏️ Nickname") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Share my online status", Modifier.weight(1f))
            Switch(checked = me.visible, onCheckedChange = { Repo.setVisible(it) })
        }

        if (me.visible) {
            Section("Doing", TAGS, me.tag) { Repo.setField("tag", it) }
            var custom by remember { mutableStateOf("") }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(custom, { custom = it }, label = { Text("Custom") }, singleLine = true, modifier = Modifier.weight(1f))
                Button(onClick = { Repo.setField("tag", custom); custom = "" }, enabled = custom.isNotBlank()) { Text("Set") }
            }
            Section("Mood", MOODS, me.mood) { Repo.setField("mood", it) }
            Section("Cycle", BODY, me.body) { Repo.setField("body", it) }
        } else {
            Text("Your partner sees that you turned this off.", color = Color.Gray)
        }
    }

    editNick?.let { p ->
        NickDialog(nicks[p.uid] ?: "", onDismiss = { editNick = null }) { Repo.setNick(p.uid, it); editNick = null }
    }
}

@Composable
fun PartnerCard(p: Person, nicks: Map<String, String>, now: Long, onEdit: () -> Unit) {
    val dot = when {
        !p.visible -> Color(0xFFFF9800)
        p.online -> Color(0xFF2ECC71)
        else -> Color(0xFF777777)
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(18.dp).background(dot, CircleShape))
                Spacer(Modifier.width(12.dp))
                Text(nicks[p.uid]?.takeIf { it.isNotBlank() } ?: p.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onEdit) { Text("✏️") }
            }
            Text(statusLine(p, now), fontSize = 16.sp)
            if (p.visible) {
                listOf(p.tag, p.mood, p.body).filter { it.isNotBlank() }.forEach { Text(it, fontSize = 18.sp) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Section(title: String, options: List<String>, current: String, onPick: (String) -> Unit) {
    Text(title, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            FilterChip(selected = current == o, onClick = { onPick(if (current == o) "" else o) }, label = { Text(o) })
        }
        if (current.isNotBlank() && current !in options) {
            FilterChip(selected = true, onClick = { onPick("") }, label = { Text(current) })
        }
    }
}

@Composable
fun NickDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nickname (both of you see it)") },
        text = { OutlinedTextField(text, { text = it }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun BatteryHint() {
    val ctx = LocalContext.current
    val pm = ctx.getSystemService(PowerManager::class.java)
    var ignoring by remember { mutableStateOf(pm.isIgnoringBatteryOptimizations(ctx.packageName)) }
    if (ignoring) return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Allow Together to run in the background so your status stays accurate.")
            TextButton(onClick = {
                ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}")))
                ignoring = true
            }) { Text("Allow") }
        }
    }
}

fun statusLine(p: Person, now: Long): String = when {
    !p.visible -> "Turned off their online status"
    p.online -> "Online · ${dur(now - p.since)}"
    p.lastSeen == 0L -> "Offline"
    else -> "Last online ${ago(now - p.lastSeen)} · was on for ${dur(p.lastSeen - p.since)}"
}

private fun dur(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return when {
        s < 60 -> "${s}s"
        s < 3600 -> "${s / 60}m ${s % 60}s"
        else -> "${s / 3600}h ${s % 3600 / 60}m"
    }
}

private fun ago(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return when {
        s < 10 -> "just now"
        s < 60 -> "${s}s ago"
        s < 3600 -> "${s / 60} min ago"
        s < 86400 -> "${s / 3600} h ago"
        else -> "${s / 86400} d ago"
    }
}
