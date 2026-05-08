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
import interactivemapuniandes.model.data.RouteDTO
import interactivemapuniandes.utils.ListsAdapter

class RouteActivity : AppCompatActivity() {

    private lateinit var routeRepository: RouteRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_route)


        val dataset: MutableList<RouteDTO> = mutableListOf()
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

}
