package com.uniandes.interactivemapuniandes.view

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.model.repository.RouteRepository
import com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
import com.uniandes.interactivemapuniandes.model.repository.AuthRepository
import interactivemapuniandes.model.remote.ApiService
import interactivemapuniandes.utils.ListsAdapter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
class RouteActivity : AppCompatActivity() {

    private lateinit var routeRepository: RouteRepository

    private lateinit var apiService: ApiService

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_route)


        /*
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

         */


        val authRepository = AuthRepository(FirebaseAuth.getInstance())
        routeRepository = RouteRepository(RetrofitInstance.api, authRepository, getApiService())

        setupNextClass()

    }

    private fun getApiService(): ApiService {
        apiService = RetrofitInstance.getInstance().create(ApiService::class.java)
        return apiService
    }

    private fun setupNextClass(){
        val nextClassButton = findViewById<Button>(R.id.next_class)
        nextClassButton.setOnClickListener {
            NextClass()
        }
    }

    private fun NextClass(){
        MainScope().launch {
            val result = routeRepository.getNextClass()
            if (result.isSuccess) {
                Log.e("RouteActivity", "${result.isSuccess}")
                val steps = result.getOrNull()?.path?.path?.path
                val customAdapter = ListsAdapter(steps ?: emptyList())
                val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
                recyclerView.layoutManager = LinearLayoutManager(this@RouteActivity)
                recyclerView.adapter = customAdapter
            } else {
                Log.e("RouteActivity", "Error loading next class", result.exceptionOrNull())
            }
        }
    }

}
