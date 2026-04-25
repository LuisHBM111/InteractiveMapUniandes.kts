package com.uniandes.interactivemapuniandes.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import coil3.load
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.model.data.Restaurant
import com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
import com.uniandes.interactivemapuniandes.utils.Telemetry
import com.uniandes.interactivemapuniandes.utils.setupNavigation
import kotlinx.coroutines.launch

class RestaurantsActivity : AppCompatActivity() {

    private lateinit var adapter: RestaurantAdapter
    private var sortByRating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Telemetry.screen("restaurants_list")
        enableEdgeToEdge()
        setContentView(R.layout.activity_restaurants)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        findViewById<BottomNavigationView>(R.id.bottomNav).setupNavigation(this, "explore")

        val rv = findViewById<RecyclerView>(R.id.rvRestaurants)
        adapter = RestaurantAdapter(
            onClick = { r -> openDetail(r) },
            onFavToggle = { r, fav -> toggleFavorite(r.id, fav) }
        )
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        loadFavorites()

        findViewById<MaterialButton>(R.id.btnSortRating).setOnClickListener {
            sortByRating = !sortByRating
            load()
        }

        listOf(R.id.chipCafe, R.id.chipComidaRapida, R.id.chipRestaurante, R.id.chipCheap, R.id.chipMid).forEach { id ->
            findViewById<Chip>(id).setOnCheckedChangeListener { _, _ -> load() } // Reload on any toggle
        }

        load()
    }

    private fun selectedFoodCategory(): String? {
        if (findViewById<Chip>(R.id.chipCafe).isChecked) return "Café"
        if (findViewById<Chip>(R.id.chipComidaRapida).isChecked) return "Comida rápida"
        if (findViewById<Chip>(R.id.chipRestaurante).isChecked) return "Restaurante"
        return null
    }

    private fun selectedMaxPrice(): Int? {
        if (findViewById<Chip>(R.id.chipCheap).isChecked) return 1 // $
        if (findViewById<Chip>(R.id.chipMid).isChecked) return 2 // $$
        return null
    }

    private fun load() {
        val empty = findViewById<TextView>(R.id.tvEmpty)
        lifecycleScope.launch {
            try {
                val sortBy = if (sortByRating) "rating" else null
                val resp = RetrofitInstance.restaurantsApi.list(
                    foodCategory = selectedFoodCategory(),
                    maxPrice = selectedMaxPrice(),
                    sortBy = sortBy
                )
                val list = if (resp.isSuccessful) resp.body().orEmpty() else emptyList()
                adapter.submit(list)
                empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(this@RestaurantsActivity, e.message ?: "Network error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadFavorites() {
        lifecycleScope.launch {
            runCatching {
                val resp = RetrofitInstance.favoritesApi.list()
                if (resp.isSuccessful) {
                    val ids = resp.body().orEmpty().mapNotNull { it.place?.id }
                    adapter.setFavorites(ids)
                }
            } // Silent if not signed in — toggle handler will show feedback
        }
    }

    private fun toggleFavorite(placeId: String, fav: Boolean) {
        lifecycleScope.launch {
            try {
                val resp = if (fav) RetrofitInstance.favoritesApi.add(placeId)
                           else RetrofitInstance.favoritesApi.remove(placeId)
                if (resp.code() == 401) {
                    Toast.makeText(this@RestaurantsActivity, "Inicia sesión para guardar favoritos", Toast.LENGTH_SHORT).show()
                } else {
                    Telemetry.event("favorite_toggle", mapOf("placeId" to placeId, "added" to fav))
                }
            } catch (_: Exception) { /* network error: leave UI optimistic */ }
        }
    }

    private fun openDetail(r: Restaurant) {
        val intent = Intent(this, RestaurantDetailActivity::class.java).apply {
            putExtra("id", r.id)
            putExtra("name", r.name)
            putExtra("category", r.foodCategory ?: "")
            putExtra("rating", r.averageRating ?: 0.0)
            putExtra("photoUrl", r.photoUrl ?: "")
        }
        startActivity(intent)
    }
}

class RestaurantAdapter(
    private val onClick: (Restaurant) -> Unit,
    private val onFavToggle: (Restaurant, Boolean) -> Unit
) : RecyclerView.Adapter<RestaurantAdapter.VH>() {
    private val items = mutableListOf<Restaurant>()
    private val favorites = mutableSetOf<String>()

    fun submit(rows: List<Restaurant>) {
        items.clear(); items.addAll(rows); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_service, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        holder.emoji.text = "🍽️"
        holder.name.text = r.name
        val rating = r.averageRating?.let { " · ⭐ %.1f".format(it) } ?: ""
        holder.details.text = (r.foodCategory ?: "Restaurant") + rating
        if (!r.photoUrl.isNullOrBlank()) {
            holder.thumb.visibility = View.VISIBLE
            holder.emoji.visibility = View.GONE
            holder.thumb.load(r.photoUrl) // Coil 3
        } else {
            holder.thumb.visibility = View.GONE
            holder.emoji.visibility = View.VISIBLE
        }
        holder.fav.text = if (favorites.contains(r.id)) "❤️" else "🤍"
        holder.fav.setOnClickListener {
            val newFav = !favorites.contains(r.id)
            if (newFav) favorites.add(r.id) else favorites.remove(r.id)
            holder.fav.text = if (newFav) "❤️" else "🤍"
            onFavToggle(r, newFav)
        }
        holder.itemView.setOnClickListener { onClick(r) }
    }

    fun setFavorites(ids: Collection<String>) {
        favorites.clear(); favorites.addAll(ids); notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val emoji: TextView = v.findViewById(R.id.tvEmoji)
        val thumb: android.widget.ImageView = v.findViewById(R.id.ivThumb)
        val name: TextView = v.findViewById(R.id.tvName)
        val details: TextView = v.findViewById(R.id.tvDetails)
        val fav: TextView = v.findViewById(R.id.btnFav)
    }
}
