package com.www.xfactor.xfactormap

import NWS
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.www.xfactor.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class map_activity : AppCompatActivity(), OnMapReadyCallback {

    // Bottom sheet for displaying weather info
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    // Google Map instance
    private lateinit var map: GoogleMap
    // NWS instance for accessing weather data
    private val nws = NWS()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.map_layout)

        // Initialize the bottom sheet and set it to a collapsed state initially
        val bottomSheet = findViewById<View>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        // Set up the Google Maps fragment and prepare for when the map is ready
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Retrieve and display weather data
        getWeather()
    }

    // Callback when the Google Map is ready for use
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
    }

    // Function to fetch weather data and update UI
    private fun getWeather() {
        // Hardcoded coordinates for Kansas City
        val lat = 39.0997
        val lon = -94.4840

        // Launch a coroutine for asynchronous weather data fetching
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Get grid point data for the specified coordinates
                val gridPointResponse = nws.getGridPoint(lat, lon)

                if (gridPointResponse != null) {
                    // Update the camera position on the main thread
                    withContext(Dispatchers.Main) {
                        val desiredLocation = LatLng(lat, lon)
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(desiredLocation, 15f))
                    }

                    // Fetch forecast data based on the grid point information
                    val forecast = nws.getForecast(gridPointResponse.properties.gridId,
                        gridPointResponse.properties.gridX,
                        gridPointResponse.properties.gridY)

                    withContext(Dispatchers.Main) {
                        if (forecast != null && forecast.isNotEmpty()) {
                            // Retrieve the first forecast item
                            val currentWeather = forecast[0]

                            // Extract weather details or set default values
                            val weatherText = currentWeather.detailedForecast ?: "N/A"
                            val tempText = "${currentWeather.temperature ?: "N/A"} ${currentWeather.temperatureUnit ?: ""}°"
                            val windText = currentWeather.windSpeed ?: "N/A"

                            // Update UI with weather information
                            findViewById<TextView>(R.id.textViewWeather).text = weatherText
                            findViewById<TextView>(R.id.textViewTemperature).text = tempText
                            findViewById<TextView>(R.id.textViewWind).text = windText

                            // Set weather icons
                            findViewById<ImageView>(R.id.weatherIcon).setImageResource(R.drawable.forecast_icon)
                            findViewById<ImageView>(R.id.temperatureIcon).setImageResource(R.drawable.temp_icon)
                            findViewById<ImageView>(R.id.windIcon).setImageResource(R.drawable.wind_speed_icon)

                            // Expand the bottom sheet to display weather details
                            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        } else {
                            // Display a message if no forecast data is available
                            findViewById<TextView>(R.id.textViewWeather).text = "No weather data available."
                            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                        }
                    }
                } else {
                    // Display a message if grid point data couldn't be retrieved
                    withContext(Dispatchers.Main) {
                        findViewById<TextView>(R.id.textViewWeather).text = "Failed to get grid point data."
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }
            } catch (e: Exception) {
                // Log any errors and update the UI with an error message
                Log.e("MainActivity", "Failed to get weather data", e)
                withContext(Dispatchers.Main) {
                    findViewById<TextView>(R.id.textViewWeather).text = "Failed to get weather data: ${e.message}"
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
        }
    }
}
