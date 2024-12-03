package com.www.xfactor

import android.os.AsyncTask
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class MainActivity2 : AppCompatActivity() {

    private lateinit var resultContainer: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private val allGames = mutableListOf<Map<String, String>>()
    private val selectedGames = mutableSetOf<Map<String, String>>()
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Reference to views
        resultContainer = findViewById(R.id.result_container)
        searchInput = findViewById(R.id.search_input)
        searchButton = findViewById(R.id.search_button)

        // Adjust the layout for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configure back button
        configureBackButton()

        // Fetch saved games from Firestore
        loadSavedGames()

        // Trigger the searchGames API call when the activity starts
        fetchGamesForNext10Days("nfl")
        fetchGamesForNext10Days("baseball")

        // Set up the search button to trigger the team name search
        searchButton.setOnClickListener {
            val teamName = searchInput.text.toString().trim()
            if (teamName.isNotEmpty()) {
                searchGameByTeam(teamName)
            } else {
                Toast.makeText(this, "Please enter a team name to search.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Load saved games from Firestore for the current user

    private fun loadSavedGames() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("selectedGames")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val game = mapOf(
                        "title" to (document.getString("title") ?: "N/A"),
                        "date" to (document.getString("date") ?: "N/A"),
                        "latitude" to (document.getString("latitude") ?: "N/A"),
                        "longitude" to (document.getString("longitude") ?: "N/A")
                    )
                    selectedGames.add(game)
                }
                Toast.makeText(this, "Saved games loaded", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading saved games", Toast.LENGTH_SHORT).show()
            }
    }


    // Function to fetch games for the next 10 days
    private fun fetchGamesForNext10Days(sportLabel: String) {
        FetchGamesTask(sportLabel).execute()
    }

    private fun configureBackButton() {
        val backButton: Button = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }
    }

    // AsyncTask to perform network operations in the background
    private inner class FetchGamesTask(private val sportLabel: String) : AsyncTask<Void, Void, String?>() {

        override fun doInBackground(vararg params: Void?): String? {
            val apiUrl = "https://api.predicthq.com/v1/events/"
            val apiKey = "8KvJVVPWmb4v5IhI-QQId8Ys38fhC01mEoXaOLAv"
            var response: String? = null

            try {
                val today = java.time.LocalDate.now().toString()
                val tenDaysLater = java.time.LocalDate.now().plusDays(10).toString()

                val urlWithParams = "$apiUrl?category=sports&label=$sportLabel&start.gte=$today&start.lte=$tenDaysLater&limit=100"
                val url: URL = URI.create(urlWithParams).toURL()
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $apiKey")

                val responseCode: Int = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val stringBuilder = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }

                    reader.close()
                    response = stringBuilder.toString()
                } else {
                    response = "Error: Response code $responseCode"
                }

                connection.disconnect()

            } catch (e: Exception) {
                e.printStackTrace()
            }

            return response
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            if (result != null && !result.startsWith("Error")) {
                parseGameResults(result)
            } else {
                Toast.makeText(this@MainActivity2, "Error fetching data for $sportLabel", Toast.LENGTH_SHORT).show()
            }
        }

        private fun parseGameResults(result: String) {
            try {
                val jsonObject = JSONObject(result)
                val events = jsonObject.getJSONArray("results")
                for (i in 0 until events.length()) {
                    val event = events.getJSONObject(i)
                    val locationArray = event.optJSONArray("location")
                    val longitude = if (locationArray != null && locationArray.length() == 2) locationArray.getDouble(0).toString() else "N/A"
                    val latitude = if (locationArray != null && locationArray.length() == 2) locationArray.getDouble(1).toString() else "N/A"
                    val country = event.optString("country", "N/A") // Extract country field

                    val gameInfo = mapOf(
                        "title" to event.optString("title", "N/A"),
                        "date" to event.optString("start", "N/A"),
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "country" to country
                    )
                    allGames.add(gameInfo)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    private fun searchGameByTeam(teamName: String) {
        resultContainer.removeAllViews()

        val foundGames = allGames.filter { game ->
            val title = game["title"] as? String
            val country = game["country"] as? String


            title?.contains(teamName, ignoreCase = true) == true &&
                    country == "US"
        }



        if (foundGames.isNotEmpty()) {
            val limitedGames = foundGames.take(5)
            for (game in limitedGames) {
                val gameButton = Button(this).apply {
                    text = "${game["title"]} on ${game["date"]}\nLat: ${game["latitude"]}, Lon: ${game["longitude"]}"
                    setBackgroundColor(
                        if (selectedGames.contains(game))
                            resources.getColor(android.R.color.holo_orange_light)
                        else
                            resources.getColor(android.R.color.darker_gray)
                    )
                    setOnClickListener {
                        toggleGameSelection(game, this)
                    }
                }
                resultContainer.addView(gameButton)
            }
        } else {
            val noResultTextView = TextView(this).apply {
                text = "No games found for the team '$teamName'."
            }
            resultContainer.addView(noResultTextView)
        }
    }

    private fun toggleGameSelection(game: Map<String, String>, button: Button) {
        if (selectedGames.contains(game)) {
            selectedGames.remove(game)
            deleteGameFromFirestore(game)
            button.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
            Toast.makeText(this, "Removed: ${game["title"]}", Toast.LENGTH_SHORT).show()
        } else {
            selectedGames.add(game)
            saveGameToFirestore(game)
            button.setBackgroundColor(resources.getColor(android.R.color.holo_orange_light))
            Toast.makeText(this, "Added: ${game["title"]}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveGameToFirestore(game: Map<String, String>) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("selectedGames")
            .document(game["title"] ?: "Untitled")
            .set(game)
            .addOnSuccessListener {
                Toast.makeText(this, "Game saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving game", Toast.LENGTH_SHORT).show()
            }
    }


    private fun deleteGameFromFirestore(game: Map<String, String>) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("selectedGames")
            .document(game["title"] ?: "Untitled")
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Game removed from saved games", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error removing game", Toast.LENGTH_SHORT).show()
            }
    }
}

