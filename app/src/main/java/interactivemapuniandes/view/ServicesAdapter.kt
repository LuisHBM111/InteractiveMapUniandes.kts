package com.uniandes.interactivemapuniandes.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.uniandes.interactivemapuniandes.R

// Generic service card used on the Home "Nearby Services" list.
// `target` is whatever the router needs — a building code or restaurant name.
data class ServiceItem(
    val name: String,
    val subtitle: String,
    val emoji: String,
    val target: String,
    val photoUrl: String? = null // Real photo overrides emoji when present
)

class ServicesAdapter(
    private val onClick: (ServiceItem) -> Unit
) : RecyclerView.Adapter<ServicesAdapter.VH>() {

    private val items = mutableListOf<ServiceItem>()

    fun submit(rows: List<ServiceItem>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_service, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.emoji.text = item.emoji
        holder.name.text = item.name
        holder.details.text = item.subtitle
        if (!item.photoUrl.isNullOrBlank()) {
            holder.thumb.visibility = View.VISIBLE
            holder.emoji.visibility = View.GONE
            holder.thumb.load(item.photoUrl) // Coil 3 view extension
        } else {
            holder.thumb.visibility = View.GONE
            holder.emoji.visibility = View.VISIBLE
        }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val emoji: TextView = v.findViewById(R.id.tvEmoji)
        val thumb: ImageView = v.findViewById(R.id.ivThumb)
        val name: TextView = v.findViewById(R.id.tvName)
        val details: TextView = v.findViewById(R.id.tvDetails)
    }
}
