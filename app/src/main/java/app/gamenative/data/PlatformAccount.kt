package app.gamenative.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a stored account for any platform (Steam, GOG, Epic, Amazon).
 * Credentials are stored on disk under [credentialsPath].
 */
@Entity(
    tableName = "platform_accounts",
    indices = [Index(value = ["platform", "account_id"], unique = true)],
)
data class PlatformAccount(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("id")
    val id: Int = 0,

    /** Platform identifier: "STEAM", "GOG", "EPIC", "AMAZON" */
    @ColumnInfo("platform")
    val platform: String,

    /** Platform-specific user ID (SteamId64, GOG userId, Epic accountId, Amazon deviceSerial) */
    @ColumnInfo("account_id")
    val accountId: String,

    /** Display name shown in the UI */
    @ColumnInfo("display_name")
    val displayName: String,

    /** Avatar URL or hash */
    @ColumnInfo("avatar_url")
    val avatarUrl: String = "",

    /** Whether this is the currently active/primary account for the platform */
    @ColumnInfo("is_primary")
    val isPrimary: Boolean = false,

    /** Directory where this account's credentials are stored */
    @ColumnInfo("credentials_path")
    val credentialsPath: String,

    /** Timestamp when the account was added */
    @ColumnInfo("added_at")
    val addedAt: Long = System.currentTimeMillis(),
)

/**
 * Stores which account should be used to launch a specific game.
 */
@Entity(
    tableName = "game_account_preferences",
    indices = [Index(value = ["game_id"])],
)
data class GameAccountPreference(
    /** Composite game ID, e.g. "STEAM_12345", "GOG_abc", "EPIC_xyz" */
    @PrimaryKey
    @ColumnInfo("game_id")
    val gameId: String,

    /** The platform account ID to use for this game */
    @ColumnInfo("account_id")
    val accountId: String,

    /** Platform identifier */
    @ColumnInfo("platform")
    val platform: String,
)
