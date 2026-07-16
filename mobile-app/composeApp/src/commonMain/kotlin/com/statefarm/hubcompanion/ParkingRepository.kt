package com.statefarm.hubcompanion

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal data class ParkingSpot(
    val id: String,
    val row: Int,
    val number: Int,
    val available: Boolean,
    val confidence: Double,
)

internal data class ParkingAnalysis(
    val totalSpots: Int,
    val freeCount: Int,
    val occupiedCount: Int,
    val spots: List<ParkingSpot>,
)

internal sealed interface ParkingAnalysisState {
    data object Idle : ParkingAnalysisState
    data object Loading : ParkingAnalysisState
    data class Ready(val analysis: ParkingAnalysis) : ParkingAnalysisState
    data class Error(val message: String) : ParkingAnalysisState
}

internal object ParkingRepository {
    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 60_000
        }
    }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun analyze(path: String): ParkingAnalysis {
        val url = "${parkingApiBaseUrl()}/api/v1/parking-lot/$path"
        val response = client.get(url)
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            error("Parking API returned ${response.status.value}: ${body.take(160)}")
        }

        val root = json.parseToJsonElement(body).jsonObject
        val spots = root.getValue("rows").jsonArray.flatMap { rowElement ->
            rowElement.jsonObject.getValue("spots").jsonArray.map { spotElement ->
                val spot = spotElement.jsonObject
                ParkingSpot(
                    id = spot.getValue("id").jsonPrimitive.content,
                    row = spot.getValue("row").jsonPrimitive.int,
                    number = spot.getValue("number").jsonPrimitive.int,
                    available = spot.getValue("status").jsonPrimitive.content == "empty",
                    confidence = spot.getValue("confidence").jsonPrimitive.double,
                )
            }
        }
        return ParkingAnalysis(
            totalSpots = root.getValue("total_spots").jsonPrimitive.int,
            freeCount = root.getValue("free_count").jsonPrimitive.int,
            occupiedCount = root.getValue("occupied_count").jsonPrimitive.int,
            spots = spots,
        )
    }

    suspend fun corporateHqSummary(): Map<String, Int> {
        val response = client.get(
            "${parkingApiBaseUrl()}/api/v1/parking-lot/locations/corporate-hq/summary",
        )
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            error("Parking summary API returned ${response.status.value}")
        }
        return json.parseToJsonElement(body).jsonObject
            .getValue("lots").jsonArray.associate { element ->
                val lot = element.jsonObject
                lot.getValue("lot_id").jsonPrimitive.content to
                    lot.getValue("free_count").jsonPrimitive.int
            }
    }
}

internal expect fun parkingApiBaseUrl(): String
