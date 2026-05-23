package interactivemapuniandes.model.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import interactivemapuniandes.model.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDAO {

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE placeCode = :code ORDER BY createdAt DESC")
    fun observeForPlace(code: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE pendingSync = 1")
    suspend fun pendingSyncBatch(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity): Long

    @Query("UPDATE notes SET pendingSync = 0 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT placeCode, COUNT(*) AS total FROM notes GROUP BY placeCode ORDER BY total DESC LIMIT :limit")
    suspend fun topPlacesByNoteCount(limit: Int): List<PlaceCount>

    @Query("SELECT COUNT(*) FROM notes WHERE pendingSync = 1")
    suspend fun pendingCount(): Int
}

data class PlaceCount(val placeCode: String, val total: Int)
