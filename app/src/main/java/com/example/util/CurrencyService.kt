package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object CurrencyService {
    val SUPPORTED_CURRENCIES = listOf(
        "USD", "EUR", "INR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY", "AED", "SAR", "SGD", "NZD", "ZAR", "BRL", "MXN"
    )

    // Hardcoded offline fallback rates (base USD)
    val DEFAULT_RATES = mapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "INR" to 83.50,
        "GBP" to 0.79,
        "JPY" to 157.80,
        "CAD" to 1.37,
        "AUD" to 1.50,
        "CHF" to 0.89,
        "CNY" to 7.25,
        "AED" to 3.67,
        "SAR" to 3.75,
        "SGD" to 1.35,
        "NZD" to 1.63,
        "ZAR" to 18.20,
        "BRL" to 5.40,
        "MXN" to 18.30
    )

    suspend fun fetchLatestRates(): Map<String, Double> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://open.er-api.com/v6/latest/USD")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val jsonObject = JSONObject(response.toString())
                if (jsonObject.getString("result") == "success") {
                    val ratesObject = jsonObject.getJSONObject("rates")
                    val parsedRates = mutableMapOf<String, Double>()
                    for (currency in SUPPORTED_CURRENCIES) {
                        if (ratesObject.has(currency)) {
                            parsedRates[currency] = ratesObject.getDouble(currency)
                        }
                    }
                    if (parsedRates.isNotEmpty()) {
                        return@withContext parsedRates
                    }
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext DEFAULT_RATES
    }
}
