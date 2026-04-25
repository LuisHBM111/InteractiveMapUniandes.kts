package com.uniandes.interactivemapuniandes.view

import android.os.Bundle
import android.view.View
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

// Pulls live alerts from /api/v1/alerts. Same card row layout as notifications.
class AlertsActivity : AppCompatActivity() {

    private lateinit var adapter: NotifAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_alerts)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        findViewById<BottomNavigationView>(R.id.bottomNav).setupNavigation(this, "alerts")

        val rv = findViewById<RecyclerView>(R.id.rvAlerts)
        adapter = NotifAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        loadFromBackend()
    }

    private fun loadFromBackend() {
        val empty = findViewById<TextView>(R.id.tvEmpty)
        lifecycleScope.launch {
            try {
                val resp = RetrofitInstance.alertsApi.list()
                val rows = if (resp.isSuccessful) resp.body().orEmpty().map { dto ->
                    AppNotification(
                        icon = dto.icon ?: "⚠️",
                        title = dto.title,
                        body = dto.body ?: "",
                        whenText = dto.createdAt?.substringAfter('T')?.substringBefore('.') ?: ""
                    )
                } else emptyList()
                adapter.submit(rows)
                empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                empty.text = "Couldn't load alerts"
                empty.visibility = View.VISIBLE
            }
        }
    }
}
