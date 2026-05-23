package interactivemapuniandes.model.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import interactivemapuniandes.model.entity.NoteEntity
import interactivemapuniandes.model.entity.ScheduleClassEntity
import interactivemapuniandes.model.entity.ScheduleEntity
import interactivemapuniandes.model.entity.TranslationEntity
import interactivemapuniandes.model.entity.VisitEntity

@Database(
    entities = [
        ScheduleEntity::class,
        ScheduleClassEntity::class,
        NoteEntity::class,
        VisitEntity::class,
        TranslationEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scheduleDao(): ScheduleDAO
    abstract fun noteDao(): NoteDAO
    abstract fun visitDao(): VisitDAO
    abstract fun translationDao(): TranslationDAO

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
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
