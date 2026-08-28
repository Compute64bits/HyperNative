package app.gamenative.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.gamenative.data.GameAccountPreference
import app.gamenative.data.PlatformAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface PlatformAccountDao {

    @Query("SELECT * FROM platform_accounts ORDER BY platform, is_primary DESC, display_name ASC")
    fun getAllFlow(): Flow<List<PlatformAccount>>

    @Query("SELECT * FROM platform_accounts WHERE platform = :platform ORDER BY is_primary DESC, display_name ASC")
    fun getByPlatformFlow(platform: String): Flow<List<PlatformAccount>>

    @Query("SELECT * FROM platform_accounts WHERE platform = :platform ORDER BY is_primary DESC, display_name ASC")
    suspend fun getByPlatform(platform: String): List<PlatformAccount>

    @Query("SELECT * FROM platform_accounts WHERE platform = :platform AND is_primary = 1 LIMIT 1")
    suspend fun getPrimary(platform: String): PlatformAccount?

    @Query("SELECT * FROM platform_accounts WHERE platform = :platform AND is_primary = 1 LIMIT 1")
    fun getPrimaryFlow(platform: String): Flow<PlatformAccount?>

    @Query("SELECT * FROM platform_accounts WHERE platform = :platform AND account_id = :accountId LIMIT 1")
    suspend fun getByAccountId(platform: String, accountId: String): PlatformAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: PlatformAccount): Long

    @Update
    suspend fun update(account: PlatformAccount)

    @Query("DELETE FROM platform_accounts WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM platform_accounts WHERE platform = :platform AND account_id = :accountId")
    suspend fun deleteByAccountId(platform: String, accountId: String)

    @Query("UPDATE platform_accounts SET is_primary = 0 WHERE platform = :platform")
    suspend fun clearPrimaryForPlatform(platform: String)

    @Query("UPDATE platform_accounts SET is_primary = 1 WHERE platform = :platform AND account_id = :accountId")
    suspend fun setPrimary(platform: String, accountId: String)

    @Query("SELECT COUNT(*) FROM platform_accounts WHERE platform = :platform")
    suspend fun countForPlatform(platform: String): Int

    @Query("SELECT COUNT(*) FROM platform_accounts WHERE platform = :platform")
    fun countForPlatformFlow(platform: String): Flow<Int>
}

@Dao
interface GameAccountPreferenceDao {

    @Query("SELECT * FROM game_account_preferences WHERE game_id = :gameId LIMIT 1")
    suspend fun getForGame(gameId: String): GameAccountPreference?

    @Query("SELECT * FROM game_account_preferences WHERE game_id = :gameId LIMIT 1")
    fun getForGameFlow(gameId: String): Flow<GameAccountPreference?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pref: GameAccountPreference)

    @Query("DELETE FROM game_account_preferences WHERE game_id = :gameId")
    suspend fun deleteForGame(gameId: String)
}
