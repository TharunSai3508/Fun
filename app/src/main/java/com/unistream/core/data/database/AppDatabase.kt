package com.unistream.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.unistream.gallery.data.HiddenMediaEntity
import com.unistream.gallery.data.HiddenMediaDao
import com.unistream.streaming.data.PlaylistEntity
import com.unistream.streaming.data.PlaylistItemEntity
import com.unistream.streaming.data.WatchHistoryEntity
import com.unistream.streaming.data.PlaylistDao
import com.unistream.streaming.data.WatchHistoryDao
import com.unistream.webnovel.data.NovelEntity
import com.unistream.webnovel.data.ChapterEntity
import com.unistream.webnovel.data.BookmarkEntity
import com.unistream.webnovel.data.ReadingProgressEntity
import com.unistream.webnovel.data.NovelDao
import com.unistream.webnovel.data.ChapterDao
import com.unistream.webnovel.data.BookmarkDao

@Database(
    entities = [
        // Gallery
        HiddenMediaEntity::class,
        // Streaming
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        WatchHistoryEntity::class,
        // WebNovel
        NovelEntity::class,
        ChapterEntity::class,
        BookmarkEntity::class,
        ReadingProgressEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {

    // Gallery DAOs
    abstract fun hiddenMediaDao(): HiddenMediaDao

    // Streaming DAOs
    abstract fun playlistDao(): PlaylistDao
    abstract fun watchHistoryDao(): WatchHistoryDao

    // WebNovel DAOs
    abstract fun novelDao(): NovelDao
    abstract fun chapterDao(): ChapterDao
    abstract fun bookmarkDao(): BookmarkDao
}
