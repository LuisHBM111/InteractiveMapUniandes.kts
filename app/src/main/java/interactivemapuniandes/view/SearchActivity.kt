package com.uniandes.interactivemapuniandes.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
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
import com.uniandes.interactivemapuniandes.model.data.Building
import com.uniandes.interactivemapuniandes.model.data.Room
import com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
import com.uniandes.interactivemapuniandes.utils.setupNavigation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    private lateinit var adapter: PlacesAdapter
    private var searchJob: Job? = null // Debounced search

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_search)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.setupNavigation(this, "alerts") // "alerts" slot hosts Search for now

        val rv = findViewById<RecyclerView>(R.id.rvResults)
        adapter = PlacesAdapter { target -> // Tap a row to route from current location
            val intent = Intent(this, HomeActivity::class.java).apply {
                putExtra("autoFetchRoute", true)
                putExtra("routeTo", target)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val search = findViewById<EditText>(R.id.etSearch)
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                queueSearch(s?.toString().orEmpty())
            }
        })

        intent.getStringExtra("prefill")?.let { // Prefill from Home chips
            search.setText(it)
            search.setSelection(it.length)
        }

        search.requestFocus() // Auto-focus so keyboard opens right away
        search.post {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(search, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun queueSearch(term: String) {
        searchJob?.cancel() // Debounce so we don't spam the BE
        val progress = findViewById<ProgressBar>(R.id.progress)
        val empty = findViewById<TextView>(R.id.tvEmpty)

        if (term.isBlank()) {
            adapter.submit(emptyList())
            progress.visibility = View.GONE
            empty.text = "Type to search the campus"
            empty.visibility = View.VISIBLE
            return
        }

        searchJob = lifecycleScope.launch {
            delay(150) // Short debounce so it feels instant
            progress.visibility = View.VISIBLE
            empty.visibility = View.GONE
            try {
                val buildingsResp = RetrofitInstance.placesApi.listBuildings(term)
                val roomsResp = RetrofitInstance.placesApi.listRooms(term)
                val rows = mutableListOf<PlaceRow>()
                if (buildingsResp.isSuccessful) {
                    buildingsResp.body()?.forEach { rows.add(PlaceRow.BuildingRow(it)) }
                }
                if (roomsResp.isSuccessful) {
                    roomsResp.body()?.forEach { rows.add(PlaceRow.RoomRow(it)) }
                }
                adapter.submit(rows)
                if (rows.isEmpty()) {
                    empty.text = "No results"
                    empty.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                empty.text = "Error: ${e.message}"
                empty.visibility = View.VISIBLE
            } finally {
                progress.visibility = View.GONE
            }
        }
    }
}

sealed class PlaceRow {
    data class BuildingRow(val b: Building) : PlaceRow()
    data class RoomRow(val r: Room) : PlaceRow()
}

class PlacesAdapter(
    private val onClick: (String) -> Unit // Receives the route target (building code or room code)
) : RecyclerView.Adapter<PlacesAdapter.VH>() {
    private val items = mutableListOf<PlaceRow>()

    fun submit(rows: List<PlaceRow>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_place, parent, false)
        return VH(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val target: String
        var placeId: String? = null // For favorite toggle
        when (val row = items[position]) {
            is PlaceRow.BuildingRow -> {
                holder.code.text = row.b.code
                holder.name.text = row.b.name
                holder.sub.text = row.b.gridReference?.let { "Grid $it" } ?: "Edificio"
                target = row.b.code
                placeId = row.b.id
            }
            is PlaceRow.RoomRow -> {
                holder.code.text = row.r.roomCode
                holder.name.text = row.r.name ?: row.r.roomCode
                holder.sub.text = row.r.building?.name ?: "Salón"
                target = row.r.roomCode
                placeId = row.r.building?.id // Save the building, not the room
            }
        }
        holder.itemView.setOnClickListener { onClick(target) }
        holder.fav.setOnClickListener {
            val pid = placeId ?: return@setOnClickListener
            kotlinx.coroutines.GlobalScope.launch {
                runCatching {
                    com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
                        .favoritesApi.add(pid)
                }
                holder.fav.post { holder.fav.text = "❤️" }
            }
        }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val code: TextView = v.findViewById(R.id.tvCode)
        val name: TextView = v.findViewById(R.id.tvName)
        val sub: TextView = v.findViewById(R.id.tvSubtitle)
        val fav: TextView = v.findViewById(R.id.btnFav)
    }
}
