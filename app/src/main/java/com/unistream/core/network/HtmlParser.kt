package com.unistream.core.network

import org.jsoup.Jsoup

object HtmlParser {

    fun extractText(html: String): String {

        val doc = Jsoup.parse(html)

        return doc.select("p")
            .joinToString("\n") { it.text() }
    }

}