package com.uniandes.interactivemapuniandes.view

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.model.repository.RouteRepository
import com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
import com.uniandes.interactivemapuniandes.model.repository.AuthRepository
import com.uniandes.interactivemapuniandes.viewmodel.RouteViewModel
import interactivemapuniandes.model.data.factories.RouteViewModelFactory
import interactivemapuniandes.model.remote.ApiService
import interactivemapuniandes.utils.ListsAdapter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch

class RouteActivity : AppCompatActivity() {

    private lateinit var routeRepository: RouteRepository
    private lateinit var routeViewModel: RouteViewModel
    private lateinit var routeViewModelFactory: RouteViewModelFactory
    private lateinit var apiService: ApiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var scheduleLoadingIndicator: CircularProgressIndicator
    private lateinit var nextClassAdapter: ListsAdapter


    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_route)


        getApiService()
        val authRepository = AuthRepository(FirebaseAuth.getInstance())
        routeRepository = RouteRepository(authRepository, apiService)

        routeViewModelFactory = RouteViewModelFactory(routeRepository)

        routeViewModel = ViewModelProvider(this, routeViewModelFactory)[RouteViewModel::class.java]

        nextClassAdapter = ListsAdapter(emptyList())

        scheduleLoadingIndicator = findViewById(R.id.scheduleLoadingIndicator)
        setupRecyclerView()
        setupNextClass()
        observeRouteState()
    }

    private fun setupRecyclerView(){
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this@RouteActivity)
        recyclerView.adapter = nextClassAdapter
    }
    private fun getApiService() {
        apiService = RetrofitInstance.getInstance().create(ApiService::class.java)
    }

    private fun setupNextClass(){
        val nextClassButton = findViewById<Button>(R.id.next_class)
        nextClassButton.setOnClickListener {
            routeViewModel.getNextClass()
        }
    }

    private fun observeRouteState(){
        lifecycleScope.launch {
            routeViewModel.uiState.collect { state ->
                if (!state.isRouteLoading && state.routeData != null) {
                    val steps = state.routeData.steps
                    nextClassAdapter.updateList(steps)
                }
                if (!state.isRouteLoading && state.errorMessage != null) {
                    Log.e("RouteActivity", "Error: ${state.errorMessage}")
                }

                scheduleLoadingIndicator.visibility = if (state.isRouteLoading) View.VISIBLE else View.GONE

            }
        }
    }

}
