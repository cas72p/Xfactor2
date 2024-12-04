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
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.*

class map_activity : AppCompatActivity(), OnMapReadyCallback {

    // Game class
    data class Game(val name: String, val latitude: Double, val longitude: Double)
    // Bottom sheet for displaying weather info
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    // Google Map instance
    private lateinit var map: GoogleMap
    // NWS instance for accessing weather data
    private val nws = NWS()
    // Firebase stuff
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Variables for dynamic coordinates
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var target_date: String = "No date provided"
    private var title: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.map_layout)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        // Arrays of game objects and game names for spinner display
        val temporaryOptions = mutableListOf<Game>()
        val gameNames = mutableListOf<String>()
        gameNames.add(0,"Select a game:") // Placeholder to avoid default selection
        // Firebase stuff
        val userId = auth.currentUser?.uid ?: return

        // Fetching info for games and initializing the spinner
        db.collection("users").document(userId).collection("selectedGames")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val title = document.getString("title") ?: "Empty Name"
                    val date = document.getString("date") ?: "N/A"
                    val latitude = document.getString("latitude") ?: "0.0"
                    val longitude = document.getString("longitude") ?: "0.0"
                    val lat = latitude.toDoubleOrNull() ?: 0.0
                    val lon = longitude.toDoubleOrNull() ?: 0.0
                    // Add games to the list
                    if (lat != 0.0 && lon != 0.0){
                        val newGame = Game(title, lat, lon)
                        temporaryOptions.add(newGame)
                        gameNames.add(title)
                    }
                }
                // Spinner setup
                val gameSelectorSpinner: Spinner = findViewById(R.id.gameSelectorSpinner)
                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item, // Layout for each item
                    gameNames // Data source
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) // Drop-down view style
                gameSelectorSpinner.adapter = adapter
                gameSelectorSpinner.setSelection(0)
                gameSelectorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View,
                        position: Int,
                        id: Long
                    ) {
                        if (position == 0){
                            return
                        }
                        val selectedOption = parent.getItemAtPosition(position).toString()
                        val selectedGame = temporaryOptions[position-1]
                        // Call weather subroutine again on new coordinates
                        Toast.makeText(this@map_activity, "Selected $selectedOption", Toast.LENGTH_SHORT).show()
                        val lat = selectedGame.latitude
                        val lon = selectedGame.longitude
                        val nameOfTheGame = selectedGame.name
                        getWeather(lat, lon)
                        val desiredLocation = LatLng(lat, lon)
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(desiredLocation, 15f))
                        map.addMarker(
                            MarkerOptions()
                                .position(desiredLocation)
                                .title(nameOfTheGame)
                        )

                    }
                    override fun onNothingSelected(parent: AdapterView<*>) {
                        // Handle the case where no item is selected, if needed
                    }
                }
                if (documents.isEmpty) {
                    Toast.makeText(this, "No saved games found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load saved games", Toast.LENGTH_SHORT).show()
            }

        // Retrieve dynamic location data from the Intent
        latitude = intent.getDoubleExtra("latitude", 39.0997) // Default: Kansas City
        longitude = intent.getDoubleExtra("longitude", -94.4840) // Default: Kansas City
        target_date = intent.getStringExtra("date") ?: "No date provided"
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
        var wanted_forecast = 0
        val calendar = Calendar.getInstance()
        val currentDate = calendar.time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate = dateFormat.format(currentDate)
        Log.d("Current date:", formattedDate)
        Log.d("Target date:", target_date)

        if (target_date != "No date provided") {
            val current_day = formattedDate.substring(8,10).toInt()
            val target_day = target_date.substring(8,10).toInt()
            if ((target_day - current_day) >= 7)
                wanted_forecast = 6
            else {
                wanted_forecast = target_day - current_day
            }
        }

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
                            val currentWeather = forecast[wanted_forecast]

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
                            findViewById<ImageView>(R.id.temperatureIcon).setImageResource(R.drawable.thermometer_1)
                            findViewById<ImageView>(R.id.windIcon).setImageResource(R.drawable.wind_white)

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
