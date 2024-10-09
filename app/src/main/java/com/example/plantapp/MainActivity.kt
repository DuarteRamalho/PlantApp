package com.example.plantapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.plantapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object{
        private const val CAMERA_PREMISSION_CODE = 1
        private const val CAMERA_REQUEST_CODE = 2
    }

    lateinit var binding: ActivityMainBinding
    private var currentImageIndex = 0 // Initialize the counter for images

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.AddFlowersBtn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(intent, CAMERA_REQUEST_CODE)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PREMISSION_CODE
                )
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragmentContainer, fragment)
        fragmentTransaction.commit()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PREMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(intent, CAMERA_REQUEST_CODE)
            } else {
                Toast.makeText(this,
                    "Oops, you just denied the permission for camera. " +
                            "Don't worry, you can allow it in the settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE) {
                val thumbNail: Bitmap = data!!.extras!!.get("data") as Bitmap

                // Assign the photo to the corresponding ImageView
                when (currentImageIndex) {
                    0 -> binding.ivImage.setImageBitmap(thumbNail)
                    1 -> binding.ivImage2.setImageBitmap(thumbNail)
                    2 -> binding.ivImage3.setImageBitmap(thumbNail)
                    3 -> binding.ivImage4.setImageBitmap(thumbNail)
                    else -> {
                        Toast.makeText(this, "All image slots are full.", Toast.LENGTH_SHORT).show()
                        return // Do not increment if all slots are full
                    }
                }

                // Increment the index for the next image
                currentImageIndex++
            }
        }
    }
}
