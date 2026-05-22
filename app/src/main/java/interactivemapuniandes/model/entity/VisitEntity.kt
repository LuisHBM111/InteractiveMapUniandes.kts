package interactivemapuniandes.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "visits")
data class VisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val placeCode: String, // Codigo del edificio o restaurante
    val enteredAt: Long, // Epoch millis cuando entro
    val dwellSeconds: Int, // Tiempo aproximado dentro del lugar
    val source: String // "schedule" | "route" | "manual"
)
