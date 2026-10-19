package com.example.candy.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "level_progress")
data class LevelProgressEntity(
    @PrimaryKey val levelNumber: Int,
    val stars: Int,
    val highScore: Int,
    val isUnlocked: Boolean
)

@Entity(tableName = "player_stats")
data class PlayerStatsEntity(
    @PrimaryKey val id: Int = 1,
    val coins: Int = 150,
    val hammers: Int = 3,
    val freeSwitches: Int = 3,
    val colorBombs: Int = 2,
    val extraMoves: Int = 2,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val language: String = "es",
    val darkModeEnabled: Boolean = false
)

@Dao
interface LevelProgressDao {
    @Query("SELECT * FROM level_progress ORDER BY levelNumber ASC")
    fun getAllProgress(): Flow<List<LevelProgressEntity>>

    @Query("SELECT * FROM level_progress WHERE levelNumber = :levelNumber")
    suspend fun getProgressForLevel(levelNumber: Int): LevelProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: LevelProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<LevelProgressEntity>)
}

@Dao
interface PlayerStatsDao {
    @Query("SELECT * FROM player_stats WHERE id = 1")
    fun getPlayerStats(): Flow<PlayerStatsEntity?>

    @Query("SELECT * FROM player_stats WHERE id = 1")
    suspend fun getPlayerStatsSync(): PlayerStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateStats(stats: PlayerStatsEntity)
}

@Database(
    entities = [LevelProgressEntity::class, PlayerStatsEntity::class],
    version = 3,
    exportSchema = false
)
abstract class CandyDatabase : RoomDatabase() {
    abstract fun levelProgressDao(): LevelProgressDao
    abstract fun playerStatsDao(): PlayerStatsDao

    companion object {
        @Volatile
        private var INSTANCE: CandyDatabase? = null

        fun getDatabase(context: Context): CandyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CandyDatabase::class.java,
                    "candy_pop_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
