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

        // Get data from intent
        val name = intent.getStringExtra("PATIENT_NAME") ?: "Unknown"
        val displayId = intent.getStringExtra("PATIENT_DISPLAY_ID") ?: ""
        val email = intent.getStringExtra("PATIENT_EMAIL") ?: ""
        val age = intent.getIntExtra("PATIENT_AGE", 0)
        val gender = intent.getStringExtra("PATIENT_GENDER") ?: ""

        // IMPORTANT: read PATIENT_ID safely (it may be sent as String in some screens)
        val databaseId: Int = intent.getIntExtra("PATIENT_ID", 0).takeIf { it != 0 }
            ?: intent.getStringExtra("PATIENT_ID")?.toIntOrNull()
            ?: 0

        if (databaseId == 0) {
            Toast.makeText(this, "Invalid patient id. Please reopen patient.", Toast.LENGTH_SHORT).show()
        }

        // Set data to views
        findViewById<TextView>(R.id.tv_detail_name).text = name
        findViewById<TextView>(R.id.tv_detail_id).text =
            if (displayId.isNotBlank()) "Patient ID: $displayId" else "Patient ID: -"
        findViewById<TextView>(R.id.tv_detail_email).text = email.ifBlank { "-" }
        findViewById<TextView>(R.id.tv_detail_age).text = if (age > 0) "$age years old" else "-"
        findViewById<TextView>(R.id.tv_detail_gender).text = gender.ifBlank { "-" }

        // Button Click Listeners
        findViewById<MaterialButton>(R.id.btn_view_pedigree).setOnClickListener {
            if (databaseId == 0) {
                Toast.makeText(this, "Patient id missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val i = Intent(this, DoctorPedigreeAnalysisActivity::class.java)
            i.putExtra("PATIENT_NAME", name)
            i.putExtra("PATIENT_ID", databaseId)          //  int for API calls
            i.putExtra("PATIENT_DISPLAY_ID", displayId)   //  for header
            i.putExtra("PATIENT_EMAIL", email)
            i.putExtra("PATIENT_AGE", age)
            i.putExtra("PATIENT_GENDER", gender)
            startActivity(i)
        }

        findViewById<MaterialButton>(R.id.btn_start_analysis).setOnClickListener {
            if (databaseId == 0) {
                Toast.makeText(this, "Patient id missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val i = Intent(this, DoctorDnaReportActivity::class.java)
            i.putExtra("PATIENT_ID", databaseId)          //  int
            i.putExtra("PATIENT_NAME", name)
            i.putExtra("PATIENT_DISPLAY_ID", displayId)
            startActivity(i)
        }

        findViewById<MaterialButton>(R.id.btn_add_notes).setOnClickListener {
            if (databaseId == 0) {
                Toast.makeText(this, "Patient id missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val i = Intent(this, DoctorNotesActivity::class.java)
            i.putExtra("PATIENT_ID", databaseId)          //  FIX: you forgot this
            i.putExtra("PATIENT_NAME", name)
            i.putExtra("PATIENT_DISPLAY_ID", displayId)
            startActivity(i)
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_pedigree

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> {
                    val i = Intent(this, DoctorHomeActivity::class.java)
                    i.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(i)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_pedigree -> {
                    val i = Intent(this, DoctorPedigreeListActivity::class.java)
                    i.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(i)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_analysis -> {
                    val i = Intent(this, DoctorAnalysisActivity::class.java)
                    i.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(i)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_settings -> {
                    val i = Intent(this, DoctorSettingsActivity::class.java)
                    i.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(i)
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