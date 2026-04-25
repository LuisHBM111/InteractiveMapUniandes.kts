package interactivemapuniandes.model.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import interactivemapuniandes.model.entity.ScheduleClassEntity
import interactivemapuniandes.model.entity.ScheduleEntity

@Database(
    entities = [
        ScheduleEntity::class,
        ScheduleClassEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scheduleDao(): ScheduleDAO

    companion object {
        //Sincronizacion entre threads
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "interactive_map_uniandes.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
