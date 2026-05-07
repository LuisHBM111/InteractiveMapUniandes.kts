package com.uniandes.interactivemapuniandes.view

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.model.repository.RouteRepository
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.listitem.ListItemViewHolder

class RouteActivity : AppCompatActivity() {

    private lateinit var routeRepository: RouteRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_route)


        val dataset: MutableList<RouteStepUi> = mutableListOf()
        val drawable: Drawable = getDrawable(R.drawable.ic_homework)!!
        val hola = RouteStepUi("hola", drawable)
        val hola2 = RouteStepUi("hola", drawable)
        dataset.add(hola)
        dataset.add(hola2)
        val customAdapter = ListsAdapter(dataset)

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = customAdapter

    }

    data class RouteStepUi(
        var name: String,
        var icon: Drawable?,
    )

    class ListsAdapter(private val items: List<RouteStepUi>) :
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
            viewHolder.bind(position)
            viewHolder.itemView.findViewById<TextView>(R.id.list_item_text)?.let { textView ->
                textView.text = items[position].name
            }
            viewHolder.itemView.findViewById<ImageView>(R.id.icon)?.let { imageView ->
                imageView.setImageDrawable(items[position].icon)
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = items.size

    }

}
