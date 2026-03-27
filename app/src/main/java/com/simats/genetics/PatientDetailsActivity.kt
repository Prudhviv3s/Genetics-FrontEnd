package com.simats.genetics

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton

class PatientDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_details)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val name = intent.getStringExtra("PATIENT_NAME") ?: "Unknown"
        val displayId = intent.getStringExtra("PATIENT_DISPLAY_ID") ?: ""
        val email = intent.getStringExtra("PATIENT_EMAIL") ?: ""
        val age = intent.getIntExtra("PATIENT_AGE", 0)
        val gender = intent.getStringExtra("PATIENT_GENDER") ?: ""

        val patientId = intent.getIntExtra("PATIENT_ID", 0).takeIf { it != 0 }
            ?: intent.getStringExtra("PATIENT_ID")?.toIntOrNull()
            ?: 0

        if (patientId == 0) {
            Toast.makeText(this, "Invalid patient id. Please reopen patient.", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.tv_detail_name).text = name
        findViewById<TextView>(R.id.tv_detail_id).text =
            if (displayId.isNotBlank()) "Patient ID: $displayId" else "Patient ID: -"
        findViewById<TextView>(R.id.tv_detail_email).text = email.ifBlank { "-" }
        findViewById<TextView>(R.id.tv_detail_age).text =
            if (age > 0) "$age years old" else "-"
        findViewById<TextView>(R.id.tv_detail_gender).text = gender.ifBlank { "-" }

        findViewById<MaterialButton>(R.id.btn_view_pedigree).setOnClickListener {
            if (patientId == 0) {
                Toast.makeText(this, "Patient id missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, DoctorPedigreeAnalysisActivity::class.java)
            intent.putExtra("PATIENT_ID", patientId)
            intent.putExtra("PATIENT_NAME", name)
            intent.putExtra("PATIENT_DISPLAY_ID", displayId)
            intent.putExtra("PATIENT_EMAIL", email)
            intent.putExtra("PATIENT_AGE", age)
            intent.putExtra("PATIENT_GENDER", gender)
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.btn_start_analysis).setOnClickListener {
            if (patientId == 0) {
                Toast.makeText(this, "Patient id missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, DoctorDnaReportActivity::class.java)
            intent.putExtra("PATIENT_ID", patientId)
            intent.putExtra("PATIENT_NAME", name)
            intent.putExtra("PATIENT_DISPLAY_ID", displayId)
            intent.putExtra("PATIENT_EMAIL", email)
            intent.putExtra("PATIENT_AGE", age)
            intent.putExtra("PATIENT_GENDER", gender)
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.btn_add_notes).setOnClickListener {
            if (patientId == 0) {
                Toast.makeText(this, "Patient id missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, DoctorNotesActivity::class.java)
            intent.putExtra("PATIENT_ID", patientId)
            intent.putExtra("PATIENT_NAME", name)
            intent.putExtra("PATIENT_DISPLAY_ID", displayId)
            intent.putExtra("PATIENT_EMAIL", email)
            intent.putExtra("PATIENT_AGE", age)
            intent.putExtra("PATIENT_GENDER", gender)
            startActivity(intent)
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_pedigree

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> {
                    val intent = Intent(this, DoctorHomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.navigation_pedigree -> {
                    val intent = Intent(this, DoctorPedigreeListActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.navigation_analysis -> {
                    val intent = Intent(this, DoctorAnalysisActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.navigation_settings -> {
                    val intent = Intent(this, DoctorSettingsActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }

                else -> false
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}