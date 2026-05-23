package com.uniandes.interactivemapuniandes.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.utils.Telemetry
import com.uniandes.interactivemapuniandes.utils.setupNavigation
import interactivemapuniandes.model.data.AppDatabase
import interactivemapuniandes.model.entity.TranslationEntity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Sprint 4 - Historial de traducciones. Espejo del HistoryView de iOS.
// Cada traduccion del VoiceTranslator se guarda en Room (tabla translations)
// y se muestra aqui en orden cronologico inverso.
class TranslatorHistoryActivity : AppCompatActivity() {

    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Telemetry.screen("translator_history")
        enableEdgeToEdge()
        setContentView(R.layout.activity_translator_history)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        findViewById<BottomNavigationView>(R.id.bottomNav).setupNavigation(this, "explore")

        val rv = findViewById<RecyclerView>(R.id.rvTranslations)
        adapter = HistoryAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        observeTranslations()
    }

    private fun observeTranslations() {
        val empty = findViewById<TextView>(R.id.tvEmpty)
        val dao = AppDatabase.getInstance(this).translationDao()
        lifecycleScope.launch {
            dao.observeAll().collectLatest { list ->
                adapter.submit(list)
                empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}

private val DATE_FMT = SimpleDateFormat("d MMM HH:mm", Locale.getDefault())

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.VH>() {
    private val items = mutableListOf<TranslationEntity>()

    fun submit(rows: List<TranslationEntity>) {
        items.clear(); items.addAll(rows); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_translation, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = items[position]
        holder.original.text = t.original
        holder.translated.text = t.translated
        holder.whenText.text = DATE_FMT.format(Date(t.createdAt))
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val original: TextView = v.findViewById(R.id.tvOriginal)
        val translated: TextView = v.findViewById(R.id.tvTranslated)
        val whenText: TextView = v.findViewById(R.id.tvWhen)
    }
}
