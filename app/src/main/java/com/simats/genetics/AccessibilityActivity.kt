package com.simats.genetics

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class AccessibilityActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accessibility)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        val clickIds = listOf(
            R.id.text_size_small, R.id.text_size_medium,
            R.id.text_size_large, R.id.text_size_extra_large,
            R.id.row_high_contrast, R.id.row_reduce_motion,
            R.id.row_screen_reader
        )

        clickIds.forEach { id ->
            findViewById<android.view.View>(id).setOnClickListener {
                android.widget.Toast.makeText(this, "Setting updated", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
