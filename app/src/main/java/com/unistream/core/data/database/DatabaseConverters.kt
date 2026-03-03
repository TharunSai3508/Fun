package com.unistream.core.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DatabaseConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(list: List<String>?): String? =
        list?.let { gson.toJson(it) }

    @TypeConverter
    fun toStringList(json: String?): List<String>? =
        json?.let { gson.fromJson(it, object : TypeToken<List<String>>() {}.type) }

    @TypeConverter
    fun fromLongList(list: List<Long>?): String? =
        list?.let { gson.toJson(it) }

    @TypeConverter
    fun toLongList(json: String?): List<Long>? =
        json?.let { gson.fromJson(it, object : TypeToken<List<Long>>() {}.type) }
}
