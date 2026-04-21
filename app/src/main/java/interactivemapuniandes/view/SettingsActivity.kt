package com.uniandes.interactivemapuniandes.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil3.load
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.utils.setupNavigation

class SettingsActivity : AppCompatActivity() {

    private val uid = FirebaseAuth.getInstance().currentUser?.uid
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
        setupLanguageChange()
        setupPrivacyPolicies()
        setupNotificationsBottomSheet()
        setupOfflineMaps()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || requestCode != 0) return

        val currentUid = uid ?: return
        val uri = data?.data ?: return
        val profileImage = findViewById<ImageView>(R.id.profile_image_settings_change)
        val uploadTask = imageRef.putFile(uri)

        uploadTask.addOnFailureListener { e ->
            Log.e("SettingsActivity", "Upload failed", e)
        }

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            imageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                profileImage.load(downloadUri)
                val payload = hashMapOf("photoURL" to downloadUri.toString())
                db.collection("users").document(currentUid).set(payload, SetOptions.merge())
            } else {
                profileImage.load("https://img.game8.co/4029048/53f27f108863a813ae1ca1d22aa50d40.png/show")
            }
        }
    }

    private fun uploadProfileImage() {
        val profileImage = findViewById<ImageView>(R.id.profile_image_settings_change)

        profileImage.setOnClickListener {
            Intent(Intent.ACTION_GET_CONTENT).also {
                it.type = "image/*"
                startActivityForResult(it, 0)
            }
        }
    }

    private fun setupImage() {
        val currentUid = uid ?: return
        val profileImage = findViewById<ImageView>(R.id.profile_image_settings_change)
        db.collection("users").document(currentUid).get().addOnSuccessListener {
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

    private fun setupLogout() {
        findViewById<View>(R.id.settings_logout).setOnClickListener {
            logout()
        }
    }

    private fun setupLanguageChange() {
        val standardBottomSheet = findViewById<ConstraintLayout>(R.id.standard_bottom_sheet)
        val standardBottomSheetBehavior = BottomSheetBehavior.from(standardBottomSheet)
        standardBottomSheetBehavior.state = STATE_HIDDEN

        findViewById<ConstraintLayout>(R.id.layout_settings_language).setOnClickListener {
            standardBottomSheetBehavior.state = STATE_EXPANDED
        }

        setupLanguageChangeButtons(standardBottomSheetBehavior)
    }

    private fun setupLanguageChangeButtons(
        standardBottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    ) {
        findViewById<Button>(R.id.language_english).setOnClickListener {
            standardBottomSheetBehavior.state = STATE_COLLAPSED
        }
        findViewById<Button>(R.id.language_spanish).setOnClickListener {
            standardBottomSheetBehavior.state = STATE_COLLAPSED
        }
    }

    private fun setupPrivacyPolicies() {
        findViewById<View>(R.id.privacy_layout_settings).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupOfflineMaps() {
        findViewById<View>(R.id.offline_maps_layout_settings).setOnClickListener {
            val intent = Intent(this, OfflineMapsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupNotificationsBottomSheet() {
        val standardBottomSheet = findViewById<ConstraintLayout>(R.id.standard_bottom_sheet_notif)
        val standardBottomSheetBehavior = BottomSheetBehavior.from(standardBottomSheet)
        standardBottomSheetBehavior.state = STATE_HIDDEN

        findViewById<ConstraintLayout>(R.id.push_notifications_layout_settings).setOnClickListener {
            standardBottomSheetBehavior.state = STATE_EXPANDED
        }

        val childrenCheckBoxes = listOf(
            findViewById<MaterialCheckBox>(R.id.checkbox_child_1),
            findViewById<MaterialCheckBox>(R.id.checkbox_child_2),
            findViewById<MaterialCheckBox>(R.id.checkbox_child_3),
            findViewById<MaterialCheckBox>(R.id.checkbox_child_4)
        )
        setChildrenState(childrenCheckBoxes)
    }

    private fun setChildrenState(childrenCheckBoxes: List<MaterialCheckBox>) {
        for (child in childrenCheckBoxes) {
            child.addOnCheckedStateChangedListener { _, state ->
                Log.e("SettingsActivity", "Notification state: $state")
                val builder = AlertDialog.Builder(this)
                builder
                    .setMessage("Notification preference updated")
                    .setTitle("Settings")
                builder.create().show()
            }
        }
    }
}
