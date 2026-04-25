package com.uniandes.interactivemapuniandes.view

import android.content.Intent
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
import com.uniandes.interactivemapuniandes.model.data.FavoriteDto
import com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
import com.uniandes.interactivemapuniandes.utils.Telemetry
import com.uniandes.interactivemapuniandes.utils.setupNavigation
import kotlinx.coroutines.launch

class FavoritesActivity : AppCompatActivity() {

    private lateinit var adapter: FavAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Telemetry.screen("favorites")
        enableEdgeToEdge()
        setContentView(R.layout.activity_favorites)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        findViewById<BottomNavigationView>(R.id.bottomNav).setupNavigation(this, "explore")

        val rv = findViewById<RecyclerView>(R.id.rvFavorites)
        adapter = FavAdapter(
            onClick = { fav -> // Tap = route there from current location
                val target = fav.place?.code ?: return@FavAdapter
                val intent = Intent(this, HomeActivity::class.java).apply {
                    putExtra("autoFetchRoute", true)
                    putExtra("routeTo", target)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
            },
            onRemove = { fav -> removeFavorite(fav) }
        )
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        loadFromBackend()
    }

    private fun loadFromBackend() {
        val empty = findViewById<TextView>(R.id.tvEmpty)
        lifecycleScope.launch {
            try {
                val resp = RetrofitInstance.favoritesApi.list()
                val list = if (resp.isSuccessful) resp.body().orEmpty() else emptyList()
                adapter.submit(list)
                empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                empty.text = "Couldn't load favorites"
                empty.visibility = View.VISIBLE
            }
        }
    }

    private fun removeFavorite(fav: FavoriteDto) {
        val placeId = fav.place?.id ?: return
        lifecycleScope.launch {
            try {
                RetrofitInstance.favoritesApi.remove(placeId)
                loadFromBackend()
            } catch (_: Exception) { /* ignore */ }
        }
    }
}

class FavAdapter(
    private val onClick: (FavoriteDto) -> Unit,
    private val onRemove: (FavoriteDto) -> Unit,
) : RecyclerView.Adapter<FavAdapter.VH>() {
    private val items = mutableListOf<FavoriteDto>()

    fun submit(rows: List<FavoriteDto>) {
        items.clear(); items.addAll(rows); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_place, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val f = items[position]
        holder.code.text = f.place?.code ?: "?"
        holder.name.text = f.place?.name ?: "—"
        holder.sub.text = "❤️ tap to route · long-press to remove"
        holder.itemView.setOnClickListener { onClick(f) }
        holder.itemView.setOnLongClickListener { onRemove(f); true }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val code: TextView = v.findViewById(R.id.tvCode)
        val name: TextView = v.findViewById(R.id.tvName)
        val sub: TextView = v.findViewById(R.id.tvSubtitle)
    }
}
