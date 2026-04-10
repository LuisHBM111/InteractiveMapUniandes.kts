package com.uniandes.interactivemapuniandes.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.uniandes.interactivemapuniandes.R
import coil3.load
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import com.uniandes.interactivemapuniandes.utils.setupNavigation

class SettingsActivity : AppCompatActivity() {

    //Profile image firebase implementation

    val uid = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private lateinit var imageRef: StorageReference
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        storage = Firebase.storage
        storageRef = storage.reference
        imageRef = storageRef.child("users/$uid/profile.jpg")
        db = Firebase.firestore

        uploadProfileImage()
        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.setupNavigation(this, "settings")

        setupImage()
        setupLogout()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 0) {
            val uri = data?.data ?: return

            val profileImage = findViewById<ImageView>(R.id.profile_image_settings_change)
            val uploadTask = imageRef.putFile(uri)

            // Register observers to listen for when the download is done or if it fails
            uploadTask.addOnFailureListener {
                // Handle unsuccessful uploads
                e -> Log.e("SettingsActivity ${uri}", "Upload failed", e)
            }.addOnSuccessListener { taskSnapshot ->
                // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                // ...
            }

            val urlTask = uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                imageRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    profileImage.load(downloadUri)
                    val data = hashMapOf("photoURL" to downloadUri.toString())
                    db.collection("users").document(uid!!).set(data, SetOptions.merge())
                } else {
                    // Handle failures
                    profileImage.load("https://img.game8.co/4029048/53f27f108863a813ae1ca1d22aa50d40.png/show")
                }
            }

        }
    }

    fun uploadProfileImage(){

        val profileImage = findViewById<ImageView>(R.id.profile_image_settings_change)

        profileImage.setOnClickListener {

            Intent(Intent.ACTION_GET_CONTENT).also {
                it.type = "image/*"
                startActivityForResult(it, 0)
            }

        }

    }

    fun setupImage(){
        val profileImage = findViewById<ImageView>(R.id.profile_image_settings_change)
        db.collection("users").document(uid!!).get().addOnSuccessListener {
            val url = it.getString("photoURL")
            profileImage.load(url)
        }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()

        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    fun setupLogout(){
        findViewById<View>(R.id.settings_logout).setOnClickListener {
            logout()
        }
    }

    // To check a switch
    //switchmaterial.isChecked = true

// To listen for a switch's checked/unchecked state changes
    //switchmaterial.setOnCheckedChangeListener { buttonView, isChecked
        // Responds to switch being checked/unchecked
    }



}
