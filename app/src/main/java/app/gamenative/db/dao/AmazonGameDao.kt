package app.gamenative.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.gamenative.data.AmazonGame
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Amazon games in the Room database.
 */
@Dao
interface AmazonGameDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<AmazonGame>)

    @Query("SELECT * FROM amazon_games WHERE product_id = :productId")
    suspend fun getByProductId(productId: String): AmazonGame?

    @Query("SELECT * FROM amazon_games WHERE app_id = :appId")
    suspend fun getByAppId(appId: Int): AmazonGame?

    /** Returns all installed Amazon games sorted by title. */
    @Query("SELECT * FROM amazon_games WHERE is_installed = 1 ORDER BY title ASC")
    suspend fun getInstalledGames(): List<AmazonGame>

    @Query("SELECT * FROM amazon_games ORDER BY title ASC")
    fun getAll(): Flow<List<AmazonGame>>

    @Query("SELECT * FROM amazon_games ORDER BY title ASC")
    suspend fun getAllAsList(): List<AmazonGame>

    @Query("SELECT * FROM amazon_games WHERE is_installed = 0")
    suspend fun getNonInstalledGames(): List<AmazonGame>

    @Query(
        "UPDATE amazon_games SET is_installed = 1, install_path = :path, install_size = :size, version_id = :versionId WHERE product_id = :productId",
    )
    suspend fun markAsInstalled(productId: String, path: String, size: Long, versionId: String)

    @Query("UPDATE amazon_games SET is_installed = 0, install_path = '', install_size = 0, version_id = '' WHERE product_id = :productId")
    suspend fun markAsUninstalled(productId: String)

    @Query("UPDATE amazon_games SET download_size = :size WHERE product_id = :productId")
    suspend fun updateDownloadSize(productId: String, size: Long)

    // Only delete non-installed games from DB — preserves any currently installed games.
    @Query("DELETE FROM amazon_games WHERE is_installed = 0")
    suspend fun deleteAllNonInstalledGames()

    @Query("SELECT * FROM amazon_games WHERE product_id IN (:productIds)")
    suspend fun getGamesByProductIds(productIds: List<String>): List<AmazonGame>

    @Query("SELECT * FROM amazon_games WHERE account_id = :accountId ORDER BY title ASC")
    suspend fun getGamesByAccountId(accountId: String): List<AmazonGame>

    @Query("SELECT DISTINCT account_id FROM amazon_games WHERE account_id != ''")
    suspend fun getAllAccountIds(): List<String>

    @Query("SELECT account_id FROM amazon_games WHERE product_id = (SELECT product_id FROM amazon_games WHERE app_id = :appId) AND account_id != ''")
    suspend fun getAccountIdsForGame(appId: Int): List<String>

    @Query("DELETE FROM amazon_games WHERE account_id = :accountId AND is_installed = 0")
    suspend fun deleteNonInstalledByAccountId(accountId: String)

    @Query("UPDATE amazon_games SET account_id = '' WHERE account_id = :accountId AND is_installed = 1")
    suspend fun unlinkInstalledByAccountId(accountId: String)

    /**
     * Upsert Amazon games while preserving install status and install path.
     * Supports multiple accounts owning the same game (one row per account).
     */
    @Transaction
    suspend fun upsertPreservingInstallStatus(games: List<AmazonGame>) {
        // Batch fetch all existing games in one query (avoids N+1)
        val productIds = games.map { it.productId }
        val existingGames = getGamesByProductIds(productIds)
        // Key by productId+accountId so each account keeps its own row
        val existingMap = existingGames.associateBy { "${it.productId}|${it.accountId}" }

        val toInsert = games.map { newGame ->
            val existing = existingMap["${newGame.productId}|${newGame.accountId}"]
            if (existing != null) {
                // Preserve appId and install-related fields from DB
                newGame.copy(
                    appId = existing.appId,  // Keep the existing appId
                    isInstalled = existing.isInstalled,
                    installPath = existing.installPath,
                    installSize = existing.installSize,
                    versionId = existing.versionId,
                    productSku = if (newGame.productSku.isNotEmpty()) newGame.productSku else existing.productSku,
                    lastPlayed = existing.lastPlayed,
                    playTimeMinutes = existing.playTimeMinutes,
                )
            } else {
                // New game - appId will be auto-generated (leave as 0)
                newGame
            }
        }

        // InsertAll with REPLACE strategy handles both insert and update
        insertAll(toInsert)
    }
}
