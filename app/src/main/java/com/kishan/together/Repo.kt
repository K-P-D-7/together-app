package com.kishan.together

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

data class Person(
    val uid: String,
    val name: String = "",
    val visible: Boolean = true,
    val online: Boolean = false,
    val since: Long = 0,
    val lastSeen: Long = 0,
    val tag: String = "",
    val mood: String = "",
    val body: String = "",
    val talk: Long = 0,
)

object Repo {
    private lateinit var prefs: SharedPreferences
    private val db get() = FirebaseDatabase.getInstance()

    val users = MutableStateFlow<Map<String, Person>>(emptyMap())
    val nicks = MutableStateFlow<Map<String, String>>(emptyMap())
    var onEvent: ((String, String) -> Unit)? = null

    private var offset = 0L
    private var started = false
    private var loaded = false
    private var prevPartner: Person? = null
    private var localOnline = false

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("together", Context.MODE_PRIVATE)
        if (!prefs.contains("uid")) prefs.edit().putString("uid", UUID.randomUUID().toString().take(12)).apply()
    }

    val uid get() = prefs.getString("uid", "")!!
    val name get() = prefs.getString("name", "")!!
    val room get() = prefs.getString("room", "")!!
    val visible get() = prefs.getBoolean("visible", true)
    val configured get() = name.isNotBlank() && room.isNotBlank()
    var autostartAsked
        get() = prefs.getBoolean("autostartAsked", false)
        set(v) = prefs.edit().putBoolean("autostartAsked", v).apply()

    fun now() = System.currentTimeMillis() + offset

    fun saveSetup(name: String, room: String) {
        prefs.edit().putString("name", name.trim()).putString("room", room.trim()).apply()
    }

    fun display(p: Person) = nicks.value[p.uid]?.takeIf { it.isNotBlank() } ?: p.name

    private fun me() = db.getReference("rooms/$room/users/$uid")

    fun start() {
        if (!configured || started) return
        started = true
        val root = db.getReference("rooms/$room")

        db.getReference(".info/serverTimeOffset").addValueEventListener(listener { offset = it.getValue(Long::class.java) ?: 0 })
        db.getReference(".info/connected").addValueEventListener(listener {
            if (it.getValue(Boolean::class.java) == true) pushPresence()
        })
        root.child("nicks").addValueEventListener(listener { s ->
            nicks.value = s.children.associate { it.key!! to (it.getValue(String::class.java) ?: "") }
        })
        root.child("users").addValueEventListener(listener { s ->
            val map = s.children.associate { it.key!! to parse(it) }
            users.value = map
            val p = map.values.firstOrNull { it.uid != uid }
            if (loaded && p != null) {
                val old = prevPartner
                if (p.talk != 0L && p.talk != (old?.talk ?: 0L)) onEvent?.invoke("talk", "${display(p)} wants to talk 💬")
                if (old?.visible == true && !p.visible) onEvent?.invoke("off", "${display(p)} turned off their online status")
            }
            prevPartner = p
            loaded = true
        })
    }

    private fun parse(s: DataSnapshot) = Person(
        uid = s.key!!,
        name = s.child("name").getValue(String::class.java) ?: "",
        visible = s.child("visible").getValue(Boolean::class.java) ?: true,
        online = s.child("online").getValue(Boolean::class.java) ?: false,
        since = s.child("since").getValue(Long::class.java) ?: 0,
        lastSeen = s.child("lastSeen").getValue(Long::class.java) ?: 0,
        tag = s.child("tag").getValue(String::class.java) ?: "",
        mood = s.child("mood").getValue(String::class.java) ?: "",
        body = s.child("body").getValue(String::class.java) ?: "",
        talk = s.child("talk").getValue(Long::class.java) ?: 0,
    )

    private fun listener(block: (DataSnapshot) -> Unit) = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) = block(snapshot)
        override fun onCancelled(error: DatabaseError) {}
    }

    /** If the connection drops while online, the server marks us offline at once. */
    private fun armDisconnect() {
        val m: Map<String, Any> =
            if (visible && localOnline) mapOf("online" to false, "lastSeen" to ServerValue.TIMESTAMP)
            else mapOf("online" to false)
        me().onDisconnect().updateChildren(m)
    }

    private fun pushPresence() {
        armDisconnect()
        me().updateChildren(mapOf("name" to name, "visible" to visible, "online" to (visible && localOnline)))
    }

    /** Called when the phone starts or stops being in use (unlocked and screen on). */
    fun setActive(on: Boolean) {
        if (!configured) return
        val was = localOnline
        localOnline = on
        if (!visible) return
        val m = mutableMapOf<String, Any>("online" to on)
        if (on && !was) m["since"] = ServerValue.TIMESTAMP
        if (!on && was) m["lastSeen"] = ServerValue.TIMESTAMP
        armDisconnect()
        me().updateChildren(m)
    }

    fun setVisible(v: Boolean) {
        prefs.edit().putBoolean("visible", v).apply()
        val m = mutableMapOf<String, Any>("visible" to v, "online" to (v && localOnline))
        if (v && localOnline) m["since"] = ServerValue.TIMESTAMP
        armDisconnect()
        me().updateChildren(m)
    }

    fun setField(key: String, value: String) {
        if (value.isBlank()) me().child(key).removeValue() else me().child(key).setValue(value)
    }

    fun setNick(uid: String, nick: String) {
        val ref = db.getReference("rooms/$room/nicks/$uid")
        if (nick.isBlank()) ref.removeValue() else ref.setValue(nick.trim())
    }

    fun talk() {
        me().child("talk").setValue(ServerValue.TIMESTAMP)
    }
}
