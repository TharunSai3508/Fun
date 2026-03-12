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

    suspend fun parseNovelPage(url: String): ParsedNovel? = withContext(Dispatchers.IO) {

        try {

            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .referrer("https://www.google.com/")
                .timeout(20000)
                .followRedirects(true)
                .get()

            val host = URL(doc.location()).host

            when {

                host.contains("webnovel") ->
                    parseWebnovel(doc, url)

                host.contains("royalroad") ->
                    parseRoyalRoad(doc, url)

                else ->
                    parseGeneric(doc, url)
            }

        } catch (e: Exception) {
            null
        }
    }

    suspend fun parseChapterContent(chapterUrl: String): String =
        withContext(Dispatchers.IO) {

            try {

                val doc = Jsoup.connect(chapterUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(20000)
                    .get()

                extractGenericChapter(doc)

            } catch (e: Exception) {
                "Failed to load chapter."
            }
        }

    private fun parseWebnovel(doc: Document, baseUrl: String): ParsedNovel {

        val title = doc.selectFirst("h1")?.text() ?: "Unknown"
        val author = doc.selectFirst(".author-name")?.text() ?: "Unknown"
        val description = doc.select("p").joinToString("\n") { it.text() }

        val cover = doc.selectFirst("img")?.attr("src")

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
        val author = doc.select("a[href*='author']").first()?.text() ?: "Unknown"

        val description = doc.select(".description p")
            .joinToString("\n") { it.text() }

        val cover = doc.selectFirst("img")?.attr("src")

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

    private fun parseGeneric(doc: Document, baseUrl: String): ParsedNovel {

        val title = doc.title()

        val chapters = doc.select("a[href]")
            .filter {

                val text = it.text().lowercase()

                text.contains("chapter")
                        || text.contains("ch.")
                        || it.attr("href").contains("chapter")
            }
            .mapIndexed { i, el ->

                ParsedChapter(
                    number = (i + 1).toFloat(),
                    title = el.text(),
                    url = normalizeUrl(el.attr("href"), baseUrl)
                )
            }
            .distinctBy { it.url }
            .take(2000)

        return ParsedNovel(
            title,
            "Unknown",
            "",
            null,
            chapters
        )
    }

    private fun extractGenericChapter(doc: Document): String {

        val selectors = listOf(
            ".chapter-content",
            "#chapter-content",
            ".entry-content",
            ".post-content",
            ".content"
        )

        for (s in selectors) {

            val text = doc.select("$s p")
                .joinToString("\n\n") { it.text() }

            if (text.length > 200) return text
        }

        return doc.select("p")
            .joinToString("\n\n") { it.text() }
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