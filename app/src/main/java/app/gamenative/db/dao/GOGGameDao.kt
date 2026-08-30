package app.gamenative.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import app.gamenative.data.GOGGame
import kotlinx.coroutines.flow.Flow

/**
 * DAO for GOG games in the Room database
 */
@Dao
interface GOGGameDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(game: GOGGame)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<GOGGame>)

    @Update
    suspend fun update(game: GOGGame)

    @Delete
    suspend fun delete(game: GOGGame)

    @Query("DELETE FROM gog_games WHERE id = :gameId")
    suspend fun deleteById(gameId: String)

    @Query("SELECT * FROM gog_games WHERE id = :gameId")
    suspend fun getById(gameId: String): GOGGame?

    @Query("SELECT * FROM gog_games WHERE id = :gameId AND account_id = :accountId LIMIT 1")
    suspend fun getByIdAndAccountId(gameId: String, accountId: String): GOGGame?

    @Query("SELECT * FROM gog_games WHERE id IN (:gameIds)")
    suspend fun getGamesByGameIds(gameIds: List<String>): List<GOGGame>

    @Query("SELECT * FROM gog_games WHERE exclude = 0 ORDER BY title ASC")
    fun getAll(): Flow<List<GOGGame>>

    @Query("SELECT * FROM gog_games WHERE exclude = 0 ORDER BY title ASC")
    suspend fun getAllAsList(): List<GOGGame>

    @Query("SELECT * FROM gog_games WHERE is_installed = :isInstalled AND exclude = 0 ORDER BY title ASC")
    fun getByInstallStatus(isInstalled: Boolean): Flow<List<GOGGame>>

    /** Returns all installed GOG games, excluding excluded entries, sorted by title. */
    @Query("SELECT * FROM gog_games WHERE is_installed = 1 AND exclude = 0 ORDER BY title ASC")
    suspend fun getInstalledGames(): List<GOGGame>

    @Query("SELECT * FROM gog_games WHERE is_installed = 0 AND exclude = 0")
    suspend fun getNonInstalledGames(): List<GOGGame>

    @Query("SELECT * FROM gog_games WHERE exclude = 0 AND title LIKE '%' || :searchQuery || '%' ORDER BY title ASC")
    fun searchByTitle(searchQuery: String): Flow<List<GOGGame>>

    @Query("DELETE FROM gog_games WHERE is_installed = 0")
    suspend fun deleteAllNonInstalledGames()

    @Query("SELECT COUNT(*) FROM gog_games WHERE exclude = 0")
    fun getCount(): Flow<Int>

    @Query("SELECT DISTINCT id FROM gog_games")
    suspend fun getAllGameIdsIncludingExcluded(): List<String>

    @Query("SELECT DISTINCT id FROM gog_games WHERE exclude = 0 AND vertical_cover_url = ''")
    suspend fun getGameIdsMissingVerticalCover(): List<String>

    @Query("UPDATE gog_games SET vertical_cover_url = :url WHERE id = :gameId")
    suspend fun updateVerticalCoverUrl(gameId: String, url: String)

    @Query("SELECT * FROM gog_games WHERE account_id = :accountId AND exclude = 0 ORDER BY title ASC")
    suspend fun getGamesByAccountId(accountId: String): List<GOGGame>

    @Query("SELECT DISTINCT account_id FROM gog_games WHERE account_id != ''")
    suspend fun getAllAccountIds(): List<String>

    @Query("SELECT account_id FROM gog_games WHERE id = :gameId AND account_id != ''")
    suspend fun getAccountIdsForGame(gameId: String): List<String>

    @Query("DELETE FROM gog_games WHERE account_id = :accountId AND is_installed = 0")
    suspend fun deleteNonInstalledByAccountId(accountId: String)

    @Query("UPDATE gog_games SET account_id = '' WHERE account_id = :accountId AND is_installed = 1")
    suspend fun unlinkInstalledByAccountId(accountId: String)

    /**
     * Upsert GOG games while preserving install status and paths.
     * Supports multiple accounts owning the same game (one row per account).
     */
    @Transaction
    suspend fun upsertPreservingInstallStatus(games: List<GOGGame>) {
        // Batch fetch existing games to avoid N+1 queries
        val gameIds = games.map { it.id }.distinct()
        val existingGames = getGamesByGameIds(gameIds)
        // Key by "id|accountId" to support multiple accounts per game
        val existingMap = existingGames.associateBy { "${it.id}|${it.accountId}" }

        val toInsert = games.map { newGame ->
            val existingGame = existingMap["${newGame.id}|${newGame.accountId}"]
            if (existingGame != null) {
                // Preserve internalId and installation fields from existing game
                newGame.copy(
                    internalId = existingGame.internalId,
                    isInstalled = existingGame.isInstalled,
                    installPath = existingGame.installPath,
                    installSize = existingGame.installSize,
                    lastPlayed = existingGame.lastPlayed,
                    playTime = existingGame.playTime,
                )
            } else {
                // New game for this account, insert as-is (internalId=0 auto-generated)
                newGame
            }
        }
        insertAll(toInsert)
    }
}
