package com.example.imrsaaes.ui.theme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.imrsaaes.MainActivity
import com.example.imrsaaes.R

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val tvName: TextView = findViewById(R.id.tvName)
        val tvEmail: TextView = findViewById(R.id.tvEmail)
        val btnLogout: Button = findViewById(R.id.btnLogout)

        val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        val userName = sharedPreferences.getString("userName", "Nama tidak ditemukan")
        val userEmail = sharedPreferences.getString("email", "Email tidak ditemukan")

        tvName.text = userName
        tvEmail.text = userEmail

        btnLogout.setOnClickListener {
            val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        }
    }
}

