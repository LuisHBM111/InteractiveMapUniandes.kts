package interactivemapuniandes.model.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import interactivemapuniandes.model.entity.TranslationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDAO {

    @Query("SELECT * FROM translations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TranslationEntity>>

    @Query("SELECT * FROM translations ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recent(limit: Int = 50): List<TranslationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TranslationEntity): Long

    @Query("DELETE FROM translations WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM translations")
    suspend fun clearAll()
}
