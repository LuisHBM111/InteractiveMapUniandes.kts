package interactivemapuniandes.view

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.uniandes.interactivemapuniandes.R
import interactivemapuniandes.model.data.AppDatabase
import interactivemapuniandes.model.data.input.ScheduleClassInput
import interactivemapuniandes.model.repository.ManageClassesRepository
import interactivemapuniandes.viewmodel.ManageClassesViewModel
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

class ManageClassesActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var classNameInput: TextInputEditText
    private lateinit var courseCodeInput: TextInputEditText
    private lateinit var sectionInput: TextInputEditText
    private lateinit var nrcInput: TextInputEditText
    private lateinit var instructorInput: TextInputEditText
    private lateinit var daysChipGroup: ChipGroup
    private lateinit var chipMo: Chip
    private lateinit var chipTu: Chip
    private lateinit var chipWe: Chip
    private lateinit var chipTh: Chip
    private lateinit var chipFr: Chip
    private lateinit var chipSa: Chip
    private lateinit var chipSu: Chip
    private lateinit var startTimeInput: TextInputEditText
    private lateinit var endTimeInput: TextInputEditText
    private lateinit var startDateInput: TextInputEditText
    private lateinit var untilDateInput: TextInputEditText
    private lateinit var buildingCodeInput: TextInputEditText
    private lateinit var roomCodeInput: TextInputEditText
    private lateinit var saveClassButton: Button
    private lateinit var manageClassesViewModel: ManageClassesViewModel

    private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manage_classes)
        setupViews()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val database = AppDatabase.getInstance(applicationContext)
        val manageClassesRepository = ManageClassesRepository(database.scheduleDao())
        manageClassesViewModel = ManageClassesViewModel(manageClassesRepository)
        setupTimeInputs()
        setupDateInputs()
        setupSaveClassButton()
        observeSaveClassResult()
    }

    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        classNameInput = findViewById(R.id.class_name_input)
        courseCodeInput = findViewById(R.id.course_code_input)
        sectionInput = findViewById(R.id.section_input)
        nrcInput = findViewById(R.id.nrc_input)
        instructorInput = findViewById(R.id.instructor_input)
        daysChipGroup = findViewById(R.id.days_chip_group)
        chipMo = findViewById(R.id.chip_mo)
        chipTu = findViewById(R.id.chip_tu)
        chipWe = findViewById(R.id.chip_we)
        chipTh = findViewById(R.id.chip_th)
        chipFr = findViewById(R.id.chip_fr)
        chipSa = findViewById(R.id.chip_sa)
        chipSu = findViewById(R.id.chip_su)
        startTimeInput = findViewById(R.id.start_time_input)
        endTimeInput = findViewById(R.id.end_time_input)
        startDateInput = findViewById(R.id.start_date_input)
        untilDateInput = findViewById(R.id.until_date_input)
        buildingCodeInput = findViewById(R.id.building_code_input)
        roomCodeInput = findViewById(R.id.room_code_input)
        saveClassButton = findViewById(R.id.save_class_button)
    }

    private fun setupSaveClassButton() {
        saveClassButton.setOnClickListener {
            readClassInputFromForm()
            manageClassesViewModel.saveClass()
        }
    }

    private fun setupTimeInputs() {
        startTimeInput.setModalOnly()
        endTimeInput.setModalOnly()
        startTimeInput.setOnClickListener {
            showTimePicker("Select start time", startTimeInput)
        }
        endTimeInput.setOnClickListener {
            showTimePicker("Select end time", endTimeInput)
        }
    }

    private fun setupDateInputs() {
        startDateInput.setModalOnly()
        untilDateInput.setModalOnly()
        startDateInput.setOnClickListener {
            showDatePicker("Select start date", startDateInput)
        }
        untilDateInput.setOnClickListener {
            showDatePicker("Select until date", untilDateInput)
        }
    }

    private fun TextInputEditText.setModalOnly() {
        isFocusable = false
        isCursorVisible = false
    }

    private fun showDatePicker(title: String, targetInput: TextInputEditText) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(title)
            .setInputMode(MaterialDatePicker.INPUT_MODE_TEXT)
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        picker.addOnPositiveButtonClickListener { selectedDateMillis ->
            val formattedDate = Instant.ofEpochMilli(selectedDateMillis)
                .atZone(ZoneOffset.UTC)
                .format(dateFormatter)
            targetInput.setText(formattedDate)
        }
        picker.show(supportFragmentManager, title)
    }

    private fun showTimePicker(title: String, targetInput: TextInputEditText) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(8)
            .setMinute(0)
            .setTitleText(title)
            .build()

        picker.addOnPositiveButtonClickListener {
            val formattedTime = formatTime(picker.hour, picker.minute)
            targetInput.setText(formattedTime)
        }
        picker.show(supportFragmentManager, title)
    }

    private fun formatTime(hour24: Int, minute: Int): String {
        val suffix = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return "%02d:%02d %s".format(hour12, minute, suffix)
    }

    private fun readClassInputFromForm() {
        val className = classNameInput.text.toString()
        val courseCode = courseCodeInput.text.toString()
        val section = sectionInput.text.toString()
        val nrc = nrcInput.text.toString()
        val instructor = instructorInput.text.toString()
        val days = mutableListOf<String>()
        if (chipMo.isChecked) days.add("MO")
        if (chipTu.isChecked) days.add("TU")
        if (chipWe.isChecked) days.add("WE")
        if (chipTh.isChecked) days.add("TH")
        if (chipFr.isChecked) days.add("FR")
        if (chipSa.isChecked) days.add("SA")
        if (chipSu.isChecked) days.add("SU")
        val startTime = startTimeInput.text.toString()
        val endTime = endTimeInput.text.toString()
        val startDate = startDateInput.text.toString()
        val untilDate = untilDateInput.text.toString()
        val buildingCode = buildingCodeInput.text.toString()
        val roomCode = roomCodeInput.text.toString()
        val scheduleClassInput = ScheduleClassInput(
            className,
            courseCode,
            section,
            nrc,
            instructor,
            days,
            startTime,
            endTime,
            startDate,
            untilDate,
            buildingCode,
            roomCode
        )
        manageClassesViewModel.updateScheduleClassInput(scheduleClassInput)
    }

    private fun observeSaveClassResult() {
        lifecycleScope.launch {
            manageClassesViewModel.uiState.collect { state ->
                saveClassButton.isEnabled = !state.isSaving
                saveClassButton.text = if (state.isSaving) "Saving..." else "Save Class"

                if (state.errorMessage != null) {
                    Toast.makeText(this@ManageClassesActivity, state.errorMessage, Toast.LENGTH_SHORT).show()
                }

                if (state.isSavedSuccessfully) {
                    Toast.makeText(this@ManageClassesActivity, "Class saved", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}
