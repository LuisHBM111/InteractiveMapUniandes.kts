package interactivemapuniandes.model.data.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.uniandes.interactivemapuniandes.model.repository.RouteRepository
import com.uniandes.interactivemapuniandes.viewmodel.RouteViewModel

class RouteViewModelFactory(private val routeRepository: RouteRepository): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RouteViewModel::class.java)) {
            return RouteViewModel(routeRepository) as T
        }else{
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
