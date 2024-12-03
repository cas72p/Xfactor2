package com.www.xfactor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class register : AppCompatActivity() {
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var registerButton: Button
    private lateinit var verbutton: Button
    private lateinit var vermsg: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Set up UI components
        email = findViewById(R.id.username_reg)
        password = findViewById(R.id.password_reg)
        registerButton = findViewById(R.id.register_button)
        verbutton = findViewById(R.id.verbut)
        vermsg = findViewById(R.id.vertext)

        // Set up the register button to handle user registration
        registerButton.setOnClickListener {
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()

            if (emailText.isNotEmpty() && passwordText.isNotEmpty()) {
                registerUser(emailText, passwordText)
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle window insets for full-screen layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configureBackButton()
    }

    // Function to register a new user with Firebase Authentication
    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registration successful
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()

                    // Automatically sign in the user after registration
                    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { signInTask ->
                        if (signInTask.isSuccessful) {
                            // Send verification email
                            val user = auth.currentUser
                            user?.sendEmailVerification()?.addOnSuccessListener {
                                Toast.makeText(this, "Verification email sent. Please check your inbox.", Toast.LENGTH_SHORT).show()

                                // Make the verification button visible for manual email verification check
                                verbutton.visibility = View.VISIBLE
                                vermsg.visibility = View.VISIBLE

                                verbutton.setOnClickListener {
                                    user.reload().addOnCompleteListener {
                                        if (user.isEmailVerified) {
                                            Toast.makeText(this, "Email verified! You can now proceed.", Toast.LENGTH_SHORT).show()
                                            val intent = Intent(this, MenuActivity::class.java)
                                            startActivity(intent)
                                        } else {
                                            Toast.makeText(this, "Please verify your email before proceeding.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }?.addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to send verification email: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "Sign-in failed: ${signInTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Registration failed
                    val errorMessage = task.exception?.message ?: "Registration failed"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    Log.e("FirebaseAuth", "Error: $errorMessage")
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
}
