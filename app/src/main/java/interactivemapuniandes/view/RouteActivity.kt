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
        val eta = intent.getStringExtra("eta") ?: "--"
        val path = intent.getStringArrayListExtra("path") ?: arrayListOf()
        val totalTime = intent.getIntExtra("total_time", 0)

        tvDestinationTitle.text = "Route to $destination"
        tvFromLocation.text = "From $from"
        tvEta.text = eta

        // Dato corto para la card derecha
        val stepsCount = if (path.isNotEmpty()) path.size - 1 else 0
        tvDistance.text = "$stepsCount steps"

        // Ruta completa, multilínea
        tvRouteSteps.text = if (path.isNotEmpty()) {
            path.joinToString(separator = "\n↓\n")
        } else {
            "No route available"
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
            }
            startActivity(intent)
            finish()
        }

        btnCalendar.text = "Open schedule"
        btnCalendar.setOnClickListener {
            Toast.makeText(this, "Open schedule (prototype)", Toast.LENGTH_SHORT).show()
        }
    }
}
