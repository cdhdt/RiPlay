package database

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.RoomWarnings.Companion.QUERY_MISMATCH
import androidx.room.Upsert
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import database.entities.Album
import database.entities.Artist
import database.entities.Event
import database.entities.Format
import database.entities.Lyrics
import database.entities.Playlist
import database.entities.PlaylistPreview
import database.entities.Queues
import database.entities.SearchQuery
import database.entities.Song
import database.entities.SongAlbumMap
import database.entities.SongArtistMap
import database.entities.SongEntity
import database.entities.SongPlaylistMap
import it.fast4x.riplay.commonutils.LOCAL_KEY_PREFIX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [
        Album::class,
        Artist::class,
        Event::class,
        Format::class,
        Lyrics::class,
        //QueuedMediaItem::class, //TODO: implement
        Playlist::class,
        Queues::class,
        SearchQuery::class,
        Song::class,
        SongAlbumMap::class,
        SongArtistMap::class,
        SongPlaylistMap::class,
    ],
    version = 24,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 23, to = 24)
    ]
)
@ConstructedBy(MusicDatabaseConstructor::class)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun getDao(): MusicDatabaseDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object MusicDatabaseConstructor : RoomDatabaseConstructor<MusicDatabase>

@Dao
interface MusicDatabaseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: Song): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: Album): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: SongAlbumMap): Long

    @Upsert
    suspend fun upsert(item: Song): Long

    @Upsert
    suspend fun upsert(item: Album): Long

    @Upsert
    suspend fun upsert(item: SongAlbumMap): Long

    @Delete
    suspend fun delete(item: Song)

    @Query("SELECT * FROM Song")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM Album")
    fun getAllAlbums(): Flow<List<Album>>

    @Query("SELECT * FROM Song WHERE id = :id")
    suspend fun getSong(id: String): Song?

    @SuppressWarnings(QUERY_MISMATCH)
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.title COLLATE NOCASE ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByTitleAsc(): Flow<List<SongEntity>>

    @Query("SELECT * FROM Album WHERE id = :id")
    fun album(id: String): Flow<Album?>

    // --- Playlists ------------------------------------------------------------------------
    // The Playlist and SongPlaylistMap entities have been in the schema all along; nothing read
    // or wrote them from this DAO, which is why the desktop library's Playlists tab was empty.

    /** LEFT JOIN, so a playlist with no songs still appears — with songCount 0. */
    @Query(
        "SELECT Playlist.*, COUNT(SongPlaylistMap.songId) AS songCount FROM Playlist " +
            "LEFT JOIN SongPlaylistMap ON Playlist.id = SongPlaylistMap.playlistId " +
            "GROUP BY Playlist.id ORDER BY Playlist.name COLLATE NOCASE ASC"
    )
    fun playlistPreviewsByNameAsc(): Flow<List<PlaylistPreview>>

    @Query(
        "SELECT Song.* FROM Song JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
            "WHERE SongPlaylistMap.playlistId = :playlistId ORDER BY SongPlaylistMap.position ASC"
    )
    fun playlistSongs(playlistId: Long): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: Playlist): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: SongPlaylistMap): Long

    @Query("UPDATE Playlist SET name = :name WHERE id = :id")
    suspend fun renamePlaylist(id: Long, name: String)

    /** SongPlaylistMap rows go with it: the foreign key is ON DELETE CASCADE. */
    @Query("DELETE FROM Playlist WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM SongPlaylistMap WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeFromPlaylist(playlistId: Long, songId: String)

    /** Append position for the next song. COALESCE covers the empty playlist, where MAX is NULL. */
    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM SongPlaylistMap WHERE playlistId = :playlistId")
    suspend fun nextPositionIn(playlistId: Long): Int

    // --- Artists --------------------------------------------------------------------------

    /** Followed artists only: bookmarkedAt is what the app sets when one is followed. */
    @Query("SELECT * FROM Artist WHERE bookmarkedAt IS NOT NULL ORDER BY name COLLATE NOCASE ASC")
    fun bookmarkedArtists(): Flow<List<Artist>>
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<MusicDatabase>
): MusicDatabase {
    return builder
        //.addMigrations(MIGRATIONS)
        //.fallbackToDestructiveMigrationOnDowngrade()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}


