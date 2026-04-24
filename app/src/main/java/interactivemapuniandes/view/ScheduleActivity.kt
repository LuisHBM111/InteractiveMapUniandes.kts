package interactivemapuniandes.view

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.CarouselSnapHelper
import com.google.android.material.carousel.UncontainedCarouselStrategy
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.utils.setupNavigation
import interactivemapuniandes.utils.CustomAdapter
import interactivemapuniandes.model.repository.ScheduleRepository
import interactivemapuniandes.viewmodel.ScheduleViewModel
import kotlinx.coroutines.launch

class ScheduleActivity : AppCompatActivity() {

    private lateinit var carousel: RecyclerView
    private lateinit var carouselAdapter: CustomAdapter
    private lateinit var scheduleViewModel: ScheduleViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_schedule)

        scheduleViewModel = ScheduleViewModel(ScheduleRepository())

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
        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.setupNavigation(this, "schedules")

        configureCarousel()
        setupCarouselAdapter()
        observeUiState()
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

    private fun observeUiState() {
        lifecycleScope.launch {
            scheduleViewModel.uiState.collect { state ->
                carouselAdapter.updateItems(state.dayItems)
            }
        }
    }
}
