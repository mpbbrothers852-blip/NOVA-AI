package com.nova.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.*
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var output: TextView
    private lateinit var input: EditText
    private lateinit var tts: TextToSpeech
    private var pendingAction: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildUi()
    }

    private fun buildUi() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 40, 28, 24)
            setBackgroundColor(0xFF07111F.toInt())
        }
        val title = TextView(this).apply {
            text = "N O V A"; textSize = 30f; gravity = Gravity.CENTER
            setTextColor(0xFF66C7FF.toInt())
        }
        val subtitle = TextView(this).apply {
            text = "PERSONAL AI  •  USER-CONTROLLED MODE"
            textSize = 11f; gravity = Gravity.CENTER
            setTextColor(0xFF9CB6CC.toInt())
        }
        output = TextView(this).apply {
            text = "Systems ready. Give NOVA a command."
            textSize = 16f; setTextColor(0xFFEAF5FF.toInt())
            setPadding(8, 36, 8, 36)
        }
        input = EditText(this).apply {
            hint = "Type a command..."; setHintTextColor(0xFF8096AA.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }
        val send = Button(this).apply {
            text = "SEND COMMAND"
            setOnClickListener { handleCommand(input.text.toString().trim()) }
        }
        val voice = Button(this).apply {
            text = "TALK TO NOVA"
            setOnClickListener { startVoiceInput() }
        }
        layout.addView(title); layout.addView(subtitle)
        layout.addView(output, LinearLayout.LayoutParams(-1, 0, 1f))
        layout.addView(input); layout.addView(send); layout.addView(voice)
        setContentView(layout)
    }

    private fun handleCommand(command: String) {
        if (command.isBlank()) return
        input.setText("")
        val lower = command.lowercase(Locale.ROOT)
        when {
            lower.startsWith("open ") -> proposeOpenApp(command.substringAfter("open ").trim())
            lower.startsWith("search the web for ") -> {
                val query = command.substringAfter("search the web for ").trim()
                propose("Search the web for: $query") {
                    startActivity(Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", query))
                }
            }
            lower == "open settings" || lower == "open phone settings" ->
                propose("Open Android Settings") {
                    startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                }
            lower == "open wi-fi settings" || lower == "open wifi settings" ->
                propose("Open Wi-Fi settings") {
                    startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS))
                }
            else -> askCloudAi(command)
        }
    }

    private fun proposeOpenApp(name: String) {
        val packages = mapOf(
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "gmail" to "com.google.android.gm",
            "instagram" to "com.instagram.android",
            "facebook" to "com.facebook.katana",
            "maps" to "com.google.android.apps.maps",
            "play store" to "com.android.vending"
        )
        val pkg = packages[name.lowercase(Locale.ROOT)]
        if (pkg == null) {
            say("I don't have a launch shortcut for $name yet.")
            return
        }
        propose("Open $name") {
            val launch = packageManager.getLaunchIntentForPackage(pkg)
            if (launch != null) startActivity(launch)
            else say("$name is not installed or Android did not make it available.")
        }
    }

    private fun propose(description: String, action: () -> Unit) {
        pendingAction = action
        output.text = "I can: $description\n\nDo you want me to do this?"
        val parent = output.parent as LinearLayout
        val old = parent.findViewWithTag<LinearLayout>("confirmation_controls")
        if (old != null) parent.removeView(old)
        val controls = LinearLayout(this).apply { tag = "confirmation_controls" }
        controls.addView(Button(this).apply {
            text = "CONFIRM"
            setOnClickListener {
                val task = pendingAction
                pendingAction = null
                task?.invoke()
                output.text = "Command executed: $description"
                say("Done.")
                parent.removeView(controls)
            }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        controls.addView(Button(this).apply {
            text = "CANCEL"
            setOnClickListener {
                pendingAction = null
                output.text = "Cancelled. Waiting for your next command."
                parent.removeView(controls)
            }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        parent.addView(controls)
    }

    private fun askCloudAi(command: String) {
        output.text = "NOVA is thinking..."
        scope.launch {
            try {
                val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel("gemini-3.8-flash")
                val prompt = "You are NOVA, a concise Android personal assistant. " +
                    "You can explain and plan tasks but cannot claim to have performed phone actions. " +
                    "Phone actions require supported commands and user confirmation. " +
                    "For cybersecurity, help only with defensive work and official account recovery. " +
                    "Never request passwords, one-time codes or recovery codes. User command: $command"
                val response = model.generateContent(prompt)
                val answer = response.text ?: "I couldn't produce an answer."
                output.text = answer
                say(answer)
            } catch (e: Exception) {
                output.text = "Cloud AI connection failed. Check Firebase setup and internet. " +
                    (e.message ?: "Unknown error")
            }
        }
    }

    private fun startVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to NOVA")
        }
        try { startActivityForResult(intent, 102) }
        catch (_: Exception) { say("Voice recognition is unavailable on this device.") }
    }

    @Deprecated("Legacy activity result API for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 102 && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) handleCommand(spoken)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale.US
    }

    private fun say(text: String) {
        if (::tts.isInitialized) tts.speak(text.take(3000), TextToSpeech.QUEUE_FLUSH, null, "nova_reply")
    }

    override fun onDestroy() {
        scope.cancel()
        if (::tts.isInitialized) tts.shutdown()
        super.onDestroy()
    }
}
