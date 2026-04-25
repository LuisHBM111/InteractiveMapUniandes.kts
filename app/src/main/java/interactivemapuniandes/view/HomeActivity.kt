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
import com.uniandes.interactivemapuniandes.BuildConfig
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

    // Voice flow moved to VoiceTranslatorActivity — kept this empty hook in case
    // someone re-attaches inline mic later.

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
        handleDeepLink() // interactivemap://route?from=&to=
        handleAutoFetchRoute() // Triggered by SchedulesActivity
        loadNextClass() // Fills the next-class card if user is signed in
        loadNearbyServices() // Populates the bottom-sheet list from /restaurants
        setupServiceChips() // Cafes / Libraries / Study Spaces chips
        requestLocationIfNeeded() // Ask up-front so routing isn't blocked later
        showOfflineBannerIfNeeded() // Red banner when no internet
    }

    private fun showOfflineBannerIfNeeded() {
        val online = com.uniandes.interactivemapuniandes.utils.NetworkMonitor.isOnline(this)
        findViewById<android.widget.TextView>(R.id.tvOfflineBanner).visibility =
            if (online) View.GONE else View.VISIBLE
    }

    private fun requestLocationIfNeeded() {
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
        }
    }

    private fun setupServiceChips() { // Each chip just opens Search with a pre-term
        findViewById<View>(R.id.btnCafes).setOnClickListener { openSearchWith("café") }
        findViewById<View>(R.id.btnLibraries).setOnClickListener { openSearchWith("biblioteca") }
        findViewById<View>(R.id.btnStudySpaces).setOnClickListener { openSearchWith("sala") }
    }

    private fun openSearchWith(term: String) {
        val intent = Intent(this, SearchActivity::class.java).apply {
            putExtra("prefill", term)
        }
        startActivity(intent)
    }

    private fun loadNearbyServices() {
        val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvServices)
        val empty = findViewById<android.widget.TextView>(R.id.tvServicesEmpty)
        rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        val adapter = ServicesAdapter { item -> routeFromCurrentLocationTo(item.target) } // Tap a row → route there
        rv.adapter = adapter

        lifecycleScope.launch {
            try {
                // Try restaurants first; fall back to buildings so the section is never empty on campus.
                val rows = mutableListOf<ServiceItem>()
                val restResp = RetrofitInstance.restaurantsApi.list()
                if (restResp.isSuccessful) {
                    restResp.body()?.forEach { r ->
                        rows.add(ServiceItem(
                            name = r.name,
                            subtitle = r.foodCategory ?: "Restaurant",
                            emoji = "🍽️",
                            target = r.name // No routeTarget on restaurants yet, name is best
                        ))
                    }
                }
                if (rows.isEmpty()) { // No restaurants seeded, show buildings
                    val bResp = RetrofitInstance.placesApi.listBuildings(null)
                    if (bResp.isSuccessful) {
                        bResp.body()?.take(10)?.forEach { b ->
                            rows.add(ServiceItem(
                                name = b.name,
                                subtitle = b.gridReference?.let { "Grid $it" } ?: "Edificio",
                                emoji = buildingEmoji(b.code),
                                target = b.code
                            ))
                        }
                    }
                }
                adapter.submit(rows)
                empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
                rv.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
            } catch (e: Exception) {
                rv.visibility = View.GONE
                empty.text = "Couldn't load services"
                empty.visibility = View.VISIBLE
            }
        }
    }

    private fun buildingEmoji(code: String): String = when (code.uppercase()) {
        "ML" -> "🏛️"
        "W" -> "🏢"
        "N" -> "☕"
        "O" -> "📚"
        "RGD", "CENTRO CIVICO" -> "🏛️"
        "X" -> "🍽️"
        else -> "📍"
    }

    private fun handleDeepLink() {
        val data = intent.data ?: return // Only set when launched via URI
        val from = data.getQueryParameter("from")
        val to = data.getQueryParameter("to")
        if (to.isNullOrBlank()) return
        if (!from.isNullOrBlank()) {
            fetchRouteFromBackend(from, to)
        } else {
            routeFromCurrentLocationTo(to) // QR may just specify destination
        }
    }

    private fun handleAutoFetchRoute() {
        if (!intent.getBooleanExtra("autoFetchRoute", false)) return
        val to = intent.getStringExtra("routeTo") ?: return
        val from = intent.getStringExtra("routeFrom")
        if (from.isNullOrBlank()) {
            routeFromCurrentLocationTo(to) // Resolve origin via GPS
        } else {
            fetchRouteFromBackend(from, to)
        }
    }

    private fun loadNextClass() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return // Card only for logged in
        lifecycleScope.launch {
            try {
                val resp = RetrofitInstance.meApi.getNextClass()
                if (!resp.isSuccessful) return@launch
                val body = resp.body() ?: return@launch
                val klass = body.scheduledClass ?: return@launch
                if (!body.hasUpcomingClass) return@launch

                val card = findViewById<androidx.cardview.widget.CardView>(R.id.cvNextClass)
                findViewById<android.widget.TextView>(R.id.tvNextClassTitle).text = klass.title
                findViewById<android.widget.TextView>(R.id.tvNextClassWhen).text = formatWhen(klass.startsAt)
                card.visibility = android.view.View.VISIBLE

                findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRouteToNext)
                    .setOnClickListener {
                        val target = klass.destination?.routeTarget
                            ?: klass.room?.roomCode
                            ?: klass.rawLocation
                        if (!target.isNullOrBlank()) {
                            routeFromCurrentLocationTo(target) // Real user origin
                        } else {
                            Toast.makeText(this@HomeActivity, "No location", Toast.LENGTH_SHORT).show()
                        }
                    }
            } catch (_: Exception) {
                // Silent — card stays hidden, user can still use map
            }
        }
    }

    private fun formatWhen(iso: String): String { // Short "at HH:mm" format
        return try {
            val inFmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.getDefault())
            val outFmt = java.text.SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault())
            val date = inFmt.parse(iso) ?: return iso
            "at ${outFmt.format(date)}"
        } catch (e: Exception) {
            iso
        }
    }

    private fun setupVoiceAssistant() { // Mic icon now opens the dedicated translator screen
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.forLanguageTag(BuildConfig.TRANSLATE_TARGET_LANG)
            }
        }
        findViewById<ImageView>(R.id.ivMic).setOnClickListener {
            startActivity(Intent(this, VoiceTranslatorActivity::class.java))
        }
    }

    // Voice translation now lives in VoiceTranslatorActivity.

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
                enableMyLocationLayerIfAllowed() // Turn on blue dot now we have perm
                val retryDest = pendingRouteDestination
                pendingRouteDestination = null
                if (retryDest != null) {
                    routeFromCurrentLocationTo(retryDest) // Continue the flow that triggered the request
                } else {
                    showCurrentLocation()
                }
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

        mMap.uiSettings.isMapToolbarEnabled = false
        mMap.uiSettings.isCompassEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.uiSettings.isMyLocationButtonEnabled = true // Native blue dot button
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL

        enableMyLocationLayerIfAllowed() // Shows blue dot on user location
        drawRouteIfNeeded()
    }

    private fun enableMyLocationLayerIfAllowed() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                mMap.isMyLocationEnabled = true // Google Maps draws the blue dot
            } catch (_: SecurityException) {
                // Perm revoked mid-session, ignore
            }
        }
    }

    private fun centerOnCurrentLocation() {
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
        val client = LocationServices.getFusedLocationProviderClient(this)
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null && ::mMap.isInitialized) {
                val here = LatLng(loc.latitude, loc.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(here, 18f))
            } else {
                Toast.makeText(this, "Location unavailable", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fetch nearest campus node from the backend, then route to the destination.
    // Uses getCurrentLocation so we always get a fresh reading (lastLocation can
    // be null when the app hasn't asked for location recently).
    private var pendingRouteDestination: String? = null // If we need to wait for permission

    private fun routeFromCurrentLocationTo(destination: String) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingRouteDestination = destination // Retry once user grants
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionRequest
            )
            return
        }

        Toast.makeText(this, "Getting your location…", Toast.LENGTH_SHORT).show()
        val client = LocationServices.getFusedLocationProviderClient(this)
        val tokenSource = com.google.android.gms.tasks.CancellationTokenSource()
        client.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            tokenSource.token
        ).addOnSuccessListener { loc ->
            if (loc == null) {
                Toast.makeText(this, "Location unavailable — using campus center", Toast.LENGTH_SHORT).show()
                fetchRouteFromBackend("ML 2", destination) // Last-resort fallback
                return@addOnSuccessListener
            }
            resolveNearestNodeAndRoute(loc.latitude, loc.longitude, destination)
        }.addOnFailureListener {
            Toast.makeText(this, "Could not get location", Toast.LENGTH_SHORT).show()
            fetchRouteFromBackend("ML 2", destination)
        }
    }

    private fun resolveNearestNodeAndRoute(lat: Double, lng: Double, destination: String) {
        lifecycleScope.launch {
            try {
                val resp = RetrofitInstance.api.findNearest(lat, lng)
                val from = if (resp.isSuccessful) resp.body()?.node?.label ?: "ML 2" else "ML 2"
                fetchRouteFromBackend(from, destination)
            } catch (_: Exception) {
                fetchRouteFromBackend("ML 2", destination)
            }
        }
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
            startActivity(Intent(this, SearchActivity::class.java)) // Real screen now
        }

        findViewById<View>(R.id.fabDirections).setOnClickListener {
            Toast.makeText(this, "Tap a building on search to route", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SearchActivity::class.java)) // Directions flow starts with picking a destination
        }

        findViewById<View>(R.id.fabLocation).setOnClickListener {
            centerOnCurrentLocation() // Jumps to user's real position
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
            startActivity(Intent(this, SearchActivity::class.java)) // Jump to full search
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
                        com.uniandes.interactivemapuniandes.model.repository.RouteHistoryStore(this@HomeActivity)
                            .record(route.from, route.to) // Track for "5 most common"
                        val labels = route.path.map { it.label }
                        val lats = route.path.map { it.latitude ?: 0.0 } // Real coords from BE
                        val lngs = route.path.map { it.longitude ?: 0.0 }
                        val intent = Intent(this@HomeActivity, RouteActivity::class.java).apply {
                            putExtra("destination", route.to)
                            putExtra("from", route.from)
                            putExtra("eta", "${route.totalTimeSeconds} s")
                            putExtra("distance", "${(labels.size - 1).coerceAtLeast(0)} hops")
                            putStringArrayListExtra("path", ArrayList(labels))
                            putExtra("total_time", route.totalTimeSeconds)
                            putExtra("pathLats", lats.toDoubleArray()) // Carry coords through
                            putExtra("pathLngs", lngs.toDoubleArray())
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

        val lats = intent.getDoubleArrayExtra("pathLats")
        val lngs = intent.getDoubleArrayExtra("pathLngs")
        val labels = intent.getStringArrayListExtra("path") ?: return

        val latLngPath: List<LatLng> = if (lats != null && lngs != null && lats.size == lngs.size && lats.isNotEmpty()) {
            lats.indices.map { LatLng(lats[it], lngs[it]) } // Use real backend coords
        } else {
            emptyList() // Old-style intent without coords, skip polyline
        }

        if (latLngPath.isEmpty()) {
            Toast.makeText(this, "No hay coordenadas para esa ruta", Toast.LENGTH_SHORT).show()
            return
        }

        mMap.clear()
        mMap.addMarker(MarkerOptions().position(latLngPath.first()).title("Inicio: ${labels.firstOrNull() ?: ""}"))
        mMap.addMarker(MarkerOptions().position(latLngPath.last()).title("Destino: ${labels.lastOrNull() ?: ""}"))

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
