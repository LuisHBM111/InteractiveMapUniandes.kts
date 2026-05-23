package interactivemapuniandes.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val placeCode: String, // Edificio o restaurante (ej: "ML", "SD")
    val text: String,
    val createdAt: Long, // Epoch millis
    val pendingSync: Boolean = false // True si fue creada offline y todavia no se subio
)
