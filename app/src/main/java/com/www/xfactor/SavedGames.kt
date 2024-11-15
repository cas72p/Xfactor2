package com.www.xfactor

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SavedGames : AppCompatActivity() {

    private lateinit var gamesContainer: LinearLayout
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_games)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Set up UI components
        gamesContainer = findViewById(R.id.games_container)

        // Load saved games from Firestore
        loadSavedGames()
    }

    private fun loadSavedGames() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("selectedGames")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val title = document.getString("title") ?: "N/A"
                    val date = document.getString("date") ?: "N/A"
                    val latitude = document.getString("latitude") ?: "N/A"
                    val longitude = document.getString("longitude") ?: "N/A"

                    // Display each game
                    addGameToView(title, date, latitude, longitude)
                }
                if (documents.isEmpty) {
                    Toast.makeText(this, "No saved games found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load saved games", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addGameToView(title: String, date: String, latitude: String, longitude: String) {
        val gameTextView = TextView(this).apply {
            text = "$title on $date\nLocation: ($latitude, $longitude)"
            textSize = 16f
            setPadding(16, 16, 16, 16)
        }
        gamesContainer.addView(gameTextView)
    }
}
