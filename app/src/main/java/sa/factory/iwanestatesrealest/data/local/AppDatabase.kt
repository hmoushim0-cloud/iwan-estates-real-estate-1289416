package sa.factory.iwanestatesrealest.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [IwanEstatesEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun real_estateDao(): IwanEstatesDao
}
