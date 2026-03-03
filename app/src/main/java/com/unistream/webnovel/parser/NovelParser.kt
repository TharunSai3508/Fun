package com.unistream.webnovel.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL
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
    val content: String = ""
)

@Singleton
class NovelParser @Inject constructor() {

    // Realistic browser headers that bypass most bot-detection systems
    private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/115.0.0.0 Mobile Safari/537.36",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language" to "en-US,en;q=0.9",
        "Accept-Encoding" to "gzip, deflate, br",
        "Connection" to "keep-alive",
        "Upgrade-Insecure-Requests" to "1",
        "Sec-Fetch-Mode" to "navigate",
        "Sec-Fetch-Site" to "none",
        "Sec-Fetch-User" to "?1",
        "DNT" to "1"
    )

    // ─────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────

    suspend fun parseNovelPage(url: String): ParsedNovel? = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        try {
            val doc = fetchDocument(cleanUrl) ?: return@withContext null
            val host = safeHost(doc.location().ifBlank { cleanUrl })
            when {
                host.contains("webnovel.com") -> parseWebnovel(doc, cleanUrl)
                host.contains("royalroad.com") -> parseRoyalRoad(doc, cleanUrl)
                host.contains("wuxiaworld.com") -> parseWuxiaWorld(doc, cleanUrl)
                host.contains("novelupdates.com") -> parseNovelUpdates(doc, cleanUrl)
                host.contains("scribblehub.com") -> parseScribbleHub(doc, cleanUrl)
                else -> parseGeneric(doc, cleanUrl)
            }
        } catch (e: Exception) {
            ParsedNovel(
                title = "Import failed",
                author = "",
                description = "Could not fetch novel: ${e.message ?: "Network error"}. " +
                    "Ensure the URL is correct and you have an internet connection.",
                coverUrl = null,
                chapters = emptyList()
            )
        }
    }

    suspend fun parseChapterContent(chapterUrl: String): String = withContext(Dispatchers.IO) {
        try {
            val doc = fetchDocument(chapterUrl)
                ?: return@withContext "Could not load chapter – check internet connection."
            val host = safeHost(doc.location().ifBlank { chapterUrl })
            when {
                host.contains("webnovel.com") -> extractWebnovelChapter(doc)
                host.contains("royalroad.com") -> extractRoyalRoadChapter(doc)
                host.contains("scribblehub.com") -> extractScribbleHubChapter(doc)
                else -> extractGenericChapter(doc)
            }.ifBlank {
                "Chapter content could not be extracted. The site may require JavaScript."
            }
        } catch (e: Exception) {
            "Error loading chapter: ${e.message ?: "Unknown error"}"
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Fetch helper with retry + exponential back-off
    // ─────────────────────────────────────────────────────────────────────

    private fun fetchDocument(url: String, retries: Int = 3): Document? {
        var lastEx: Exception? = null
        repeat(retries) { attempt ->
            try {
                return Jsoup.connect(url)
                    .headers(headers)
                    .timeout(20_000)
                    .maxBodySize(0)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(false)
                    .followRedirects(true)
                    .get()
            } catch (e: Exception) {
                lastEx = e
                if (attempt < retries - 1) Thread.sleep(1_500L * (attempt + 1))
            }
        }
        throw lastEx ?: RuntimeException("Failed to fetch $url")
    }

    // ─────────────────────────────────────────────────────────────────────
    // Site-specific parsers
    // ─────────────────────────────────────────────────────────────────────

    private fun parseWebnovel(doc: Document, url: String): ParsedNovel {
        val title = doc.select("h1[class*=title], .pt4.pb4 h1, h1").first()?.text() ?: doc.title()
        val author = doc.select(".author-name, [class*=author]").first()?.text() ?: "Unknown"
        val description = doc.select("[class*=synopsis] p, [class*=desc] p, .j_synopsis p")
            .joinToString("\n") { it.text() }
        val coverUrl = doc.select("[class*=thumb] img, [class*=cover] img, .g_thumb img")
            .first()?.let { it.attr("data-src").ifBlank { it.attr("src") } }
            ?.let { fixUrl(it, url) }
        val chapters = doc.select("[class*=chapter] a, [href*=chapter]")
            .mapIndexed { i, el ->
                ParsedChapter(
                    number = extractChapterNumber(el.text(), i),
                    title = el.text().trim().ifBlank { "Chapter ${i + 1}" },
                    url = fixUrl(el.attr("href"), url)
                )
            }.distinctBy { it.url }.filter { it.url.startsWith("http") }
        return ParsedNovel(title, author, description, coverUrl, chapters)
    }

    private fun extractWebnovelChapter(doc: Document): String {
        for (sel in listOf(
            ".chapter-content p", "#chapter-content p",
            ".cha-content p", "[class*=chapter-content] p"
        )) {
            val t = doc.select(sel).joinToString("\n\n") { it.text() }
            if (t.length > 100) return t
        }
        return extractGenericChapter(doc)
    }

    private fun parseRoyalRoad(doc: Document, url: String): ParsedNovel {
        val title = doc.select("h1[property='name'], h1.font-white, h1").first()?.text() ?: doc.title()
        val author = doc.select("[property='author'] [property='name'], .author-name").first()
            ?.text() ?: "Unknown"
        val description = doc.select(".description .hidden-content p, .description p")
            .joinToString("\n") { it.text() }
        val coverUrl = doc.select(".thumbnail img, [class*=cover] img").first()
            ?.let { it.attr("src").ifBlank { it.attr("data-src") } }?.let { fixUrl(it, url) }
        val chapters = doc.select("table#chapters a, [data-url*=chapter]")
            .mapIndexed { i, el ->
                ParsedChapter(
                    number = (i + 1).toFloat(),
                    title = el.text().trim().ifBlank { "Chapter ${i + 1}" },
                    url = fixUrl(el.attr("href"), "https://www.royalroad.com")
                )
            }.distinctBy { it.url }.filter { it.url.startsWith("http") }
        return ParsedNovel(title, author, description, coverUrl, chapters)
    }

    private fun extractRoyalRoadChapter(doc: Document): String =
        doc.select(".chapter-content p, .chapter-inner p").joinToString("\n\n") { it.text() }
            .ifBlank { extractGenericChapter(doc) }

    private fun parseWuxiaWorld(doc: Document, url: String): ParsedNovel {
        val title = doc.select("h1.novel-title, h1").first()?.text() ?: doc.title()
        val author = doc.select(".author-name, [itemprop=author]").first()?.text() ?: "Unknown"
        val description = doc.select("#editdescription p, [class*=desc] p")
            .joinToString("\n") { it.text() }
        val coverUrl = doc.select(".book-img img, [class*=cover] img").first()
            ?.attr("src")?.let { fixUrl(it, url) }
        val chapters = doc.select(".chapter-list li a, [class*=chapter-list] a")
            .mapIndexed { i, el ->
                ParsedChapter(
                    number = (i + 1).toFloat(),
                    title = el.text().trim().ifBlank { "Chapter ${i + 1}" },
                    url = fixUrl(el.attr("href"), url)
                )
            }.distinctBy { it.url }.filter { it.url.startsWith("http") }
        return ParsedNovel(title, author, description, coverUrl, chapters)
    }

    private fun parseNovelUpdates(doc: Document, url: String): ParsedNovel {
        val title = doc.select(".seriestitlenu, h1").first()?.text() ?: doc.title()
        val author = doc.select("a[href*=author]").first()?.text() ?: "Unknown"
        val description = doc.select("#editdescription p").joinToString("\n") { it.text() }
        val coverUrl = doc.select(".seriesimg img, img[src*=cover]").first()
            ?.attr("src")?.let { fixUrl(it, url) }
        return ParsedNovel(title, author, description, coverUrl, emptyList())
    }

    private fun parseScribbleHub(doc: Document, url: String): ParsedNovel {
        val title = doc.select(".story-name, h1").first()?.text() ?: doc.title()
        val author = doc.select(".auth_name_fic, [class*=author]").first()?.text() ?: "Unknown"
        val description = doc.select(".wi_fic_desc p").joinToString("\n") { it.text() }
        val coverUrl = doc.select(".fic_image img").first()
            ?.let { it.attr("src").ifBlank { it.attr("data-src") } }?.let { fixUrl(it, url) }
        val chapters = doc.select("li.chapter-item a, [class*=toc] a")
            .mapIndexed { i, el ->
                ParsedChapter(
                    number = (i + 1).toFloat(),
                    title = el.text().trim().ifBlank { "Chapter ${i + 1}" },
                    url = fixUrl(el.attr("href"), url)
                )
            }.distinctBy { it.url }.filter { it.url.startsWith("http") }
        return ParsedNovel(title, author, description, coverUrl, chapters)
    }

    private fun extractScribbleHubChapter(doc: Document): String =
        doc.select(".chp_raw p, #chapter-content p").joinToString("\n\n") { it.text() }
            .ifBlank { extractGenericChapter(doc) }

    private fun parseGeneric(doc: Document, url: String): ParsedNovel {
        val title = doc.select("meta[property='og:title']").attr("content").ifBlank { doc.title() }
        val author = doc.select("meta[name='author']").attr("content").ifBlank { "Unknown" }
        val description = doc.select(
            "meta[name='description'], meta[property='og:description']"
        ).first()?.attr("content") ?: ""
        val coverUrl = doc.select("meta[property='og:image']").attr("content")
            .ifBlank { null }?.let { fixUrl(it, url) }

        val chapterLinks = doc.select("a[href]").filter { el ->
            val text = el.text().lowercase()
            val href = el.attr("href").lowercase()
            el.text().isNotBlank() && el.text().length < 200 &&
                (text.contains("chapter") || text.matches(Regex(".*ch\\.?\\s*\\d+.*")) ||
                 href.contains("/chapter/") || href.contains("/ch/") ||
                 Regex("/c/\\d").containsMatchIn(href) || href.contains("/ep/"))
        }
        val chapters = chapterLinks
            .mapIndexed { i, el ->
                ParsedChapter(
                    number = extractChapterNumber(el.text(), i),
                    title = el.text().trim(),
                    url = fixUrl(el.attr("href"), url)
                )
            }
            .distinctBy { it.url }
            .filter { it.url.startsWith("http") }
            .take(3000)

        return ParsedNovel(title, author, description, coverUrl, chapters)
    }

    private fun extractGenericChapter(doc: Document): String {
        val candidates = listOf(
            ".chapter-content p", "#chapter-content p", ".reading-content p",
            ".entry-content p", "article p", ".post-content p",
            "#content p", ".content p", ".text-content p", "main p"
        )
        for (sel in candidates) {
            val text = doc.select(sel)
                .filter { it.text().length > 30 }
                .joinToString("\n\n") { it.text() }
            if (text.length > 300) return text
        }
        return doc.select("p")
            .filter { it.text().length > 40 }
            .take(500)
            .joinToString("\n\n") { it.text() }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────

    private fun fixUrl(href: String, base: String): String {
        val h = href.trim()
        return when {
            h.startsWith("http://") || h.startsWith("https://") -> h
            h.startsWith("//") -> "https:$h"
            h.startsWith("/") -> {
                val b = runCatching { URL(base) }.getOrNull()
                if (b != null) "${b.protocol}://${b.host}$h" else h
            }
            else -> "${base.substringBeforeLast("/")}/$h"
        }
    }

    private fun safeHost(url: String): String =
        runCatching { URL(url).host }.getOrDefault("")

    private fun extractChapterNumber(text: String, fallbackIndex: Int): Float {
        val regex = Regex("""(?:chapter|ch\.?)\s*([\d.]+)""", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.get(1)?.toFloatOrNull()
            ?: (fallbackIndex + 1).toFloat()
    }
}
