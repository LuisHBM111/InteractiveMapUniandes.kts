package interactivemapuniandes.view

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.CarouselSnapHelper
import com.google.android.material.carousel.UncontainedCarouselStrategy
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
import com.uniandes.interactivemapuniandes.model.repository.AuthRepository
import com.uniandes.interactivemapuniandes.utils.setupNavigation
import interactivemapuniandes.model.data.AppDatabase
import interactivemapuniandes.model.entity.ScheduleClassEntity
import interactivemapuniandes.model.repository.ScheduleRepository
import interactivemapuniandes.model.state.ScheduleUiState
import interactivemapuniandes.utils.CustomAdapter
import interactivemapuniandes.utils.ScheduleClassAdapter
import interactivemapuniandes.viewmodel.ScheduleViewModel
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScheduleActivity : AppCompatActivity() {

    private lateinit var carousel: RecyclerView
    private lateinit var carouselAdapter: CustomAdapter
    private lateinit var scheduleViewModel: ScheduleViewModel
    private lateinit var scheduleRecyclerView: RecyclerView
    private lateinit var scheduleClassAdapter: ScheduleClassAdapter
    private lateinit var scheduleLoadingIndicator: CircularProgressIndicator
    private lateinit var scheduleStateText: TextView
    private lateinit var btnRefreshSchedule: MaterialButton
    private lateinit var btnImportSchedule: MaterialButton
    private lateinit var btnClearScheduleCache: MaterialButton
    private var lastScheduleSnackbarMessage: String? = null

    private val importScheduleLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { importScheduleFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_schedule)

        scheduleViewModel = createScheduleViewModel()

        val root = findViewById<View>(R.id.main)
        val initialPaddingStart = root.paddingStart
        val initialPaddingTop = root.paddingTop
        val initialPaddingEnd = root.paddingEnd
        val initialPaddingBottom = root.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                initialPaddingStart + systemBars.left,
                initialPaddingTop + systemBars.top,
                initialPaddingEnd + systemBars.right,
                initialPaddingBottom + systemBars.bottom
            )
            insets
        }

        carousel = findViewById(R.id.recycler_view)
        scheduleRecyclerView = findViewById(R.id.recycler_view_schedule)
        scheduleLoadingIndicator = findViewById(R.id.scheduleLoadingIndicator)
        scheduleStateText = findViewById(R.id.tvScheduleState)
        btnRefreshSchedule = findViewById(R.id.btnRefreshSchedule)
        btnImportSchedule = findViewById(R.id.btnImportSchedule)
        btnClearScheduleCache = findViewById(R.id.btnClearScheduleCache)
        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.setupNavigation(this, "schedules")

        configureCarousel()
        setupCarouselAdapter()
        setupScheduleClassAdapter()
        setupDebugActions()
        observeUiState()
        setUpSchedule()
    }

    private fun createScheduleViewModel(): ScheduleViewModel {
        val database = AppDatabase.getInstance(applicationContext)
        val scheduleRepository = ScheduleRepository(
            api = RetrofitInstance.api,
            authRepository = AuthRepository(FirebaseAuth.getInstance()),
            scheduleDao = database.scheduleDao()
        )
        return ScheduleViewModel(scheduleRepository)
    }

    private fun renderScheduleClasses(selectedDate: LocalDate, classes: List<ScheduleClassEntity>) {
        Log.d("ScheduleActivity", "Classes for $selectedDate: ${classes.size}")
    }

    private fun configureCarousel() {
        carousel.layoutManager = CarouselLayoutManager(
            UncontainedCarouselStrategy(),
            CarouselLayoutManager.HORIZONTAL
        ).apply {
            carouselAlignment = CarouselLayoutManager.ALIGNMENT_START
        }
        CarouselSnapHelper().attachToRecyclerView(carousel)
        carousel.setHasFixedSize(true)
    }

    private fun setupCarouselAdapter() {
        carouselAdapter = CustomAdapter(emptyList()) { selectedDay ->
            scheduleViewModel.selectDate(selectedDay.date)
        }
        carousel.adapter = carouselAdapter
    }

    private fun setupScheduleClassAdapter() {
        scheduleClassAdapter = ScheduleClassAdapter()
        scheduleRecyclerView.layoutManager = LinearLayoutManager(this)
        scheduleRecyclerView.adapter = scheduleClassAdapter
    }

    private fun setupDebugActions() {
        btnRefreshSchedule.setOnClickListener {
            lifecycleScope.launch {
                scheduleViewModel.loadSchedule()
            }
        }

        btnImportSchedule.setOnClickListener {
            openScheduleFilePicker()
        }

        btnClearScheduleCache.setOnClickListener {
            lifecycleScope.launch {
                scheduleViewModel.clearLocalCache()
            }
        }
    }


    private fun observeUiState() {
        lifecycleScope.launch {
            scheduleViewModel.uiState.collect { state ->
                carouselAdapter.updateItems(state.dayItems)
                renderScheduleClasses(state.selectedDate, state.classesForSelectedDay)
                scheduleClassAdapter.updateItems(state.classesForSelectedDay)
                renderScheduleState(state)
                renderScheduleActions(state)
            }
        }
    }

    private fun renderScheduleState(state: ScheduleUiState) {
        val isInitialLoading = state.isLoading && state.scheduleClasses.isEmpty()
        val hasClassesForSelectedDay = state.classesForSelectedDay.isNotEmpty()
        val hasErrorWithoutCache = state.scheduleError != null && state.scheduleClasses.isEmpty()

        scheduleLoadingIndicator.visibility = if (isInitialLoading) View.VISIBLE else View.GONE
        scheduleRecyclerView.visibility = if (!isInitialLoading && hasClassesForSelectedDay) {
            View.VISIBLE
        } else {
            View.GONE
        }

        val stateMessage = when {
            hasErrorWithoutCache -> state.scheduleError
            !isInitialLoading && !hasClassesForSelectedDay -> "No classes for this day"
            else -> null
        }

        scheduleStateText.text = stateMessage.orEmpty()
        scheduleStateText.visibility = if (stateMessage != null) View.VISIBLE else View.GONE

        showScheduleSnackbarIfNeeded(state)
    }

    private fun renderScheduleActions(state: ScheduleUiState) {
        val isBusy = state.isRefreshing || state.isImportingSchedule

        btnRefreshSchedule.isEnabled = !isBusy
        btnImportSchedule.isEnabled = !isBusy
        btnClearScheduleCache.isEnabled = !isBusy

        btnRefreshSchedule.text = if (state.isRefreshing && !state.isImportingSchedule) {
            "Refreshing..."
        } else {
            "Refresh"
        }
        btnImportSchedule.text = if (state.isImportingSchedule) "Uploading..." else "Upload .ics"
    }

    private fun showScheduleSnackbarIfNeeded(state: ScheduleUiState) {
        val snackbarMessage = when {
            state.scheduleImportSuccess -> "Schedule imported."
            state.isShowingCachedData && state.scheduleError != null ->
                "Showing saved schedule. We couldn't refresh it."
            state.scheduleError != null -> state.scheduleError
            else -> null
        }

        if (snackbarMessage == null) {
            lastScheduleSnackbarMessage = null
            return
        }

        if (lastScheduleSnackbarMessage == snackbarMessage) {
            return
        }

        val snackbar = Snackbar.make(
            findViewById(R.id.main),
            snackbarMessage,
            Snackbar.LENGTH_LONG
        ).setAnchorView(findViewById<View>(R.id.bottomNav))

        if (state.canRetryScheduleRefresh) {
            snackbar.setAction("Retry") {
                retryScheduleLoad()
            }
        }

        snackbar.show()
        lastScheduleSnackbarMessage = snackbarMessage

        if (state.scheduleImportSuccess) {
            scheduleViewModel.clearScheduleImportSuccess()
        }
    }

    private fun retryScheduleLoad() {
        lifecycleScope.launch {
            scheduleViewModel.loadSchedule()
        }
    }

    private fun openScheduleFilePicker() {
        importScheduleLauncher.launch(
            arrayOf(
                "text/calendar",
                "text/*",
                "application/octet-stream",
                "*/*"
            )
        )
    }

    private fun importScheduleFromUri(uri: Uri) {
        lifecycleScope.launch {
            val selectedFile = readScheduleFile(uri)
            if (selectedFile == null) {
                showScheduleMessage("We couldn't read that file.")
                return@launch
            }

            if (!selectedFile.fileName.endsWith(".ics", ignoreCase = true)) {
                showScheduleMessage("Please choose a .ics schedule file.")
                return@launch
            }

            scheduleViewModel.importScheduleFile(
                fileName = selectedFile.fileName,
                mimeType = selectedFile.mimeType,
                fileBytes = selectedFile.fileBytes
            )
        }
    }

    private suspend fun readScheduleFile(uri: Uri): ScheduleFileSelection? {
        return withContext(Dispatchers.IO) {
            val fileName = getDisplayName(uri) ?: "schedule.ics"
            val mimeType = contentResolver.getType(uri) ?: "text/calendar"
            val fileBytes = contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes()
            } ?: return@withContext null

            ScheduleFileSelection(
                fileName = fileName,
                mimeType = mimeType,
                fileBytes = fileBytes
            )
        }
    }

    private fun getDisplayName(uri: Uri): String? {
        return contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (displayNameIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(displayNameIndex)
            } else {
                null
            }
        }
    }

    private fun showScheduleMessage(message: String) {
        Snackbar.make(
            findViewById(R.id.main),
            message,
            Snackbar.LENGTH_LONG
        ).setAnchorView(findViewById<View>(R.id.bottomNav)).show()
    }

    fun setUpSchedule() {
        lifecycleScope.launch {
            scheduleViewModel.observeScheduleClasses()
        }
        lifecycleScope.launch {
            scheduleViewModel.loadSchedule()
        }
    }

    private data class ScheduleFileSelection(
        val fileName: String,
        val mimeType: String,
        val fileBytes: ByteArray
    )
}
