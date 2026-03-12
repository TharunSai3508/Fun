package com.unistream.core.network

import okhttp3.OkHttpClient
import okhttp3.Request

object UrlDownloader {

    private val client = OkHttpClient()

    suspend fun getBytes(url: String): ByteArray {
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful)
            throw Exception("Download failed")

        return response.body!!.bytes()
    }

    suspend fun getText(url: String): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful)
            throw Exception("Download failed")

        return response.body!!.string()
    }
}