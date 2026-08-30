package app.gamenative.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import app.gamenative.data.EpicGame
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * DAO for Epic games in the Room database
 */
@Dao
interface EpicGameDao {
    @Query("SELECT * FROM epic_games WHERE catalog_id IN (:catalogIds)")
    suspend fun getGamesByCatalogIds(catalogIds: List<String>): List<EpicGame>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(game: EpicGame)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<EpicGame>)

    @Update
    suspend fun update(game: EpicGame)

    @Delete
    suspend fun delete(game: EpicGame)

    @Query("UPDATE epic_games SET is_installed = 0, install_path='',install_size = 0 WHERE id = :appId")
    suspend fun uninstall(appId: Int)

    @Query("DELETE FROM epic_games WHERE id = :appId")
    suspend fun deleteById(appId: Int)

    @Query("SELECT * FROM epic_games WHERE id = :appId")
    suspend fun getById(appId: Int): EpicGame?

    /** Returns installed Epic games, excluding DLC and Unreal Engine content, sorted by title. */
    @Query("SELECT * FROM epic_games WHERE is_installed = 1 AND is_dlc = 0 AND namespace != 'ue' AND namespace != '89efe5924d3d467c839449ab6ab52e7f' ORDER BY title ASC")
    suspend fun getInstalledGames(): List<EpicGame>

    @Query("SELECT * FROM epic_games WHERE id IN (:gameIds)")
    suspend fun getGamesById(gameIds: List<Int>): List<EpicGame>

    @Query("SELECT * FROM epic_games WHERE catalog_id = :catalogId")
    suspend fun getByCatalogId(catalogId: String): EpicGame?

    @Query("SELECT * FROM epic_games WHERE app_name = :appName")
    suspend fun getByAppName(appName: String): EpicGame?

    // Note: '89efe5924d3d467c839449ab6ab52e7f' and 'ue' are the namespaces for Unreal Engine software/assets.
    @Query("SELECT * FROM epic_games WHERE is_dlc = 0 AND namespace != 'ue' AND namespace != '89efe5924d3d467c839449ab6ab52e7f' ORDER BY title ASC")
    fun getAll(): Flow<List<EpicGame>>

    @Query("SELECT * FROM epic_games WHERE is_dlc = 0 AND namespace != 'ue' AND namespace != '89efe5924d3d467c839449ab6ab52e7f' ORDER BY title ASC")
    suspend fun getAllAsList(): List<EpicGame>

    @Query("SELECT * FROM epic_games WHERE is_installed = :isInstalled ORDER BY title ASC")
    fun getByInstallStatus(isInstalled: Boolean): Flow<List<EpicGame>>

    @Query("SELECT * FROM epic_games WHERE is_installed = 0 ORDER BY title ASC")
    suspend fun getNonInstalledGames(): List<EpicGame>

    @Query("SELECT * FROM epic_games WHERE base_game_app_name = (SELECT catalog_id FROM epic_games WHERE id = :appId)")
    fun getDLCForTitle(appId: Int): Flow<List<EpicGame>>

    @Query("SELECT * FROM epic_games WHERE base_game_app_name IS NOT NULL AND is_dlc = 1")
    fun getAllDlcTitles(): Flow<List<EpicGame>>

    @Query("SELECT * FROM epic_games WHERE is_dlc = 0 AND namespace != 'ue' AND namespace != '89efe5924d3d467c839449ab6ab52e7f' AND title LIKE '%' || :searchQuery || '%' ORDER BY title ASC")
    fun searchByTitle(searchQuery: String): Flow<List<EpicGame>>

    // Only delete non-installed games from DB - Need to preserve any currently installed games.
    @Query("DELETE FROM epic_games WHERE is_installed = 0")
    suspend fun deleteAllNonInstalledGames()

    @Query("SELECT COUNT(*) FROM epic_games")
    fun getCount(): Flow<Int>

    @Query("SELECT catalog_id FROM epic_games")
    suspend fun getAllCatalogIds(): List<String>

    @Query("SELECT * FROM epic_games WHERE account_id = :accountId AND is_dlc = 0 AND namespace != 'ue' AND namespace != '89efe5924d3d467c839449ab6ab52e7f' ORDER BY title ASC")
    suspend fun getGamesByAccountId(accountId: String): List<EpicGame>

    @Query("SELECT DISTINCT account_id FROM epic_games WHERE account_id != ''")
    suspend fun getAllAccountIds(): List<String>

    @Query("SELECT account_id FROM epic_games WHERE catalog_id = (SELECT catalog_id FROM epic_games WHERE id = :appId) AND account_id != ''")
    suspend fun getAccountIdsForGame(appId: Int): List<String>

    @Query("DELETE FROM epic_games WHERE account_id = :accountId AND is_installed = 0")
    suspend fun deleteNonInstalledByAccountId(accountId: String)

    @Query("UPDATE epic_games SET account_id = '' WHERE account_id = :accountId AND is_installed = 1")
    suspend fun unlinkInstalledByAccountId(accountId: String)

    /**
     * Upsert Epic games while preserving install status and paths.
     * Supports multiple accounts owning the same game (one row per account).
     */
    @Transaction
    suspend fun upsertPreservingInstallStatus(games: List<EpicGame>) {

        val catalogIds = games.map { it.catalogId }
        val existingGames = getGamesByCatalogIds(catalogIds)
        // Key by catalogId+accountId so each account keeps its own row
        val existingMap = existingGames.associateBy { "${it.catalogId}|${it.accountId}" }

        val toInsert = games.map { newGame ->
            val existingGame = existingMap["${newGame.catalogId}|${newGame.accountId}"]
            if (existingGame != null) {
                newGame.copy(
                    id = existingGame.id,
                    isInstalled = existingGame.isInstalled,
                    installPath = existingGame.installPath,
                    installSize = existingGame.installSize,
                    lastPlayed = existingGame.lastPlayed,
                    playTime = existingGame.playTime,
                )
            } else {
                newGame
            }
        }
        insertAll(toInsert)
    }
}
