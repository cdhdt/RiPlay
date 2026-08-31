package it.fast4x.riplay.enums

enum class PageType {
    ARTIST,
    ALBUM,
    PLAYLIST,
    /** A playlist stored in Room, keyed by its autoincrement id — not a YouTube browseId. */
    LOCAL_PLAYLIST,
    MOOD,
    QUICKPICS
}