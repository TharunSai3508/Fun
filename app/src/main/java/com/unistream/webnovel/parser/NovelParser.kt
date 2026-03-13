package com.unistream.webnovel.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedNovel(
    val title: String,
    val author: String,
    val description: String,
    val coverUrl: String?,
    val chapters: List<ParsedChapter>
)

data class ParsedChapter(
    val number: Float,
    val title: String,
    val url: String,
    val content: String = ""  // Empty until downloaded
)

@Singleton
class NovelParser @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    // Full browser headers to bypass bot-detection on Webnovel / WuxiaWorld
    private fun buildRequest(url: String): Request = Request.Builder()
        .url(url)
        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
        .header("Accept-Language", "en-US,en;q=0.9")
        .header("Accept-Encoding", "gzip, deflate, br")
        .header("Connection", "keep-alive")
        .header("Upgrade-Insecure-Requests", "1")
        .header("Sec-Fetch-Dest", "document")
        .header("Sec-Fetch-Mode", "navigate")
        .header("Sec-Fetch-Site", "none")
        .header("Cache-Control", "max-age=0")
        .build()

    private fun fetchHtml(url: String): String? = runCatching {
        okHttpClient.newCall(buildRequest(url)).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        }
    }.getOrNull()

    private fun htmlToDoc(html: String, baseUrl: String): Document = Jsoup.parse(html, baseUrl)

    suspend fun parseNovelPage(url: String): ParsedNovel? = withContext(Dispatchers.IO) {
        try {
            val html = fetchHtml(url) ?: return@withContext null
            val doc = htmlToDoc(html, url)
            val host = runCatching { java.net.URL(url).host }.getOrDefault("")

            when {
                host.contains("webnovel.com") -> parseWebnovel(doc, html, url)
                host.contains("royalroad.com") -> parseRoyalRoad(doc, url)
                host.contains("wuxiaworld.com") -> parseWuxiaWorld(doc, html, url)
                host.contains("scribblehub.com") -> parseScribbleHub(doc, url)
                host.contains("novelupdates.com") -> parseNovelUpdates(doc, url)
                else -> parseGeneric(doc, url)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun parseChapterContent(chapterUrl: String): String = withContext(Dispatchers.IO) {
        try {
            val html = fetchHtml(chapterUrl) ?: return@withContext ""
            val doc = htmlToDoc(html, chapterUrl)
            val host = runCatching { java.net.URL(chapterUrl).host }.getOrDefault("")

            when {
                host.contains("webnovel.com") -> extractWebnovelChapter(doc, html)
                host.contains("royalroad.com") -> extractRoyalRoadChapter(doc)
                host.contains("scribblehub.com") -> extractScribbleHubChapter(doc)
                else -> extractGenericChapter(doc)
            }
        } catch (e: Exception) {
            "" // Return empty — caller will not mark chapter as downloaded if blank
        }
    }

    // ── Webnovel.com Parser ──────────────────────────────────────────────
    // Webnovel is a React/Next.js SPA. Raw HTML from the server has an empty DOM.
    // Next.js embeds the full page state as JSON in <script id="__NEXT_DATA__">.
    // We extract that JSON instead of relying on CSS selectors against an empty DOM.
    private fun parseWebnovel(doc: Document, html: String, url: String): ParsedNovel {
        val nextData = extractNextData(html)
        if (nextData != null) {
            return parseWebnovelFromNextData(nextData, url)
        }

        // CSS fallback (multiple selector candidates for markup changes)
        val title = listOf(
            ".pt4.pb4.oh.mb4 h1", "h1.novel-title", ".book-name",
            "h1[class*='title']", "h1"
        ).firstNotNullOfOrNull { sel -> doc.select(sel).text().ifBlank { null } } ?: "Unknown"

        val author = listOf(".author-name", ".author a", "a[href*='/author/']")
            .firstNotNullOfOrNull { sel -> doc.select(sel).text().ifBlank { null } } ?: "Unknown"

        val description = listOf(".j_synopsis p", ".synopsis p", "[class*='synopsis'] p", ".description p")
            .flatMap { sel -> doc.select(sel).map { it.text() } }
            .filter { it.isNotBlank() }.joinToString("\n")

        val coverUrl = listOf(".g_thumb img", ".book-img img", "img[class*='cover']", "img[class*='thumb']")
            .firstNotNullOfOrNull { sel -> doc.select(sel).attr("src").ifBlank { null } }

        val chapterLinks = doc.select(".chapter-item a, .content-list a[href*='/chapter/'], a[href*='/chapter/']")
        val chapters = chapterLinks.mapIndexed { i, el ->
            ParsedChapter(number = (i + 1).toFloat(), title = el.text().ifBlank { "Chapter ${i + 1}" }, url = normalizeUrl(el.attr("href"), url))
        }
        return ParsedNovel(title, author, description, coverUrl, chapters)
    }

    private fun parseWebnovelFromNextData(json: JSONObject, baseUrl: String): ParsedNovel {
        val props = json.optJSONObject("props")?.optJSONObject("pageProps")
            ?: json.optJSONObject("props") ?: json

        val bookInfo = props.optJSONObject("bookInfo")
            ?: props.optJSONObject("novel") ?: props.optJSONObject("book")

        val title = bookInfo?.optString("bookName")
            ?: bookInfo?.optString("name") ?: bookInfo?.optString("title") ?: "Unknown"

        val author = bookInfo?.optJSONArray("authorItems")?.optJSONObject(0)?.optString("name")
            ?: bookInfo?.optString("authorName") ?: "Unknown"

        val description = bookInfo?.optString("description") ?: bookInfo?.optString("summary") ?: ""
        val coverUrl = bookInfo?.optString("coverUrl") ?: bookInfo?.optString("cover")

        val chapterList = props.optJSONArray("chapterList") ?: bookInfo?.optJSONArray("chapters")
        val chapters = mutableListOf<ParsedChapter>()
        if (chapterList != null) {
            for (i in 0 until chapterList.length()) {
                val ch = chapterList.optJSONObject(i) ?: continue
                val chUrl = ch.optString("link").let {
                    if (it.isNotBlank()) normalizeUrl(it, baseUrl) else ""
                }
                if (chUrl.isNotBlank()) {
                    chapters.add(ParsedChapter(
                        number = ch.optInt("chapterIndex", i + 1).toFloat(),
                        title = ch.optString("chapterName").ifBlank { "Chapter ${i + 1}" },
                        url = chUrl
                    ))
                }
            }
        }
        return ParsedNovel(title, author, description, coverUrl, chapters)
    }

    private fun extractWebnovelChapter(doc: Document, html: String): String {
        val nextData = extractNextData(html)
        if (nextData != null) {
            val content = nextData.optJSONObject("props")?.optJSONObject("pageProps")
                ?.optJSONObject("chapterInfo")?.optJSONObject("chapterInfo")?.optString("content")
                ?: nextData.optJSONObject("props")?.optJSONObject("pageProps")?.optString("content")
            if (!content.isNullOrBlank()) return content
        }
        return listOf(
            ".chapter-content p", ".cha-content p", "#chapter-content p",
            "[class*='chapter-content'] p", ".content-wrap p"
        ).firstNotNullOfOrNull { sel ->
            doc.select(sel).joinToString("\n\n") { it.text() }.ifBlank { null }
        } ?: doc.select("p").filter { it.text().length > 80 }.joinToString("\n\n") { it.text() }
    }

    // ── RoyalRoad.com Parser ─────────────────────────────────────────────
    // RoyalRoad is server-rendered — CSS selectors work reliably
    private fun parseRoyalRoad(doc: Document, url: String): ParsedNovel {
        val title = listOf("h1[property='name']", ".fic-title h1", "h1")
            .firstNotNullOfOrNull { sel -> doc.select(sel).text().ifBlank { null } } ?: "Unknown"

        val author = listOf("span[property='name']", ".author-name a", "a[href*='/profile/']")
            .firstNotNullOfOrNull { sel -> doc.select(sel).first()?.text()?.ifBlank { null } } ?: "Unknown"

        val description = listOf(".description .hidden-content p", ".description p", ".fiction-description p")
            .flatMap { sel -> doc.select(sel).map { it.text() } }
            .filter { it.isNotBlank() }.joinToString("\n")

        val coverUrl = listOf(".thumbnail img", ".cover img", ".book-cover img")
            .firstNotNullOfOrNull { sel -> doc.select(sel).attr("src").ifBlank { null } }

        val chapterLinks = doc.select("table#chapters tbody tr td a, .chapter-row a, a[href*='/chapter/']")
            .filter { it.attr("href").contains("/chapter/") }
        val chapters = chapterLinks.mapIndexed { i, el ->
            ParsedChapter(number = (i + 1).toFloat(), title = el.text().ifBlank { "Chapter ${i + 1}" }, url = normalizeUrl(el.attr("href"), "https://www.royalroad.com"))
        }
        return ParsedNovel(title, author, description, coverUrl, chapters)
    }

    private fun extractRoyalRoadChapter(doc: Document): String {
        return listOf(".chapter-content p", ".fiction-chapter p", ".chapter p")
            .firstNotNullOfOrNull { sel ->
                doc.select(sel).joinToString("\n\n") { it.text() }.ifBlank { null }
            } ?: extractGenericChapter(doc)
    }

    // ── WuxiaWorld Parser ────────────────────────────────────────────────
    private fun parseWuxiaWorld(doc: Document, html: String, url: String): ParsedNovel {
        val nextData = extractNextData(html)
        if (nextData != null) {
            val novel = nextData.optJSONObject("props")?.optJSONObject("pageProps")?.optJSONObject("novel")
            if (novel != null) {
                val title = novel.optString("name").ifBlank { "Unknown" }
                val author = novel.optJSONArray("translators")?.optJSONObject(0)?.optString("name")
                    ?: novel.optJSONArray("authors")?.optJSONObject(0)?.optString("name") ?: "Unknown"
                val description = novel.optString("synopsis")
                val coverUrl = novel.optString("coverUrl").ifBlank { null }
                val chaptersJson = novel.optJSONArray("chapters")
                val chapters = mutableListOf<ParsedChapter>()
                if (chaptersJson != null) {
                    val novelSlug = novel.optString("slug")
                    for (i in 0 until chaptersJson.length()) {
                        val ch = chaptersJson.optJSONObject(i) ?: continue
                        val slug = ch.optString("slug")
                        if (slug.isBlank() || novelSlug.isBlank()) continue
                        chapters.add(ParsedChapter(
                            number = (i + 1).toFloat(),
                            title = ch.optString("title").ifBlank { "Chapter ${i + 1}" },
                            url = "https://www.wuxiaworld.com/novel/$novelSlug/$slug"
                        ))
                    }
                }
                return ParsedNovel(title, author, description, coverUrl, chapters)
            }
        }

        val title = listOf("h1.novel-title", "h1[class*='title']", "h1")
            .firstNotNullOfOrNull { sel -> doc.select(sel).text().ifBlank { null } } ?: "Unknown"
        val author = doc.select(".author-name, a[href*='/author/']").text().ifBlank { "Unknown" }
        val description = doc.select("#editdescription p, .description p").joinToString("\n") { it.text() }
        val coverUrl = doc.select(".book-img img, img[class*='cover']").attr("src").ifBlank { null }
        val chapterLinks = doc.select(".chapter-list li a, a[href*='/novel/']")
            .filter { it.attr("href").contains(Regex("novel/.+/.+")) }
        val chapters = chapterLinks.mapIndexed { i, el ->
            ParsedChapter(number = (i + 1).toFloat(), title = el.text().ifBlank { "Chapter ${i + 1}" }, url = normalizeUrl(el.attr("href"), url))
        }
        return ParsedNovel(title, author, description, coverUrl, chapters)
    }

    // ── ScribbleHub.com Parser ───────────────────────────────────────────
    private fun parseScribbleHub(doc: Document, url: String): ParsedNovel {
        val title = doc.select("div.fic_title, h1.fic_title, .fiction h1").text()
            .ifBlank { doc.select("h1").first()?.text() ?: "Unknown" }
        val author = doc.select("span.auth_name_fic a, .author a").text().ifBlank { "Unknown" }
        val description = doc.select(".wi_fic_desc p").joinToString("\n") { it.text() }
        val coverUrl = doc.select(".fic_image img, .cover-wrap img").attr("src").ifBlank { null }

        val tocUrl = url.trimEnd('/') + "/toc"
        val tocHtml = fetchHtml(tocUrl)
        val tocDoc = if (tocHtml != null) htmlToDoc(tocHtml, tocUrl) else doc
        val chapterLinks = tocDoc.select("ol.toc li a, .toc_ol li a, a[href*='/chapter/']")
        val chapters = chapterLinks.mapIndexed { i, el ->
            ParsedChapter(number = (i + 1).toFloat(), title = el.text().ifBlank { "Chapter ${i + 1}" }, url = normalizeUrl(el.attr("href"), url))
        }
        return ParsedNovel(title, author, description, coverUrl, chapters)
    }

    private fun extractScribbleHubChapter(doc: Document): String {
        return doc.select("div.chp_raw p, .chapter-content p, #chp_raw p")
            .joinToString("\n\n") { it.text() }
            .ifBlank { extractGenericChapter(doc) }
    }

    // ── NovelUpdates Parser ──────────────────────────────────────────────
    private fun parseNovelUpdates(doc: Document, url: String): ParsedNovel {
        val title = doc.select(".seriestitlenu, h1.entry-title").text()
            .ifBlank { doc.select("h1").first()?.text() ?: "Unknown" }
        val author = doc.select("a[href*='author']").first()?.text() ?: "Unknown"
        val description = doc.select("#editdescription p").joinToString("\n") { it.text() }
        val coverUrl = doc.select(".wpb_wrapper img, .seriesimg img").first()?.attr("src")
        return ParsedNovel(title, author, description, coverUrl, emptyList())
    }

    // ── Generic Parser ───────────────────────────────────────────────────
    private fun parseGeneric(doc: Document, url: String): ParsedNovel {
        val jsonLd = doc.select("script[type='application/ld+json']")
            .mapNotNull { runCatching { JSONObject(it.data()) }.getOrNull() }
            .firstOrNull { it.optString("@type").contains("Book", ignoreCase = true) }

        val title = jsonLd?.optString("name")?.ifBlank { null }
            ?: doc.select("meta[property='og:title']").attr("content").ifBlank { null }
            ?: doc.title().ifBlank { "Novel" }

        val author = jsonLd?.optJSONObject("author")?.optString("name")?.ifBlank { null }
            ?: doc.select("meta[name='author']").attr("content").ifBlank { "Unknown" }

        val description = jsonLd?.optString("description")?.ifBlank { null }
            ?: doc.select("meta[property='og:description'], meta[name='description']")
                .firstOrNull()?.attr("content") ?: ""

        val coverUrl = jsonLd?.optString("image")?.ifBlank { null }
            ?: doc.select("meta[property='og:image']").attr("content").ifBlank { null }

        val chapterLinks = doc.select("a[href]").filter { el ->
            val text = el.text().lowercase()
            val href = el.attr("href").lowercase()
            (text.contains("chapter") || text.contains("ch.") ||
             href.contains("chapter") || href.contains("/ch/") || href.contains("/c/")) &&
            el.text().isNotBlank()
        }

        val chapters = chapterLinks.mapIndexed { i, el ->
            ParsedChapter(
                number = extractChapterNumber(el.text(), i),
                title = el.text().trim(),
                url = normalizeUrl(el.attr("href"), url)
            )
        }.distinctBy { it.url }.take(2000)

        return ParsedNovel(title, author, description, coverUrl, chapters)
    }

    private fun extractGenericChapter(doc: Document): String {
        val selectors = listOf(
            ".chapter-content", "#chapter-content", ".reading-content",
            ".text-left", ".entry-content", "article", ".post-content",
            "#content", ".content", "main"
        )
        for (selector in selectors) {
            val text = doc.select("$selector p").joinToString("\n\n") { it.text() }
            if (text.length > 200) return text
        }
        return doc.select("p").filter { it.text().length > 50 }.joinToString("\n\n") { it.text() }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /**
     * Extract the JSON embedded by Next.js in <script id="__NEXT_DATA__"> tags.
     * This lets us parse React SPA sites (Webnovel, WuxiaWorld) without JS execution.
     */
    private fun extractNextData(html: String): JSONObject? {
        val start = html.indexOf("""<script id="__NEXT_DATA__"""")
        if (start == -1) return null
        val jsonStart = html.indexOf('>', start) + 1
        val jsonEnd = html.indexOf("</script>", jsonStart)
        if (jsonStart <= 0 || jsonEnd <= jsonStart) return null
        return runCatching { JSONObject(html.substring(jsonStart, jsonEnd).trim()) }.getOrNull()
    }

    private fun normalizeUrl(href: String, baseUrl: String): String {
        return when {
            href.startsWith("http") -> href
            href.startsWith("//") -> "https:$href"
            href.startsWith("/") -> {
                val base = runCatching { java.net.URL(baseUrl) }.getOrNull()
                if (base != null) "${base.protocol}://${base.host}$href" else href
            }
            else -> "$baseUrl/$href"
        }
    }

    private fun extractChapterNumber(text: String, fallbackIndex: Int): Float {
        val match = Regex("(?:chapter|ch\\.?)\\s*([\\d.]+)", RegexOption.IGNORE_CASE).find(text)
        return match?.groupValues?.get(1)?.toFloatOrNull() ?: (fallbackIndex + 1).toFloat()
    }
}
