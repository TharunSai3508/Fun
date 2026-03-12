package com.unistream.core.network

enum class UrlContentType {
    IMAGE,
    GIF,
    VIDEO,
    STREAM,
    HTML
}

object UrlContentDetector {

    fun detect(url: String): UrlContentType {

        return when {
            url.endsWith(".jpg") || url.endsWith(".png") -> UrlContentType.IMAGE
            url.endsWith(".gif") -> UrlContentType.GIF
            url.endsWith(".mp4") -> UrlContentType.VIDEO
            url.endsWith(".m3u8") -> UrlContentType.STREAM
            else -> UrlContentType.HTML
        }
    }
}