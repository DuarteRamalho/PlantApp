package com.example.plantapp.data

import android.graphics.Bitmap
import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Plant(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    var imageUrl: String = "",
    var localImageUri: Uri? = null,
    var bitmap: Bitmap? = null,
    val userId: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null
) {
    // Empty constructor for Firebase
    constructor() : this(
        id = "",
        name = "",
        description = "",
        imageUrl = "",
        localImageUri = null,
        bitmap = null,
        userId = "",
        timestamp = null
    )

    // Constructor for local use with bitmap
    constructor(
        id: String,
        name: String,
        description: String,
        bitmap: Bitmap,
        userId: String
    ) : this(
        id = id,
        name = name,
        description = description,
        imageUrl = "",
        localImageUri = null,
        bitmap = bitmap,
        userId = userId
    )

    // Constructor for local use with Uri
    constructor(
        id: String,
        name: String,
        description: String,
        localImageUri: Uri,
        userId: String
    ) : this(
        id = id,
        name = name,
        description = description,
        imageUrl = "",
        localImageUri = localImageUri,
        bitmap = null,
        userId = userId
    )
}
