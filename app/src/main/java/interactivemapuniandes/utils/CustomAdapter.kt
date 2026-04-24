package interactivemapuniandes.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.google.android.material.card.MaterialCardView
import com.uniandes.interactivemapuniandes.R
import interactivemapuniandes.model.state.ScheduleDayUi

class CustomAdapter(
    private var dataSet: List<ScheduleDayUi>,
    private val onDaySelected: (ScheduleDayUi) -> Unit
) :
    Adapter<CustomAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardDayChip: MaterialCardView = view.findViewById(R.id.cardDayChip)
        val tvDayLabel: TextView = view.findViewById(R.id.tvDayLabel)
        val tvDayNumber: TextView = view.findViewById(R.id.tvDayNumber)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.text_row_item, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val dayItem = dataSet[position]

        viewHolder.tvDayLabel.text = dayItem.dayLabel
        viewHolder.tvDayNumber.text = dayItem.dayNumber

        val context = viewHolder.itemView.context
        val backgroundColor = if (dayItem.isSelected) {
            context.getColor(R.color.schedule_chip_selected)
        } else {
            context.getColor(R.color.schedule_chip_background)
        }
        val textColor = if (dayItem.isSelected) {
            context.getColor(R.color.schedule_chip_selected_text)
        } else {
            context.getColor(R.color.dark_blue)
        }
        val dayLabelColor = if (dayItem.isSelected) {
            context.getColor(R.color.schedule_chip_selected_text)
        } else {
            context.getColor(R.color.schedule_chip_day_text)
        }

        viewHolder.cardDayChip.setCardBackgroundColor(backgroundColor)
        viewHolder.tvDayLabel.setTextColor(dayLabelColor)
        viewHolder.tvDayNumber.setTextColor(textColor)

        viewHolder.itemView.setOnClickListener {
            onDaySelected(dayItem)
        }
    }

    fun updateItems(newItems: List<ScheduleDayUi>) {
        dataSet = newItems
        notifyDataSetChanged()
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}
