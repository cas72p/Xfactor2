package com.www.xfactor

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.maps.model.LatLng
import com.www.xfactor.xfactormap.map_activity
import java.text.SimpleDateFormat
import java.util.*

class MenuActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 4589
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)

        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Apply window insets for edge-to-edge UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Remove outdated games when the menu loads
        removeOutdatedGames()

        // Configure the buttons to switch to different features
        configureSearchButton()
        configureSavedButton()
        configureWeatherButton()
        configureSettingsButton()

        // Configure back button
        configureLogoutButton()
    }

    // Removes outdated games from Firestore
    private fun removeOutdatedGames() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("selectedGames")
            .get()
            .addOnSuccessListener { documents ->
                val currentDate = Calendar.getInstance().time
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                for (document in documents) {
                    val gameDateStr = document.getString("date") ?: continue
                    val gameDate = try {
                        dateFormat.parse(gameDateStr)
                    } catch (e: Exception) {
                        Log.e("MenuActivity", "Invalid date format for game: $gameDateStr")
                        null
                    }

                    // Check if the game date is before the current date
                    if (gameDate != null && gameDate.before(currentDate)) {
                        // Remove outdated game
                        db.collection("users").document(userId)
                            .collection("selectedGames").document(document.id)
                            .delete()
                            .addOnSuccessListener {
                                Log.d("MenuActivity", "Removed outdated game: $gameDateStr")
                            }
                            .addOnFailureListener { e ->
                                Log.e("MenuActivity", "Failed to remove outdated game: $e")
                            }
                    }
                }
                Toast.makeText(this, "Outdated games removed", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("MenuActivity", "Error loading saved games: $e")
                Toast.makeText(this, "Error removing outdated games", Toast.LENGTH_SHORT).show()
            }
    }

    private fun configureSearchButton() {
        val searchButton: Button = findViewById(R.id.search_button)
        searchButton.setOnClickListener {
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
        }
    }

    private fun configureSavedButton() {
        val savedButton: Button = findViewById(R.id.saved_button)
        savedButton.setOnClickListener {
            val intent = Intent(this, SavedGames::class.java)
            startActivity(intent)
        }
    }

    private fun configureWeatherButton() {
        val weatherButton: Button = findViewById(R.id.weather_button)
        weatherButton.setOnClickListener {
            // Ask user for location
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            } else {
                loadUserLocation()
            }
        }
    }

    private fun configureSettingsButton() {
        val settingsButton: Button = findViewById(R.id.settings_button)
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java) //
            startActivity(intent)
            finish()
        }
    }

    // Override the permission results to continue as normal if not met
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed to load user location
                loadUserLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Function to check permission and return lon and lat
    @SuppressLint("MissingPermission")
    private fun loadUserLocation() {
        fusedLocationClient.lastLocation
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val location = task.result
                    val userLatLng = LatLng(location.latitude, location.longitude)

                    // Pass the location to map_activity
                    val intent = Intent(this, map_activity::class.java).apply {
                        putExtra("latitude", userLatLng.latitude)
                        putExtra("longitude", userLatLng.longitude)
                    }
                    startActivity(intent)
                } else {
                    Log.e("loadUserLocation", "Failed to get location.")
                    Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Log.e("loadUserLocation", "Location retrieval error: ${it.message}")
                Toast.makeText(this, "Failed to retrieve location", Toast.LENGTH_SHORT).show()
            }
    }

    // TODO: MAKE SURE YOU'RE ACTUALLY LOGGED OUT OF YOUR ACCOUNT
    private fun configureLogoutButton() {
        val logoutButton: Button = findViewById(R.id.logout_button)
        logoutButton.setOnClickListener {
            finish()
        }
    }
}

