package com.simats.genetics

import android.os.Bundle
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class LanguageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupLanguageSelections()
    }

    private fun setupLanguageSelections() {
        val languageItems = listOf(
            LanguageItem(R.id.lang_english, R.id.check_english),
            LanguageItem(R.id.lang_spanish, R.id.check_spanish),
            LanguageItem(R.id.lang_french, R.id.check_french),
            LanguageItem(R.id.lang_german, R.id.check_german),
            LanguageItem(R.id.lang_chinese, R.id.check_chinese),
            LanguageItem(R.id.lang_japanese, R.id.check_japanese),
            LanguageItem(R.id.lang_arabic, R.id.check_arabic),
            LanguageItem(R.id.lang_hindi, R.id.check_hindi),
            LanguageItem(R.id.lang_portuguese, R.id.check_portuguese),
            LanguageItem(R.id.lang_russian, R.id.check_russian)
        )

        var selectedId = R.id.lang_english // Default

        languageItems.forEach { item ->
            val card = findViewById<com.google.android.material.card.MaterialCardView>(item.cardId)
            card.setOnClickListener {
                if (selectedId != item.cardId) {
                    // Update UI for previously selected
                    val prevItem = languageItems.find { it.cardId == selectedId }
                    prevItem?.let { prev ->
                        val prevCard = findViewById<com.google.android.material.card.MaterialCardView>(prev.cardId)
                        prevCard.strokeColor = android.graphics.Color.parseColor("#E0E0E0")
                        findViewById<android.view.View>(prev.checkId).visibility = android.view.View.GONE
                    }

                    // Update UI for newly selected
                    card.strokeColor = android.graphics.Color.parseColor("#2196F3")
                    findViewById<android.view.View>(item.checkId).visibility = android.view.View.VISIBLE
                    
                    selectedId = item.cardId
                    Toast.makeText(this, "Language updated", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private data class LanguageItem(val cardId: Int, val checkId: Int)

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
