package interactivemapuniandes.model.data.dtos

data class SearchClassDTO(
    val from: String,
    val to: String,
    val totalTimeSeconds: Int,
    val totalTimeMinutes: Double,
    val path: List<PathSearch>,
    val traversedEdges: List<TraversedEdge>,
)

data class PathSearch(
    val id: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val place: String,
)

data class TraversedEdge(
    val from: String,
    val to: String,
    val travelTimeSeconds: Int,
)