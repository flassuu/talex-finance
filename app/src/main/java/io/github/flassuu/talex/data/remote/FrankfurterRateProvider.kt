package io.github.flassuu.talex.data.remote

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.domain.repository.RateProvider
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
 * Fetches ECB reference rates from frankfurter.app on an explicit user action
 * only (ADR-0032). No background access, no telemetry, no key.
 *
 * The public endpoint does not cover every currency (UAH is not part of the ECB
 * reference set), so unsupported targets are simply omitted and stay manual.
 */
@Singleton
class FrankfurterRateProvider @Inject constructor() : RateProvider {

    override suspend fun fetch(
        base: CurrencyCode,
        targets: Set<CurrencyCode>,
    ): Result<Map<CurrencyCode, BigDecimal>> = withContext(Dispatchers.IO) {
        val wanted = targets.filter { it != base }
        if (wanted.isEmpty()) return@withContext Result.success(emptyMap())
        runCatching {
            try {
                parseRates(get(url(base, wanted)))
            } catch (batchError: Exception) {
                // One unsupported currency can reject the whole batch. Fall back to
                // individual requests so the supported pairs are still stored.
                if (wanted.size == 1) throw batchError
                val collected = mutableMapOf<CurrencyCode, BigDecimal>()
                wanted.forEach { target ->
                    runCatching { parseRates(get(url(base, listOf(target)))) }
                        .getOrNull()
                        ?.let { collected.putAll(it) }
                }
                if (collected.isEmpty()) throw batchError
                collected
            }
        }
    }

    private fun url(base: CurrencyCode, targets: List<CurrencyCode>): URL = URL(
        "$BASE_URL/latest?from=${base.value}&to=" + targets.joinToString(",") { it.value },
    )

    internal fun parseRates(body: String): Map<CurrencyCode, BigDecimal> {
        val rates = JSONObject(body).optJSONObject("rates") ?: return emptyMap()
        val result = mutableMapOf<CurrencyCode, BigDecimal>()
        rates.keys().forEach { code ->
            val value = rates.optString(code).toBigDecimalOrNull() ?: return@forEach
            val currency = CurrencyCode.ofOrNull(code) ?: return@forEach
            if (value.signum() > 0) result[currency] = value
        }
        return result
    }

    private fun get(url: URL): String {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
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
        const val BASE_URL = "https://api.frankfurter.app"
        private const val TIMEOUT_MS = 10_000
    }
}
