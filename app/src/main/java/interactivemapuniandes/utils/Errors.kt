package com.uniandes.interactivemapuniandes.utils

import android.content.Context
import retrofit2.HttpException
import java.io.IOException

// Sprint 4 - convertir errores crudos del backend en mensajes que el usuario entienda.
// Sprint 3 feedback: el usuario veia "HTTP 404" o "java.net.SocketTimeout" en lugar de
// "sin conexion" o "no lo encontramos".
fun friendlyError(context: Context, e: Throwable): String {
    if (!NetworkMonitor.isOnline(context)) return "Sin conexion, intenta de nuevo cuando vuelva el internet"
    return when (e) {
        is HttpException -> when (e.code()) {
            401 -> "Sesion expirada, inicia sesion otra vez"
            403 -> "No tienes permiso para esto"
            404 -> "No encontramos eso"
            in 500..599 -> "Error del servidor, intenta luego"
            else -> "Algo salio mal (${e.code()})"
        }
        is IOException -> "Sin conexion, intenta de nuevo"
        else -> "Algo salio mal"
    }
}
