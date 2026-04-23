package com.uniandes.interactivemapuniandes.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
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
import com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
import com.uniandes.interactivemapuniandes.utils.setupNavigation
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var scanner: GmsBarcodeScanner
    private val locationPermissionRequest = 1001
    private lateinit var mMap: GoogleMap
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    private lateinit var textToSpeech: TextToSpeech

    private val speechRecognizerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) { // Check if recognition succeeded
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) // Extract spoken text
            val text = spokenText?.firstOrNull() // Get the first match
            if (!text.isNullOrBlank()) {
                translateAndSpeak(text) // Translate and play the text
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        setupMap()
        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.setupNavigation(this, "explore")
        setupBottomSheet()
        setupPrototypeClicks()
        handleRouteFromIntent()
        setupFilterChips()
        setupQrScanner()
        setupVoiceAssistant()
    }

    private fun setupVoiceAssistant() { // Initialize voice assistant features
        textToSpeech = TextToSpeech(this) { status -> // Set up TTS engine
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale("es", "ES") // Default TTS language
            }
        }

        findViewById<ImageView>(R.id.ivMic).setOnClickListener { // Listen for mic clicks
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { // Prepare speech intent
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US") // Default listening language.
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...") // User prompt
            }
            try {
                speechRecognizerLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Speech recognition missing", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun translateAndSpeak(text: String) { // Handle translation process
        lifecycleScope.launch { // Run on a coroutine
            try {
                Toast.makeText(this@HomeActivity, "Translating: $text", Toast.LENGTH_SHORT).show() // Notify user
                val response = RetrofitInstance.translateApi.translateText(text, "es") // Call translation API
                if (response.isSuccessful) {
                    val translated = response.body()?.translated // Extract result
                    if (translated != null) {
                        textToSpeech.speak(translated, TextToSpeech.QUEUE_FLUSH, null, null) // Speak translation
                        Toast.makeText(this@HomeActivity, translated, Toast.LENGTH_LONG).show() // Display result
                    }
                } else {
                    Toast.makeText(this@HomeActivity, "Translation error: ${response.code()}", Toast.LENGTH_SHORT).show() // Handle API error
                }
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() { // Clean up resources
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop() // Stop TTS
            textToSpeech.shutdown() // Release TTS resources
        }
        super.onDestroy()
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
            val toNode = uri.getQueryParameter("to")

            if (fromNode.isNullOrBlank() || toNode.isNullOrBlank()) {
                Toast.makeText(this, "El QR no tiene from/to validos", Toast.LENGTH_SHORT).show()
                return
            }

            fetchRouteFromBackend(fromNode, toNode)
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
        val bottomSheet = findViewById<NestedScrollView>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels

        bottomSheetBehavior.maxHeight = screenHeight / 2
        bottomSheetBehavior.isFitToContents = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.peekHeight = 180.dpToPx()
        bottomSheetBehavior.isHideable = false
    }

    private fun setupPrototypeClicks() {
        findViewById<View>(R.id.cvSearch).setOnClickListener {
            Toast.makeText(this, "Search prototype", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.fabDirections).setOnClickListener {
            fetchRouteFromBackend("ML 2", "W 3")
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

        findViewById<View>(R.id.profileContainer).setOnClickListener {
            logout()
        }
    }

    private fun fetchRouteFromBackend(fromNode: String, toNode: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getRoute(fromNode, toNode)

                if (response.isSuccessful) {
                    val route = response.body()

                    if (route != null) {
                        val intent = Intent(this@HomeActivity, RouteActivity::class.java).apply {
                            putExtra("destination", route.to)
                            putExtra("from", route.from)
                            putExtra("eta", "${route.total_time} s")
                            putExtra("distance", "${route.path.size - 1} hops")
                            putStringArrayListExtra("path", ArrayList(route.path))
                            putExtra("total_time", route.total_time)
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@HomeActivity, "Respuesta vacia", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(
                        this@HomeActivity,
                        "Error del backend: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@HomeActivity,
                    "Error de red: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
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
}
