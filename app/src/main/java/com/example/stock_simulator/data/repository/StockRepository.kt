package com.example.stock_simulator.data.repository

import com.example.stock_simulator.data.remote.RetrofitClient
import com.example.stock_simulator.data.remote.dto.StockMsgDto
import com.example.stock_simulator.domain.model.IntradayTick
import com.example.stock_simulator.domain.model.KLinePoint
import com.example.stock_simulator.domain.model.MarketIndex
import com.example.stock_simulator.domain.model.StockQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class StockRepository {

    private val twseMisApi = RetrofitClient.twseMisApi
    private val twseOpenApi = RetrofitClient.twseOpenApi
    private val tpexOpenApi = RetrofitClient.tpexOpenApi
    private val finMindApi = RetrofitClient.finMindApi

    companion object {
        // 全台股快取字典 (~2,000+ 檔股票名稱與歷史行情快取)
        private val cachedAllQuotes = ConcurrentHashMap<String, StockQuote>()
    }

    // 熱門台股預設清單 (可供首頁列表與搜尋)
    val popularStocks = listOf(
        Pair("2330", "台積電"),
        Pair("2317", "鴻海"),
        Pair("2454", "聯發科"),
        Pair("2308", "台達電"),
        Pair("2881", "富邦金"),
        Pair("2382", "廣達"),
        Pair("0050", "元大台灣50"),
        Pair("00878", "國泰永續高股息")
    )

    /**
     * 取得全台股所有上市與上櫃股票列表 (~2,000+ 檔股票，即時可供搜尋與瀏覽)
     */
    suspend fun getAllTaiwanStocks(): List<StockQuote> = withContext(Dispatchers.IO) {
        if (cachedAllQuotes.isNotEmpty()) {
            return@withContext cachedAllQuotes.values.toList()
        }

        val allQuotes = mutableListOf<StockQuote>()

        try {
            // 1. 上市股票 (TWSE OpenAPI)
            val listed = twseOpenApi.getAllListedStocks()
            listed.forEach { dto ->
                val code = dto.code ?: return@forEach
                val name = dto.name ?: code
                val close = dto.closingPrice?.toDoubleOrNull() ?: return@forEach
                val change = dto.change?.replace("+", "")?.toDoubleOrNull() ?: 0.0
                val prevClose = close - change
                val pct = if (prevClose > 0) (change / prevClose) * 100 else 0.0
                val vol = (dto.tradeVolume?.toLongOrNull() ?: 0L) / 1000L

                val quote = StockQuote(
                    symbol = code,
                    name = name,
                    currentPrice = close,
                    change = change,
                    changePercent = pct,
                    openPrice = dto.openingPrice?.toDoubleOrNull() ?: close,
                    highPrice = dto.highestPrice?.toDoubleOrNull() ?: close,
                    lowPrice = dto.lowestPrice?.toDoubleOrNull() ?: close,
                    previousClose = prevClose,
                    volume = vol
                )
                allQuotes.add(quote)
                cachedAllQuotes[code] = quote
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            // 2. 上櫃股票 (TPEX OpenAPI)
            val otc = tpexOpenApi.getAllOtcStocks()
            otc.forEach { dto ->
                val code = dto.code ?: return@forEach
                val name = dto.name ?: code
                val close = dto.closingPrice?.toDoubleOrNull() ?: return@forEach
                val change = dto.change?.replace("+", "")?.toDoubleOrNull() ?: 0.0
                val prevClose = close - change
                val pct = if (prevClose > 0) (change / prevClose) * 100 else 0.0
                val vol = (dto.tradeVolume?.toLongOrNull() ?: 0L) / 1000L

                val quote = StockQuote(
                    symbol = code,
                    name = name,
                    currentPrice = close,
                    change = change,
                    changePercent = pct,
                    openPrice = dto.openingPrice?.toDoubleOrNull() ?: close,
                    highPrice = dto.highestPrice?.toDoubleOrNull() ?: close,
                    lowPrice = dto.lowestPrice?.toDoubleOrNull() ?: close,
                    previousClose = prevClose,
                    volume = vol
                )
                allQuotes.add(quote)
                cachedAllQuotes[code] = quote
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 若開放 API 加載失敗，為熱門股票提供備用資料
        if (allQuotes.isEmpty()) {
            popularStocks.map { getMockQuote(it.first) }
        } else {
            allQuotes
        }
    }

    /**
     * 取得大盤與國際指數列表 (優先調用 TWSE 官方即時 API)
     */
    suspend fun getMarketIndices(category: String = "台股"): List<MarketIndex> = withContext(Dispatchers.IO) {
        when (category) {
            "台股" -> {
                try {
                    val response = twseMisApi.getStockInfo("tse_t00.tw|otc_o00.tw")
                    val msgList = response.msgArray ?: emptyList()
                    if (msgList.isNotEmpty()) {
                        val tseItem = msgList.find { it.code == "t00" }
                        val otcItem = msgList.find { it.code == "o00" }

                        val result = mutableListOf<MarketIndex>()
                        if (tseItem != null) {
                            val prev = tseItem.yesterdayClose?.toDoubleOrNull() ?: 48353.49
                            val curr = tseItem.currentPrice?.toDoubleOrNull() ?: prev
                            val change = curr - prev
                            val pct = if (prev > 0) (change / prev) * 100 else 0.0

                            result.add(
                                MarketIndex(
                                    symbol = "tse_t00",
                                    name = "上市 (加權指數)",
                                    category = "台股",
                                    currentPrice = curr,
                                    change = change,
                                    changePercent = pct,
                                    turnoverAmount = "8,997.10 億",
                                    openPrice = tseItem.openPrice?.toDoubleOrNull() ?: curr,
                                    highPrice = tseItem.highPrice?.toDoubleOrNull() ?: curr,
                                    lowPrice = tseItem.lowPrice?.toDoubleOrNull() ?: curr,
                                    previousClose = prev,
                                    trendPoints = listOf(prev, tseItem.lowPrice?.toDoubleOrNull() ?: curr, curr, tseItem.highPrice?.toDoubleOrNull() ?: curr, curr)
                                )
                            )
                        }

                        if (otcItem != null) {
                            val prev = otcItem.yesterdayClose?.toDoubleOrNull() ?: 418.82
                            val curr = otcItem.currentPrice?.toDoubleOrNull() ?: prev
                            val change = curr - prev
                            val pct = if (prev > 0) (change / prev) * 100 else 0.0

                            result.add(
                                MarketIndex(
                                    symbol = "otc_o00",
                                    name = "上櫃 (櫃買指數)",
                                    category = "台股",
                                    currentPrice = curr,
                                    change = change,
                                    changePercent = pct,
                                    turnoverAmount = "2,721.75 億",
                                    openPrice = otcItem.openPrice?.toDoubleOrNull() ?: curr,
                                    highPrice = otcItem.highPrice?.toDoubleOrNull() ?: curr,
                                    lowPrice = otcItem.lowPrice?.toDoubleOrNull() ?: curr,
                                    previousClose = prev,
                                    trendPoints = listOf(prev, otcItem.openPrice?.toDoubleOrNull() ?: curr, curr)
                                )
                            )
                        }

                        if (result.size >= 2) return@withContext result
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 備用大盤資料 (僅留 上市 與 上櫃)
                listOf(
                    MarketIndex(
                        symbol = "tse_t00",
                        name = "上市 (加權指數)",
                        category = "台股",
                        currentPrice = 48475.74,
                        change = 122.25,
                        changePercent = 0.25,
                        turnoverAmount = "8,997.10 億",
                        openPrice = 48390.65,
                        highPrice = 48491.62,
                        lowPrice = 48205.81,
                        previousClose = 48353.49,
                        trendPoints = listOf(48353.49, 48205.81, 48280.0, 48390.65, 48410.0, 48450.0, 48491.62, 48475.74)
                    ),
                    MarketIndex(
                        symbol = "otc_o00",
                        name = "上櫃 (櫃買指數)",
                        category = "台股",
                        currentPrice = 426.93,
                        change = 8.11,
                        changePercent = 1.94,
                        turnoverAmount = "2,721.75 億",
                        openPrice = 419.94,
                        highPrice = 426.93,
                        lowPrice = 419.94,
                        previousClose = 418.82,
                        trendPoints = listOf(418.82, 419.94, 421.0, 423.5, 425.0, 426.0, 426.93)
                    )
                )
            }

            "亞股" -> listOf(
                MarketIndex(
                    symbol = "N225",
                    name = "日經 225",
                    category = "亞股",
                    currentPrice = 38720.50,
                    change = 245.10,
                    changePercent = 0.64,
                    turnoverAmount = "3.2 萬億",
                    openPrice = 38500.00,
                    highPrice = 38800.00,
                    lowPrice = 38450.00,
                    previousClose = 38475.40,
                    trendPoints = listOf(38475.40, 38500.00, 38650.00, 38800.00, 38720.50)
                ),
                MarketIndex(
                    symbol = "KS11",
                    name = "韓國 KOSPI",
                    category = "亞股",
                    currentPrice = 2590.20,
                    change = -12.40,
                    changePercent = -0.48,
                    turnoverAmount = "8,520 億",
                    openPrice = 2605.00,
                    highPrice = 2610.00,
                    lowPrice = 2585.00,
                    previousClose = 2602.60,
                    trendPoints = listOf(2602.60, 2605.00, 2585.00, 2590.20)
                ),
                MarketIndex(
                    symbol = "HSI",
                    name = "香港恒生",
                    category = "亞股",
                    currentPrice = 20630.10,
                    change = 310.80,
                    changePercent = 1.53,
                    turnoverAmount = "1,420 億",
                    openPrice = 20350.00,
                    highPrice = 20700.00,
                    lowPrice = 20300.00,
                    previousClose = 20319.30,
                    trendPoints = listOf(20319.30, 20350.00, 20500.00, 20700.00, 20630.10)
                )
            )

            "美股" -> listOf(
                MarketIndex(
                    symbol = "DJI",
                    name = "道瓊工業",
                    category = "美股",
                    currentPrice = 42352.70,
                    change = 120.40,
                    changePercent = 0.29,
                    turnoverAmount = "--",
                    openPrice = 42250.00,
                    highPrice = 42400.00,
                    lowPrice = 42200.00,
                    previousClose = 42232.30,
                    trendPoints = listOf(42232.30, 42250.00, 42300.00, 42400.00, 42352.70)
                ),
                MarketIndex(
                    symbol = "IXIC",
                    name = "納斯達克",
                    category = "美股",
                    currentPrice = 18137.80,
                    change = 158.20,
                    changePercent = 0.88,
                    turnoverAmount = "--",
                    openPrice = 18000.00,
                    highPrice = 18200.00,
                    lowPrice = 17980.00,
                    previousClose = 17979.60,
                    trendPoints = listOf(17979.60, 18000.00, 18100.00, 18200.00, 18137.80)
                ),
                MarketIndex(
                    symbol = "GSPC",
                    name = "標普 500",
                    category = "美股",
                    currentPrice = 5751.10,
                    change = 24.30,
                    changePercent = 0.42,
                    turnoverAmount = "--",
                    openPrice = 5730.00,
                    highPrice = 5760.00,
                    lowPrice = 5720.00,
                    previousClose = 5726.80,
                    trendPoints = listOf(5726.80, 5730.00, 5750.00, 5760.00, 5751.10)
                ),
                MarketIndex(
                    symbol = "SOX",
                    name = "費城半導體",
                    category = "美股",
                    currentPrice = 5210.60,
                    change = 82.50,
                    changePercent = 1.61,
                    turnoverAmount = "--",
                    openPrice = 5140.00,
                    highPrice = 5230.00,
                    lowPrice = 5130.00,
                    previousClose = 5128.10,
                    trendPoints = listOf(5128.10, 5140.00, 5190.00, 5230.00, 5210.60)
                )
            )

            "歐股" -> listOf(
                MarketIndex(
                    symbol = "FTSE",
                    name = "英國富時 100",
                    category = "歐股",
                    currentPrice = 8280.60,
                    change = -15.20,
                    changePercent = -0.18,
                    turnoverAmount = "--",
                    openPrice = 8295.00,
                    highPrice = 8300.00,
                    lowPrice = 8270.00,
                    previousClose = 8295.80,
                    trendPoints = listOf(8295.80, 8295.00, 8270.00, 8280.60)
                ),
                MarketIndex(
                    symbol = "GDAXI",
                    name = "德國 DAX",
                    category = "歐股",
                    currentPrice = 19210.40,
                    change = 95.10,
                    changePercent = 0.50,
                    turnoverAmount = "--",
                    openPrice = 19120.00,
                    highPrice = 19250.00,
                    lowPrice = 19110.00,
                    previousClose = 19115.30,
                    trendPoints = listOf(19115.30, 19120.00, 19200.00, 19250.00, 19210.40)
                )
            )

            else -> emptyList()
        }
    }

    /**
     * 取得盤中分時走勢點 (09:00 - 13:30)
     */
    suspend fun getIntradayTicks(symbol: String, previousClose: Double = 100.0): List<IntradayTick> = withContext(Dispatchers.IO) {
        val quote = getStockQuote(symbol)
        val open = if (quote.openPrice > 0) quote.openPrice else previousClose
        val high = maxOf(quote.highPrice, open, quote.currentPrice)
        val low = minOf(quote.lowPrice, open, quote.currentPrice).coerceAtLeast(0.01)
        val close = quote.currentPrice
        val prevClose = if (quote.previousClose > 0) quote.previousClose else previousClose

        val ticks = mutableListOf<IntradayTick>()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 0)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.TAIWAN)

        val totalTicks = 55 // 09:00 到 13:30，共 55 個時間點
        val seed = symbol.hashCode() + SimpleDateFormat("yyyyMMdd", Locale.TAIWAN).format(Date()).hashCode()
        val rng = kotlin.random.Random(seed)

        val highTickIdx = rng.nextInt(10, 40)
        var lowTickIdx = rng.nextInt(5, 50)
        if (lowTickIdx == highTickIdx) lowTickIdx = (highTickIdx + 15) % totalTicks

        val cumulativeVolume = quote.volume.coerceAtLeast(100L)
        val avgVolPerTick = (cumulativeVolume / totalTicks).toInt().coerceAtLeast(5)

        for (i in 0 until totalTicks) {
            val timeStr = timeFormat.format(calendar.time)

            val baseP = when {
                i == 0 -> open
                i == totalTicks - 1 -> close
                i == highTickIdx -> high
                i == lowTickIdx -> low
                else -> {
                    val progress = i.toDouble() / (totalTicks - 1)
                    val trendP = open + (close - open) * progress

                    val distHigh = 1.0 - kotlin.math.abs(i - highTickIdx).toDouble() / totalTicks
                    val distLow = 1.0 - kotlin.math.abs(i - lowTickIdx).toDouble() / totalTicks

                    val highWeight = Math.pow(distHigh.coerceAtLeast(0.0), 2.0)
                    val lowWeight = Math.pow(distLow.coerceAtLeast(0.0), 2.0)

                    trendP + (high - trendP) * highWeight * 0.6 - (trendP - low) * lowWeight * 0.6
                }
            }

            val noisePct = (rng.nextDouble() - 0.5) * 0.003
            var tickPrice = baseP * (1.0 + noisePct)

            if (i == 0) tickPrice = open
            if (i == totalTicks - 1) tickPrice = close
            tickPrice = tickPrice.coerceIn(low, high)

            val volNoise = rng.nextInt(-avgVolPerTick / 2, avgVolPerTick / 2 + 1)
            val tickVol = (avgVolPerTick + volNoise).toLong().coerceAtLeast(1L)

            ticks.add(
                IntradayTick(
                    time = timeStr,
                    price = tickPrice,
                    volume = tickVol,
                    change = tickPrice - prevClose
                )
            )
            calendar.add(Calendar.MINUTE, 5)
        }
        ticks
    }

    /**
     * 批次取得股票即時行情 (例如代號列表 ["2330", "6789"])
     * 為確保不論上市(tse)或上櫃(otc)都能 100% 成功抓取，自動同時向 TWSE MIS 查詢 tse_ 及 otc_
     */
    suspend fun getStockQuotes(symbols: List<String>): List<StockQuote> = withContext(Dispatchers.IO) {
        if (symbols.isEmpty()) return@withContext emptyList()

        val cleanSymbols = symbols.map { it.replace("tse_", "").replace("otc_", "") }
        val exCh = cleanSymbols.joinToString("|") { symbol ->
            if (symbol == "t00" || symbol == "o00") {
                if (symbol == "t00") "tse_t00.tw" else "otc_o00.tw"
            } else {
                "tse_$symbol.tw|otc_$symbol.tw"
            }
        }

        try {
            val response = twseMisApi.getStockInfo(exCh = exCh)
            val msgList = response.msgArray ?: emptyList()
            val parsedQuotes = msgList.mapNotNull { dto ->
                if (dto.code.isNullOrEmpty() || dto.name.isNullOrEmpty()) null
                else parseStockMsgDto(dto)
            }

            // 整合 TWSE MIS 即時資料與全台股快取字典
            val resultMap = mutableMapOf<String, StockQuote>()
            parsedQuotes.forEach { quote ->
                resultMap[quote.symbol] = quote
                cachedAllQuotes[quote.symbol] = quote
            }

            cleanSymbols.map { sym ->
                resultMap[sym] ?: cachedAllQuotes[sym] ?: getMockQuote(sym)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            cleanSymbols.map { sym ->
                cachedAllQuotes[sym] ?: getMockQuote(sym)
            }
        }
    }

    /**
     * 取得單一股票或指數即時行情
     */
    suspend fun getStockQuote(symbol: String): StockQuote = withContext(Dispatchers.IO) {
        val cleanSym = symbol.replace("tse_", "").replace("otc_", "")
        val quotes = getStockQuotes(listOf(cleanSym))
        quotes.firstOrNull() ?: cachedAllQuotes[cleanSym] ?: getMockQuote(cleanSym)
    }

    /**
     * 取得 K 線數據 (FinMind 官方 API，立即線上下載)
     */
    suspend fun getKLineData(symbol: String): List<KLinePoint> = withContext(Dispatchers.IO) {
        val cleanSymbol = symbol.replace("tse_", "").replace("otc_", "")

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.TAIWAN)
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, -3) // 拉取近 3 個月 K 線
            val startDate = dateFormat.format(calendar.time)

            val response = finMindApi.getKLineData(dataId = cleanSymbol, startDate = startDate)
            val dataList = response.data ?: emptyList()

            if (dataList.isNotEmpty()) {
                return@withContext dataList.map { dto ->
                    KLinePoint(
                        date = dto.date ?: "",
                        open = dto.open ?: 0.0,
                        high = dto.max ?: 0.0,
                        low = dto.min ?: 0.0,
                        close = dto.close ?: 0.0,
                        volume = (dto.tradingVolume ?: 0L) / 1000L
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 若線上 K 線 API 未回傳或無交易紀錄，根據該股票當前即時價格動態生成 K 線
        generateMockKLines(cleanSymbol)
    }

    private fun parseStockMsgDto(dto: StockMsgDto): StockQuote {
        val symbol = dto.code ?: ""
        val name = dto.name ?: symbol
        val yesterdayClose = dto.yesterdayClose?.toDoubleOrNull() ?: 100.0
        val currentPrice = dto.currentPrice?.toDoubleOrNull()
            ?: dto.openPrice?.toDoubleOrNull()
            ?: yesterdayClose

        val change = currentPrice - yesterdayClose
        val changePercent = if (yesterdayClose > 0) (change / yesterdayClose) * 100 else 0.0

        val buyPrices = parseDoubleList(dto.buyPrices)
        val buyVolumes = parseIntList(dto.buyVolumes)
        val sellPrices = parseDoubleList(dto.sellPrices)
        val sellVolumes = parseIntList(dto.sellVolumes)

        val timeStr = dto.timestamp?.toLongOrNull()?.let {
            SimpleDateFormat("HH:mm:ss", Locale.TAIWAN).format(Date(it))
        } ?: SimpleDateFormat("HH:mm:ss", Locale.TAIWAN).format(Date())

        return StockQuote(
            symbol = symbol,
            name = name,
            currentPrice = currentPrice,
            change = change,
            changePercent = changePercent,
            openPrice = dto.openPrice?.toDoubleOrNull() ?: currentPrice,
            highPrice = dto.highPrice?.toDoubleOrNull() ?: currentPrice,
            lowPrice = dto.lowPrice?.toDoubleOrNull() ?: currentPrice,
            previousClose = yesterdayClose,
            volume = dto.volume?.toLongOrNull() ?: 0L,
            buyFivePrices = buyPrices,
            buyFiveVolumes = buyVolumes,
            sellFivePrices = sellPrices,
            sellFiveVolumes = sellVolumes,
            updateTime = timeStr
        )
    }

    private fun parseDoubleList(raw: String?): List<Double> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split("_").mapNotNull { it.toDoubleOrNull() }
    }

    private fun parseIntList(raw: String?): List<Int> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split("_").mapNotNull { it.toIntOrNull() }
    }

    private fun getMockQuote(symbol: String): StockQuote {
        val cleanSym = symbol.replace("tse_", "").replace("otc_", "")
        val cached = cachedAllQuotes[cleanSym]
        if (cached != null) return cached

        val name = when (cleanSym) {
            "t00" -> "加權指數"
            "o00" -> "櫃買指數"
            "6789" -> "采鈺"
            "2330" -> "台積電"
            "2317" -> "鴻海"
            "2454" -> "聯發科"
            else -> popularStocks.find { it.first == cleanSym }?.second ?: "台股股票 ($cleanSym)"
        }
        val basePrice = when (cleanSym) {
            "t00" -> 48475.74
            "o00" -> 426.93
            "6789" -> 490.50
            "2330" -> 1050.0
            "2317" -> 210.0
            "2454" -> 1350.0
            "2308" -> 390.0
            "2881" -> 92.0
            "2382" -> 280.0
            "0050" -> 195.0
            "00878" -> 23.0
            else -> 100.0
        }
        val change = 2.0
        val changePercent = (change / (basePrice - change)) * 100

        return StockQuote(
            symbol = cleanSym,
            name = name,
            currentPrice = basePrice,
            change = change,
            changePercent = changePercent,
            openPrice = basePrice - 2,
            highPrice = basePrice + 5,
            lowPrice = basePrice - 3,
            previousClose = basePrice - change,
            volume = 1200,
            buyFivePrices = listOf(basePrice - 0.5, basePrice - 1.0, basePrice - 1.5, basePrice - 2.0, basePrice - 2.5),
            buyFiveVolumes = listOf(120, 85, 230, 410, 105),
            sellFivePrices = listOf(basePrice + 0.5, basePrice + 1.0, basePrice + 1.5, basePrice + 2.0, basePrice + 2.5),
            sellFiveVolumes = listOf(95, 140, 310, 80, 200),
            updateTime = SimpleDateFormat("HH:mm:ss", Locale.TAIWAN).format(Date())
        )
    }

    private fun generateMockKLines(symbol: String): List<KLinePoint> {
        val cleanSym = symbol.replace("tse_", "").replace("otc_", "")
        val quote = cachedAllQuotes[cleanSym] ?: getMockQuote(cleanSym)
        val list = mutableListOf<KLinePoint>()
        var price = quote.currentPrice * 0.95

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.TAIWAN)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -30)

        for (i in 0 until 30) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val delta = (-5..8).random().toDouble()
            val open = price
            val close = if (i == 29) quote.currentPrice else open + delta
            val high = maxOf(open, close) + (0..3).random().toDouble()
            val low = minOf(open, close) - (0..3).random().toDouble()
            val volume = (1000..8000).random().toLong()

            list.add(
                KLinePoint(
                    date = dateFormat.format(calendar.time),
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume
                )
            )
            price = close
        }
        return list
    }
}
