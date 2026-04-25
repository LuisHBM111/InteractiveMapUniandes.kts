package com.uniandes.interactivemapuniandes.view

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.model.data.RouteResponse
import com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
import com.uniandes.interactivemapuniandes.model.repository.AuthRepository
import com.uniandes.interactivemapuniandes.model.repository.RouteRepository
import com.uniandes.interactivemapuniandes.utils.Telemetry
import interactivemapuniandes.view.ScheduleActivity
import kotlinx.coroutines.launch

class RouteActivity : AppCompatActivity() {
    private lateinit var routeRepository: RouteRepository
    private lateinit var tvDestinationLabel: TextView
    private lateinit var tvDestinationTitle: TextView
    private lateinit var tvFromLocation: TextView
    private lateinit var tvStartLabel: TextView
    private lateinit var tvStartSub: TextView
    private lateinit var tvEndLabel: TextView
    private lateinit var tvEndSub: TextView
    private lateinit var tvEta: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvRouteSteps: TextView
    private lateinit var btnStartNavigation: MaterialButton
    private lateinit var btnCustomRoute: MaterialButton
    private lateinit var btnCalendar: MaterialButton

    private var defaultFromNode: String = "ML"
    private var currentRoute: RouteResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Telemetry.screen("route_view")
        enableEdgeToEdge()
        setContentView(R.layout.activity_route)

        routeRepository = RouteRepository(
            api = RetrofitInstance.api,
            authRepository = AuthRepository(FirebaseAuth.getInstance())
        )
        defaultFromNode = intent.getStringExtra("from") ?: "ML"

        bindViews()
        currentRoute = buildInitialRoute()
        if (currentRoute != null) {
            renderRoute(currentRoute!!)
        } else {
            renderInitialState()
        }
        setupClicks()
    }

    private fun bindViews() {
        tvDestinationLabel = findViewById(R.id.tvDestinationLabel)
        tvDestinationTitle = findViewById(R.id.tvDestinationTitle)
        tvFromLocation = findViewById(R.id.tvFromLocation)
        tvStartLabel = findViewById(R.id.tvStartLabel)
        tvStartSub = findViewById(R.id.tvStartSub)
        tvEndLabel = findViewById(R.id.tvEndLabel)
        tvEndSub = findViewById(R.id.tvEndSub)
        tvEta = findViewById(R.id.tvEta)
        tvDistance = findViewById(R.id.tvDistance)
        tvRouteSteps = findViewById(R.id.tvRouteSteps)
        btnStartNavigation = findViewById(R.id.btnStartNavigation)
        btnCustomRoute = findViewById(R.id.btnCustomRoute)
        btnCalendar = findViewById(R.id.btnCalendar)
    }

    private fun buildInitialRoute(): RouteResponse? {
        val destination = intent.getStringExtra("destination")
        val from = intent.getStringExtra("from") ?: defaultFromNode
        val path = intent.getStringArrayListExtra("path")
        val totalTime = intent.getIntExtra("total_time", 0)
        val pathLatitudes = intent.getDoubleArrayExtra("pathLats")
        val pathLongitudes = intent.getDoubleArrayExtra("pathLngs")

        val hasRoutePayload = !destination.isNullOrBlank() || !path.isNullOrEmpty() || totalTime > 0
        if (!hasRoutePayload) {
            return null
        }

        return RouteResponse(
            from = from,
            to = destination ?: "Unknown",
            path = path ?: arrayListOf(),
            totalTime = totalTime,
            pathLatitudes = pathLatitudes,
            pathLongitudes = pathLongitudes
        )
    }

    private fun setupClicks() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnStartNavigation.setOnClickListener {
            val route = currentRoute
            if (route == null) {
                loadNextClassRoute()
                return@setOnClickListener
            }

            if (!canRenderOnMap(route)) {
                Toast.makeText(this, "Map preview is not available for this route yet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, HomeActivity::class.java).apply {
                putExtra("showRoute", true)
                putStringArrayListExtra("path", ArrayList(route.path))
                putExtra("from", route.from)
                putExtra("to", route.to)
                putExtra("total_time", route.totalTime)
                route.pathLatitudes?.let { putExtra("pathLats", it) }
                route.pathLongitudes?.let { putExtra("pathLngs", it) }
            }
            startActivity(intent)
            finish()
        }

        btnCustomRoute.setOnClickListener {
            showCustomRouteDialog()
        }

        btnCalendar.text = "Open schedule"
        btnCalendar.setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
        }
    }

    private fun renderInitialState() {
        currentRoute = null
        tvDestinationLabel.text = "Ready to navigate"
        tvDestinationTitle.text = "Choose your next route"
        tvFromLocation.text = "From $defaultFromNode"
        tvStartLabel.text = defaultFromNode
        tvStartSub.text = "Current origin"
        tvEndLabel.text = "Next class or custom route"
        tvEndSub.text = "Pick how you want to plan the trip"
        tvEta.text = "--"
        tvDistance.text = "--"
        tvRouteSteps.text = "Tap Next class route to fetch the route automatically, or Plan custom route to choose your own origin and destination."
        btnStartNavigation.isEnabled = true
        btnStartNavigation.alpha = 1f
        btnStartNavigation.text = "Next class route"
    }

    private fun renderRoute(route: RouteResponse) {
        currentRoute = route

        val stepCount = (route.path.size - 1).coerceAtLeast(0)
        val isSamePlace = route.from.equals(route.to, ignoreCase = true)
        val canRenderOnMap = canRenderOnMap(route)

        tvDestinationLabel.text = if (route.classId != null) "Next class route" else "Custom route"
        tvDestinationTitle.text = "Route to ${route.to}"
        tvFromLocation.text = "From ${route.from}"
        tvStartLabel.text = route.from
        tvStartSub.text = "Origin node"
        tvEndLabel.text = route.to
        tvEndSub.text = if (isSamePlace) "You are already here" else "Destination node"
        tvEta.text = route.totalTime.toEtaLabel()
        tvDistance.text = formatDistance(route.pathLatitudes, route.pathLongitudes, route.path.size)
        tvRouteSteps.text = when {
            isSamePlace -> "You are already at your destination."
            route.path.isNotEmpty() -> route.path.joinToString(separator = "\n->\n")
            else -> "No route available."
        }

        btnStartNavigation.isEnabled = canRenderOnMap
        btnStartNavigation.alpha = if (canRenderOnMap) 1f else 0.55f
        btnStartNavigation.text = when {
            isSamePlace -> "Already there"
            canRenderOnMap -> "View on map"
            else -> "Map preview unavailable"
        }
    }

    private fun loadNextClassRoute() {
        btnStartNavigation.isEnabled = false
        btnStartNavigation.text = "Loading..."

        lifecycleScope.launch {
            val result = routeRepository.getRouteToNextClass(defaultFromNode)
            result.fold(
                onSuccess = { route ->
                    renderRoute(route)
                },
                onFailure = { error ->
                    renderInitialState()
                    Toast.makeText(
                        this@RouteActivity,
                        error.message ?: "Could not load next class route",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun showCustomRouteDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_route, null)
        val etRouteFrom = dialogView.findViewById<EditText>(R.id.etRouteFrom)
        val etRouteTo = dialogView.findViewById<EditText>(R.id.etRouteTo)

        etRouteFrom.setText(prefillNodeInput(currentRoute?.from))
        etRouteTo.setText(prefillNodeInput(currentRoute?.to))

        val dialog = MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Plan custom route")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Load route", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                etRouteFrom.error = null
                etRouteTo.error = null

                val from = normalizeRouteInput(etRouteFrom.text?.toString().orEmpty())
                val to = normalizeRouteInput(etRouteTo.text?.toString().orEmpty())

                var hasError = false
                if (from.isBlank()) {
                    etRouteFrom.error = "Enter an origin"
                    hasError = true
                }
                if (to.isBlank()) {
                    etRouteTo.error = "Enter a destination"
                    hasError = true
                }
                if (hasError) {
                    return@setOnClickListener
                }

                positiveButton.isEnabled = false
                positiveButton.text = "Loading..."

                lifecycleScope.launch {
                    val result = routeRepository.getGraphPath(from, to)
                    positiveButton.isEnabled = true
                    positiveButton.text = "Load route"

                    result.fold(
                        onSuccess = { route ->
                            renderRoute(route)
                            dialog.dismiss()
                            Toast.makeText(
                                this@RouteActivity,
                                "Custom route loaded",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onFailure = { error ->
                            Toast.makeText(
                                this@RouteActivity,
                                error.message ?: "Could not load route",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            }
        }

        dialog.show()
    }

    private fun normalizeRouteInput(rawValue: String): String {
        val trimmed = rawValue.trim()
        if (trimmed.isBlank()) {
            return ""
        }

        return if (!trimmed.contains(' ') && trimmed.length <= 6) {
            trimmed.uppercase()
        } else {
            trimmed
        }
    }

    private fun prefillNodeInput(value: String?): String {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isBlank()) {
            return ""
        }

        val shortCode = trimmed.substringBefore(" ")
        return if (shortCode.length in 2..6) shortCode.uppercase() else trimmed
    }

    private fun canRenderOnMap(route: RouteResponse): Boolean {
        return hasCoordinatePath(route.pathLatitudes, route.pathLongitudes) || canRenderOnPrototypeMap(route.path)
    }

    private fun hasCoordinatePath(lats: DoubleArray?, lngs: DoubleArray?): Boolean {
        return lats != null && lngs != null && lats.size == lngs.size && lats.isNotEmpty()
    }

    private fun canRenderOnPrototypeMap(path: List<String>): Boolean {
        if (path.isEmpty()) {
            return false
        }

        val supportedNodes = setOf(
            "ML 2",
            "ML 3",
            "ML 4",
            "ML 5",
            "W puente",
            "W 5",
            "W 4",
            "W 3"
        )

        return path.all { node -> supportedNodes.contains(node) }
    }

    private fun formatEta(seconds: Int): String {
        if (seconds <= 0) return "--"
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return when {
            minutes == 0 -> "${remainingSeconds}s"
            remainingSeconds == 0 -> "$minutes min"
            else -> "$minutes min ${remainingSeconds}s"
        }
    }

    private fun formatDistance(lats: DoubleArray?, lngs: DoubleArray?, fallbackHops: Int): String {
        if (!hasCoordinatePath(lats, lngs) || lats == null || lngs == null) {
            val hops = (fallbackHops - 1).coerceAtLeast(0)
            return if (hops == 1) "1 step" else "$hops steps"
        }

        var meters = 0.0
        for (index in 1 until lats.size) {
            meters += haversineMeters(lats[index - 1], lngs[index - 1], lats[index], lngs[index])
        }

        return if (meters >= 1000) {
            "%.1f km".format(meters / 1000.0)
        } else {
            "${meters.toInt()} m"
        }
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2).let { it * it }
        return 2 * earthRadius * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    private fun Int.toEtaLabel(): String = formatEta(this)
}
