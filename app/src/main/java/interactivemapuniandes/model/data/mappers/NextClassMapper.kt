package interactivemapuniandes.model.data.mappers

import interactivemapuniandes.model.data.dtos.NextClassDTO
import interactivemapuniandes.model.data.dtos.Path3
import interactivemapuniandes.model.data.dtos.PathSearch
import interactivemapuniandes.model.data.dtos.PreviousClassDTO
import interactivemapuniandes.model.data.dtos.SearchClassDTO
import interactivemapuniandes.model.data.dtos.TraversedEdge

fun NextClassDTO.toRouteUiData(): RouteUiData{
    val routePath = path?.path
    val nodes = routePath?.path.orEmpty()

    return RouteUiData(
        hasUpcomingClass = hasUpcomingClass,
        classTitle = nextClass?.title ?: "Class not found",
        from = routePath?.from ?: nodes.firstOrNull()?.label ?: "ML",
        to = routePath?.to ?: nodes.lastOrNull()?.label ?: "Destination not found",
        steps = nodes.toRouteStepUi(),
    )
}

fun List<Path3>.toRouteStepUi(): List<RouteStepUi>{
    return this.mapIndexed { index, path3 ->
        RouteStepUi(
            id = path3.id,
            label = path3.label,
            latitude = path3.latitude,
            longitude = path3.longitude,
            place = path3.place?.name,
            type = when(index){
                0 -> RouteStepType.START
                size - 1 -> RouteStepType.END
                else -> RouteStepType.MIDDLE
            }
        )
    }
}

fun SearchClassDTO.toSearchRouteUiData(): RouteUiData{
    return RouteUiData(
        hasUpcomingClass = true,
        classTitle = "",
        from = path.firstOrNull()?.label ?: from,
        to = path.lastOrNull()?.label ?: to,
        steps = path.toSearchRouteStepUi(),
    )
}

fun PreviousClassDTO.toRouteUiData(): RouteUiData{
    return RouteUiData(
        hasUpcomingClass = hasPreviousClass,
        classTitle = previousClass?.title ?: "Class not found",
        from = path?.path?.path?.firstOrNull()?.label ?: "ML",
        to = path?.path?.path?.lastOrNull()?.label ?: "Destination not found",
        steps = path?.path?.path?.toRouteStepUi() ?: emptyList(),
    )
}

fun List<PathSearch>.toSearchRouteStepUi(): List<RouteStepUi>{
    return this.mapIndexed { index, path ->
        RouteStepUi(
            id = path.id,
            label = path.label,
            latitude = path.latitude,
            longitude = path.longitude,
            place = path.place?.name,
            type = when(index){
                0 -> RouteStepType.START
                size - 1 -> RouteStepType.END
                else -> RouteStepType.MIDDLE
            }
        )
    }
}
