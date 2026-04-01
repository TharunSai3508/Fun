package com.unistream.core.network

import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UrlDownloader @Inject constructor(
    private val client: OkHttpClient
) {

    suspend fun getBytes(url: String): ByteArray {
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .addHeader("Referer", url)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful)
            throw Exception("Download failed: HTTP ${response.code}")

        return response.body?.bytes() ?: throw Exception("Empty response body")
    }

    suspend fun getText(url: String): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .addHeader("Referer", url)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful)
            throw Exception("Download failed: HTTP ${response.code}")

        return response.body?.string() ?: throw Exception("Empty response body")
    }

    fun getContentType(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .head()
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.header("Content-Type")
        } catch (e: Exception) {
            null
        }
    }
}
