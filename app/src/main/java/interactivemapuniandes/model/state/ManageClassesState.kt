package interactivemapuniandes.model.state

import interactivemapuniandes.model.data.input.ScheduleClassInput

data class ManageClassesState(
    val classInput: ScheduleClassInput? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSavedSuccessfully: Boolean = false
)
