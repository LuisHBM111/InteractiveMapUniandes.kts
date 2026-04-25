package com.uniandes.interactivemapuniandes.view

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.uniandes.interactivemapuniandes.R

class RouteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_route)

        val tvDestinationTitle = findViewById<TextView>(R.id.tvDestinationTitle)
        val tvFromLocation = findViewById<TextView>(R.id.tvFromLocation)
        val tvEta = findViewById<TextView>(R.id.tvEta)
        val tvDistance = findViewById<TextView>(R.id.tvDistance)
        val tvRouteSteps = findViewById<TextView>(R.id.tvRouteSteps)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnStartNavigation = findViewById<MaterialButton>(R.id.btnStartNavigation)
        val btnCalendar = findViewById<MaterialButton>(R.id.btnCalendar)

        val destination = intent.getStringExtra("destination") ?: "Unknown"
        val from = intent.getStringExtra("from") ?: "Current location"
        val path = intent.getStringArrayListExtra("path") ?: arrayListOf()
        val totalTime = intent.getIntExtra("total_time", 0)
        val lats = intent.getDoubleArrayExtra("pathLats")
        val lngs = intent.getDoubleArrayExtra("pathLngs")

        tvDestinationTitle.text = "Ruta a $destination"
        tvFromLocation.text = "Desde $from"
        tvEta.text = formatEta(totalTime)
        tvDistance.text = formatDistance(lats, lngs, path.size)

        // Ruta completa, multilínea
        tvRouteSteps.text = if (path.isNotEmpty()) {
            path.joinToString(separator = "\n↓\n")
        } else {
            "No hay ruta disponible"
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnStartNavigation.text = "View on map"
        btnStartNavigation.setOnClickListener {
            if (path.isEmpty()) {
                Toast.makeText(this, "No route available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, HomeActivity::class.java).apply {
                putExtra("showRoute", true)
                putStringArrayListExtra("path", path)
                putExtra("from", from)
                putExtra("to", destination)
                putExtra("total_time", totalTime)
                getIntent().getDoubleArrayExtra("pathLats")?.let { putExtra("pathLats", it) } // Pass coords through
                getIntent().getDoubleArrayExtra("pathLngs")?.let { putExtra("pathLngs", it) }
            }
            startActivity(intent)
            finish()
        }

        btnCalendar.text = "Abrir horario"
        btnCalendar.setOnClickListener {
            startActivity(Intent(this, SchedulesActivity::class.java))
        }
    }

    private fun formatEta(seconds: Int): String {
        if (seconds <= 0) return "--"
        val m = seconds / 60
        val s = seconds % 60
        return when {
            m == 0 -> "${s}s"
            s == 0 -> "${m} min"
            else -> "${m} min ${s}s"
        }
    }

    private fun formatDistance(lats: DoubleArray?, lngs: DoubleArray?, fallbackHops: Int): String {
        if (lats == null || lngs == null || lats.size < 2 || lats.size != lngs.size) {
            val hops = (fallbackHops - 1).coerceAtLeast(0)
            return "$hops tramos"
        }
        var meters = 0.0
        for (i in 1 until lats.size) {
            meters += haversineMeters(lats[i - 1], lngs[i - 1], lats[i], lngs[i])
        }
        return if (meters >= 1000) "%.1f km".format(meters / 1000.0) else "${meters.toInt()} m"
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2).let { it * it }
        return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}
