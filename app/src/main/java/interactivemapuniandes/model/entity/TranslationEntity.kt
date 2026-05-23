package interactivemapuniandes.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Sprint 4 - historial de traducciones, espejo del TranslationEntry en iOS.
@Entity(tableName = "translations")
data class TranslationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val original: String,
    val translated: String,
    val createdAt: Long // Epoch millis
)
