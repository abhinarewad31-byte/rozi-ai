package com.rozi.assistant

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val prefs by lazy { getSharedPreferences("rozi_local", MODE_PRIVATE) }
    private lateinit var chat: LinearLayout
    private lateinit var input: EditText
    private lateinit var language: Spinner
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private var pendingPermissionCommand: String? = null
    private var pendingRecipient: String? = null
    private var pendingMessage: String? = null
    private var pendingNumber: String? = null
    private var pendingMatches: List<Pair<String, String>> = emptyList()
    private var waitingForRecipientChoice = false
    private lateinit var titleView: TextView

    private val languages = listOf("English", "हिन्दी", "मराठी")
    private val locales = listOf(Locale.US, Locale("hi", "IN"), Locale("mr", "IN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildUi()
        say("Hi, I’m ${assistantName()}. What’s on your mind?")
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 20, 22, 14)
            setBackgroundColor(0xFFF7F5FC.toInt())
        }
        titleView = TextView(this).apply {
            text = assistantName()
            textSize = 28f
            setTextColor(0xFF28223A.toInt())
            gravity = Gravity.CENTER_VERTICAL
        }
        root.addView(titleView, LinearLayout.LayoutParams(-1, 52))
        language = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, languages)
            setSelection(prefs.getInt("language", 0).coerceIn(0, 2))
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    prefs.edit().putInt("language", position).apply()
                    updateTtsLanguage()
                }
            }
        }
        root.addView(language, LinearLayout.LayoutParams(-1, 48))

        val scroll = ScrollView(this).apply { isFillViewport = true }
        chat = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 12, 0, 12) }
        scroll.addView(chat)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        input = EditText(this).apply {
            hint = "Talk to ${assistantName()} or type here"
            minLines = 1
            maxLines = 4
            textSize = 16f
            setPadding(14, 8, 14, 8)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        root.addView(input, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val mic = Button(this).apply { text = "🎙 Speak"; setOnClickListener { listen() } }
        val send = Button(this).apply {
            text = "Send"
            setOnClickListener {
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) { input.setText(""); handle(text) }
            }
        }
        actions.addView(mic, LinearLayout.LayoutParams(0, 54, 1f))
        actions.addView(send, LinearLayout.LayoutParams(0, 54, 1f))
        root.addView(actions)
        setContentView(root)
    }

    private fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            say("Speech recognition isn’t available on this device yet. You can type to me for now.")
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_MIC)
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { addLine("Rozi", "Listening…", true) }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onError(error: Int) { addLine("Rozi", "I didn’t catch that. Please try again.", true) }
                override fun onResults(results: Bundle?) {
                    val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (words.isNullOrBlank()) say("I didn’t catch that. Please try again.") else handle(words)
                }
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locales[language.selectedItemPosition].toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            sr.startListening(intent)
        }
    }

    private fun handle(raw: String) {
        val text = raw.trim()
        addLine("You", text)
        if (waitingForRecipientChoice) {
            waitingForRecipientChoice = false
            val key = text.trim().lowercase(Locale.ROOT)
            val chosen = if (pendingMatches.isEmpty()) {
                findContacts(text).filter { it.first.equals(text, ignoreCase = true) || it.second == text.trim() }
            } else pendingMatches.filter { (name, number) ->
                name.lowercase(Locale.ROOT) == key || number.takeLast(4) == key || number == text.trim()
            }
            if (chosen.size == 1) sendText(chosen.first().first, pendingMessage.orEmpty(), chosen.first().second)
            else {
                pendingMatches = emptyList()
                pendingMessage = null
                say("I still can’t tell which contact you mean, so I haven’t sent anything. Please give the full name or phone number.")
            }
            return
        }

        parseSendCommand(text)?.let { (recipient, body) ->
            if (recipient.isBlank() || body.isBlank()) {
                say("Who should I send it to, and what should the message say?")
                return
            }
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                pendingPermissionCommand = text
                say("I need Contacts access to find the recipient. You can allow it in the Android prompt.")
                requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), REQ_CONTACTS)
                return
            }
            val matches = findContacts(recipient)
            when {
                matches.isEmpty() -> {
                    pendingMessage = body
                    pendingMatches = emptyList()
                    waitingForRecipientChoice = true
                    say("I couldn’t find $recipient. Please give me the contact’s full saved name or phone number. I won’t send anything until I resolve it.")
                }
                matches.size > 1 -> {
                    pendingMessage = body
                    pendingMatches = matches
                    say("I found ${matches.joinToString { (name, number) -> "$name ending ${number.takeLast(4)}" }}. Which one should I send it to?")
                    waitingForRecipientChoice = true
                }
                else -> sendText(matches.first().first, body, matches.first().second)
            }
            return
        }

        if (isNameChange(text)) {
            val newName = text.substringAfterLast(" to ", "").trim().trim('"', '\'')
            if (newName.isBlank() || newName.length > 32) say("What would you like to call me?")
            else {
                prefs.edit().putString("assistant_name", newName).apply()
                rebuildNameHeader(newName)
                say("Okay, you can call me $newName.")
            }
            return
        }
        if (text.contains("what time", true) || text.contains("time is it", true) || text.contains("समय", true) || text.contains("वेळ", true)) {
            say("It’s ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())}.")
            return
        }
        if (text.contains("set an alarm", true) || text.contains("अलार्म", true) || text.contains("गजर", true)) {
            val time = Regex("(\\d{1,2})[:.]([0-5]\\d)").find(text)
            if (time == null) say("What time should I set the alarm for? For example, 7:30.")
            else {
                val hour = time.groupValues[1].toIntOrNull() ?: 0
                val minute = time.groupValues[2].toIntOrNull() ?: 0
                startActivity(Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
                    putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, "Rozi alarm")
                })
            }
            return
        }
        when (language.selectedItemPosition) {
            1 -> say("अरे, ऐसा होना सच में परेशान कर सकता है। बताइए, क्या हुआ?")
            2 -> say("अरे, असं झालं की त्रास होऊ शकतो. काय झालं ते सांगाल?")
            else -> say(if (text.endsWith("?")) "That’s a good question. Tell me a little more so I can help." else "Oh, that sounds like a lot. What happened next?")
        }
    }

    private fun parseSendCommand(text: String): Pair<String, String>? {
        val s = text.trim().removePrefix("Rozi,").removePrefix("Rozi ").trim()
        val explicit = Regex("^(?:please )?send (?:the )?message to (.+?):\\s*(.+)$", RegexOption.IGNORE_CASE)
        val hindi = Regex("^(.+?)\\s+को\\s+(?:संदेश|मैसेज)\\s+भेज(?:ो|ना)?[: ]+(.+)$")
        val marathi = Regex("^(.+?)\\s+ला\\s+(?:संदेश|मेसेज)\\s+पाठव(?:ा|ायचा)?[: ]+(.+)$")
        val m = explicit.matchEntire(s) ?: hindi.matchEntire(s) ?: marathi.matchEntire(s) ?: return null
        return m.groupValues[1].trim() to m.groupValues[2].trim()
    }

    private fun isNameChange(text: String) = Regex("^(?:Rozi[, ]+)?change your name to .+", RegexOption.IGNORE_CASE).matches(text.trim())

    private fun findContacts(query: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER)
        val cursor = contentResolver.query(uri, projection, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?", arrayOf("%$query%"), null)
        cursor?.use {
            while (it.moveToNext() && result.size < 8) {
                val name = it.getString(0) ?: continue
                val number = it.getString(1) ?: continue
                if (result.none { item -> item.second == number }) result.add(name to number)
            }
        }
        return result
    }

    private fun sendText(contact: String, body: String, knownNumber: String? = null) {
        val number = knownNumber ?: findContacts(contact).firstOrNull()?.second
        if (number.isNullOrBlank()) {
            say("I couldn’t find a phone number for $contact. Please clarify the contact.")
            return
        }
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            pendingRecipient = contact
            pendingMessage = body
            pendingNumber = number
            say("Android needs to grant Rozi permission to send SMS. The message will send only if you allow that permission.")
            requestPermissions(arrayOf(Manifest.permission.SEND_SMS), REQ_SMS)
            return
        }
        try {
            SmsManager.getDefault().sendTextMessage(number, null, body, null, null)
            say("Sent to $contact: $body")
        } catch (_: Exception) {
            say("I couldn’t send that message. Please check your SMS service and try again.")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQ_MIC -> if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) listen() else say("No problem. You can type to me instead.")
            REQ_CONTACTS -> {
                val command = pendingPermissionCommand
                pendingPermissionCommand = null
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED && command != null) handle(command)
                else say("Okay, I won’t look through your contacts. No message was sent.")
            }
            REQ_SMS -> {
                val recipient = pendingRecipient
                val body = pendingMessage
                val number = pendingNumber
                pendingRecipient = null
                pendingMessage = null
                pendingNumber = null
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED && recipient != null && body != null && number != null) sendText(recipient, body, number)
                else say("Okay, no message was sent.")
            }
        }
    }

    private fun addLine(who: String, message: String, replaceLast: Boolean = false) {
        if (!::chat.isInitialized) return
        if (replaceLast && chat.childCount > 0) chat.removeViewAt(chat.childCount - 1)
        val row = TextView(this).apply {
            text = "$who  $message"
            textSize = 16f
            setTextColor(if (who == "You") 0xFF514A61.toInt() else 0xFF28223A.toInt())
            setPadding(14, 12, 14, 12)
            setBackgroundColor(if (who == "You") 0xFFEAE6F5.toInt() else 0xFFFFFFFF.toInt())
        }
        val lp = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 5, 0, 5) }
        chat.addView(row, lp)
    }

    private fun say(message: String) {
        addLine(assistantName(), message)
        val locale = locales.getOrElse(prefs.getInt("language", 0)) { Locale.US }
        tts?.language = locale
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "rozi-${System.nanoTime()}")
    }

    private fun assistantName() = prefs.getString("assistant_name", "Rozi") ?: "Rozi"
    private fun rebuildNameHeader(name: String) {
        // Name is local to this installation. Recreate visible labels without changing app identity.
        titleView.text = name
        if (::chat.isInitialized) addLine("System", "Display name updated to $name.")
    }

    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) updateTtsLanguage() }
    private fun updateTtsLanguage() {
        val locale = locales.getOrElse(prefs.getInt("language", 0)) { Locale.US }
        tts?.language = locale
    }

    override fun onDestroy() {
        recognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val REQ_MIC = 41
        private const val REQ_CONTACTS = 42
        private const val REQ_SMS = 43
    }
}

