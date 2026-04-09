package interactivemapuniandes.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil3.compose.AsyncImage
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
import com.uniandes.interactivemapuniandes.ui.HomeActivity
import interactivemapuniandes.utils.setupNavigation
import java.io.File
import kotlin.math.log

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

}