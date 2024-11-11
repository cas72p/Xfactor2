import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonClass

// Adapter class to parse JSON into GridPointResponse object
class GridPointResponseAdapter {
    fun fromJson(json: String): GridPointResponse? {
        // Parse JSON string into a Map using Moshi
        val jsonObject = Moshi.Builder().build().adapter(Map::class.java).fromJson(json)
        val propertiesMap = jsonObject?.get("properties") as? Map<*, *>

        // Extract grid point details from properties and create GridPoint object
        val gridPoint = propertiesMap?.let {
            GridPoint(it["gridId"] as String, it["gridX"] as Double, it["gridY"] as Double)
        }
        return gridPoint?.let { GridPointResponse(it) }
    }
}

// Adapter class to parse JSON into ForecastResponse object
class ForecastResponseAdapter {
    fun fromJson(json: String): ForecastResponse? {
        // Parse JSON string into a Map using Moshi
        val jsonObject = Moshi.Builder().build().adapter(Map::class.java).fromJson(json)
        val propertiesMap = jsonObject?.get("properties") as? Map<*, *>

        // Extract periods list from properties and create a list of Period objects
        val periodsList = propertiesMap?.get("periods") as? List<*>
        val periods = periodsList?.map { period ->
            (period as? Map<*, *>)?.let {
                Period(
                    name = it["name"] as? String,
                    temperature = (it["temperature"] as? Number)?.toInt(),
                    temperatureUnit = it["temperatureUnit"] as? String,
                    windSpeed = it["windSpeed"] as? String,
                    shortForecast = it["shortForecast"] as? String,
                    detailedForecast = it["detailedForecast"] as? String
                )
            }
        }?.filterNotNull() ?: emptyList()

        return ForecastResponse(ForecastProperties(periods))
    }
}

// Main class for interfacing with the National Weather Service API
class NWS {
    private val client = OkHttpClient()  // HTTP client for making requests
    private val baseUrl = "https://api.weather.gov"  // Base URL for NWS API
    private val gridPointResponseAdapter = GridPointResponseAdapter()
    private val forecastResponseAdapter = ForecastResponseAdapter()

    // Function to retrieve grid point data based on latitude and longitude
    fun getGridPoint(lat: Double, lon: Double): GridPointResponse? {
        val url = "$baseUrl/points/$lat,$lon"  // Construct URL for grid point
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        val responseData = response.body?.string()
        if (response.isSuccessful && !responseData.isNullOrEmpty()) {
            Log.d("NWS", "GridPoint Response: $responseData")
            return gridPointResponseAdapter.fromJson(responseData)
        } else {
            throw Exception("Failed to get grid point data: ${response.code}")
        }
    }

    // Function to retrieve forecast data based on grid location
    fun getForecast(gridId: String, gridX: Double, gridY: Double): List<Period>? {
        val url = "$baseUrl/gridpoints/$gridId/$gridX,$gridY/forecast"  // Construct URL for forecast
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        val responseData = response.body?.string()
        if (response.isSuccessful && !responseData.isNullOrEmpty()) {
            Log.d("NWS", "Forecast Response: $responseData")
            return forecastResponseAdapter.fromJson(responseData)?.properties?.periods
        } else {
            throw Exception("Failed to get forecast data: ${response.code}")
        }
    }
}

// Data class representing a grid point with an ID and coordinates
data class GridPoint(val gridId: String, val gridX: Double, val gridY: Double)

// Data class for grid point response
data class GridPointResponse(val properties: GridPoint)

// Data class for forecast response
data class ForecastResponse(val properties: ForecastProperties)

// Data class containing the list of forecast periods
data class ForecastProperties(val periods: List<Period>)

// Data class for each forecast period with relevant weather details
data class Period(
    val name: String?,
    val temperature: Int?,
    val temperatureUnit: String?,
    val windSpeed: String?,
    val shortForecast: String?,
    val detailedForecast: String?
)
