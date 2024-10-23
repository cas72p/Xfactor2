package com.www.xfactor

import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

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

    // Back button setup to return to the previous activity
    private fun configureBackButton() {
        val backButton: Button = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }
    }

    // Function to fetch games for the next 10 days
    private fun fetchGamesForNext10Days(sportLabel: String) {
        FetchGamesTask(sportLabel).execute()
    }

    // AsyncTask to perform network operations in the background
    private inner class FetchGamesTask(private val sportLabel: String) : AsyncTask<Void, Void, String?>() {

        override fun doInBackground(vararg params: Void?): String? {
            val apiUrl = "https://api.predicthq.com/v1/events/"
            val apiKey = "4-1RjZ7hsw9vcPMbzs_PzyZFmn1yarOTImOw7ys5"
            var response: String? = null

            try {
                val today = java.time.LocalDate.now().toString()
                val tenDaysLater = java.time.LocalDate.now().plusDays(10).toString()

                val urlWithParams = "$apiUrl?category=sports&label=$sportLabel&start.gte=$today&start.lte=$tenDaysLater&limit=100"
                val url: URL = URI.create(urlWithParams).toURL()
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $apiKey") // Add Authorization header

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

            // Update the games list with the API result
            if (result != null && !result.startsWith("Error")) {
                parseGameResults(result)
            } else {
                Toast.makeText(this@MainActivity2, "Error fetching data for $sportLabel", Toast.LENGTH_SHORT).show()
            }
        }

        // Parse the JSON response and add relevant games to the list
        private fun parseGameResults(result: String) {
            try {
                val jsonObject = JSONObject(result)
                val events = jsonObject.getJSONArray("results")
                for (i in 0 until events.length()) {
                    val event = events.getJSONObject(i)

                    val locationArray = event.optJSONArray("location")
                    val longitude = if (locationArray != null && locationArray.length() == 2) locationArray.getDouble(0).toString() else "N/A"
                    val latitude = if (locationArray != null && locationArray.length() == 2) locationArray.getDouble(1).toString() else "N/A"

                    val gameInfo = mapOf(
                        "title" to event.optString("title", "N/A"),
                        "date" to event.optString("start", "N/A"),
                        "latitude" to latitude,
                        "longitude" to longitude
                    )
                    allGames.add(gameInfo)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Function to search for games by team name
    private fun searchGameByTeam(teamName: String) {
        resultContainer.removeAllViews() // Clear previous search results

        val foundGames = allGames.filter { it["title"]?.contains(teamName, ignoreCase = true) == true }

        if (foundGames.isNotEmpty()) {
            val limitedGames = foundGames.take(5) // Limit to the first 5 matches
            for (game in limitedGames) {
                val gameButton = Button(this).apply {
                    text = "${game["title"]} on ${game["date"]}\nLat: ${game["latitude"]}, Lon: ${game["longitude"]}"
                    setOnClickListener {
                        Toast.makeText(this@MainActivity2, "Clicked on: ${game["title"]}", Toast.LENGTH_SHORT).show()
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
}


