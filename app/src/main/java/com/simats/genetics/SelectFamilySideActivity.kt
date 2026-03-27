package com.simats.genetics

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.TokenManager
import com.simats.genetics.network.requests.FamilyMemberUpdateRequest
import com.simats.genetics.network.responses.FamilyMemberUpdateResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SelectFamilySideActivity : AppCompatActivity() {

    private var selectedSide: String? = null
    private var memberId: Int = -1
    private var relationship: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_family_side)

        memberId = intent.getIntExtra("MEMBER_ID", -1)
        relationship = intent.getStringExtra("RELATIONSHIP")
        if (memberId == -1) {
            Toast.makeText(this, "Member ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // ... (rest of onCreate remains similar but calls navigateNext)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val cardFather = findViewById<MaterialCardView>(R.id.card_father_side)
        val cardMother = findViewById<MaterialCardView>(R.id.card_mother_side)
        val rbFather = findViewById<RadioButton>(R.id.rb_father_side)
        val rbMother = findViewById<RadioButton>(R.id.rb_mother_side)
        val btnContinue = findViewById<Button>(R.id.btn_continue)

        cardFather.setOnClickListener {
            selectSide("Father Side", cardFather, rbFather, cardMother, rbMother, btnContinue)
        }

        cardMother.setOnClickListener {
            selectSide("Mother Side", cardMother, rbMother, cardFather, rbFather, btnContinue)
        }

        btnContinue.setOnClickListener {
            if (selectedSide == null) {
                Toast.makeText(this, "Please select a side", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            navigateNext(selectedSide!!)
        }
    }

    private fun selectSide(
        side: String,
        selectedCard: MaterialCardView,
        selectedRb: RadioButton,
        otherCard: MaterialCardView,
        otherRb: RadioButton,
        btnContinue: Button
    ) {
        selectedSide = side
        
        val activeColor = Color.parseColor("#1C57D9")
        val inactiveColor = Color.parseColor("#E0E0E0")

        selectedCard.setStrokeColor(ColorStateList.valueOf(activeColor))
        selectedCard.strokeWidth = 4
        selectedRb.isChecked = true
        
        otherCard.setStrokeColor(ColorStateList.valueOf(inactiveColor))
        otherCard.strokeWidth = 2
        otherRb.isChecked = false
        
        btnContinue.backgroundTintList = ColorStateList.valueOf(activeColor)
        btnContinue.isEnabled = true
    }

    private fun navigateNext(side: String) {
        val i = Intent(this, HealthStatusActivity::class.java)
        i.putExtra("MEMBER_ID", memberId)
        i.putExtra("RELATIONSHIP", relationship)
        i.putExtra("SIDE", side)
        startActivity(i)
    }
}
