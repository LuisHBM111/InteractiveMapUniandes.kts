package com.uniandes.interactivemapuniandes.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.uniandes.interactivemapuniandes.R

class HomeActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        setupMap()
        setupBottomNav()
        setupBottomSheet()
        setupPrototypeClicks()
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Uniandes Bogotá (aproximado)
        val uniandes = LatLng(4.6019, -74.0661)

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(uniandes, 17f))

        mMap.addMarker(
            MarkerOptions()
                .position(uniandes)
                .title("Universidad de los Andes")
        )

        // Configuración básica del mapa
        mMap.uiSettings.isMapToolbarEnabled = false
        mMap.uiSettings.isCompassEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.uiSettings.isMyLocationButtonEnabled = false

        // Tipo de mapa inicial
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
    }

    private fun setupBottomSheet() {
        val bottomSheet = findViewById<androidx.core.widget.NestedScrollView>(R.id.bottomSheet)
        bottomSheet?.let {
            bottomSheetBehavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(it)

            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels

            // Configuración para que el límite sea la mitad de la pantalla
            bottomSheetBehavior.maxHeight = screenHeight / 2
            bottomSheetBehavior.isFitToContents = true

            // Estado inicial contraído
            bottomSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
            bottomSheetBehavior.peekHeight = 180.dpToPx()

            // Evitar que se oculte del todo al deslizar hacia abajo
            bottomSheetBehavior.isHideable = false
        }
    }
    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_explore

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_explore -> true
                R.id.nav_schedules -> {
                    Toast.makeText(this, "Schedules screen prototype", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_alerts -> {
                    Toast.makeText(this, "Alerts screen prototype", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Settings screen prototype", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupPrototypeClicks() {
        findViewById<View>(R.id.cvSearch).setOnClickListener {
            Toast.makeText(this, "Search prototype", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.fabDirections).setOnClickListener {
            Toast.makeText(this, "Directions prototype", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.fabLocation).setOnClickListener {
            if (::mMap.isInitialized) {
                val uniandes = LatLng(4.6019, -74.0661)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(uniandes, 18f))
            }
        }

        findViewById<View>(R.id.fabLayers).setOnClickListener {
            if (::mMap.isInitialized) {
                mMap.mapType = when (mMap.mapType) {
                    GoogleMap.MAP_TYPE_NORMAL -> GoogleMap.MAP_TYPE_SATELLITE
                    GoogleMap.MAP_TYPE_SATELLITE -> GoogleMap.MAP_TYPE_TERRAIN
                    else -> GoogleMap.MAP_TYPE_NORMAL
                }
            }
        }

        findViewById<View>(R.id.tvSeeAll).setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}