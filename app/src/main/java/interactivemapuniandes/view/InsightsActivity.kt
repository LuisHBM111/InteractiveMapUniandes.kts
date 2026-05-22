package com.uniandes.interactivemapuniandes.view

import android.os.Bundle
import android.util.LruCache
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.utils.Telemetry
import com.uniandes.interactivemapuniandes.utils.setupNavigation
import interactivemapuniandes.model.data.AppDatabase
import interactivemapuniandes.model.data.HourCount
import interactivemapuniandes.model.data.PlaceCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Sprint 4 - Personal Insights
// Multi-threading: 3 Room aggregations corren en paralelo con async{} en Dispatchers.Default
// Local storage: Room (visits table)
// Caching: LruCache de 64 entradas con TTL en lectura (1h)
class InsightsActivity : AppCompatActivity() {

    private data class Snapshot(
        val topPlaces: List<PlaceCount>,
        val busiestHours: List<HourCount>,
        val totalDwellSec: Int,
        val computedAt: Long
    )

    private val cache = LruCache<String, Snapshot>(64)
    private val ttlMillis = 60 * 60 * 1000L // 1h

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Telemetry.screen("insights")
        enableEdgeToEdge()
        setContentView(R.layout.activity_insights)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        findViewById<BottomNavigationView>(R.id.bottomNav).setupNavigation(this, "explore")

        loadInsights()
    }

    private fun loadInsights() {
        val loading = findViewById<TextView>(R.id.tvLoading)
        val top = findViewById<TextView>(R.id.tvTopPlaces)
        val hour = findViewById<TextView>(R.id.tvBusiestHour)
        val total = findViewById<TextView>(R.id.tvTotalDwell)

        val sinceMillis = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000 // Ventana de 30 dias

        val cached = cache.get("month")
        if (cached != null && System.currentTimeMillis() - cached.computedAt < ttlMillis) {
            renderSnapshot(cached, top, hour, total)
            loading.visibility = View.GONE
            return
        }

        loading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(this@InsightsActivity).visitDao()
            // Fan-out: 3 queries Room corren en paralelo. awaitAll asegura que se rendere
            // solo cuando las 3 terminaron.
            val snapshot = withContext(Dispatchers.Default) {
                val a = async { dao.topPlaces(sinceMillis, limit = 5) }
                val b = async { dao.visitsByHour(sinceMillis) }
                val c = async { dao.totalDwellSecondsSince(sinceMillis) }
                val results = awaitAll(a, b, c)
                @Suppress("UNCHECKED_CAST")
                Snapshot(
                    topPlaces = results[0] as List<PlaceCount>,
                    busiestHours = results[1] as List<HourCount>,
                    totalDwellSec = results[2] as Int,
                    computedAt = System.currentTimeMillis()
                )
            }
            cache.put("month", snapshot)
            renderSnapshot(snapshot, top, hour, total)
            loading.visibility = View.GONE
        }
    }

    private fun renderSnapshot(s: Snapshot, top: TextView, hour: TextView, total: TextView) {
        if (s.topPlaces.isEmpty()) {
            top.text = "Sin visitas registradas todavia"
        } else {
            top.text = s.topPlaces.joinToString("\n") { "${it.placeCode}  -  ${it.total} visitas" }
        }
        val busy = s.busiestHours.firstOrNull()
        hour.text = if (busy == null) "-" else "${busy.hour.toString().padStart(2, '0')}:00 (${busy.total} visitas)"
        val totalMin = s.totalDwellSec / 60
        total.text = "$totalMin min en edificios"
    }
}
