package com.simats.genetics

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.simats.genetics.databinding.ActivityPrivacyDisclaimerBinding

class PrivacyDisclaimerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPrivacyDisclaimerBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyDisclaimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Read-only page, listeners for "Full" versions can be added here if needed
        binding.linkPrivacyPolicy.setOnClickListener {
            Toast.makeText(this, "Full Privacy Policy coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.linkMedicalDisclaimer.setOnClickListener {
            Toast.makeText(this, "Full Medical Disclaimer coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}