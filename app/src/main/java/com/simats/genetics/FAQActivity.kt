package com.simats.genetics

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class FAQActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faq)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        findViewById<TextView>(R.id.contact_support_text).setOnClickListener {
            val intent = Intent(this, HelpCenterActivity::class.java)
            startActivity(intent)
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        val clickIds = listOf(
            R.id.faq_row_1, R.id.faq_row_2, R.id.faq_row_3,
            R.id.faq_row_4, R.id.faq_row_5, R.id.faq_row_6,
            R.id.faq_row_7, R.id.faq_row_8, R.id.faq_row_9
        )

        clickIds.forEach { id ->
            findViewById<android.view.View>(id).setOnClickListener {
                android.widget.Toast.makeText(this, "Question expanded", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
