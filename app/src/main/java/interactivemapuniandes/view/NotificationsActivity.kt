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
import com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
import com.uniandes.interactivemapuniandes.utils.setupNavigation
import kotlinx.coroutines.launch

data class AppNotification(val icon: String, val title: String, val body: String, val whenText: String)

class NotificationsActivity : AppCompatActivity() {

    private lateinit var adapter: NotifAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        findViewById<BottomNavigationView>(R.id.bottomNav).setupNavigation(this, "alerts")

        val rv = findViewById<RecyclerView>(R.id.rvNotifications)
        adapter = NotifAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        loadFromBackend()
    }

    private fun loadFromBackend() {
        val empty = findViewById<TextView>(R.id.tvEmpty)
        lifecycleScope.launch {
            try {
                val resp = RetrofitInstance.notificationsApi.list()
                val items = if (resp.isSuccessful) {
                    resp.body().orEmpty().map { dto ->
                        AppNotification(
                            icon = dto.icon ?: "🔔",
                            title = dto.title,
                            body = dto.body ?: "",
                            whenText = relativeTime(dto.createdAt)
                        )
                    }
                } else emptyList()
                adapter.submit(items)
                empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                empty.text = "Couldn't load notifications"
                empty.visibility = View.VISIBLE
            }
        }
    }

    private fun relativeTime(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return iso.substringAfter('T').substringBefore('.') // HH:mm:ss is enough
    }
}

class NotifAdapter : RecyclerView.Adapter<NotifAdapter.VH>() {
    private val items = mutableListOf<AppNotification>()

    fun submit(rows: List<AppNotification>) {
        items.clear(); items.addAll(rows); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val n = items[position]
        holder.icon.text = n.icon
        holder.title.text = n.title
        holder.body.text = n.body
        holder.whenText.text = n.whenText
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: TextView = v.findViewById(R.id.tvIcon)
        val title: TextView = v.findViewById(R.id.tvTitle)
        val body: TextView = v.findViewById(R.id.tvBody)
        val whenText: TextView = v.findViewById(R.id.tvWhen)
    }
}
