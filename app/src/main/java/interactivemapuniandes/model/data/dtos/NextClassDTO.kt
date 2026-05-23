package interactivemapuniandes.model.data.dtos

import com.google.gson.annotations.SerializedName


data class NextClassDTO(
    val hasUpcomingClass: Boolean,
    @SerializedName("class")
    val nextClass: NextRouteClass?,
    val path: Path?,
    )

data class NextRouteClass(
    val id: String?,
    val title: String?,
)

data class Path(
    val path: Path2?,
)

data class Path2(
    val from: String? = null,
    val to: String? = null,
    val totalTimeSeconds: Int? = null,
    val totalTimeMinutes: Double? = null,
    val path: List<Path3> = emptyList(),
)

data class Path3(
    val id: String,
    val label: String,
    val latitude: Double?,
    val longitude: Double?,
    val place: RoutePlace?,
)

data class RoutePlace(
    val id: String?,
    val name: String?,
)
