package com.uniandes.interactivemapuniandes.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
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
import com.uniandes.interactivemapuniandes.model.data.UpdatePreferencesBody
import com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
import com.uniandes.interactivemapuniandes.model.repository.PreferencesRepository
import com.uniandes.interactivemapuniandes.utils.setupNavigation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private val currentUid get() = FirebaseAuth.getInstance().currentUser?.uid // Fresh every access
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private lateinit var db: FirebaseFirestore
    private lateinit var prefs: PreferencesRepository

    private val imagePicker = registerForActivityResult( // Modern result API
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) handleProfileImagePicked(uri)
    }

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
        db = Firebase.firestore
        prefs = PreferencesRepository(this)

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.setupNavigation(this, "settings")

        setupImageClick()
        setupImage()
        setupLogout()
        setupLanguageChange()
        setupPrivacyPolicies()
        setupNotificationsBottomSheet()
        setupOfflineMaps()
        setupDarkModeToggle() // Dark mode switch
        fillProfileFields() // Populate name / program / email from Firebase + BE
        fillLanguageField() // Show current language tag
        fillAppVersion() // Real versionName from BuildConfig
    }

    private fun setupDarkModeToggle() {
        val sw = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_dark_mode)
        lifecycleScope.launch { sw.isChecked = prefs.darkModeFlow().first() } // Restore saved state
        sw.setOnCheckedChangeListener { _, on ->
            lifecycleScope.launch {
                prefs.setDarkMode(on)
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    if (on) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                    else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }
    }

    private fun fillProfileFields() {
        val user = FirebaseAuth.getInstance().currentUser
        val nameTv = findViewById<android.widget.TextView>(R.id.tv_profile_name)
        val programTv = findViewById<android.widget.TextView>(R.id.tv_profile_program)
        val emailTv = findViewById<android.widget.TextView>(R.id.tv_profile_email)

        nameTv.text = user?.displayName?.takeIf { it.isNotBlank() } ?: user?.email ?: "Guest"
        emailTv.text = user?.email ?: "—"
        programTv.text = "Uniandes" // Placeholder until BE /me gives us a program

        lifecycleScope.launch { // Pull full profile from BE, override if richer
            try {
                val resp = RetrofitInstance.meApi.getMe()
                if (resp.isSuccessful) {
                    val me = resp.body() ?: return@launch
                    me.profile?.fullName?.takeIf { it.isNotBlank() }?.let { nameTv.text = it }
                    me.profile?.program?.takeIf { it.isNotBlank() }?.let { programTv.text = it }
                    me.email?.takeIf { it.isNotBlank() }?.let { emailTv.text = it }
                }
            } catch (e: Exception) {
                // Silent, already showed Firebase-derived values
            }
        }
    }

    private fun fillLanguageField() {
        val tv = findViewById<android.widget.TextView>(R.id.tv_language_value)
        lifecycleScope.launch {
            val tag = prefs.languageFlow().first()
            tv.text = if (tag.startsWith("en")) "English (US) >" else "Español (CO) >"
        }
    }

    private fun fillAppVersion() {
        val tv = findViewById<android.widget.TextView>(R.id.tv_app_version)
        tv.text = "v${com.uniandes.interactivemapuniandes.BuildConfig.VERSION_NAME}"
    }

    private fun handleProfileImagePicked(uri: Uri) {
        val uid = currentUid ?: return // Bail if logged out
        val profileImage = findViewById<ImageView>(R.id.profile_image_settings_change)
        val imageRef = storageRef.child("users/$uid/profile.jpg")
        val uploadTask = imageRef.putFile(uri)

        uploadTask.addOnFailureListener { e ->
            Log.e("SettingsActivity", "Upload failed", e)
            Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
        }

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) task.exception?.let { throw it }
            imageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                profileImage.load(downloadUri)
                val payload = hashMapOf("photoURL" to downloadUri.toString())
                db.collection("users").document(uid).set(payload, SetOptions.merge())
                pushProfileToBackend(downloadUri.toString()) // Sync to our BE too
            }
        }
    }

    private fun pushProfileToBackend(photoUrl: String) {
        lifecycleScope.launch {
            try {
                RetrofitInstance.meApi.updateProfile(
                    com.uniandes.interactivemapuniandes.model.data.UpdateProfileBody(profileImage = photoUrl)
                )
            } catch (e: Exception) {
                Log.w("SettingsActivity", "backend profile sync failed: ${e.message}") // Non-fatal
            }
        }
    }

    private fun setupImageClick() {
        val profileImage = findViewById<ImageView>(R.id.profile_image_settings_change)
        profileImage.setOnClickListener {
            imagePicker.launch("image/*")
        }
    }

    private fun setupImage() {
        val uid = currentUid ?: return
        val profileImage = findViewById<ImageView>(R.id.profile_image_settings_change)
        db.collection("users").document(uid).get().addOnSuccessListener {
            val url = it.getString("photoURL")
            if (!url.isNullOrBlank()) profileImage.load(url)
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

        findViewById<Button>(R.id.language_english).setOnClickListener {
            applyLanguage("en-US")
            standardBottomSheetBehavior.state = STATE_COLLAPSED
        }
        findViewById<Button>(R.id.language_spanish).setOnClickListener {
            applyLanguage("es-CO")
            standardBottomSheetBehavior.state = STATE_COLLAPSED
        }
    }

    private fun applyLanguage(tag: String) {
        lifecycleScope.launch {
            prefs.setLanguage(tag)
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag)) // Switch app locale
            try {
                RetrofitInstance.meApi.updatePreferences(UpdatePreferencesBody(language = tag)) // Sync BE
            } catch (e: Exception) {
                Log.w("SettingsActivity", "lang sync failed: ${e.message}")
            }
        }
    }

    private fun setupPrivacyPolicies() {
        findViewById<View>(R.id.privacy_layout_settings).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://uniandes.edu.co/privacidad")) // Real link
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
        wireNotificationToggles(childrenCheckBoxes)
    }

    private fun wireNotificationToggles(boxes: List<MaterialCheckBox>) {
        lifecycleScope.launch { // Load persisted state on first run
            val enabled = prefs.notificationsFlow().first()
            boxes.forEach { it.isChecked = enabled }
        }
        boxes.forEach { child ->
            child.setOnCheckedChangeListener { _, isChecked ->
                lifecycleScope.launch {
                    prefs.setNotifications(isChecked) // Persist
                    try {
                        RetrofitInstance.meApi.updatePreferences(
                            UpdatePreferencesBody(notificationsEnabled = isChecked)
                        )
                    } catch (e: Exception) {
                        Log.w("SettingsActivity", "notif sync failed: ${e.message}")
                    }
                }
            }
        }
    }
}
