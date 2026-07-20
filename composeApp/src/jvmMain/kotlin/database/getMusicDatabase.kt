package database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

/** Per-OS data directory: %APPDATA% on Windows, $XDG_DATA_HOME on Linux. */
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
    // Was java.io.tmpdir, which wiped favourites and history on every restart.
    val dbFile = File(riplayDataDirectory(), "riplay.db")
    return Room.databaseBuilder<MusicDatabase>(
        name = dbFile.absolutePath,
    )
}

val MusicDatabaseDesktop: MusicDatabaseDao
    get() = getRoomDatabase(getDesktopDatabaseBuilder()).getDao()

val DB = MusicDatabaseDesktop
