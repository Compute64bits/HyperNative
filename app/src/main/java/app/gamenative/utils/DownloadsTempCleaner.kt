package app.gamenative.utils

import android.os.Environment
import timber.log.Timber
import java.io.File

/**
 * Cleans up temporary directories created by Wine/Windows applications
 * in the device's public Downloads folder.
 *
 * Wine containers map D: to `Downloads/HyperNative/`, but some Windows
 * applications bypass this and write directly to the parent Downloads
 * directory, leaving behind short-lived temp folders.
 */
object DownloadsTempCleaner {

    private val KNOWN_DIRS = setOf(
        "HyperNative",
        "Winlator",
        "Pluvia",
        "Android",
    )

    private val TEMP_DIR_PATTERN = Regex(
        // Short alphanumeric names (1-12 chars) that mix letters and digits,
        // or are purely numeric with <= 6 chars (e.g. shader caches like "0a", "1b3f")
        "^([a-zA-Z]{1,6}[0-9]{1,6}|[0-9]{1,6}[a-zA-Z]{1,6}|[0-9]{1,6})$"
    )

    /**
     * Scans the public Downloads folder and deletes directories that
     * look like Wine/Windows temp artifacts.
     *
     * @param maxAgeMs Only delete directories older than this age (default: 2 hours).
     *                 Set to 0 to delete regardless of age.
     * @return Number of directories deleted.
     */
    fun cleanUp(maxAgeMs: Long = 2 * 60 * 60 * 1000L): Int {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.isDirectory) return 0

        val now = System.currentTimeMillis()
        var deleted = 0

        val children = downloadsDir.listFiles() ?: return 0
        for (child in children) {
            if (!child.isDirectory) continue
            val name = child.name

            // Skip known application directories
            if (name in KNOWN_DIRS) continue

            // Skip hidden directories
            if (name.startsWith(".")) continue

            // Check if it matches temp directory pattern
            if (!matchesTempPattern(name)) continue

            // Check age if maxAgeMs > 0
            if (maxAgeMs > 0) {
                val age = now - child.lastModified()
                if (age < maxAgeMs) continue
            }

            // Delete recursively
            try {
                if (deleteRecursively(child)) {
                    deleted++
                    Timber.d("[DownloadsTempCleaner] Deleted temp dir: $name")
                }
            } catch (e: Exception) {
                Timber.w(e, "[DownloadsTempCleaner] Failed to delete: $name")
            }
        }

        if (deleted > 0) {
            Timber.i("[DownloadsTempCleaner] Cleaned up $deleted temp directories from Downloads")
        }
        return deleted
    }

    /**
     * Takes a snapshot of directory names currently in Downloads.
     * Used to detect new directories created during a game session.
     */
    fun snapshot(): Set<String> {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.isDirectory) return emptySet()
        return downloadsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()
    }

    /**
     * Deletes directories in Downloads that appeared since the given snapshot
     * and match the temp directory pattern. Does NOT delete known directories.
     *
     * @param before Snapshot of directory names before the game session.
     */
    fun cleanUpSince(before: Set<String>) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.isDirectory) return

        val children = downloadsDir.listFiles() ?: return
        var deleted = 0

        for (child in children) {
            if (!child.isDirectory) continue
            val name = child.name

            // Only delete directories that didn't exist before
            if (name in before) continue

            // Skip known directories
            if (name in KNOWN_DIRS) continue
            if (name.startsWith(".")) continue

            // Match any temp-like pattern (more aggressive since we know these are new)
            if (!matchesTempPattern(name)) continue

            try {
                if (deleteRecursively(child)) {
                    deleted++
                    Timber.d("[DownloadsTempCleaner] Deleted session temp dir: $name")
                }
            } catch (e: Exception) {
                Timber.w(e, "[DownloadsTempCleaner] Failed to delete: $name")
            }
        }

        if (deleted > 0) {
            Timber.i("[DownloadsTempCleaner] Cleaned up $deleted session temp directories")
        }
    }

    private fun matchesTempPattern(name: String): Boolean {
        if (name.isEmpty() || name.length > 12) return false
        return TEMP_DIR_PATTERN.matches(name)
    }

    private fun deleteRecursively(file: File): Boolean {
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children != null) {
                for (child in children) {
                    deleteRecursively(child)
                }
            }
        }
        return file.delete()
    }
}
