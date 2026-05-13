package interactivemapuniandes.model.data.dtos

import com.google.gson.annotations.SerializedName


data class NextClassDTO(
    val hasUpcomingClass: Boolean,
    @SerializedName("class")
    val nextClass: Class?,
    val path: Path?,
    )

data class Class(
    val id: String,
    val title: String,
)

data class Path(
    val path: Path2?,
)

data class Path2(
    val path: List<Path3>,
)

data class Path3(
    val id: String,
    val label: String,
    val latitude: Double?,
    val longitude: Double?,
    val place: String?,
)
