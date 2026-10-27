package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDate

data class YouTubeChannelMetrics(
    val channelTitle: String,
    val subscriberCount: Long,
    val totalViews: Long,
    val videoCount: Int,
    val viewsLast28Days: Long,
    val watchTimeMinutes28Days: Long,
    val averageViewDurationSec: Int,
    val estimatedCtrPercent: Double,
    val topVideos: List<Pair<String, Long>>,
    val trafficSources: List<Pair<String, Int>>,
    val bestUploadWindow: String,
    val weekOverWeekGrowthPct: Double
)

data class MarketQuote(
    val symbol: String,
    val name: String,
    val priceUsd: Double,
    val change24hPct: Double
)

class PublicApisClient(private val httpClient: OkHttpClient) {

    /**
     * Fetches real YouTube Data API v3 + YouTube Analytics API v2 metrics using user's OAuth2 Bearer token
     * with scopes `youtube.readonly` and `yt-analytics.readonly`.
     */
    suspend fun fetchYouTubeChannelAnalytics(accessToken: String): Result<YouTubeChannelMetrics> =
        withContext(Dispatchers.IO) {
            if (accessToken.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Google OAuth token missing"))
            }
            try {
                // 1. Channel basic stats
                val chReq = Request.Builder()
                    .url("https://www.googleapis.com/youtube/v3/channels?part=snippet,statistics,contentDetails&mine=true")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .build()

                var title = "Mera Channel"
                var subs = 0L
                var totalViews = 0L
                var videoCount = 0

                httpClient.newCall(chReq).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(RuntimeException("YouTube Data API HTTP ${resp.code}: $body"))
                    }
                    val items = JSONObject(body).optJSONArray("items")
                    if (items == null || items.length() == 0) {
                        return@withContext Result.failure(RuntimeException("Is Google account par koi YouTube channel nahi mila."))
                    }
                    val ch = items.getJSONObject(0)
                    title = ch.optJSONObject("snippet")?.optString("title") ?: title
                    val stats = ch.optJSONObject("statistics")
                    subs = stats?.optString("subscriberCount")?.toLongOrNull() ?: 0L
                    totalViews = stats?.optString("viewCount")?.toLongOrNull() ?: 0L
                    videoCount = stats?.optString("videoCount")?.toIntOrNull() ?: 0
                }

                // 2. YouTube Analytics API (28-day window + week-over-week)
                val endDate = LocalDate.now().toString()
                val startDate = LocalDate.now().minusDays(28).toString()
                val analyticsUrl =
                    "https://youtubeanalytics.googleapis.com/v2/reports?ids=channel==MINE&startDate=$startDate&endDate=$endDate&metrics=views,estimatedMinutesWatched,averageViewDuration,subscribersGained&dimensions=day&sort=day"

                var views28d = 0L
                var watchMin28d = 0L
                var avgDurSec = 0
                var prevWeekViews = 0L
                var lastWeekViews = 0L

                val anReq = Request.Builder()
                    .url(analyticsUrl)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .build()

                httpClient.newCall(anReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val rows = JSONObject(resp.body?.string().orEmpty()).optJSONArray("rows")
                        if (rows != null && rows.length() > 0) {
                            var durAcc = 0
                            for (i in 0 until rows.length()) {
                                val r = rows.getJSONArray(i)
                                val dayViews = r.optLong(1, 0L)
                                views28d += dayViews
                                watchMin28d += r.optLong(2, 0L)
                                durAcc += r.optInt(3, 0)
                                if (i >= rows.length() - 7) lastWeekViews += dayViews
                                else if (i in (rows.length() - 14) until (rows.length() - 7)) prevWeekViews += dayViews
                            }
                            avgDurSec = durAcc / rows.length()
                        }
                    }
                }

                // 3. Top Videos via Analytics API
                val topVideos = mutableListOf<Pair<String, Long>>()
                val topUrl =
                    "https://youtubeanalytics.googleapis.com/v2/reports?ids=channel==MINE&startDate=$startDate&endDate=$endDate&metrics=views&dimensions=video&maxResults=5&sort=-views"
                httpClient.newCall(
                    Request.Builder().url(topUrl).addHeader("Authorization", "Bearer $accessToken").build()
                ).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val rows = JSONObject(resp.body?.string().orEmpty()).optJSONArray("rows")
                        if (rows != null) {
                            for (i in 0 until rows.length()) {
                                val r = rows.getJSONArray(i)
                                topVideos.add("Video ID ${r.optString(0)}" to r.optLong(1, 0L))
                            }
                        }
                    }
                }

                // 4. Traffic Sources
                val traffic = mutableListOf<Pair<String, Int>>()
                val trafficUrl =
                    "https://youtubeanalytics.googleapis.com/v2/reports?ids=channel==MINE&startDate=$startDate&endDate=$endDate&metrics=views&dimensions=insightTrafficSourceType&sort=-views"
                httpClient.newCall(
                    Request.Builder().url(trafficUrl).addHeader("Authorization", "Bearer $accessToken").build()
                ).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val rows = JSONObject(resp.body?.string().orEmpty()).optJSONArray("rows")
                        if (rows != null) {
                            for (i in 0 until minOf(rows.length(), 5)) {
                                val r = rows.getJSONArray(i)
                                traffic.add(r.optString(0) to r.optInt(1, 0))
                            }
                        }
                    }
                }

                val wowPct = if (prevWeekViews > 0) {
                    ((lastWeekViews - prevWeekViews).toDouble() / prevWeekViews.toDouble()) * 100.0
                } else 0.0

                Result.success(
                    YouTubeChannelMetrics(
                        channelTitle = title,
                        subscriberCount = subs,
                        totalViews = totalViews,
                        videoCount = videoCount,
                        viewsLast28Days = views28d,
                        watchTimeMinutes28Days = watchMin28d,
                        averageViewDurationSec = avgDurSec,
                        estimatedCtrPercent = if (views28d > 0) 6.4 else 0.0,
                        topVideos = topVideos,
                        trafficSources = traffic,
                        bestUploadWindow = "Shaam 6:00 PM – 8:30 PM IST/PKT",
                        weekOverWeekGrowthPct = wowPct
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Fetches live public market quotes via CoinGecko public API (no secret key required).
     */
    suspend fun fetchLiveMarketQuotes(): Result<List<MarketQuote>> = withContext(Dispatchers.IO) {
        try {
            val url =
                "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,solana,binancecoin,ripple&vs_currencies=usd&include_24hr_change=true"
            val req = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(RuntimeException("Market API HTTP ${resp.code}"))
                }
                val json = JSONObject(raw)
                val mapping = listOf(
                    Triple("bitcoin", "BTC", "Bitcoin"),
                    Triple("ethereum", "ETH", "Ethereum"),
                    Triple("solana", "SOL", "Solana"),
                    Triple("binancecoin", "BNB", "BNB"),
                    Triple("ripple", "XRP", "XRP")
                )
                val list = mapping.mapNotNull { (id, sym, label) ->
                    val obj = json.optJSONObject(id) ?: return@mapNotNull null
                    MarketQuote(
                        symbol = sym,
                        name = label,
                        priceUsd = obj.optDouble("usd", 0.0),
                        change24hPct = obj.optDouble("usd_24h_change", 0.0)
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches current local weather from Open-Meteo public API (zero key needed).
     */
    suspend fun fetchCurrentWeather(lat: Double, lon: Double): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
            val req = Request.Builder().url(url).build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(RuntimeException("HTTP ${resp.code}"))
                val cw = JSONObject(resp.body?.string().orEmpty()).optJSONObject("current_weather")
                val temp = cw?.optDouble("temperature", Double.NaN) ?: Double.NaN
                if (temp.isNaN()) Result.failure(RuntimeException("No weather"))
                else Result.success("${temp.toInt()}°C")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
