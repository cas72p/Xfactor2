package com.www.xfactor
import android.util.Log
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.EditText
import android.widget.Toast
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException

class register : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        configureBackButton()
    }
    // Back button setup to return to the previous activity
    private fun configureBackButton() {
        // Database connection details
        val jdbcUrl = "jdbc:mysql://x-factor.cnmyeqgwshz0.us-east-2.rds.amazonaws.com:3306/xFactor?useSSL=false&autoReconnect=true"
        val dbUser = "admin"
        val dbPassword = "X-Factor"

        val emailEditText: EditText = findViewById(R.id.username_reg)
        val passwordEditText: EditText = findViewById(R.id.password_reg)
        val backButton: Button = findViewById(R.id.register_button)

        backButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Attempt to add the user to the database
                val success = addUserToDatabase(email, password, jdbcUrl, dbUser, dbPassword)

                if (success) {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                    finish() // Finish the activity to go back
                } else {
                    Toast.makeText(this, "Failed to register user", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addUserToDatabase(email: String, password: String, jdbcUrl: String, dbUser: String, dbPassword: String): Boolean {
        var connection: Connection? = null
        var preparedStatement: PreparedStatement? = null

        return try {
            // Establish connection to the database
            connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)

            // Insert the new user into the database
            val sql = "INSERT INTO User (username, password) VALUES (?, ?)"
            preparedStatement = connection.prepareStatement(sql)
            preparedStatement.setString(1, email)
            preparedStatement.setString(2, password)

            val rowsInserted = preparedStatement.executeUpdate()
            rowsInserted > 0 // Return true if insertion was successful

        } catch (e: SQLException) {
            Log.e("Register", "SQL Exception: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e("Register", "Exception: ${e.message}")
            false
        } finally {
            // Close resources
            try {
                preparedStatement?.close()
                connection?.close()
            } catch (e: SQLException) {
                Log.e("Register", "Error closing connection: ${e.message}")
            }
        }
    }
}
//git test