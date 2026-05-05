package com.uniandes.interactivemapuniandes.domain.routing

data class Route(
    val from: String,
    val to: String,
    val path: List<String>,
    val totalTime: Int,
    val classId: String? = null,
    val classTitle: String? = null,
    val pathLatitudes: DoubleArray? = null,
    val pathLongitudes: DoubleArray? = null
) {
    val hopCount: Int
        get() = (path.size - 1).coerceAtLeast(0)

    val isSamePlace: Boolean
        get() = from.equals(to, ignoreCase = true)
}
