package com.simats.genetics

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.android.material.appbar.MaterialToolbar

class ProfileSetupActivity : AppCompatActivity() {

    private var selectedGender: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_setup)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val btnMale = findViewById<AppCompatButton>(R.id.btn_male)
        val btnFemale = findViewById<AppCompatButton>(R.id.btn_female)
        val btnOther = findViewById<AppCompatButton>(R.id.btn_other)

        val genderButtons = listOf(btnMale, btnFemale, btnOther)

        genderButtons.forEach { button ->
            button.setOnClickListener {
                genderButtons.forEach { it.isSelected = false }
                button.isSelected = true
                selectedGender = button.text.toString()
            }
        }

        val dobInput = findViewById<EditText>(R.id.dob_input)
        dobInput.setOnClickListener {
            Toast.makeText(this, "Date picker coming soon", Toast.LENGTH_SHORT).show()
        }

        val continueButton = findViewById<Button>(R.id.continue_button)
        continueButton.setOnClickListener {
            if (selectedGender == null) {
                Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Proceed to Privacy & Consent page
            val intent = Intent(this, PrivacyDisclaimerActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
