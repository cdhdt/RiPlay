package database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

/**
 * Répertoire où RiPlay range ses données (base, et plus tard cache et réglages).
 *
 * Respecte les conventions de chaque OS plutôt que d'écrire dans le dossier personnel :
 * `%APPDATA%` sous Windows, `$XDG_DATA_HOME` (défaut `~/.local/share`) sous Linux.
 */
fun riplayDataDirectory(): File {
    val os = System.getProperty("os.name").orEmpty().lowercase()
    val home = System.getProperty("user.home")

    val base = when {
        os.contains("win") -> System.getenv("APPDATA")?.let(::File)
        os.contains("mac") -> File(home, "Library/Application Support")
        else -> System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }?.let(::File)
            ?: File(home, ".local/share")
    } ?: File(home)

    return File(base, "RiPlay").apply { mkdirs() }
}

fun getDesktopDatabaseBuilder(): RoomDatabase.Builder<MusicDatabase> {
    // ponytail: était java.io.tmpdir, donc favoris et historique disparaissaient au redémarrage.
    val dbFile = File(riplayDataDirectory(), "riplay.db")
    return Room.databaseBuilder<MusicDatabase>(
        name = dbFile.absolutePath,
    )
}

val MusicDatabaseDesktop: MusicDatabaseDao
    get() = getRoomDatabase(getDesktopDatabaseBuilder()).getDao()

val DB = MusicDatabaseDesktop
