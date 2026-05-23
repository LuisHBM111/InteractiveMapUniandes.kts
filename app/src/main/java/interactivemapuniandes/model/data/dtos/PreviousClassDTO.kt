package interactivemapuniandes.model.data.dtos

import com.google.gson.annotations.SerializedName

data class PreviousClassDTO(
    val hasPreviousClass: Boolean,
    @SerializedName("class")
    val previousClass: RouteClass?,
    val path: Path?,
)

data class NextClassInfoDTO(
    val hasUpcomingClass: Boolean,
    @SerializedName("class")
    val nextClass: RouteClass?,
)

data class RouteClass(
    val id: String?,
    val title: String?,
    val destination: Destination?,
)

data class Destination(
    val room: Room?,
    val building: Building?,
)

data class Room(
    val id: String?,
    val name: String?,
    val roomCode: String?,
)

data class Building(
    val id: String?,
    val code: String?,
    val name: String?,
)
