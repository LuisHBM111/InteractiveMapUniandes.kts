package interactivemapuniandes.viewmodel

import androidx.lifecycle.ViewModel
import interactivemapuniandes.model.data.input.ScheduleClassInput
import interactivemapuniandes.model.repository.ManageClassesRepository
import interactivemapuniandes.model.state.ManageClassesState
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ManageClassesViewModel(
    private val manageClassesRepository: ManageClassesRepository
): ViewModel() {

    private val _uiState = MutableStateFlow(ManageClassesState())
    val uiState: StateFlow<ManageClassesState> = _uiState.asStateFlow()

    fun updateScheduleClassInput(input: ScheduleClassInput) {
        _uiState.value = _uiState.value.copy(classInput = input)
    }

    fun saveClass(){

        _uiState.value = _uiState.value.copy(
            isSaving = true,
            errorMessage = null,
            isSavedSuccessfully = false
        )

        val input = _uiState.value.classInput

        if (input == null) {
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                errorMessage = "Fill the class form first"
            )
            return
        }

        viewModelScope.launch {
            try {
                val result = manageClassesRepository.saveClass(input)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = null,
                        isSavedSuccessfully = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = result.exceptionOrNull()?.message
                    )
                }
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = error.message
                )
            }
        }

    }

}
