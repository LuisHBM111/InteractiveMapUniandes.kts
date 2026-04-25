package interactivemapuniandes.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.uniandes.interactivemapuniandes.R
import interactivemapuniandes.model.entity.ScheduleClassEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ScheduleClassAdapter(
    private var items: List<ScheduleClassEntity> = emptyList()
) : RecyclerView.Adapter<ScheduleClassAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvClassTime: TextView = view.findViewById(R.id.tvClassTime)
        val tvClassTitle: TextView = view.findViewById(R.id.tvClassTitle)
        val tvClassCode: TextView = view.findViewById(R.id.tvClassCode)
        val tvClassLocation: TextView = view.findViewById(R.id.tvClassLocation)
        val tvClassInstructor: TextView = view.findViewById(R.id.tvClassInstructor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule_class, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val scheduleClass = items[position]

        holder.tvClassTime.text = scheduleClass.formatTimeRange()
        holder.tvClassTitle.text = scheduleClass.title
        holder.tvClassCode.text = buildClassCode(scheduleClass)
        holder.tvClassLocation.text = buildLocation(scheduleClass)
        holder.tvClassInstructor.text = scheduleClass.instructorName ?: "Instructor not available"
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<ScheduleClassEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun buildClassCode(scheduleClass: ScheduleClassEntity): String {
        return listOfNotNull(
            scheduleClass.courseCode,
            scheduleClass.section?.let { "Section $it" }
        ).joinToString(" • ")
    }

    private fun buildLocation(scheduleClass: ScheduleClassEntity): String {
        return listOfNotNull(
            scheduleClass.roomName ?: scheduleClass.roomCode,
            scheduleClass.buildingName ?: scheduleClass.buildingCode
        ).joinToString(" • ").ifBlank {
            scheduleClass.rawLocation ?: "Location not available"
        }
    }

    private fun ScheduleClassEntity.formatTimeRange(): String {
        val formatter = DateTimeFormatter
            .ofPattern("h:mm a", Locale.US)

        return try {
            val zoneId = ZoneId.of(timezone)
            val start = Instant.parse(startsAt)
                .atZone(zoneId)
                .format(formatter)
            val end = Instant.parse(endsAt)
                .atZone(zoneId)
                .format(formatter)

            "$start - $end"
        } catch (e: Exception) {
            ""
        }
    }
}
