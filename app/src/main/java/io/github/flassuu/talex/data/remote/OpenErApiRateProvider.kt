package io.github.flassuu.talex.data.remote

import io.github.flassuu.talex.core.money.CurrencyCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fallback rate source that covers the currencies ECB data omits, including UAH
 * (ADR-0037). It returns a full rate map for the requested base, so only the
 * targets still missing from the primary provider are asked for.
 */
@Singleton
class OpenErApiRateProvider @Inject constructor() {

    suspend fun fetch(
        base: CurrencyCode,
        targets: Set<CurrencyCode>,
    ): Result<Map<CurrencyCode, BigDecimal>> = withContext(Dispatchers.IO) {
        val wanted = targets.filter { it != base }
        if (wanted.isEmpty()) return@withContext Result.success(emptyMap())
        runCatching {
            val body = get(URL("$BASE_URL/${base.value}"))
            val json = JSONObject(body)
            if (json.optString("result") != "success") {
                throw IOException("Rate provider returned an error")
            }
            val rates = json.optJSONObject("rates")
                ?: throw IOException("Rate provider returned no rates")
            val result = mutableMapOf<CurrencyCode, BigDecimal>()
            wanted.forEach { target ->
                val value = rates.optString(target.value).toBigDecimalOrNull() ?: return@forEach
                if (value.signum() > 0) result[target] = value
            }
            result
        }
    }

    private fun get(url: URL): String {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode !in 200..299) {
                throw IOException("Rate provider returned ${connection.responseCode}")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val BASE_URL = "https://open.er-api.com/v6/latest"
        private const val TIMEOUT_MS = 10_000
    }
}
