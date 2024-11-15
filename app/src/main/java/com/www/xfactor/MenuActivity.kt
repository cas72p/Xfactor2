package com.www.xfactor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

//THIS IS BUILT TO HOLD BUTTONS TO TAKE YOU TO ALL FUNCTIONS IN THE APP
class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)

        // Apply window insets for edge-to-edge UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configure the buttons to switch to different features
        configureSearchButton()
        configureSavedButton() //TODO:: create page
        //configureWeatherButton() //TODO:: create page
        //configureSettingsButton() //TODO:: create page

        // Configure back button
        configureLogoutButton()
    }

    //this takes you to the game search
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



    //this takes you to the weather map
    private fun configureWeatherButton() {
        val weatherButton: Button = findViewById(R.id.weather_button)
        weatherButton.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java) //TODO:: ADD WEATHER PAGE instead of menu page
            startActivity(intent)
        }
    }

    //this takes you to the settings
    private fun configureSettingsButton() {
        val settingsButton: Button = findViewById(R.id.settings_button)
        settingsButton.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java) //TODO:: ADD SETTINGS PAGE instead of menu page
            startActivity(intent)
        }
    }

    // Back button setup to return to the previous activity
    //TODO: MAKE SURE YOU'RE ACTUALLY LOGGED OUT OF YOUR ACCOUNT
    private fun configureLogoutButton() {
        val logoutButton: Button = findViewById(R.id.logout_button)
        logoutButton.setOnClickListener {
            finish()
        }
    }
}