package interactivemapuniandes.model.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import interactivemapuniandes.model.entity.VisitEntity

@Dao
interface VisitDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(visit: VisitEntity)

    @Query("SELECT * FROM visits WHERE enteredAt >= :sinceMillis ORDER BY enteredAt DESC")
    suspend fun since(sinceMillis: Long): List<VisitEntity>

    @Query("SELECT placeCode, COUNT(*) AS total FROM visits WHERE enteredAt >= :sinceMillis GROUP BY placeCode ORDER BY total DESC LIMIT :limit")
    suspend fun topPlaces(sinceMillis: Long, limit: Int): List<PlaceCount>

    @Query("SELECT CAST(((enteredAt / 1000 / 3600) % 24) AS INTEGER) AS hour, COUNT(*) AS total FROM visits WHERE enteredAt >= :sinceMillis GROUP BY hour ORDER BY total DESC")
    suspend fun visitsByHour(sinceMillis: Long): List<HourCount>

    @Query("SELECT COALESCE(SUM(dwellSeconds), 0) FROM visits WHERE enteredAt >= :sinceMillis")
    suspend fun totalDwellSecondsSince(sinceMillis: Long): Int

    @Query("DELETE FROM visits WHERE enteredAt < :olderThanMillis")
    suspend fun prune(olderThanMillis: Long)
}

data class HourCount(val hour: Int, val total: Int)
