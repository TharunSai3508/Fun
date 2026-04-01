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

data class ParseResult(
    val novel: ParsedNovel?,
    val error: String? = null
)

@Singleton
class NovelParser @Inject constructor() {

    suspend fun parseNovelPage(url: String): ParsedNovel? = withContext(Dispatchers.IO) {

        try {

            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .referrer("https://www.google.com/")
                .timeout(30000)
                .followRedirects(true)
                .maxBodySize(0)
                .get()

            val host = URL(doc.location()).host

            when {

                host.contains("webnovel") ->
                    parseWebnovel(doc, url)

                host.contains("royalroad") ->
                    parseRoyalRoad(doc, url)

                host.contains("wuxiaworld") ->
                    parseWuxiaWorld(doc, url)

                host.contains("novelfull") ->
                    parseNovelFull(doc, url)

                host.contains("lightnovelworld") || host.contains("lnmtl") ->
                    parseLightNovelWorld(doc, url)

                else ->
                    parseGeneric(doc, url)
            }

        } catch (e: Exception) {
            null
        }
    }

    suspend fun parseNovelPageWithResult(url: String): ParseResult = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .referrer("https://www.google.com/")
                .timeout(30000)
                .followRedirects(true)
                .maxBodySize(0)
                .get()

            val host = URL(doc.location()).host

            val novel = when {
                host.contains("webnovel") -> parseWebnovel(doc, url)
                host.contains("royalroad") -> parseRoyalRoad(doc, url)
                host.contains("wuxiaworld") -> parseWuxiaWorld(doc, url)
                host.contains("novelfull") -> parseNovelFull(doc, url)
                else -> parseGeneric(doc, url)
            }

            if (novel != null && novel.chapters.isNotEmpty()) {
                ParseResult(novel = novel)
            } else {
                ParseResult(novel = null, error = "Could not find chapters on this page. Try a different URL or source.")
            }
        } catch (e: org.jsoup.HttpStatusException) {
            ParseResult(novel = null, error = "HTTP ${e.statusCode}: ${e.message}")
        } catch (e: java.net.SocketTimeoutException) {
            ParseResult(novel = null, error = "Connection timed out. Check your internet and try again.")
        } catch (e: java.net.UnknownHostException) {
            ParseResult(novel = null, error = "Could not connect to ${URL(url).host}. Check your internet connection.")
        } catch (e: Exception) {
            ParseResult(novel = null, error = "Failed to parse: ${e.message ?: "Unknown error"}")
        }
    }

    suspend fun parseChapterContent(chapterUrl: String): String =
        withContext(Dispatchers.IO) {

            try {

                val doc = Jsoup.connect(chapterUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .referrer("https://www.google.com/")
                    .timeout(30000)
                    .followRedirects(true)
                    .maxBodySize(0)
                    .get()

                val extracted = extractGenericChapter(doc)
                if (extracted.isBlank()) {
                    extractFromScripts(doc).ifBlank { "Unable to extract chapter content from this source." }
                } else extracted

            } catch (e: java.net.SocketTimeoutException) {
                "Connection timed out. Please try again."
            } catch (e: Exception) {
                "Failed to load chapter: ${e.message ?: "Unknown error"}"
            }
        }

    private fun parseWebnovel(doc: Document, baseUrl: String): ParsedNovel {

        val title = doc.selectFirst("h1")?.text()
            ?: doc.selectFirst(".story-title")?.text()
            ?: "Unknown"

        val author = doc.selectFirst(".author-name")?.text()
            ?: doc.selectFirst("a[href*=author]")?.text()
            ?: "Unknown"

        val description = doc.select(".story-desc p, .synopsis p, .description p")
            .joinToString("\n") { it.text() }
            .ifBlank { doc.select("p").take(5).joinToString("\n") { it.text() } }

        val cover = doc.selectFirst(".story-cover img, .book-cover img, img.cover")?.attr("src")
            ?: doc.selectFirst("img[src*=cover]")?.attr("src")

        val chapters = doc.select("a[href*='chapter']")
            .mapIndexed { index, el ->
                ParsedChapter(
                    number = (index + 1).toFloat(),
                    title = el.text(),
                    url = normalizeUrl(el.attr("href"), baseUrl)
                )
            }
            .distinctBy { it.url }

        return ParsedNovel(title, author, description, cover, chapters)
    }

    private fun parseRoyalRoad(doc: Document, baseUrl: String): ParsedNovel {

        val title = doc.selectFirst("h1")?.text() ?: "Unknown"
        val author = doc.select("a[href*='author'], a[href*='profile']").first()?.text() ?: "Unknown"

        val description = doc.select(".description p, .fiction-info .portlet-body p")
            .joinToString("\n") { it.text() }

        val cover = doc.selectFirst(".cover-art-container img, .fic-header img")?.attr("src")

        val chapters = doc.select("table#chapters a, a[href*='chapter']")
            .mapIndexed { i, el ->
                ParsedChapter(
                    number = (i + 1).toFloat(),
                    title = el.text().trim(),
                    url = normalizeUrl(el.attr("href"), baseUrl)
                )
            }
            .distinctBy { it.url }

        return ParsedNovel(title, author, description, cover, chapters)
    }

    private fun parseWuxiaWorld(doc: Document, baseUrl: String): ParsedNovel {
        val title = doc.selectFirst("h1, .novel-title")?.text() ?: "Unknown"
        val author = doc.selectFirst(".author-name, a[href*=author]")?.text() ?: "Unknown"
        val description = doc.select(".novel-summary p, .synopsis p").joinToString("\n") { it.text() }
        val cover = doc.selectFirst("img.novel-cover, img[src*=cover]")?.attr("src")

        val chapters = doc.select("a[href*='chapter']")
            .mapIndexed { i, el ->
                ParsedChapter(
                    number = (i + 1).toFloat(),
                    title = el.text(),
                    url = normalizeUrl(el.attr("href"), baseUrl)
                )
            }
            .distinctBy { it.url }

        return ParsedNovel(title, author, description, cover, chapters)
    }

    private fun parseNovelFull(doc: Document, baseUrl: String): ParsedNovel {
        val title = doc.selectFirst("h3.title, h1")?.text() ?: "Unknown"
        val author = doc.selectFirst("a[href*=author]")?.text() ?: "Unknown"
        val description = doc.select(".desc-text p").joinToString("\n") { it.text() }
        val cover = doc.selectFirst(".book img")?.attr("src")

        val chapters = doc.select(".list-chapter a")
            .mapIndexed { i, el ->
                ParsedChapter(
                    number = (i + 1).toFloat(),
                    title = el.text(),
                    url = normalizeUrl(el.attr("href"), baseUrl)
                )
            }
            .distinctBy { it.url }

        return ParsedNovel(title, author, description, cover, chapters)
    }

    private fun parseLightNovelWorld(doc: Document, baseUrl: String): ParsedNovel {
        val title = doc.selectFirst("h1, .novel-title")?.text() ?: "Unknown"
        val author = doc.selectFirst(".author a, a[href*=author]")?.text() ?: "Unknown"
        val description = doc.select(".summary p, .description p").joinToString("\n") { it.text() }
        val cover = doc.selectFirst("img.cover, img[src*=cover]")?.attr("src")

        val chapters = doc.select("a[href*=chapter]")
            .mapIndexed { i, el ->
                ParsedChapter(
                    number = (i + 1).toFloat(),
                    title = el.text(),
                    url = normalizeUrl(el.attr("href"), baseUrl)
                )
            }
            .distinctBy { it.url }

        return ParsedNovel(title, author, description, cover, chapters)
    }

    private fun parseGeneric(doc: Document, baseUrl: String): ParsedNovel {

        val title = doc.selectFirst("h1")?.text() ?: doc.title()

        val author = doc.select("a[href*=author], .author, [class*=author]")
            .firstOrNull()?.text() ?: "Unknown"

        val description = doc.select(".description p, .synopsis p, .summary p")
            .joinToString("\n") { it.text() }

        val cover = doc.selectFirst("img[src*=cover], img[class*=cover]")?.attr("src")

        val chapters = doc.select("a[href]")
            .filter {
                val text = it.text().lowercase()
                val href = it.attr("href").lowercase()
                text.contains("chapter") || text.contains("ch.") ||
                    href.contains("chapter") || href.contains("/ch-") || href.contains("/ch/")
            }
            .mapIndexed { i, el ->
                ParsedChapter(
                    number = (i + 1).toFloat(),
                    title = el.text().trim(),
                    url = normalizeUrl(el.attr("href"), baseUrl)
                )
            }
            .distinctBy { it.url }
            .take(2000)

        return ParsedNovel(title, author, description, cover, chapters)
    }

    private fun extractGenericChapter(doc: Document): String {

        val selectors = listOf(
            ".chapter-content",
            "#chapter-content",
            ".reading-content",
            ".text-left",
            ".entry-content",
            ".post-content",
            ".content",
            "#content",
            ".chapter-text",
            ".reader-content",
            "article"
        )

        for (s in selectors) {

            val text = doc.select("$s p")
                .joinToString("\n\n") { it.text() }

            if (text.length > 200) return text
        }

        // Fallback: all paragraphs
        val allParagraphs = doc.select("p")
            .joinToString("\n\n") { it.text() }

        if (allParagraphs.length > 200) return allParagraphs

        // Last resort: body text
        return doc.body()?.text() ?: "No content found"
    }


    private fun extractFromScripts(doc: Document): String {
        val scripts = doc.select("script")
        val candidates = scripts.mapNotNull {
            it.data().takeIf { data -> data.contains("chapter", true) || data.contains("content", true) }
        }
        val jsonLike = candidates.firstOrNull {
            it.contains("\"content\"") || it.contains("chapterBody", true)
        } ?: return ""

        val regex = Regex(
            "\"content\"\\s*:\\s*\"(.*?)\"",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )
        val raw = regex.find(jsonLike)?.groupValues?.getOrNull(1) ?: return ""
        return raw.replace("\\n", "\n")
            .replace("<br>", "\n")
            .replace(Regex("<[^>]+>"), "")
            .trim()
    }

    private fun normalizeUrl(href: String, baseUrl: String): String {

        return when {

            href.startsWith("http") ->
                href

            href.startsWith("//") ->
                "https:$href"

            href.startsWith("/") -> {
                val base = URL(baseUrl)
                "${base.protocol}://${base.host}$href"
            }

            else ->
                "$baseUrl/$href"
        }
    }
}
