package com.unistream.core.di

import android.content.Context
import androidx.room.Room
import com.unistream.core.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.unistream.app/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "unistream_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    // Gallery DAOs
    @Provides
    fun provideHiddenMediaDao(db: AppDatabase) = db.hiddenMediaDao()

    // Streaming DAOs
    @Provides
    fun providePlaylistDao(db: AppDatabase) = db.playlistDao()

    @Provides
    fun provideWatchHistoryDao(db: AppDatabase) = db.watchHistoryDao()

    // WebNovel DAOs
    @Provides
    fun provideNovelDao(db: AppDatabase) = db.novelDao()

    @Provides
    fun provideChapterDao(db: AppDatabase) = db.chapterDao()

    @Provides
    fun provideBookmarkDao(db: AppDatabase) = db.bookmarkDao()
}
