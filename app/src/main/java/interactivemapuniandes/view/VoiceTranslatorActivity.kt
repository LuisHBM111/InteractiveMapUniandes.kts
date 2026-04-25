package com.uniandes.interactivemapuniandes.view

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
import com.uniandes.interactivemapuniandes.utils.setupNavigation
import kotlinx.coroutines.launch

// Standalone translator screen. User picks direction (es↔en), taps mic, sees
// the result, optionally sends it to Search.
class VoiceTranslatorActivity : AppCompatActivity() {

    private var sourceLang = "es-ES" // Default: speak Spanish
    private var targetLang = "en-US" // Default: get English

    private val recognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!text.isNullOrBlank()) handleHeard(text)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_translator)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        findViewById<BottomNavigationView>(R.id.bottomNav).setupNavigation(this, "explore")

        val toggle = findViewById<MaterialButtonToggleGroup>(R.id.langToggle)
        toggle.check(R.id.btnEsToEn) // Default selection
        toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            if (checkedId == R.id.btnEsToEn) {
                sourceLang = "es-ES"; targetLang = "en-US"
            } else {
                sourceLang = "en-US"; targetLang = "es-ES"
            }
        }

        findViewById<FloatingActionButton>(R.id.fabMic).setOnClickListener { startListening() }
        findViewById<MaterialButton>(R.id.btnUseInSearch).setOnClickListener {
            val translated = findViewById<TextView>(R.id.tvTranslated).text.toString()
            if (translated.isBlank() || translated == "—") return@setOnClickListener
            val intent = Intent(this, SearchActivity::class.java).apply {
                putExtra("prefill", translated)
            }
            startActivity(intent)
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, sourceLang)
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.translator_listening))
        }
        try {
            recognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech recognition missing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleHeard(text: String) {
        findViewById<TextView>(R.id.tvHeard).text = text
        val src = sourceLang.substringBefore('-') // "es-ES" -> "es"
        val tgt = targetLang.substringBefore('-')
        lifecycleScope.launch {
            try {
                val resp = RetrofitInstance.translateApi.translateText(text, tgt, src)
                val translated = if (resp.isSuccessful) resp.body()?.translated ?: text else text
                findViewById<TextView>(R.id.tvTranslated).text = translated
                findViewById<MaterialButton>(R.id.btnUseInSearch).isEnabled = true
            } catch (e: Exception) {
                Toast.makeText(this@VoiceTranslatorActivity, "Translate error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
