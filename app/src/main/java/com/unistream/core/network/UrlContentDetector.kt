package com.unistream.core.network

enum class UrlContentType {
    IMAGE,
    GIF,
    VIDEO,
    STREAM,
    HTML
}

object UrlContentDetector {

    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp", "svg")
    private val gifExtensions = setOf("gif")
    private val videoExtensions = setOf("mp4", "webm", "mkv", "avi", "mov", "flv", "wmv", "3gp")
    private val streamExtensions = setOf("m3u8", "mpd")

    fun detect(url: String): UrlContentType {
        val lower = url.lowercase().substringBefore("?").substringBefore("#")
        val ext = lower.substringAfterLast(".", "")

        return when {
            ext in gifExtensions -> UrlContentType.GIF
            ext in imageExtensions -> UrlContentType.IMAGE
            ext in videoExtensions -> UrlContentType.VIDEO
            ext in streamExtensions -> UrlContentType.STREAM
            else -> UrlContentType.HTML
        }
    }

    fun detectFromContentType(contentType: String?): UrlContentType {
        if (contentType == null) return UrlContentType.HTML
        val ct = contentType.lowercase()
        return when {
            ct.contains("image/gif") -> UrlContentType.GIF
            ct.contains("image/") -> UrlContentType.IMAGE
            ct.contains("video/") -> UrlContentType.VIDEO
            ct.contains("application/x-mpegurl") || ct.contains("application/dash+xml") -> UrlContentType.STREAM
            else -> UrlContentType.HTML
        }
    }
}
