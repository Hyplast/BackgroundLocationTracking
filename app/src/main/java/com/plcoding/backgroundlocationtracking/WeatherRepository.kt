package com.plcoding.backgroundlocationtracking

import java.io.InputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

interface WeatherRepository {
    suspend fun getObservationData(lat: Double, long: Double): Resource<ObservationInfo>
}

class WeatherRepositoryImpl (
    private val apiFMI: ObservationApi
) : WeatherRepository {

    override suspend fun getObservationData(lat: Double, long: Double): Resource<ObservationInfo> {
        return try {
            Resource.Success(
                data = apiFMI.getWeatherObservationData(
                    bbox = "${(long - 0.4)},${(lat - 0.4)},${(long + 0.4)},${(lat + 0.4)}"
                ).toObservationInfo(long, lat)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(e.message ?: "An unknown error occurred.")
        }
    }


}

private fun String.toObservationInfo(long: Double, lat: Double): ObservationInfo {
    val inputStream: InputStream = this.byteInputStream()
    val parser = FMIXmlParser()
    val parsed = parser.parse(inputStream)

    val observationDataMap = parsed.toObservationDataMap()

    val highestTimeStamp = observationDataMap.keys.maxByOrNull { it }
    val highestTimeStampList = observationDataMap[highestTimeStamp]

    val closestLocation = highestTimeStampList?.findClosestLocation(lat, long)
    return ObservationInfo(
        observationDataPerDay = observationDataMap,//weatherDataMap,
        currentObservationData = closestLocation//currentWeatherData
    )
}

fun List<Observation>.toObservationDataMap(): Map<Int, List<Observation>> {
    return this.groupBy { it.timeStamp }
        .mapValues { (_, observations) -> observations.sortedByDescending { it.timeStamp } }
        .toList()
        .sortedByDescending { it.first }
        .toMap()
}


fun List<Observation>.findClosestLocation(lat: Double, long: Double): Observation? {
    return this.filter { it.ws10min != null }
        .minByOrNull { distance(lat, long, it.lat, it.long) }
}

sealed class Resource<T>(val data: T? = null, val message: String? = null) {
    class Success<T>(data: T?): Resource<T>(data)
    class Error<T>(message: String, data: T? = null): Resource<T>(data, message)
}

data class ObservationInfo(
    val observationDataPerDay: Map<Int, List<Observation>>?,
    val currentObservationData: Observation?
)

fun distance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
    val r = 6371 // Earth radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLong = Math.toRadians(long2 - long1)
    val a = (sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLong / 2) * sin(dLong / 2))
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

fun bearing(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
    val dLon = Math.toRadians(long2 - long1)
    val y = sin(dLon) * cos(Math.toRadians(lat2))
    val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) - sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(dLon)
    return Math.toDegrees(atan2(y, x))
}

fun bearingToDirection(bearing: Double): String {
    val directions = arrayOf("pohjoiseen", "koiliseen", "itään", "kaakkoon", "etelään", "lounaaseen", "länteen", "luoteeseen")
    val index = ((bearing + 22.5) / 45).toInt() and 7
    return directions[index]
}