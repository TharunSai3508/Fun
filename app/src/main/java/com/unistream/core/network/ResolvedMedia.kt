package com.unistream.core.network

/**
 * A single resolved media item extracted from a URL.
 *
 * A webpage URL may resolve to multiple media items (e.g. a Reddit post with both
 * a video and its audio track, or an Imgur album with several images). The resolver
 * returns an ordered list — best quality or most relevant first.
 */
data class ResolvedMedia(
    /** Direct, playable/downloadable URL for this media. */
    val url: String,

    /** MIME type: "video/mp4", "image/jpeg", "image/gif", "application/x-mpegURL", etc. */
    val mimeType: String,

    /** Human-readable quality label: "1080p", "720p", "HD", "SD", "" */
    val quality: String = "",

    /** Display title derived from page or filename. */
    val title: String = "",

    /** Optional thumbnail for preview in the UI. */
    val thumbnailUrl: String? = null,

    /** Duration in milliseconds — 0 if unknown or not applicable. */
    val durationMs: Long = 0L,

    /** File size estimate in bytes — 0 if unknown. */
    val sizeBytes: Long = 0L
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/") || mimeType == "application/x-mpegURL"
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isGif: Boolean get() = mimeType == "image/gif"
    val isStream: Boolean get() = mimeType == "application/x-mpegURL" || mimeType == "application/dash+xml"

    /** Extension inferred from MIME type for saving files. */
    val fileExtension: String get() = when (mimeType) {
        "video/mp4" -> "mp4"
        "video/webm" -> "webm"
        "video/x-matroska" -> "mkv"
        "application/x-mpegURL" -> "m3u8"
        "application/dash+xml" -> "mpd"
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        else -> mimeType.substringAfter("/").substringBefore(";").ifBlank { "bin" }
    }
}
