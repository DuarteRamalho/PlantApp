package com.example.plantapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.plantapp.auth.LoginActivity
import com.example.plantapp.data.Plant
import com.example.plantapp.databinding.ActivityMainBinding
import com.example.plantapp.firebase.FirebaseManager
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_CODE = 1
    }

    private lateinit var binding: ActivityMainBinding
    private var currentImageIndex = 0
    private lateinit var firebaseManager: FirebaseManager
    private var currentPlant: Plant? = null
    private val plants = mutableListOf<Plant>()

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            displayImage(it)
            // Get description from current EditText
            val description = getCurrentDescriptionText()
            uploadCurrentPlant(it, description)
        } ?: run {
            Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager()

        // Check if user is authenticated
        if (firebaseManager.auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupUI()
        loadSavedPlants()
    }


    private fun setupUI() {
        binding.AddFlowersBtn.setOnClickListener {
            if (currentImageIndex >= 4) {
                Toast.makeText(this, "Maximum number of plants reached", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isCameraPermissionGranted()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        // Setup description text watchers
        setupDescriptionListeners()
    }

    private fun setupDescriptionListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val description = s.toString()
                getCurrentPlant()?.let { plant ->
                    updatePlantDescription(plant.id, description)
                }
            }
        }

        binding.descImage1.addTextChangedListener(textWatcher)
        binding.descImage2.addTextChangedListener(textWatcher)
        binding.descImage3.addTextChangedListener(textWatcher)
        binding.descImage4.addTextChangedListener(textWatcher)
    }

    private fun getCurrentPlant(): Plant? {
        return if (currentImageIndex > 0 && plants.size >= currentImageIndex) {
            plants[currentImageIndex - 1]
        } else null
    }

    private fun getCurrentDescriptionText(): String {
        return when (currentImageIndex) {
            0 -> binding.descImage1.text.toString()
            1 -> binding.descImage2.text.toString()
            2 -> binding.descImage3.text.toString()
            3 -> binding.descImage4.text.toString()
            else -> ""
        }
    }

    private fun loadSavedPlants() {
        lifecycleScope.launch {
            try {
                val result = firebaseManager.getPlants()
                if (result.isSuccess) {
                    plants.clear()
                    plants.addAll(result.getOrNull() ?: emptyList())
                    plants.forEachIndexed { index, plant ->
                        displaySavedPlant(plant, index)
                    }
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to load plants: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error loading plants: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun displaySavedPlant(plant: Plant, index: Int) {
        val imageView = when (index) {
            0 -> binding.ivImage
            1 -> binding.ivImage2
            2 -> binding.ivImage3
            3 -> binding.ivImage4
            else -> return
        }

        val descriptionView = when (index) {
            0 -> binding.descImage1
            1 -> binding.descImage2
            2 -> binding.descImage3
            3 -> binding.descImage4
            else -> return
        }

        // Load image using Glide
        Glide.with(this)
            .load(plant.imageUrl)
            .centerCrop()
            .into(imageView)

        descriptionView.setText(plant.description)
        currentImageIndex = index + 1
    }

    private fun updatePlantDescription(plantId: String, description: String) {
        lifecycleScope.launch {
            try {
                firebaseManager.updatePlantDescription(plantId, description)
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to update description: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun bitmapToUri(bitmap: Bitmap): Uri {
        val file = File(cacheDir, "plant_${UUID.randomUUID()}.jpg")
        file.createNewFile()

        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos)
        val bitmapData = bos.toByteArray()

        FileOutputStream(file).use { fos ->
            fos.write(bitmapData)
            fos.flush()
        }

        return Uri.fromFile(file)
    }

    private fun uploadCurrentPlant(bitmap: Bitmap, description: String) {
        lifecycleScope.launch {
            try {
                val imageUri = bitmapToUri(bitmap)
                val result = firebaseManager.uploadPlant(
                    name = "Plant ${currentImageIndex}",
                    description = description,
                    imageUri = imageUri
                )
                if (result.isSuccess) {
                    result.getOrNull()?.let { plant ->
                        plants.add(plant)
                        currentPlant = plant
                        Toast.makeText(
                            this@MainActivity,
                            "Plant uploaded successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to upload plant: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error uploading plant: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
    }

    private fun openCamera() {
        cameraLauncher.launch(null)
    }

    private fun displayImage(bitmap: Bitmap) {
        val imageView = when (currentImageIndex) {
            0 -> binding.ivImage
            1 -> binding.ivImage2
            2 -> binding.ivImage3
            3 -> binding.ivImage4
            else -> {
                Toast.makeText(this, "All image slots are full.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        imageView.setImageBitmap(bitmap)
        currentImageIndex++
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            Toast.makeText(
                this,
                "Camera permission denied. Please enable it in settings.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
