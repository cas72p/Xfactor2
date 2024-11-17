package com.www.xfactor.xfactormap

import NWS
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.www.xfactor.MenuActivity
import com.www.xfactor.R
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

    // Variables for dynamic coordinates
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var title: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.map_layout)

        // Retrieve dynamic location data from the Intent
        latitude = intent.getDoubleExtra("latitude", 39.0997) // Default: Kansas City
        longitude = intent.getDoubleExtra("longitude", -94.4840) // Default: Kansas City
        title = intent.getStringExtra("title") ?: "Selected Location"

        // Initialize the bottom sheet and set it to a collapsed state initially
        val bottomSheet = findViewById<View>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        // Set up the Google Maps fragment and prepare for when the map is ready
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Retrieve and display weather data
        latitude?.let { lat ->
            longitude?.let { lon ->
                getWeather(lat, lon)
            }
        }

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
            finish()  // Finish the current activity so it doesn't remain in the back stack
        }
    }

    // Callback when the Google Map is ready for use
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        latitude?.let { lat ->
            longitude?.let { lon ->
                val location = LatLng(lat, lon)

                // Center the map at the provided coordinates and add a marker
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                map.addMarker(
                    MarkerOptions()
                        .position(location)
                        .title(title)
                )
            }
        }
    }

    // Function to fetch weather data and update UI
    private fun getWeather(lat: Double, lon: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Get grid point data for the specified coordinates
                val gridPointResponse = nws.getGridPoint(lat, lon)

                if (gridPointResponse != null) {
                    // Fetch forecast data based on the grid point information
                    val forecast = nws.getForecast(
                        gridPointResponse.properties.gridId,
                        gridPointResponse.properties.gridX,
                        gridPointResponse.properties.gridY
                    )

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
                Log.e("map_activity", "Failed to get weather data", e)
                withContext(Dispatchers.Main) {
                    findViewById<TextView>(R.id.textViewWeather).text = "Failed to get weather data: ${e.message}"
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
        }
    }
}
