package com.uniandes.interactivemapuniandes.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
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
import com.uniandes.interactivemapuniandes.model.data.CreateReviewBody
import com.uniandes.interactivemapuniandes.model.data.Review
import com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
import kotlinx.coroutines.launch

class RestaurantDetailActivity : AppCompatActivity() {

    private lateinit var adapter: ReviewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_restaurant_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val id = intent.getStringExtra("id") ?: return
        findViewById<TextView>(R.id.tvName).text = intent.getStringExtra("name") ?: "—"
        findViewById<TextView>(R.id.tvCategory).text = intent.getStringExtra("category") ?: ""
        val rating = intent.getDoubleExtra("rating", 0.0)
        findViewById<TextView>(R.id.tvRating).text = if (rating > 0) "⭐ %.1f".format(rating) else "No ratings yet"
        intent.getStringExtra("photoUrl")?.takeIf { it.isNotBlank() }?.let { url ->
            findViewById<ImageView>(R.id.ivPhoto).load(url) // Coil 3 view extension
        }

        val rv = findViewById<RecyclerView>(R.id.rvReviews)
        adapter = ReviewsAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        loadReviews(id)

        findViewById<MaterialButton>(R.id.btnSubmit).setOnClickListener {
            val starsRaw = findViewById<RatingBar>(R.id.rbInput).rating.toInt().coerceIn(1, 5)
            val comment = findViewById<EditText>(R.id.etComment).text.toString().trim().ifBlank { null }
            submitReview(id, starsRaw, comment)
        }
    }

    private fun loadReviews(id: String) {
        val empty = findViewById<TextView>(R.id.tvNoReviews)
        lifecycleScope.launch {
            try {
                val resp = RetrofitInstance.restaurantsApi.listReviews(id)
                val list = if (resp.isSuccessful) resp.body().orEmpty() else emptyList()
                adapter.submit(list)
                empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            } catch (_: Exception) { /* ignore */ }
        }
    }

    private fun submitReview(id: String, stars: Int, comment: String?) {
        lifecycleScope.launch {
            try {
                val resp = RetrofitInstance.restaurantsApi.createReview(id, CreateReviewBody(stars, comment))
                if (resp.isSuccessful) {
                    Toast.makeText(this@RestaurantDetailActivity, "Thanks!", Toast.LENGTH_SHORT).show()
                    findViewById<EditText>(R.id.etComment).setText("")
                    loadReviews(id)
                } else if (resp.code() == 401 || resp.code() == 503) {
                    Toast.makeText(this@RestaurantDetailActivity, "Sign in to leave reviews", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@RestaurantDetailActivity, "Failed: ${resp.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RestaurantDetailActivity, e.message ?: "Network error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class ReviewsAdapter : RecyclerView.Adapter<ReviewsAdapter.VH>() {
    private val items = mutableListOf<Review>()

    fun submit(rows: List<Review>) {
        items.clear(); items.addAll(rows); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        holder.stars.text = "★".repeat(r.rating) + "☆".repeat(5 - r.rating)
        holder.comment.text = r.comment ?: "(No comment)"
        holder.who.text = r.user?.email ?: ""
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val stars: TextView = v.findViewById(R.id.tvStars)
        val comment: TextView = v.findViewById(R.id.tvComment)
        val who: TextView = v.findViewById(R.id.tvWho)
    }
}
