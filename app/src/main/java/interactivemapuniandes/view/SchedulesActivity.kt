package com.uniandes.interactivemapuniandes.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.model.data.ScheduledClass
import com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
import com.uniandes.interactivemapuniandes.utils.setupNavigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Locale

class SchedulesActivity : AppCompatActivity() {

    private lateinit var adapter: ClassesAdapter

    private val icsPicker = registerForActivityResult( // Pick .ics from files
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) uploadIcs(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_schedules)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        findViewById<BottomNavigationView>(R.id.bottomNav).setupNavigation(this, "schedules")

        val rv = findViewById<RecyclerView>(R.id.rvClasses)
        adapter = ClassesAdapter { klass -> routeToClass(klass) }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<MaterialButton>(R.id.btnImport).setOnClickListener {
            icsPicker.launch("*/*") // ICS mime varies by device, let user pick anything
        }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddClass)
            .setOnClickListener { showAddClassDialog() }

        refresh()
    }

    private fun showAddClassDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_class, null)
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.schedule_add_manual)
            .setView(view)
            .setPositiveButton(R.string.common_save) { _, _ ->
                val title = view.findViewById<android.widget.EditText>(R.id.etTitle).text.toString().trim()
                val location = view.findViewById<android.widget.EditText>(R.id.etLocation).text.toString().trim()
                val day = view.findViewById<android.widget.EditText>(R.id.etDay).text.toString().trim()
                val start = view.findViewById<android.widget.EditText>(R.id.etStart).text.toString().trim()
                val end = view.findViewById<android.widget.EditText>(R.id.etEnd).text.toString().trim()
                if (title.isBlank() || location.isBlank()) {
                    Toast.makeText(this, "Title + location required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                com.uniandes.interactivemapuniandes.model.repository.LocalScheduleStore(this)
                    .add(com.uniandes.interactivemapuniandes.model.repository.LocalClass(title, location, day, start, end))
                Toast.makeText(this, "Saved locally", Toast.LENGTH_SHORT).show()
                refresh()
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    private fun refresh() {
        val progress = findViewById<View>(R.id.progress)
        val empty = findViewById<TextView>(R.id.tvEmpty)
        progress.visibility = View.VISIBLE
        empty.visibility = View.GONE

        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) { // Not logged in
            empty.text = "Log in to see your schedule"
            empty.visibility = View.VISIBLE
            progress.visibility = View.GONE
            return
        }

        lifecycleScope.launch {
            try {
                val resp = RetrofitInstance.meApi.listCurrentScheduleClasses()
                val remote = if (resp.isSuccessful) resp.body().orEmpty() else emptyList()
                val local = com.uniandes.interactivemapuniandes.model.repository.LocalScheduleStore(this@SchedulesActivity)
                    .all().map { lc -> // Convert local class to ScheduledClass shape so adapter is happy
                        com.uniandes.interactivemapuniandes.model.data.ScheduledClass(
                            id = "local:" + lc.title,
                            title = lc.title,
                            startsAt = lc.startsAt,
                            endsAt = lc.endsAt,
                            rawLocation = lc.location
                        )
                    }
                val merged = remote + local
                adapter.submit(merged)
                if (merged.isEmpty()) {
                    empty.text = getString(R.string.schedule_empty)
                    empty.visibility = View.VISIBLE
                } else {
                    empty.visibility = View.GONE
                }
            } catch (e: Exception) { // Offline → fall back to local-only
                val local = com.uniandes.interactivemapuniandes.model.repository.LocalScheduleStore(this@SchedulesActivity).all()
                if (local.isEmpty()) {
                    empty.text = "Network error: ${e.message}"
                    empty.visibility = View.VISIBLE
                } else {
                    val rows = local.map { lc ->
                        com.uniandes.interactivemapuniandes.model.data.ScheduledClass(
                            id = "local:" + lc.title,
                            title = lc.title,
                            startsAt = lc.startsAt,
                            endsAt = lc.endsAt,
                            rawLocation = lc.location
                        )
                    }
                    adapter.submit(rows)
                    empty.visibility = View.GONE
                    Toast.makeText(this@SchedulesActivity, R.string.common_offline_banner, Toast.LENGTH_SHORT).show()
                }
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun uploadIcs(uri: Uri) {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) { // Guard
            Toast.makeText(this, "Log in before importing", Toast.LENGTH_SHORT).show()
            return
        }

        val progress = findViewById<View>(R.id.progress)
        progress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                if (bytes == null || bytes.isEmpty()) {
                    Toast.makeText(this@SchedulesActivity, "File is empty or unreadable", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // Quick sanity check: .ics files start with BEGIN:VCALENDAR
                val head = String(bytes, 0, minOf(50, bytes.size)).uppercase()
                if (!head.contains("BEGIN:VCALENDAR")) {
                    Toast.makeText(this@SchedulesActivity, "Not an .ics file", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val filePart = MultipartBody.Part.createFormData(
                    "file",
                    pickedName(uri),
                    bytes.toRequestBody("text/calendar".toMediaTypeOrNull())
                )
                val replace: RequestBody = "true".toRequestBody("text/plain".toMediaTypeOrNull())

                val resp = RetrofitInstance.meApi.importScheduleFile(filePart, null, replace)
                when {
                    resp.isSuccessful -> {
                        Toast.makeText(this@SchedulesActivity, "Imported!", Toast.LENGTH_SHORT).show()
                        refresh()
                    }
                    resp.code() == 401 || resp.code() == 503 -> {
                        Toast.makeText(
                            this@SchedulesActivity,
                            "Please sign in again and retry",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else -> {
                        Toast.makeText(
                            this@SchedulesActivity,
                            "Import failed: ${resp.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@SchedulesActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun pickedName(uri: Uri): String { // Best-effort display name from URI
        var name = "schedule.ics"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = cursor.getString(idx) ?: name
                }
            }
        } catch (_: Exception) {}
        return if (name.endsWith(".ics", ignoreCase = true)) name else "$name.ics"
    }

    private fun routeToClass(klass: ScheduledClass) {
        val target = klass.destination?.routeTarget
            ?: klass.room?.roomCode
            ?: klass.rawLocation
        if (target.isNullOrBlank()) {
            Toast.makeText(this, "No location for this class", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, HomeActivity::class.java).apply {
            putExtra("routeTo", target) // No "routeFrom" → Home uses current location
            putExtra("autoFetchRoute", true)
        }
        startActivity(intent)
    }
}

class ClassesAdapter(
    private val onRoute: (ScheduledClass) -> Unit
) : RecyclerView.Adapter<ClassesAdapter.VH>() {

    private val items = mutableListOf<ScheduledClass>()
    private val isoIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.getDefault())
    private val timeOut = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun submit(rows: List<ScheduledClass>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_class, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.title.text = c.title
        holder.location.text = c.room?.roomCode ?: c.rawLocation ?: "—"
        holder.time.text = safeFormatTime(c.startsAt) // Graceful on weird dates
        holder.routeBtn.setOnClickListener { onRoute(c) }
    }

    private fun safeFormatTime(iso: String): String { // Backend sends ISO timestamps
        return try {
            val date = isoIn.parse(iso) ?: return iso
            timeOut.format(date)
        } catch (e: Exception) {
            iso.substringAfter('T').substring(0, 5)
        }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val time: TextView = v.findViewById(R.id.tvTime)
        val title: TextView = v.findViewById(R.id.tvTitle)
        val location: TextView = v.findViewById(R.id.tvLocation)
        val routeBtn: MaterialButton = v.findViewById(R.id.btnRoute)
    }
}
