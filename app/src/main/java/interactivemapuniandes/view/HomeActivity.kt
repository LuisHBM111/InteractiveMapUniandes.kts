package com.uniandes.interactivemapuniandes.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.uniandes.interactivemapuniandes.BuildConfig
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.model.data.RouteResponse
import com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
import com.uniandes.interactivemapuniandes.model.repository.AuthRepository
import com.uniandes.interactivemapuniandes.model.repository.CachedRoute
import com.uniandes.interactivemapuniandes.model.repository.OfflineRouteCache
import com.uniandes.interactivemapuniandes.model.repository.RouteHistoryStore
import com.uniandes.interactivemapuniandes.model.repository.RouteRepository
import com.uniandes.interactivemapuniandes.utils.NetworkMonitor
import com.uniandes.interactivemapuniandes.utils.NextClassNotifier
import com.uniandes.interactivemapuniandes.utils.Telemetry
import com.uniandes.interactivemapuniandes.utils.setupNavigation
import com.uniandes.interactivemapuniandes.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var scanner: GmsBarcodeScanner
    private lateinit var mMap: GoogleMap
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var routeRepository: RouteRepository
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var btnDirections: View

    private val locationPermissionRequest = 1001
    private var pendingRouteDestination: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Telemetry.screen("home_map")
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        val authRepository = AuthRepository(FirebaseAuth.getInstance())
        routeRepository = RouteRepository(RetrofitInstance.api, authRepository)
        homeViewModel = HomeViewModel(routeRepository)
        observeUiState()

        setupMap()
        findViewById<BottomNavigationView>(R.id.bottomNav).setupNavigation(this, "explore")
        setupBottomSheet()
        setupPrototypeClicks()
        handleRouteFromIntent()
        setupFilterChips()
        setupQrScanner()
        setupVoiceAssistant()
        handleDeepLink()
        handleAutoFetchRoute()
        loadNextClass()
        loadNearbyServices()
        setupServiceChips()
        requestLocationIfNeeded()
        showOfflineBannerIfNeeded()
        renderRecents()
        renderOfflineRouteCard()
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

    private fun renderRecents() {
        val rows = RouteHistoryStore(this).topFive()
        val card = findViewById<androidx.cardview.widget.CardView>(R.id.cvRecents)
        val container = findViewById<android.widget.LinearLayout>(R.id.recentsContainer)
        container.removeAllViews()

        if (rows.isEmpty()) {
            card.visibility = View.GONE
            return
        }

        card.visibility = View.VISIBLE
        rows.forEach { route ->
            val row = TextView(this).apply {
                text = "${route.from} -> ${route.to}  (${route.count}x)"
                textSize = 14f
                setPadding(0, 8, 0, 8)
                setTextColor(Color.parseColor("#101828"))
                setOnClickListener {
                    Telemetry.event("recents_tap", mapOf("from" to route.from, "to" to route.to))
                    fetchRouteFromBackend(route.from, route.to)
                }
            }
            container.addView(row)
        }
    }

    private fun renderOfflineRouteCard() {
        val cached = OfflineRouteCache(this).load() ?: return
        val card = findViewById<androidx.cardview.widget.CardView>(R.id.cvOfflineRoute)
        val tv = findViewById<TextView>(R.id.tvOfflineRouteSummary)
        tv.text = "${cached.from} -> ${cached.to}"
        card.visibility = View.VISIBLE

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRestoreRoute)
            .setOnClickListener {
                Telemetry.event("offline_route_restore")
                val intent = Intent(this, RouteActivity::class.java).apply {
                    putExtra("destination", cached.to)
                    putExtra("from", cached.from)
                    putStringArrayListExtra("path", ArrayList(cached.labels))
                    putExtra("total_time", cached.totalTimeSeconds)
                    putExtra("pathLats", cached.lats)
                    putExtra("pathLngs", cached.lngs)
                }
                startActivity(intent)
            }
    }

    private fun showOfflineBannerIfNeeded() {
        findViewById<TextView>(R.id.tvOfflineBanner).visibility =
            if (NetworkMonitor.isOnline(this)) View.GONE else View.VISIBLE
    }

    private fun setupServiceChips() {
        findViewById<View>(R.id.btnCafes).setOnClickListener { openSearchWith("cafe") }
        findViewById<View>(R.id.btnLibraries).setOnClickListener { openSearchWith("biblioteca") }
        findViewById<View>(R.id.btnStudySpaces).setOnClickListener { openSearchWith("sala") }
    }

    private fun openSearchWith(term: String) {
        startActivity(
            Intent(this, SearchActivity::class.java).apply {
                putExtra("prefill", term)
            }
        )
    }

    private fun loadNearbyServices() {
        val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvServices)
        val empty = findViewById<TextView>(R.id.tvServicesEmpty)
        rv.layoutManager = LinearLayoutManager(this)
        val adapter = ServicesAdapter { item -> routeFromCurrentLocationTo(item.target) }
        rv.adapter = adapter

        lifecycleScope.launch {
            try {
                val rows = mutableListOf<ServiceItem>()
                val restaurantResponse = RetrofitInstance.restaurantsApi.list()
                if (restaurantResponse.isSuccessful) {
                    restaurantResponse.body()?.forEach { restaurant ->
                        rows.add(
                            ServiceItem(
                                name = restaurant.name,
                                subtitle = restaurant.foodCategory ?: "Restaurant",
                                emoji = "R",
                                target = restaurant.name,
                                photoUrl = restaurant.photoUrl
                            )
                        )
                    }
                }

                if (rows.isEmpty()) {
                    val buildingResponse = RetrofitInstance.placesApi.listBuildings(null)
                    if (buildingResponse.isSuccessful) {
                        buildingResponse.body()?.take(10)?.forEach { building ->
                            rows.add(
                                ServiceItem(
                                    name = building.name,
                                    subtitle = building.gridReference?.let { "Grid $it" } ?: "Building",
                                    emoji = buildingEmoji(building.code),
                                    target = building.code,
                                    photoUrl = building.photoUrl
                                )
                            )
                        }
                    }
                }

                adapter.submit(rows)
                empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
                rv.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
            } catch (_: Exception) {
                rv.visibility = View.GONE
                empty.text = "Couldn't load services"
                empty.visibility = View.VISIBLE
            }
        }
    }

    private fun buildingEmoji(code: String): String = when (code.uppercase()) {
        "ML" -> "ML"
        "RGD" -> "RGD"
        "W" -> "W"
        else -> "B"
    }

    private fun handleDeepLink() {
        val data = intent.data ?: return
        val from = data.getQueryParameter("from")
        val to = data.getQueryParameter("to")
        if (to.isNullOrBlank()) return

        if (from.isNullOrBlank()) {
            routeFromCurrentLocationTo(to)
        } else {
            fetchRouteFromBackend(from, to)
        }
    }

    private fun handleAutoFetchRoute() {
        if (!intent.getBooleanExtra("autoFetchRoute", false)) return
        val to = intent.getStringExtra("routeTo") ?: return
        val from = intent.getStringExtra("routeFrom")
        if (from.isNullOrBlank()) {
            routeFromCurrentLocationTo(to)
        } else {
            fetchRouteFromBackend(from, to)
        }
    }

    private fun loadNextClass() {
        if (FirebaseAuth.getInstance().currentUser == null) return

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.meApi.getNextClass()
                if (!response.isSuccessful) return@launch
                val body = response.body() ?: return@launch
                val nextClass = body.scheduledClass ?: return@launch
                if (!body.hasUpcomingClass) return@launch

                val card = findViewById<androidx.cardview.widget.CardView>(R.id.cvNextClass)
                findViewById<TextView>(R.id.tvNextClassTitle).text = nextClass.title
                findViewById<TextView>(R.id.tvNextClassWhen).text = formatWhen(nextClass.startsAt)
                card.visibility = View.VISIBLE

                findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRouteToNext)
                    .setOnClickListener {
                        val target = nextClass.destination?.routeTarget
                            ?: nextClass.room?.roomCode
                            ?: nextClass.rawLocation

                        if (target.isNullOrBlank()) {
                            Toast.makeText(this@HomeActivity, "No location", Toast.LENGTH_SHORT).show()
                        } else {
                            Telemetry.event(
                                "route_to_next_class",
                                mapOf("classId" to nextClass.id, "target" to target)
                            )
                            routeFromCurrentLocationTo(target)
                        }
                    }

                NextClassNotifier.scheduleFor(this@HomeActivity, nextClass)
            } catch (_: Exception) {
                // The card stays hidden when the next class cannot be loaded.
            }
        }
    }

    private fun formatWhen(iso: String): String {
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.getDefault())
            val output = SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault())
            val date = input.parse(iso) ?: return iso
            output.format(date)
        } catch (_: Exception) {
            iso
        }
    }

    private fun setupVoiceAssistant() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.forLanguageTag(BuildConfig.TRANSLATE_TARGET_LANG)
            }
        }

        findViewById<ImageView>(R.id.ivMic).setOnClickListener {
            startActivity(Intent(this, VoiceTranslatorActivity::class.java))
        }
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
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
            .addOnFailureListener { error ->
                Toast.makeText(this, "Error al escanear: ${error.message}", Toast.LENGTH_SHORT).show()
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
                    fetchRouteFromBackend(fromNode, toNode)
                }
                else -> {
                    requestRouteToNextClass(fromNode)
                }
            }
        } catch (error: Exception) {
            Toast.makeText(this, "QR invalido: ${error.message}", Toast.LENGTH_SHORT).show()
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

    private fun requestLocationIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionRequest
            )
        }
    }

    private fun requestAndShowCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null && ::mMap.isInitialized) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f))
                    mMap.addMarker(MarkerOptions().position(currentLatLng).title("Your current location"))

                    findViewById<TextView>(R.id.tvCurrentLocationChip).text =
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

        if (requestCode != locationPermissionRequest) return

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocationLayerIfAllowed()
            val retryDestination = pendingRouteDestination
            pendingRouteDestination = null
            if (retryDestination != null) {
                routeFromCurrentLocationTo(retryDestination)
            } else {
                showCurrentLocation()
            }
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
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
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL

        enableMyLocationLayerIfAllowed()
        drawRouteIfNeeded()
        loadAlertMarkers()
    }

    private fun loadAlertMarkers() {
        if (!::mMap.isInitialized) return

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.alertsApi.list()
                if (!response.isSuccessful) return@launch
                response.body().orEmpty().forEach { alert ->
                    val lat = alert.place?.latitude
                    val lng = alert.place?.longitude
                    if (lat != null && lng != null) {
                        mMap.addMarker(
                            MarkerOptions()
                                .position(LatLng(lat, lng))
                                .title(alert.title)
                                .snippet(alert.body ?: alert.type)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        )
                    }
                }
            } catch (_: Exception) {
                // Offline banner already covers this case.
            }
        }
    }

    private fun enableMyLocationLayerIfAllowed() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                mMap.isMyLocationEnabled = true
            } catch (_: SecurityException) {
                // Permission was revoked while the screen was open.
            }
        }
    }

    private fun centerOnCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionRequest
            )
            return
        }

        val client = LocationServices.getFusedLocationProviderClient(this)
        client.lastLocation.addOnSuccessListener { location ->
            if (location != null && ::mMap.isInitialized) {
                val here = LatLng(location.latitude, location.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(here, 18f))
                Telemetry.lunchPing(this, location.latitude, location.longitude, location.accuracy)
            } else {
                Toast.makeText(this, "Location unavailable", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun routeFromCurrentLocationTo(destination: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pendingRouteDestination = destination
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionRequest
            )
            return
        }

        Toast.makeText(this, "Getting your location...", Toast.LENGTH_SHORT).show()
        val client = LocationServices.getFusedLocationProviderClient(this)
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { location ->
                if (location == null) {
                    Toast.makeText(this, "Location unavailable. Using campus center.", Toast.LENGTH_SHORT).show()
                    fetchRouteFromBackend("ML 2", destination)
                    return@addOnSuccessListener
                }

                Telemetry.lunchPing(this, location.latitude, location.longitude, location.accuracy)
                resolveNearestNodeAndRoute(location.latitude, location.longitude, destination)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Could not get location", Toast.LENGTH_SHORT).show()
                fetchRouteFromBackend("ML 2", destination)
            }
    }

    private fun resolveNearestNodeAndRoute(lat: Double, lng: Double, destination: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.findNearest(lat, lng)
                val from = if (response.isSuccessful) response.body()?.node?.label ?: "ML 2" else "ML 2"
                fetchRouteFromBackend(from, destination)
            } catch (_: Exception) {
                fetchRouteFromBackend("ML 2", destination)
            }
        }
    }

    private fun setupBottomSheet() {
        val bottomSheet = findViewById<NestedScrollView>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.maxHeight = resources.displayMetrics.heightPixels / 2
        bottomSheetBehavior.isFitToContents = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.peekHeight = 180.dpToPx()
        bottomSheetBehavior.isHideable = false
    }

    private fun setupPrototypeClicks() {
        findViewById<View>(R.id.cvSearch).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        btnDirections = findViewById(R.id.fabDirections)
        btnDirections.setOnClickListener {
            openNavigationScreen("ML")
        }

        findViewById<View>(R.id.fabLocation).setOnClickListener {
            centerOnCurrentLocation()
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
            startActivity(Intent(this, SearchActivity::class.java))
        }

        findViewById<View>(R.id.profileContainer).setOnClickListener {
            logout()
        }
    }

    private fun fetchRouteFromBackend(fromNode: String, toNode: String) {
        lifecycleScope.launch {
            routeRepository.getGraphPath(fromNode, toNode).fold(
                onSuccess = { route ->
                    RouteHistoryStore(this@HomeActivity).record(route.from, route.to)
                    saveRouteForOffline(route)
                    Telemetry.event(
                        "route_computed",
                        mapOf(
                            "from" to route.from,
                            "to" to route.to,
                            "hops" to route.path.size,
                            "totalTimeSec" to route.totalTime
                        )
                    )
                    openRouteDetails(route)
                },
                onFailure = { error ->
                    Toast.makeText(
                        this@HomeActivity,
                        error.message ?: "Could not load route",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun saveRouteForOffline(route: RouteResponse) {
        OfflineRouteCache(this).save(
            CachedRoute(
                from = route.from,
                to = route.to,
                labels = route.path,
                lats = route.pathLatitudes ?: doubleArrayOf(),
                lngs = route.pathLongitudes ?: doubleArrayOf(),
                totalTimeSeconds = route.totalTime,
                savedAtMillis = System.currentTimeMillis()
            )
        )
    }

    private fun openRouteDetails(route: RouteResponse) {
        val intent = Intent(this, RouteActivity::class.java).apply {
            putExtra("destination", route.to)
            putExtra("from", route.from)
            putExtra("eta", route.totalTime.toEtaLabel())
            putExtra("distance", "${(route.path.size - 1).coerceAtLeast(0)} hops")
            putStringArrayListExtra("path", ArrayList(route.path))
            putExtra("total_time", route.totalTime)
            route.pathLatitudes?.let { putExtra("pathLats", it) }
            route.pathLongitudes?.let { putExtra("pathLngs", it) }
        }
        startActivity(intent)
    }

    private fun openNavigationScreen(defaultFrom: String) {
        startActivity(
            Intent(this, RouteActivity::class.java).apply {
                putExtra("from", defaultFrom)
            }
        )
    }

    private fun handleRouteFromIntent() {
        val showRoute = intent.getBooleanExtra("showRoute", false)
        if (showRoute && ::mMap.isInitialized) {
            drawRouteIfNeeded()
        }
    }

    private fun drawRouteIfNeeded() {
        val showRoute = intent.getBooleanExtra("showRoute", false)
        if (!showRoute || !::mMap.isInitialized) return

        val labels = intent.getStringArrayListExtra("path") ?: return
        val lats = intent.getDoubleArrayExtra("pathLats")
        val lngs = intent.getDoubleArrayExtra("pathLngs")

        val latLngPath = if (hasCoordinatePath(lats, lngs)) {
            lats!!.indices.map { index -> LatLng(lats[index], lngs!![index]) }
        } else {
            labels.mapNotNull { nodeCoordinates[it] }
        }

        if (latLngPath.isEmpty()) {
            Toast.makeText(this, "No hay coordenadas para esa ruta", Toast.LENGTH_SHORT).show()
            return
        }

        mMap.clear()
        mMap.addMarker(
            MarkerOptions()
                .position(latLngPath.first())
                .title("Inicio: ${labels.firstOrNull().orEmpty()}")
        )
        mMap.addMarker(
            MarkerOptions()
                .position(latLngPath.last())
                .title("Destino: ${labels.lastOrNull().orEmpty()}")
        )

        mMap.addPolyline(
            PolylineOptions()
                .addAll(latLngPath)
                .width(12f)
                .color(Color.parseColor("#FAD400"))
        )
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngPath.first(), 17f))
    }

    private fun hasCoordinatePath(lats: DoubleArray?, lngs: DoubleArray?): Boolean {
        return lats != null && lngs != null && lats.size == lngs.size && lats.isNotEmpty()
    }

    private val nodeCoordinates: Map<String, LatLng> = mapOf(
        "ML" to LatLng(4.6019, -74.0661),
        "ML 2" to LatLng(4.6019, -74.0661),
        "ML 3" to LatLng(4.6020, -74.0659),
        "ML 4" to LatLng(4.6021, -74.0657),
        "ML 5" to LatLng(4.6022, -74.0655),
        "W puente" to LatLng(4.6023, -74.0652),
        "W 5" to LatLng(4.6024, -74.0650),
        "W 4" to LatLng(4.6025, -74.0648),
        "W 3" to LatLng(4.6026, -74.0646),
        "RGD" to LatLng(4.6028, -74.0654)
    )

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
            "${this / 60} min"
        } else {
            "$this s"
        }
    }
}
