package interactivemapuniandes.model.data

data class RouteDTO (
    val hasUpcomingClass: Boolean,
    val nextClass: NextClass,
)

data class NextClass(
    val id: String,
    val title: String,
)