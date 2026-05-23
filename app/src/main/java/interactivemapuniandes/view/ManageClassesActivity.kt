package interactivemapuniandes.view

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.uniandes.interactivemapuniandes.R

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
}
