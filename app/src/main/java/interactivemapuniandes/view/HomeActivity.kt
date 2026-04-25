package com.uniandes.interactivemapuniandes.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.model.data.RouteResponse
import com.uniandes.interactivemapuniandes.model.repository.AuthRepository
import com.uniandes.interactivemapuniandes.model.repository.RouteRepository
import com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
import com.uniandes.interactivemapuniandes.utils.setupNavigation
import com.uniandes.interactivemapuniandes.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var scanner: GmsBarcodeScanner
    private val locationPermissionRequest = 1001
    private lateinit var mMap: GoogleMap
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var btnDirections: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        val authRepository = AuthRepository(FirebaseAuth.getInstance())
        val routeRepository = RouteRepository(RetrofitInstance.api, authRepository)
        homeViewModel = HomeViewModel(routeRepository)
        observeUiState()

        setupMap()
        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.setupNavigation(this, "explore")
        setupBottomSheet()
        setupPrototypeClicks()
        handleRouteFromIntent()
        setupFilterChips()
        setupQrScanner()
    }

    private fun setupQrScanner() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        scanner = GmsBarcodeScanning.getClient(this, options)
    }

    private fun setupFilterChips() {
        findViewById<View>(R.id.chipQrCode).setOnClickListener {
            scanQrCode()
        }

        findViewById<View>(R.id.chipCurrentLocation).setOnClickListener {
            requestAndShowCurrentLocation()
        }
    }

    private fun scanQrCode() {
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue

                if (rawValue.isNullOrBlank()) {
                    Toast.makeText(this, "QR vacio o invalido", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                handleQrRoute(rawValue)
            }
            .addOnCanceledListener {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al escanear: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleQrRoute(qrContent: String) {
        try {
            val uri = Uri.parse(qrContent)
            val fromNode = uri.getQueryParameter("from")
            val classId = uri.getQueryParameter("classId")
            val toNode = uri.getQueryParameter("to")

            when {
                fromNode.isNullOrBlank() -> {
                    Toast.makeText(this, "El QR no tiene un nodo de origen valido", Toast.LENGTH_SHORT).show()
                }
                !classId.isNullOrBlank() -> {
                    requestRouteToClass(classId, fromNode)
                }
                !toNode.isNullOrBlank() -> {
                    Toast.makeText(
                        this,
                        "Este QR usa el formato viejo. Falta classId para el backend nuevo.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    requestRouteToNextClass(fromNode)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "QR invalido: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestAndShowCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionRequest
            )
            return
        }

        showCurrentLocation()
    }

    private fun showCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null && ::mMap.isInitialized) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)

                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f)
                    )

                    mMap.addMarker(
                        MarkerOptions()
                            .position(currentLatLng)
                            .title("Your current location")
                    )

                    val chipText = findViewById<TextView>(R.id.tvCurrentLocationChip)
                    chipText.text =
                        "(${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)})"
                } else {
                    Toast.makeText(this, "Could not get current location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error getting current location", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == locationPermissionRequest) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val uniandes = LatLng(4.6019, -74.0661)

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(uniandes, 17f))

        mMap.addMarker(
            MarkerOptions()
                .position(uniandes)
                .title("Universidad de los Andes")
        )

        mMap.uiSettings.isMapToolbarEnabled = false
        mMap.uiSettings.isCompassEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL

        drawRouteIfNeeded()
    }

    private fun setupBottomSheet() {
    }

    private fun setupPrototypeClicks() {
        findViewById<View>(R.id.cvSearch).setOnClickListener {
            Toast.makeText(this, "Search prototype", Toast.LENGTH_SHORT).show()
        }

        btnDirections = findViewById(R.id.fabDirections)
        btnDirections.setOnClickListener {
            openNavigationScreen("ML")
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

        findViewById<View>(R.id.profileContainer).setOnClickListener {
            logout()
        }
    }

    private fun requestRouteToNextClass(fromNode: String) {
        lifecycleScope.launch {
            homeViewModel.loadRouteToNextClass(fromNode)
        }
    }

    private fun requestRouteToClass(classId: String, fromNode: String) {
        lifecycleScope.launch {
            homeViewModel.loadRouteToClass(classId, fromNode)
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            homeViewModel.uiState.collect { state ->
                if (::btnDirections.isInitialized) {
                    btnDirections.isEnabled = !state.isRouteLoading
                }

                state.route?.let { route ->
                    openRouteDetails(route)
                    homeViewModel.clearRoute()
                }

                state.routeError?.let { error ->
                    Toast.makeText(this@HomeActivity, error, Toast.LENGTH_LONG).show()
                    homeViewModel.clearRouteError()
                }
            }
        }
    }

    private fun openRouteDetails(route: RouteResponse) {
        val intent = Intent(this, RouteActivity::class.java).apply {
            putExtra("destination", route.to)
            putExtra("from", route.from)
            putExtra("eta", route.totalTime.toEtaLabel())
            putExtra("distance", "${(route.path.size - 1).coerceAtLeast(0)} hops")
            putStringArrayListExtra("path", ArrayList(route.path))
            putExtra("total_time", route.totalTime)
        }
        startActivity(intent)
    }

    private fun openNavigationScreen(defaultFrom: String) {
        val intent = Intent(this, RouteActivity::class.java).apply {
            putExtra("from", defaultFrom)
        }
        startActivity(intent)
    }

    private fun handleRouteFromIntent() {
        val showRoute = intent.getBooleanExtra("showRoute", false)
        if (!showRoute) return

        if (::mMap.isInitialized) {
            drawRouteIfNeeded()
        }
    }

    private fun drawRouteIfNeeded() {
        val showRoute = intent.getBooleanExtra("showRoute", false)
        if (!showRoute || !::mMap.isInitialized) return

        val path = intent.getStringArrayListExtra("path") ?: return

        val nodeCoordinates = mapOf(
            "ML 2" to LatLng(4.6019, -74.0661),
            "ML 3" to LatLng(4.6020, -74.0659),
            "ML 4" to LatLng(4.6021, -74.0657),
            "ML 5" to LatLng(4.6022, -74.0655),
            "W puente" to LatLng(4.6023, -74.0652),
            "W 5" to LatLng(4.6024, -74.0650),
            "W 4" to LatLng(4.6025, -74.0648),
            "W 3" to LatLng(4.6026, -74.0646)
        )

        val latLngPath = path.mapNotNull { nodeCoordinates[it] }

        if (latLngPath.isEmpty()) {
            Toast.makeText(this, "No hay coordenadas para esa ruta", Toast.LENGTH_SHORT).show()
            return
        }

        mMap.clear()
        mMap.addMarker(MarkerOptions().position(latLngPath.first()).title("Inicio"))
        mMap.addMarker(MarkerOptions().position(latLngPath.last()).title("Destino"))

        val polyline = PolylineOptions()
            .addAll(latLngPath)
            .width(12f)
            .color(Color.parseColor("#FAD400"))

        mMap.addPolyline(polyline)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngPath.first(), 17f))
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun Int.toEtaLabel(): String {
        return if (this >= 60) {
            val minutes = this / 60
            "$minutes min"
        } else {
            "$this s"
        }
    }
}
