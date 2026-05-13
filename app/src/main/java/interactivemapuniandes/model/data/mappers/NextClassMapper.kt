package interactivemapuniandes.model.data.mappers

import interactivemapuniandes.model.data.dtos.NextClassDTO
import interactivemapuniandes.model.data.dtos.Path3

fun NextClassDTO.toRouteUiData(): RouteUiData{
    return RouteUiData(
        hasUpcomingClass = hasUpcomingClass,
        classTitle = nextClass?.title ?: "Class not found",
        from = path?.path?.path?.firstOrNull()?.label ?: "ML",
        to = path?.path?.path?.lastOrNull()?.label ?: "Destination not found",
        steps = path?.path?.path?.toRouteStepUi() ?: emptyList(),
    )
}

fun List<Path3>.toRouteStepUi(): List<RouteStepUi>{
    return this.mapIndexed { index, path3 ->
        RouteStepUi(
            id = path3.id,
            label = path3.label,
            latitude = path3.latitude,
            longitude = path3.longitude,
            place = path3.place,
            type = when(index){
                0 -> RouteStepType.START
                size - 1 -> RouteStepType.END
                else -> RouteStepType.MIDDLE
            }
        )
    }
}