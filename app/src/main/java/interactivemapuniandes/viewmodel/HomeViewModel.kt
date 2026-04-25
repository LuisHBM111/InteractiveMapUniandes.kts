package com.uniandes.interactivemapuniandes.viewmodel

import com.uniandes.interactivemapuniandes.model.data.NextClass
import com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeUiState(
    val nextClass: NextClass? = null,
    val isLoadingNext: Boolean = false,
    val error: String? = null
)

class HomeViewModel {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    suspend fun refreshNextClass() { // Called from an Activity coroutine
        _state.value = _state.value.copy(isLoadingNext = true, error = null)
        try {
            val resp = RetrofitInstance.meApi.getNextClass()
            if (resp.isSuccessful) {
                _state.value = _state.value.copy(
                    nextClass = resp.body(),
                    isLoadingNext = false
                )
            } else {
                _state.value = _state.value.copy(
                    isLoadingNext = false,
                    error = "Backend ${resp.code()}"
                )
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(isLoadingNext = false, error = e.message)
        }
    }
}
