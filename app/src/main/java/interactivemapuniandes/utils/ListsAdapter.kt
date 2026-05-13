package interactivemapuniandes.utils

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.listitem.ListItemViewHolder
import com.uniandes.interactivemapuniandes.R
import interactivemapuniandes.model.data.dtos.Path3
import kotlin.let


class ListsAdapter(private val items: List<Path3>) :
    RecyclerView.Adapter<ListItemViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ListItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_viewholder, parent, false)
        return ListItemViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ListItemViewHolder, position: Int) {
        viewHolder.bind(position, items.size)
        viewHolder.itemView.findViewById<TextView>(R.id.list_item_text)?.let { textView ->
            textView.text = items[position].label
        }
        /*
        viewHolder.itemView.findViewById<ImageView>(R.id.icon)?.let { imageView ->
            imageView.setImageDrawable(items[position].icon)
        }

         */
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = items.size

}
