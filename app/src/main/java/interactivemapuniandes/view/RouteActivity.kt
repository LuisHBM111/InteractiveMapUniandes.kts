package com.uniandes.interactivemapuniandes.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputLayout
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

    private lateinit var button_search: Button
    private lateinit var button_swap: Button
    private lateinit var button_next_class: Button
    private lateinit var button_previous_class: Button
    private lateinit var view_on_map_button: Button
    private lateinit var button_random_class: Button

    private lateinit var toolbar: MaterialToolbar

    private lateinit var outlinedTextField_from: TextInputLayout

    private lateinit var outlinedTextField_to: TextInputLayout




    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_route)


        setupButtons()
        setupToolbar()
        getApiService()
        setupSwapButton()
        setupViewOnMapClass()

        val authRepository = AuthRepository(FirebaseAuth.getInstance())
        routeRepository = RouteRepository(authRepository, apiService)

        routeViewModelFactory = RouteViewModelFactory(routeRepository)

        routeViewModel = ViewModelProvider(this, routeViewModelFactory)[RouteViewModel::class.java]

        nextClassAdapter = ListsAdapter(emptyList())

        scheduleLoadingIndicator = findViewById(R.id.scheduleLoadingIndicator)
        setupRecyclerView()
        setupNextClass()
        setupPreviousClass()
        setupRandomClass()
        setupSearchClass()
        observeRouteState()
    }

    private fun setupButtons(){
        button_search = findViewById(R.id.button_search)
        button_swap = findViewById(R.id.button_swap)
        button_next_class = findViewById(R.id.next_class)
        button_previous_class = findViewById(R.id.previous_class)
        view_on_map_button = findViewById(R.id.view_on_map_button)
        button_random_class = findViewById(R.id.random_class)
        toolbar = findViewById(R.id.toolbar)
        outlinedTextField_from = findViewById(R.id.outlinedTextField_from)
        outlinedTextField_to = findViewById(R.id.outlinedTextField_to)
    }

    private fun setupToolbar(){
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupSwapButton(){
        button_swap.setOnClickListener {
            val from = outlinedTextField_from.editText?.text.toString()
            val to = outlinedTextField_to.editText?.text.toString()

            outlinedTextField_from.editText?.setText(to)
            outlinedTextField_to.editText?.setText(from)
        }
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
        button_next_class.setOnClickListener {
            routeViewModel.getNextClass()
        }
    }

    private fun setupPreviousClass(){
        button_previous_class.setOnClickListener {
            routeViewModel.getPreviousClass()
        }
    }

    private fun setupRandomClass(){
        button_random_class.setOnClickListener {
            routeViewModel.getRandomClass()
        }
    }

    private fun setupViewOnMapClass(){
        view_on_map_button.setOnClickListener {
            intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupSearchClass(){
        button_search.setOnClickListener {
            val from = outlinedTextField_from.editText?.text.toString()
            val to = outlinedTextField_to.editText?.text.toString()
            routeViewModel.getSearchClass(from, to)
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
                    Toast.makeText(this@RouteActivity, "Route not found", Toast.LENGTH_SHORT).show()
                }

                scheduleLoadingIndicator.visibility = if (state.isRouteLoading) View.VISIBLE else View.GONE

            }
        }
    }

}
