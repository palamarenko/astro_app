package com.iruna.app.data

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ── Response models ───────────────────────────────────────────────────────────

@Serializable
data class PlacePrediction(
    val description: String,
    @SerialName("place_id") val placeId: String,
    @SerialName("structured_formatting") val formatting: PlaceFormatting = PlaceFormatting(),
)

@Serializable
data class PlaceFormatting(
    @SerialName("main_text") val mainText: String = "",
    @SerialName("secondary_text") val secondaryText: String = "",
)

@Serializable
data class PlaceDetails(
    val name: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
)

@Serializable
private data class AutocompleteResponse(
    val predictions: List<PlacePrediction> = emptyList(),
    val status: String = "",
)

@Serializable
private data class DetailsResponse(
    val result: DetailsResult = DetailsResult(),
    val status: String = "",
)

@Serializable
private data class DetailsResult(
    val geometry: Geometry = Geometry(),
    @SerialName("formatted_address") val formattedAddress: String = "",
)

@Serializable
private data class Geometry(
    val location: LatLng = LatLng(),
)

@Serializable
private data class LatLng(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
)

// ── Client ────────────────────────────────────────────────────────────────────

class PlacesApiClient(private val apiKey: String) {

    private val client = createHttpClient(Json { ignoreUnknownKeys = true; isLenient = true })

    /**
     * Возвращает список подсказок по введённому тексту.
     * Язык ru по умолчанию, типы — города.
     */
    suspend fun autocomplete(query: String, language: String = "ru"): List<PlacePrediction> {
        if (query.isBlank() || apiKey.isEmpty()) return emptyList()
        return try {
            val response: AutocompleteResponse = client.get(
                "https://maps.googleapis.com/maps/api/place/autocomplete/json"
            ) {
                parameter("input", query)
                parameter("key", apiKey)
                parameter("language", language)
                parameter("types", "(cities)")
            }.body()
            if (response.status == "OK") response.predictions else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Возвращает координаты и адрес по placeId.
     */
    suspend fun placeDetails(placeId: String): PlaceDetails {
        return try {
            val response: DetailsResponse = client.get(
                "https://maps.googleapis.com/maps/api/place/details/json"
            ) {
                parameter("place_id", placeId)
                parameter("key", apiKey)
                parameter("fields", "geometry,formatted_address")
            }.body()
            if (response.status == "OK") {
                PlaceDetails(
                    name = response.result.formattedAddress,
                    lat  = response.result.geometry.location.lat,
                    lng  = response.result.geometry.location.lng,
                )
            } else PlaceDetails()
        } catch (e: Exception) {
            PlaceDetails()
        }
    }
}
