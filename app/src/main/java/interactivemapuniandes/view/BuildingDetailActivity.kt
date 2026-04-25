package com.uniandes.interactivemapuniandes.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.google.android.material.button.MaterialButton
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.model.data.Room
import com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
import com.uniandes.interactivemapuniandes.utils.Telemetry
import kotlinx.coroutines.launch

class BuildingDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Telemetry.screen("building_detail")
        enableEdgeToEdge()
        setContentView(R.layout.activity_building_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val id = intent.getStringExtra("id") ?: return finish()
        loadBuilding(id)
        loadRooms(id)
    }

    private fun loadBuilding(id: String) {
        lifecycleScope.launch {
            try {
                val resp = RetrofitInstance.placesApi.getBuilding(id)
                if (!resp.isSuccessful) {
                    Toast.makeText(this@BuildingDetailActivity, "No se pudo cargar el edificio", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val b = resp.body() ?: return@launch
                findViewById<TextView>(R.id.tvCode).text = b.code
                findViewById<TextView>(R.id.tvName).text = b.name
                findViewById<TextView>(R.id.tvSubtitle).text = b.gridReference?.let { "Grid $it" } ?: ""
                findViewById<TextView>(R.id.tvDescription).text = b.description ?: ""
                if (!b.photoUrl.isNullOrBlank()) {
                    findViewById<ImageView>(R.id.ivPhoto).load(b.photoUrl)
                }
                findViewById<MaterialButton>(R.id.btnRoute).setOnClickListener {
                    Telemetry.event("building_route_tap", mapOf("buildingCode" to b.code))
                    val intent = Intent(this@BuildingDetailActivity, HomeActivity::class.java).apply {
                        putExtra("autoFetchRoute", true)
                        putExtra("routeTo", b.code)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(intent)
                }
                wireFavorite(id)
            } catch (e: Exception) {
                Toast.makeText(this@BuildingDetailActivity, e.message ?: "Network error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadRooms(buildingId: String) {
        val rv = findViewById<RecyclerView>(R.id.rvRooms)
        val empty = findViewById<TextView>(R.id.tvNoRooms)
        rv.layoutManager = LinearLayoutManager(this)
        val adapter = RoomAdapter()
        rv.adapter = adapter
        lifecycleScope.launch {
            try {
                val resp = RetrofitInstance.placesApi.listRoomsInBuilding(buildingId)
                val list = if (resp.isSuccessful) resp.body().orEmpty() else emptyList()
                adapter.submit(list)
                empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            } catch (_: Exception) { /* silent — empty state stays */ }
        }
    }

    private fun wireFavorite(placeId: String) {
        val btn = findViewById<TextView>(R.id.btnFav)
        var isFav = false
        lifecycleScope.launch {
            runCatching {
                val resp = RetrofitInstance.favoritesApi.list()
                if (resp.isSuccessful) {
                    isFav = resp.body().orEmpty().any { it.place?.id == placeId }
                    btn.text = if (isFav) "❤️" else "🤍"
                }
            }
        }
        btn.setOnClickListener {
            val newFav = !isFav
            isFav = newFav
            btn.text = if (newFav) "❤️" else "🤍"
            lifecycleScope.launch {
                try {
                    val resp = if (newFav) RetrofitInstance.favoritesApi.add(placeId)
                               else RetrofitInstance.favoritesApi.remove(placeId)
                    if (resp.code() == 401) {
                        Toast.makeText(this@BuildingDetailActivity, "Inicia sesión para guardar favoritos", Toast.LENGTH_SHORT).show()
                    } else {
                        Telemetry.event("favorite_toggle", mapOf("placeId" to placeId, "added" to newFav))
                    }
                } catch (_: Exception) { /* keep optimistic UI */ }
            }
        }
    }
}

private class RoomAdapter : RecyclerView.Adapter<RoomAdapter.VH>() {
    private val items = mutableListOf<Room>()

    fun submit(rows: List<Room>) {
        items.clear(); items.addAll(rows); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        holder.title.text = r.name ?: r.roomCode
        holder.subtitle.text = r.floor?.let { "Piso $it" } ?: r.roomCode
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(android.R.id.text1)
        val subtitle: TextView = v.findViewById(android.R.id.text2)
    }
}
