package com.example.plantapp

import com.example.plantapp.authz.LoginActivity
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.plantapp.data.Plant
import com.example.plantapp.databinding.ActivityMainBinding
import com.example.plantapp.firebase.FirebaseManager
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_CODE = 1
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseManager: FirebaseManager
    private var currentImageIndex = 0
    private var currentPlant: Plant? = null
    private val plants = mutableListOf<Plant>()

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            displayImage(it)
            val description = getCurrentDescriptionText()
            uploadCurrentPlant(it, description)
        } ?: run {
            showToast("Failed to capture image")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)
            firebaseManager = FirebaseManager()

            // Check authentication
            if (firebaseManager.auth.currentUser == null) {
                Log.d(TAG, "User not authenticated, redirecting to login")
                redirectToLogin()
                return
            }

            // Initialize UI
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            setupUI()
            loadSavedPlants()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            showToast("Error initializing app: ${e.message}")
            redirectToLogin()
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupUI() {
        try {
            binding.AddFlowersBtn.setOnClickListener {
                handleAddFlowersClick()
            }
            setupDescriptionListeners()
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupUI", e)
            showToast("Error setting up UI: ${e.message}")
        }
    }

    private fun handleAddFlowersClick() {
        when {
            currentImageIndex >= 4 -> {
                showToast("Maximum number of plants reached")
            }
            isCameraPermissionGranted() -> {
                openCamera()
            }
            else -> {
                requestCameraPermission()
            }
        }
    }

    private fun setupDescriptionListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.let { description ->
                    getCurrentPlant()?.let { plant ->
                        updatePlantDescription(plant.id, description)
                    }
                }
            }
        }

        binding.apply {
            descImage1.addTextChangedListener(textWatcher)
            descImage2.addTextChangedListener(textWatcher)
            descImage3.addTextChangedListener(textWatcher)
            descImage4.addTextChangedListener(textWatcher)
        }
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
                handlePlantsLoadResult(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading plants", e)
                showToast("Error loading plants: ${e.message}")
            }
        }
    }

    private fun handlePlantsLoadResult(result: Result<List<Plant>>) {
        if (result.isSuccess) {
            plants.clear()
            result.getOrNull()?.let { plantList ->
                plants.addAll(plantList)
                plants.forEachIndexed { index, plant ->
                    displaySavedPlant(plant, index)
                }
            }
        } else {
            showToast("Failed to load plants: ${result.exceptionOrNull()?.message}")
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
                Log.e(TAG, "Error updating plant description", e)
                showToast("Failed to update description: ${e.message}")
            }
        }
    }

    private fun bitmapToUri(bitmap: Bitmap): Uri {
        val file = File(cacheDir, "plant_${UUID.randomUUID()}.jpg")
        file.createNewFile()

        ByteArrayOutputStream().use { bos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos)
            FileOutputStream(file).use { fos ->
                fos.write(bos.toByteArray())
                fos.flush()
            }
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
                handlePlantUploadResult(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading plant", e)
                showToast("Error uploading plant: ${e.message}")
            }
        }
    }

    private fun handlePlantUploadResult(result: Result<Plant>) {
        if (result.isSuccess) {
            result.getOrNull()?.let { plant ->
                plants.add(plant)
                currentPlant = plant
                showToast("Plant uploaded successfully")
            }
        } else {
            showToast("Failed to upload plant: ${result.exceptionOrNull()?.message}")
        }
    }

    private fun isCameraPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == 
            PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
    }

    private fun openCamera() {
        cameraLauncher.launch(null)
    }

    private fun displayImage(bitmap: Bitmap) {
        if (currentImageIndex >= 4) {
            showToast("All image slots are full.")
            return
        }

        val imageView = when (currentImageIndex) {
            0 -> binding.ivImage
            1 -> binding.ivImage2
            2 -> binding.ivImage3
            3 -> binding.ivImage4
            else -> return
        }

        imageView.setImageBitmap(bitmap)
        currentImageIndex++
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
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
            showToast("Camera permission denied. Please enable it in settings.")
        }
    }
}
