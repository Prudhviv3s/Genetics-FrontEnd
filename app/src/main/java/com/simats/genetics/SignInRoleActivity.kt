package com.simats.genetics

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.appbar.MaterialToolbar

class SignInRoleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in_role)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val patientRoleSelection = findViewById<CardView>(R.id.patient_role_selection)
        patientRoleSelection.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            intent.putExtra("ROLE", "PATIENT")
            startActivity(intent)
        }

        val doctorRoleSelection = findViewById<CardView>(R.id.doctor_role_selection)
        doctorRoleSelection.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            intent.putExtra("ROLE", "DOCTOR")
            startActivity(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}