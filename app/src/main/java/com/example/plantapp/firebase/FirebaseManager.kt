package com.example.plantapp.firebase

import android.net.Uri
import com.example.plantapp.data.Plant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseManager {
    val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadPlant(name: String, description: String, imageUri: Uri): Result<Plant> {
        return try {
            // Check if user is authenticated
            val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")

            // Upload image to Firebase Storage
            val imageRef = storage.reference.child("plants/${UUID.randomUUID()}")
            imageRef.putFile(imageUri).await()
            val imageUrl = imageRef.downloadUrl.await().toString()

            // Create plant object
            val plant = Plant(
                id = UUID.randomUUID().toString(),
                name = name,
                description = description,
                imageUrl = imageUrl,
                userId = userId
            )

            // Save to Firestore
            firestore.collection("plants")
                .document(plant.id)
                .set(plant)
                .await()

            Result.success(plant)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlants(): Result<List<Plant>> {
        return try {
            val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")

            val snapshot = firestore.collection("plants")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val plants = snapshot.documents.mapNotNull { it.toObject(Plant::class.java) }
            Result.success(plants)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePlantDescription(plantId: String, description: String): Result<Unit>{

        return try {
            firestore.collection("plants")
                .document(plantId)
                .update("description", description)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    }
}
