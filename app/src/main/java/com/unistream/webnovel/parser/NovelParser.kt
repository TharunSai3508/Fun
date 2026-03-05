package com.unistream.webnovel.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
class NovelParser @Inject constructor() {

    suspend fun parseNovelPage(url: String): ParsedNovel? = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 Chrome/114.0.0.0 Mobile Safari/537.36")
                .referrer("https://www.google.com/")
                .timeout(20_000)
                .followRedirects(true)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .get()

            // Try to detect site and use appropriate parser
            val host = doc.location().let { java.net.URL(it).host }

            when {
                host.contains("webnovel.com") -> parseWebnovel(doc, url)
                host.contains("royalroad.com") -> parseRoyalRoad(doc, url)
                host.contains("wuxiaworld.com") -> parseWuxiaWorld(doc, url)
                host.contains("novelupdates.com") -> parseNovelUpdates(doc, url)
                else -> parseGeneric(doc, url)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun parseChapterContent(chapterUrl: String): String = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(chapterUrl)
                .userAgent("Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 Chrome/114.0.0.0 Mobile Safari/537.36")
                .referrer("https://www.google.com/")
                .timeout(20_000)
                .followRedirects(true)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .get()

            val host = doc.location().let { runCatching { java.net.URL(it).host }.getOrDefault("") }

            when {
                host.contains("webnovel.com") -> extractWebnovelChapter(doc)
                host.contains("royalroad.com") -> extractRoyalRoadChapter(doc)
                else -> extractGenericChapter(doc)
            }
        } catch (e: Exception) {
            "Failed to load chapter content. Please check your internet connection."
        }
    }

    // ── Webnovel.com Parser ──────────────────────────────────────────────
    private fun parseWebnovel(doc: Document, url: String): ParsedNovel {
        val title = doc.select(".pt4.pb4.oh.mb4 h1").text()
            .ifBlank { doc.select("h1").first()?.text() ?: "Unknown" }
        val author = doc.select(".author-name").text().ifBlank { "Unknown" }
        val description = doc.select(".j_synopsis p").joinToString("\n") { it.text() }
        val coverUrl = doc.select(".g_thumb img").attr("src")
            .let { if (it.isNotBlank()) it else null }

        val chapterLinks = doc.select(".chapter-item a, .content-list a[href*='/chapter/']")
        val chapters = chapterLinks.mapIndexed { i, el ->
            ParsedChapter(
                number = (i + 1).toFloat(),
                title = el.text().ifBlank { "Chapter ${i + 1}" },
                url = normalizeUrl(el.attr("href"), url)
            )
        }
        return ParsedNovel(title, author, description, coverUrl, chapters)
    }

    private fun extractWebnovelChapter(doc: Document): String {
        return doc.select(".chapter-content p, .cha-content p, #chapter-content p")
            .joinToString("\n\n") { it.text() }
            .ifBlank { doc.select("p").joinToString("\n\n") { it.text() } }
    }

    // ── RoyalRoad.com Parser ─────────────────────────────────────────────
    private fun parseRoyalRoad(doc: Document, url: String): ParsedNovel {
        val title = doc.select("h1[property='name']").text()
            .ifBlank { doc.select("h1").first()?.text() ?: "Unknown" }
        val author = doc.select("span[property='name']").first()?.text() ?: "Unknown"
        val description = doc.select(".description .hidden-content p").joinToString("\n") { it.text() }
        val coverUrl = doc.select(".thumbnail img").attr("src").let { if (it.isNotBlank()) it else null }

        val chapterLinks = doc.select("table#chapters tbody tr td a")
        val chapters = chapterLinks.mapIndexed { i, el ->
            ParsedChapter(
                number = (i + 1).toFloat(),
                title = el.text().ifBlank { "Chapter ${i + 1}" },
                url = normalizeUrl(el.attr("href"), "https://www.royalroad.com")
            )
        }
        return ParsedNovel(title, author, description, coverUrl, chapters)
    }

    private fun extractRoyalRoadChapter(doc: Document): String {
        return doc.select(".chapter-content p").joinToString("\n\n") { it.text() }
    }

    // ── WuxiaWorld Parser ────────────────────────────────────────────────
    private fun parseWuxiaWorld(doc: Document, url: String): ParsedNovel {
        val title = doc.select("h1.novel-title").text()
            .ifBlank { doc.select("h1").first()?.text() ?: "Unknown" }
        val author = doc.select(".author-name").text().ifBlank { "Unknown" }
        val description = doc.select("#editdescription p").joinToString("\n") { it.text() }
        val coverUrl = doc.select(".book-img img").attr("src").let { if (it.isNotBlank()) it else null }

        val chapterLinks = doc.select(".chapter-list li a")
        val chapters = chapterLinks.mapIndexed { i, el ->
            ParsedChapter(
                number = (i + 1).toFloat(),
                title = el.text().ifBlank { "Chapter ${i + 1}" },
                url = normalizeUrl(el.attr("href"), url)
            )
        }
        return ParsedNovel(title, author, description, coverUrl, chapters)
    }

    // ── NovelUpdates Parser ──────────────────────────────────────────────
    private fun parseNovelUpdates(doc: Document, url: String): ParsedNovel {
        val title = doc.select(".seriestitlenu").text()
            .ifBlank { doc.select("h1").first()?.text() ?: "Unknown" }
        val author = doc.select("a[href*='author']").first()?.text() ?: "Unknown"
        val description = doc.select("#editdescription p").joinToString("\n") { it.text() }
        val coverUrl = doc.select(".wpb_wrapper img").first()?.attr("src")

        return ParsedNovel(title, author, description, coverUrl, emptyList())
    }

    // ── Generic Parser ───────────────────────────────────────────────────
    private fun parseGeneric(doc: Document, url: String): ParsedNovel {
        val title = doc.title().ifBlank { "Novel" }
        val author = doc.select("meta[name='author']").attr("content").ifBlank { "Unknown" }
        val description = doc.select("meta[name='description']").attr("content")
        val coverUrl = doc.select("meta[property='og:image']").attr("content").let {
            if (it.isNotBlank()) it else null
        }

        // Try to find chapter links using common patterns
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
        // Try common chapter content selectors
        val selectors = listOf(
            ".chapter-content", "#chapter-content", ".reading-content",
            ".text-left", ".entry-content", "article", ".post-content",
            "#content", ".content"
        )

        for (selector in selectors) {
            val text = doc.select("$selector p").joinToString("\n\n") { it.text() }
            if (text.length > 200) return text
        }

        // Fallback: get all paragraphs
        return doc.select("p").filter { it.text().length > 50 }
            .joinToString("\n\n") { it.text() }
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
        val numberRegex = Regex("(?:chapter|ch\\.?)\\s*([\\d.]+)", RegexOption.IGNORE_CASE)
        val match = numberRegex.find(text)
        return match?.groupValues?.get(1)?.toFloatOrNull() ?: (fallbackIndex + 1).toFloat()
    }
}
