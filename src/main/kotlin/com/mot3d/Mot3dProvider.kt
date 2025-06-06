package com.mot3d

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup

class Mot3dProvider : MainAPI() {
    override var mainUrl = "https://mot3d.com"
    override var name = "Mot3d"
    override val hasMainPage = true
    override var lang = "vi"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override val mainPage = listOf(
        "phim-le" to "Phim lẻ",
        "phim-bo" to "Phim bộ"
    ).map { (path, title) ->
        MainPageData(title, "$mainUrl/the-loai/$path", true)
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get(request.data).document
        val items = doc.select(".film_list .item").mapNotNull {
            val title = it.selectFirst(".film-title")?.text() ?: return@mapNotNull null
            val link = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = it.selectFirst("img")?.attr("data-original") ?: ""
            val isSeries = link.contains("phim-bo")
            MovieSearchResponse(
                title,
                link,
                this.name,
                if (isSeries) TvType.TvSeries else TvType.Movie,
                poster,
                null,
                null
            )
        }
        return HomePageResponse(listOf(HomePageList(request.name, items)))
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val doc = app.get(url).document
        return doc.select(".film_list .item").mapNotNull {
            val title = it.selectFirst(".film-title")?.text() ?: return@mapNotNull null
            val link = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = it.selectFirst("img")?.attr("data-original") ?: ""
            val isSeries = link.contains("phim-bo")
            MovieSearchResponse(
                title,
                link,
                this.name,
                if (isSeries) TvType.TvSeries else TvType.Movie,
                poster,
                null,
                null
            )
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1")?.text() ?: "Không rõ tiêu đề"
        val poster = doc.selectFirst(".movie-l-img img")?.attr("src") ?: ""
        val description = doc.selectFirst(".film-content")?.text() ?: ""
        val episodes = doc.select(".server_line").flatMap {
            it.select("li").map { ep ->
                val epTitle = ep.text()
                val epLink = ep.attr("data-video") ?: return@map null
                Episode(epTitle, epLink)
            }
        }.filterNotNull()

        return if (episodes.isEmpty()) {
            MovieLoadResponse(title, url, this.name, TvType.Movie, url, poster, year = null, plot = description)
        } else {
            TvSeriesLoadResponse(title, url, this.name, TvType.TvSeries, episodes, poster, plot = description)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        callback(
            ExtractorLink(
                source = "mot3d",
                name = "mot3d",
                url = data,
                referer = mainUrl,
                quality = Qualities.Unknown.value,
                isM3u8 = data.contains(".m3u8")
            )
        )
    }
}
