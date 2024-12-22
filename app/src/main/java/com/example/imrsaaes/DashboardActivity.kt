package com.example.imrsaaes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.imrsaaes.ui.theme.ProfileActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DashboardActivity : AppCompatActivity() {

    private lateinit var imgProfile: ImageView
    private lateinit var btnSearchContacts: FloatingActionButton // Floating Action Button

    // Variabel untuk menampung data user
    private var userName: String? = null
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Inisialisasi komponen UI
        imgProfile = findViewById(R.id.imgProfile)
        btnSearchContacts = findViewById(R.id.btnSearchContacts) // Inisialisasi button

        // Ambil data user yang diterima dari LoginActivity
        userName = intent.getStringExtra("USER_NAME")
        userEmail = intent.getStringExtra("USER_EMAIL")

        // Di dalam onCreate DashboardActivity
        val sharedPref = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        val authToken = sharedPref.getString("authToken", null)


// Debugging: Log tokennya
        if (authToken != null) {
            Log.d("DashboardActivity", "Auth Token: $authToken")
        } else {
            Log.d("DashboardActivity", "Auth Token tidak ditemukan!")
        }

        // Buka ProfileActivity dengan data user saat avatar diklik
        imgProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Buka SearchContactsActivity saat tombol search diklik
        btnSearchContacts.setOnClickListener {
            val intent = Intent(this, SearchContactsActivity::class.java)
            intent.putExtra("AUTH_TOKEN", authToken)  // Kirim token ke SearchContactsActivity
            startActivity(intent)
        }
    }
}
