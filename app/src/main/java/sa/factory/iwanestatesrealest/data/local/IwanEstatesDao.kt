package sa.factory.iwanestatesrealest.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IwanEstatesDao {
    @Query("SELECT * FROM real_estate_items_cache ORDER BY id ASC")
    fun observeAllItems(): Flow<List<IwanEstatesEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<IwanEstatesEntity>)

    @Query("DELETE FROM real_estate_items_cache")
    suspend fun clearAll()
}
