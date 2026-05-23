package com.uniandes.interactivemapuniandes.view

import android.os.Bundle
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.utils.NetworkMonitor
import com.uniandes.interactivemapuniandes.utils.Telemetry
import com.uniandes.interactivemapuniandes.utils.setupNavigation
import interactivemapuniandes.model.data.AppDatabase
import interactivemapuniandes.model.entity.NoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Sprint 4 - Personal notes per place
// Local storage: Room (notes table)
// Caching: in-memory LruCache keyed by placeCode
// Eventual connectivity: pendingSync flag, flushed when network is back
class NotesActivity : AppCompatActivity() {

    private lateinit var adapter: NoteAdapter
    private val cache = LruCache<String, List<NoteEntity>>(32) // L1 cache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Telemetry.screen("notes")
        enableEdgeToEdge()
        setContentView(R.layout.activity_notes)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        findViewById<BottomNavigationView>(R.id.bottomNav).setupNavigation(this, "explore")

        val rv = findViewById<RecyclerView>(R.id.rvNotes)
        adapter = NoteAdapter(onDelete = { n -> deleteNote(n) })
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<Button>(R.id.btnAddNote).setOnClickListener { addNote() }

        observeNotes()
        trySyncPending()
    }

    private fun observeNotes() {
        val empty = findViewById<TextView>(R.id.tvEmpty)
        val dao = AppDatabase.getInstance(this).noteDao()
        lifecycleScope.launch {
            dao.observeAll().collectLatest { list ->
                cache.put("__all__", list)
                adapter.submit(list)
                empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                updatePendingBadge(list.count { it.pendingSync })
            }
        }
    }

    private fun addNote() {
        val placeInput = findViewById<EditText>(R.id.etPlace)
        val textInput = findViewById<EditText>(R.id.etText)
        val code = placeInput.text.toString().trim().uppercase(Locale.ROOT)
        val text = textInput.text.toString().trim()
        if (code.isEmpty() || text.isEmpty()) {
            Toast.makeText(this, "Falta el codigo o el texto", Toast.LENGTH_SHORT).show()
            return
        }
        val offline = !NetworkMonitor.isOnline(this)
        val note = NoteEntity(
            placeCode = code,
            text = text,
            createdAt = System.currentTimeMillis(),
            pendingSync = offline // Marca para sincronizar despues si estamos offline
        )
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.getInstance(this@NotesActivity).noteDao().upsert(note)
            }
            placeInput.setText("")
            textInput.setText("")
            cache.evictAll() // Invalida cache al escribir
            if (offline) {
                Toast.makeText(this@NotesActivity, "Sin internet - se sincroniza despues", Toast.LENGTH_SHORT).show()
            } else {
                trySyncPending()
            }
        }
    }

    private fun deleteNote(n: NoteEntity) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.getInstance(this@NotesActivity).noteDao().delete(n.id)
            }
            cache.evictAll()
        }
    }

    private fun trySyncPending() {
        // Eventual connectivity: empuja las notas pendientes cuando hay red.
        // Hoy solo marcamos como sincronizadas localmente porque el endpoint /notes
        // todavia no existe en el backend (queda para el siguiente sprint).
        if (!NetworkMonitor.isOnline(this)) return
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(this@NotesActivity).noteDao()
            val pending = withContext(Dispatchers.IO) { dao.pendingSyncBatch() }
            if (pending.isEmpty()) return@launch
            withContext(Dispatchers.IO) {
                pending.forEach { dao.markSynced(it.id) }
            }
        }
    }

    private fun updatePendingBadge(count: Int) {
        val badge = findViewById<TextView>(R.id.tvPendingBadge)
        if (count == 0) {
            badge.visibility = View.GONE
        } else {
            badge.text = "$count sin sincronizar"
            badge.visibility = View.VISIBLE
        }
    }
}

private val DATE_FMT = SimpleDateFormat("d MMM HH:mm", Locale.getDefault())

class NoteAdapter(
    private val onDelete: (NoteEntity) -> Unit
) : RecyclerView.Adapter<NoteAdapter.VH>() {

    private val items = mutableListOf<NoteEntity>()

    fun submit(rows: List<NoteEntity>) {
        items.clear(); items.addAll(rows); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val n = items[position]
        holder.place.text = n.placeCode
        holder.text.text = n.text
        holder.whenText.text = DATE_FMT.format(Date(n.createdAt))
        holder.pending.visibility = if (n.pendingSync) View.VISIBLE else View.GONE
        holder.itemView.setOnLongClickListener { onDelete(n); true }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val place: TextView = v.findViewById(R.id.tvNotePlace)
        val text: TextView = v.findViewById(R.id.tvNoteText)
        val whenText: TextView = v.findViewById(R.id.tvNoteWhen)
        val pending: TextView = v.findViewById(R.id.tvNotePending)
    }
}
