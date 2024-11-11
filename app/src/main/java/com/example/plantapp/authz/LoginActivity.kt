package com.example.plantapp.authz

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.plantapp.MainActivity
import com.example.plantapp.databinding.ActivityLoginBinding
import com.example.plantapp.firebase.FirebaseManager
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseManager: FirebaseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase first
        firebaseManager = FirebaseManager()

        // Check if user is already signed in before setting up UI
        if (firebaseManager.auth.currentUser != null) {
            startMainActivity()
            return
        }

        // Only initialize UI if user is not authenticated
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
    }

    private fun setupUI() {
        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading indicator if you have one
            loginUser(email, password)
        }

        binding.buttonRegister.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading indicator if you have one
            registerUser(email, password)
        }
    }

    private fun loginUser(email: String, password: String) {
        firebaseManager.auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                startMainActivity()
            }
            .addOnFailureListener { exception: Exception ->
                // Hide loading indicator if you have one
                when (exception) {
                    is FirebaseAuthInvalidCredentialsException ->
                        Toast.makeText(this, "Invalid email or password", Toast.LENGTH_LONG).show()
                    is FirebaseAuthInvalidUserException ->
                        Toast.makeText(this, "No account found with this email", Toast.LENGTH_LONG).show()
                    else -> 
                        Toast.makeText(this, "Login failed: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun registerUser(email: String, password: String) {
        firebaseManager.auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                startMainActivity()
            }
            .addOnFailureListener { exception: Exception ->
                // Hide loading indicator if you have one
                Toast.makeText(this, "Registration failed: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
