package com.statefarm.hubcompanion

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToInt

internal data class WeatherSnapshot(
    val temperature: Int,
    val condition: String,
    val high: Int,
    val low: Int,
    val rainChance: Int,
    val hourly: List<Pair<String, Int>>,
)

internal sealed interface WeatherState {
    data object Loading : WeatherState
    data class Ready(val weather: WeatherSnapshot) : WeatherState
    data class Error(val message: String) : WeatherState
}

internal object WeatherRepository {
    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
        install(Logging) {
            level = LogLevel.INFO
            logger = object : Logger {
                override fun log(message: String) {
                    WeatherLog.debug("Ktor: $message")
                }
            }
        }
    }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun weatherFor(hub: UserLocation): WeatherSnapshot {
        WeatherLog.debug(
            "Starting request: hub=${hub.hubName}, city=${hub.city}, " +
                "latitude=${hub.latitude}, longitude=${hub.longitude}",
        )
        val response: HttpResponse = client.get("https://api.open-meteo.com/v1/forecast") {
            parameter("latitude", hub.latitude)
            parameter("longitude", hub.longitude)
            parameter("current", "temperature_2m,weather_code")
            parameter("hourly", "temperature_2m")
            parameter("daily", "temperature_2m_max,temperature_2m_min,precipitation_probability_max")
            parameter("temperature_unit", "fahrenheit")
            parameter("timezone", "auto")
            parameter("forecast_days", 1)
            parameter("forecast_hours", 12)
        }

        WeatherLog.debug("Response received: status=${response.status.value}")
        val responseBody = response.bodyAsText()
        WeatherLog.debug("Response body length=${responseBody.length}")

        if (!response.status.isSuccess()) {
            WeatherLog.error(
                "Non-success response: status=${response.status.value}, body=${responseBody.take(500)}",
            )
            error("Weather service returned ${response.status.value}")
        }

        val root = try {
            json.parseToJsonElement(responseBody).jsonObject
        } catch (throwable: Throwable) {
            WeatherLog.error("Failed to parse JSON: ${responseBody.take(500)}", throwable)
            throw throwable
        }
        val current = root.getValue("current").jsonObject
        val daily = root.getValue("daily").jsonObject
        val hourly = root.getValue("hourly").jsonObject
        val temperatures = hourly.getValue("temperature_2m").jsonArray

        val hourlyForecast = listOf(0, 3, 6).mapNotNull { index ->
            temperatures.getOrNull(index)?.jsonPrimitive?.double?.roundToInt()?.let { temperature ->
                (if (index == 0) "Now" else "+${index}h") to temperature
            }
        }

        val snapshot = WeatherSnapshot(
            temperature = current.getValue("temperature_2m").jsonPrimitive.double.roundToInt(),
            condition = weatherDescription(current.getValue("weather_code").jsonPrimitive.int),
            high = daily.getValue("temperature_2m_max").jsonArray.first().jsonPrimitive.double.roundToInt(),
            low = daily.getValue("temperature_2m_min").jsonArray.first().jsonPrimitive.double.roundToInt(),
            rainChance = daily.getValue("precipitation_probability_max").jsonArray.first().jsonPrimitive.int,
            hourly = hourlyForecast,
        )
        WeatherLog.debug(
            "Parsed weather: temperature=${snapshot.temperature}, " +
                "condition=${snapshot.condition}, high=${snapshot.high}, " +
                "low=${snapshot.low}, rainChance=${snapshot.rainChance}, " +
                "hourlyCount=${snapshot.hourly.size}",
        )
        return snapshot
    }

    private fun weatherDescription(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        51, 53, 55, 56, 57 -> "Drizzle"
        61, 63, 65, 66, 67, 80, 81, 82 -> "Rain"
        71, 73, 75, 77, 85, 86 -> "Snow"
        95, 96, 99 -> "Thunderstorms"
        else -> "Current conditions"
    }
}
