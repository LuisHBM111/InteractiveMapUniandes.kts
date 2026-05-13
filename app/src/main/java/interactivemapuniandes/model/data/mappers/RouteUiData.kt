package interactivemapuniandes.model.data.mappers

data class RouteUiData(
    val hasUpcomingClass: Boolean,
    val classTitle: String,
    val from: String,
    val to: String,
    val steps: List<RouteStepUi>,
)

data class RouteStepUi(
    val id: String,
    val label: String,
    val latitude: Double?,
    val longitude: Double?,
    val place: String?,
    val type: RouteStepType,
)

enum class RouteStepType{
    START,
    MIDDLE,
    END
}