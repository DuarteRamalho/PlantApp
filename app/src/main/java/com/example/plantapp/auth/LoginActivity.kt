package com.example.plantapp.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.plantapp.MainActivity
import com.example.plantapp.databinding.ActivityLoginBinding
import com.example.plantapp.firebase.FirebaseManager
import com.google.firebase.FirebaseException

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseManager: FirebaseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager()

        // Check if user is already signed in
        if (firebaseManager.auth.currentUser != null) {
            startMainActivity()
            return
        }

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

            loginUser(email, password)
        }

        binding.buttonRegister.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(email, password)
        }
    }

    private fun loginUser(email: String, password: String) {
        firebaseManager.auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                startMainActivity()
            }
            .addOnFailureListener { exception: Exception ->
                Toast.makeText(this, "Login failed: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun registerUser(email: String, password: String) {
        firebaseManager.auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                startMainActivity()
            }
            .addOnFailureListener { exception: Exception ->
                Toast.makeText(this, "Registration failed: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
