package com.uniandes.interactivemapuniandes.utils

import android.content.Context
import interactivemapuniandes.model.data.AppDatabase
import interactivemapuniandes.model.entity.VisitEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Sprint 4 - registra que el usuario visito un lugar para que InsightsActivity
// tenga datos reales. Fire-and-forget: si Room esta ocupado o falla, no rompe la UI.
private val tracker = CoroutineScope(SupervisorJob() + Dispatchers.IO)

fun logVisit(context: Context, placeCode: String, source: String, dwellSec: Int = 0) {
    val code = placeCode.trim().uppercase()
    if (code.isEmpty()) return
    val ctx = context.applicationContext
    tracker.launch {
        runCatching {
            AppDatabase.getInstance(ctx).visitDao().insert(
                VisitEntity(
                    placeCode = code,
                    enteredAt = System.currentTimeMillis(),
                    dwellSeconds = dwellSec,
                    source = source
                )
            )
        }
    }
}
