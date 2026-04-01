package com.unistream.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UniversalMediaResolver — the media extraction engine.
 *
 * Works exactly like SaveFrom / cobalt.tools internally:
 *  1. User gives any URL (webpage, share link, direct CDN link)
 *  2. Resolver fetches the page HTML with full browser headers
 *  3. Site-specific parsers extract direct, downloadable media URLs
 *  4. Falls back to generic og:video / og:image / <video> / <img> extraction
 *  5. Returns an ordered List<ResolvedMedia> — best quality first
 *
 * Supported sites:
 *  ✓ Direct media URLs (.mp4 / .jpg / .gif / .webp / .m3u8 / .mpd / etc.)
 *  ✓ Google Drive share links
 *  ✓ Reddit posts (via JSON API — no authentication required)
 *  ✓ Imgur (images and gifv)
 *  ✓ Redgifs (formerly Gfycat — via their public API)
 *  ✓ Streamable
 *  ✓ Pixeldrain
 *  ✓ Catbox.moe
 *  ✓ Any site with og:video / og:image meta tags
 *  ✓ Any HTML page with <video>, <source>, or <img> tags
 */
@Singleton
class UniversalMediaResolver @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Resolve [inputUrl] to one or more direct media URLs.
     * Returns an empty list if nothing could be extracted.
     */
    suspend fun resolve(inputUrl: String): List<ResolvedMedia> = withContext(Dispatchers.IO) {
        val url = inputUrl.trim()
        if (url.isBlank()) return@withContext emptyList()

        // 1. Already a direct media file — return immediately without fetching HTML
        val directMime = detectMimeFromUrl(url)
        if (directMime != null) {
            return@withContext listOf(
                ResolvedMedia(
                    url = url,
                    mimeType = directMime,
                    title = url.substringAfterLast("/").substringBefore("?")
                )
            )
        }

        val host = runCatching { URI(url).host ?: "" }.getOrDefault("")

        return@withContext runCatching {
            when {
                host.contains("drive.google.com") -> resolveGoogleDrive(url)
                host.contains("reddit.com") || host.contains("redd.it") -> resolveReddit(url)
                host.contains("imgur.com") -> resolveImgur(url)
                host.contains("redgifs.com") -> resolveRedgifs(url)
                host.contains("gfycat.com") -> resolveGfycat(url)
                host.contains("streamable.com") -> resolveStreamable(url)
                host.contains("pixeldrain.com") -> resolvePixeldrain(url)
                host.contains("catbox.moe") || host.contains("litterbox.catbox") -> listOf(
                    ResolvedMedia(url = url, mimeType = detectMimeFromUrl(url) ?: "video/mp4", title = url.substringAfterLast("/"))
                )
                else -> resolveGenericHtml(url)
            }
        }.getOrDefault(emptyList())
    }

    // ── Google Drive ───────────────────────────────────────────────────────

    private fun resolveGoogleDrive(url: String): List<ResolvedMedia> {
        val fileIdRegex = Regex("/file/d/([a-zA-Z0-9_-]+)")
        val fileId = fileIdRegex.find(url)?.groupValues?.get(1) ?: return emptyList()
        val directUrl = "https://drive.google.com/uc?export=download&id=$fileId"
        return listOf(
            ResolvedMedia(
                url = directUrl,
                mimeType = "video/mp4",
                quality = "Original",
                title = "Google Drive file"
            )
        )
    }

    // ── Reddit ─────────────────────────────────────────────────────────────
    // Reddit provides a public JSON API — append ".json" to any post URL

    private fun resolveReddit(url: String): List<ResolvedMedia> {
        val jsonUrl = url.split("?")[0].trimEnd('/') + ".json"
        val response = fetchJson(jsonUrl) ?: return resolveGenericHtml(url)

        val post = runCatching {
            response.getJSONObject(0)
                .getJSONObject("data")
                .getJSONArray("children")
                .getJSONObject(0)
                .getJSONObject("data")
        }.getOrNull() ?: return resolveGenericHtml(url)

        val results = mutableListOf<ResolvedMedia>()

        // Reddit-hosted video (dash stream with separate audio — we grab dash URL)
        val redditVideo = post.optJSONObject("media")?.optJSONObject("reddit_video")
        if (redditVideo != null) {
            val dashUrl = redditVideo.optString("dash_url").ifBlank { null }
            val fallbackUrl = redditVideo.optString("fallback_url").ifBlank { null }
            val duration = redditVideo.optLong("duration") * 1000L
            val title = post.optString("title")

            if (dashUrl != null) {
                results.add(ResolvedMedia(url = dashUrl, mimeType = "application/dash+xml", quality = "DASH", title = title, durationMs = duration))
            }
            if (fallbackUrl != null) {
                val height = redditVideo.optInt("height")
                val quality = if (height > 0) "${height}p" else "MP4"
                results.add(ResolvedMedia(url = fallbackUrl, mimeType = "video/mp4", quality = quality, title = title, durationMs = duration))
            }
        }

        // Imgur cross-post or gallery link
        val crosspostUrl = post.optString("url_overridden_by_dest").ifBlank { post.optString("url") }
        if (crosspostUrl.contains("imgur.com")) {
            results.addAll(resolveImgur(crosspostUrl))
        }

        // Direct image
        if (results.isEmpty()) {
            val preview = post.optJSONObject("preview")?.optJSONArray("images")?.optJSONObject(0)
            val imageUrl = preview?.optJSONObject("source")?.optString("url")?.replace("&amp;", "&")
            if (imageUrl != null) {
                results.add(ResolvedMedia(url = imageUrl, mimeType = guessMime(imageUrl), title = post.optString("title")))
            }
        }

        return results
    }

    // ── Imgur ──────────────────────────────────────────────────────────────

    private fun resolveImgur(url: String): List<ResolvedMedia> {
        // Handle .gifv (Imgur's mp4 wrapper)
        if (url.endsWith(".gifv")) {
            val mp4 = url.replace(".gifv", ".mp4")
            return listOf(ResolvedMedia(url = mp4, mimeType = "video/mp4", quality = "MP4", title = "Imgur GIF"))
        }

        // Direct image link
        val directMime = detectMimeFromUrl(url)
        if (directMime != null) {
            return listOf(ResolvedMedia(url = url, mimeType = directMime, title = url.substringAfterLast("/")))
        }

        // Fetch page and extract
        val html = fetchHtml(url) ?: return emptyList()
        val doc = Jsoup.parse(html, url)

        val results = mutableListOf<ResolvedMedia>()

        // og:video
        doc.select("meta[property=og:video]").firstOrNull()?.attr("content")?.let { videoUrl ->
            if (videoUrl.isNotBlank()) {
                results.add(ResolvedMedia(url = videoUrl, mimeType = "video/mp4", quality = "HD", title = doc.title()))
            }
        }

        // video source tag
        doc.select("video source[src]").forEach { el ->
            val src = el.attr("src")
            if (src.isNotBlank()) {
                results.add(ResolvedMedia(url = normalizeUrl(src, url), mimeType = el.attr("type").ifBlank { "video/mp4" }))
            }
        }

        // image fallback
        if (results.isEmpty()) {
            doc.select("meta[property=og:image]").firstOrNull()?.attr("content")?.let { imgUrl ->
                if (imgUrl.isNotBlank()) {
                    results.add(ResolvedMedia(url = imgUrl, mimeType = guessMime(imgUrl), title = doc.title()))
                }
            }
        }

        return results
    }

    // ── Redgifs ────────────────────────────────────────────────────────────
    // Redgifs has a public v2 API — no auth needed for public GIFs

    private fun resolveRedgifs(url: String): List<ResolvedMedia> {
        // Extract GIF id from URL: https://www.redgifs.com/watch/{id}
        val id = url.substringAfter("/watch/").substringBefore("/").substringBefore("?").ifBlank {
            url.substringAfterLast("/").substringBefore("?")
        }
        if (id.isBlank()) return resolveGenericHtml(url)

        val apiUrl = "https://api.redgifs.com/v2/gifs/$id"
        val json = fetchJson(apiUrl) ?: return resolveGenericHtml(url)

        val urls = runCatching { json.getJSONObject("gif").getJSONObject("urls") }.getOrNull()
            ?: return resolveGenericHtml(url)

        val results = mutableListOf<ResolvedMedia>()
        val title = runCatching { json.getJSONObject("gif").optString("description") }.getOrDefault("")
        val thumbUrl = runCatching { json.getJSONObject("gif").optString("thumbnail") }.getOrDefault("")

        urls.optString("hd").ifBlank { null }?.let { hdUrl ->
            results.add(ResolvedMedia(url = hdUrl, mimeType = "video/mp4", quality = "HD", title = title, thumbnailUrl = thumbUrl))
        }
        urls.optString("sd").ifBlank { null }?.let { sdUrl ->
            results.add(ResolvedMedia(url = sdUrl, mimeType = "video/mp4", quality = "SD", title = title, thumbnailUrl = thumbUrl))
        }
        urls.optString("gif").ifBlank { null }?.let { gifUrl ->
            results.add(ResolvedMedia(url = gifUrl, mimeType = "image/gif", quality = "GIF", title = title, thumbnailUrl = thumbUrl))
        }

        return results
    }

    // ── Gfycat (now merged to Redgifs) ────────────────────────────────────

    private fun resolveGfycat(url: String): List<ResolvedMedia> {
        // Gfycat redirects to Redgifs — try Redgifs API with the same ID
        val id = url.substringAfterLast("/").substringBefore("?").lowercase()
        if (id.isNotBlank()) {
            val redgifsResults = resolveRedgifs("https://www.redgifs.com/watch/$id")
            if (redgifsResults.isNotEmpty()) return redgifsResults
        }
        return resolveGenericHtml(url)
    }

    // ── Streamable ─────────────────────────────────────────────────────────

    private fun resolveStreamable(url: String): List<ResolvedMedia> {
        val id = url.substringAfterLast("/").substringBefore("?")
        if (id.isBlank()) return emptyList()

        val apiUrl = "https://api.streamable.com/videos/$id"
        val json = fetchJson(apiUrl) ?: return resolveGenericHtml(url)

        val title = json.optString("title")
        val files = json.optJSONObject("files") ?: return resolveGenericHtml(url)
        val results = mutableListOf<ResolvedMedia>()

        // mp4-high first, then mp4
        for (key in listOf("mp4-high", "mp4")) {
            val file = files.optJSONObject(key) ?: continue
            val videoUrl = file.optString("url").ifBlank { continue }
            val height = file.optInt("height")
            val quality = if (height > 0) "${height}p" else key
            results.add(
                ResolvedMedia(
                    url = if (videoUrl.startsWith("//")) "https:$videoUrl" else videoUrl,
                    mimeType = "video/mp4",
                    quality = quality,
                    title = title,
                    durationMs = file.optLong("duration") * 1000L,
                    sizeBytes = file.optLong("size")
                )
            )
        }

        return results
    }

    // ── Pixeldrain ─────────────────────────────────────────────────────────

    private fun resolvePixeldrain(url: String): List<ResolvedMedia> {
        // https://pixeldrain.com/u/{fileId} → direct API download
        val fileId = url.substringAfter("/u/").substringBefore("/").substringBefore("?")
        if (fileId.isBlank()) return resolveGenericHtml(url)

        val directUrl = "https://pixeldrain.com/api/file/$fileId"
        // Fetch metadata
        val meta = fetchJson("https://pixeldrain.com/api/file/$fileId/info")
        val mimeType = meta?.optString("mime_type")?.ifBlank { null } ?: detectMimeFromUrl(directUrl) ?: "application/octet-stream"
        val name = meta?.optString("name") ?: fileId

        return listOf(ResolvedMedia(url = directUrl, mimeType = mimeType, quality = "Original", title = name))
    }

    // ── Generic HTML Extractor ─────────────────────────────────────────────
    // Extracts media URLs from any HTML page using common patterns:
    //   og:video, og:image, twitter:player, <video>, <source>, <img>, JSON-LD

    private fun resolveGenericHtml(url: String): List<ResolvedMedia> {
        val html = fetchHtml(url) ?: return emptyList()
        val doc = Jsoup.parse(html, url)
        val results = mutableListOf<ResolvedMedia>()
        val pageTitle = doc.title()

        // og:video (highest confidence)
        for (sel in listOf("meta[property=og:video:url]", "meta[property=og:video]", "meta[name=twitter:player:stream]")) {
            val videoUrl = doc.select(sel).firstOrNull()?.attr("content")?.ifBlank { null } ?: continue
            val normalized = normalizeUrl(videoUrl, url)
            val mimeType = doc.select("meta[property=og:video:type]").firstOrNull()?.attr("content")?.ifBlank { null }
                ?: guessMime(normalized)
            results.add(ResolvedMedia(url = normalized, mimeType = mimeType, quality = "HD", title = pageTitle,
                thumbnailUrl = doc.select("meta[property=og:image]").firstOrNull()?.attr("content")))
        }

        // <video src> and <source src>
        doc.select("video[src], video source[src], source[src]").forEach { el ->
            val src = el.attr("src").ifBlank { null } ?: return@forEach
            val normalized = normalizeUrl(src, url)
            if (results.none { it.url == normalized }) {
                val mimeType = el.attr("type").ifBlank { guessMime(normalized) }
                val quality = el.attr("data-quality").ifBlank { el.attr("label") }
                results.add(ResolvedMedia(url = normalized, mimeType = mimeType, quality = quality, title = pageTitle))
            }
        }

        // og:image
        val ogImage = doc.select("meta[property=og:image], meta[name=twitter:image]")
            .firstOrNull()?.attr("content")?.ifBlank { null }
        if (ogImage != null && results.none { it.url == ogImage }) {
            results.add(ResolvedMedia(url = normalizeUrl(ogImage, url), mimeType = guessMime(ogImage), title = pageTitle))
        }

        // JSON-LD contentUrl
        doc.select("script[type='application/ld+json']").forEach { el ->
            runCatching {
                val json = JSONObject(el.data())
                val contentUrl = json.optString("contentUrl").ifBlank {
                    json.optString("embedUrl")
                }
                if (contentUrl.isNotBlank() && results.none { it.url == contentUrl }) {
                    results.add(ResolvedMedia(url = contentUrl, mimeType = guessMime(contentUrl), title = pageTitle))
                }
            }
        }

        // Large images from <img> tags as last resort (filter out icons/avatars)
        if (results.isEmpty()) {
            doc.select("img[src]").forEach { el ->
                val src = el.attr("src").ifBlank { null } ?: return@forEach
                val normalized = normalizeUrl(src, url)
                // Skip tiny UI images (data URIs, 1x1 trackers, etc.)
                if (normalized.startsWith("data:")) return@forEach
                val w = el.attr("width").toIntOrNull() ?: 1000
                val h = el.attr("height").toIntOrNull() ?: 1000
                if (w < 100 || h < 100) return@forEach
                results.add(ResolvedMedia(url = normalized, mimeType = guessMime(normalized), title = pageTitle))
            }
        }

        return results.distinctBy { it.url }
    }

    // ── HTTP helpers ───────────────────────────────────────────────────────

    private val browserHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language" to "en-US,en;q=0.9",
        "Accept-Encoding" to "gzip, deflate, br"
    )

    private fun buildRequest(url: String, extraHeaders: Map<String, String> = emptyMap()): Request {
        val builder = Request.Builder().url(url)
        (browserHeaders + extraHeaders).forEach { (k, v) -> builder.header(k, v) }
        return builder.build()
    }

    private fun fetchHtml(url: String): String? = runCatching {
        okHttpClient.newCall(buildRequest(url)).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        }
    }.getOrNull()

    private fun fetchJson(url: String, extraHeaders: Map<String, String> = emptyMap()): JSONObject? =
        runCatching {
            val req = buildRequest(url, mapOf("Accept" to "application/json") + extraHeaders)
            okHttpClient.newCall(req).execute().use { response ->
                if (!response.isSuccessful) null
                else response.body?.string()?.let { JSONObject(it) }
            }
        }.getOrNull()

    // ── URL / MIME helpers ─────────────────────────────────────────────────

    /**
     * Returns a MIME type if [url] path ends with a known media extension, else null.
     * Used to short-circuit HTML fetching for direct CDN links.
     */
    fun detectMimeFromUrl(url: String): String? {
        val path = url.substringBefore("?").substringBefore("#").lowercase()
        return when {
            path.endsWith(".mp4") -> "video/mp4"
            path.endsWith(".webm") -> "video/webm"
            path.endsWith(".mkv") -> "video/x-matroska"
            path.endsWith(".mov") -> "video/quicktime"
            path.endsWith(".avi") -> "video/x-msvideo"
            path.endsWith(".m3u8") -> "application/x-mpegURL"
            path.endsWith(".mpd") -> "application/dash+xml"
            path.endsWith(".ts") -> "video/mp2t"
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            path.endsWith(".png") -> "image/png"
            path.endsWith(".gif") -> "image/gif"
            path.endsWith(".webp") -> "image/webp"
            path.endsWith(".bmp") -> "image/bmp"
            path.endsWith(".avif") -> "image/avif"
            else -> null
        }
    }

    private fun guessMime(url: String): String =
        detectMimeFromUrl(url) ?: "application/octet-stream"

    private fun normalizeUrl(href: String, baseUrl: String): String {
        return when {
            href.startsWith("http") -> href
            href.startsWith("//") -> "https:$href"
            href.startsWith("/") -> {
                val base = runCatching { URI(baseUrl) }.getOrNull()
                if (base != null) "${base.scheme}://${base.host}$href" else href
            }
            else -> "$baseUrl/$href"
        }
    }
}
