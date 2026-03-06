package com.simats.genetics

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class HelpCenterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_center)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        val clickIds = listOf(
            R.id.row_guide, R.id.row_faq, R.id.row_tutorials,
            R.id.row_email, R.id.row_phone, R.id.row_live_chat
        )

        clickIds.forEach { id ->
            findViewById<android.view.View>(id).setOnClickListener {
                android.widget.Toast.makeText(this, "Redirecting...", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
