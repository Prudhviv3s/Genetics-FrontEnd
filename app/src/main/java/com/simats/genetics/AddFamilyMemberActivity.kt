package com.simats.genetics

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.TokenManager
import com.simats.genetics.network.requests.FamilyMemberCreateRequest
import com.simats.genetics.network.responses.FamilyMemberCreateResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddFamilyMemberActivity : AppCompatActivity() {

    private var selectedGender: String? = null

    private lateinit var etFullName: EditText
    private lateinit var etAge: EditText

    private lateinit var btnMale: Button
    private lateinit var btnFemale: Button
    private lateinit var btnOther: Button
    private lateinit var btnContinue: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_family_member)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        //  CHANGE THESE TWO IDs if your XML uses different ones
        etFullName = findViewById(R.id.et_full_name)
        etAge = findViewById(R.id.et_age)

        btnMale = findViewById(R.id.btn_male)
        btnFemale = findViewById(R.id.btn_female)
        btnOther = findViewById(R.id.btn_other)
        btnContinue = findViewById(R.id.btn_continue)

        val genderButtons = listOf(btnMale, btnFemale, btnOther)

        btnMale.setOnClickListener { updateGenderSelection(btnMale, genderButtons, "Male") }
        btnFemale.setOnClickListener { updateGenderSelection(btnFemale, genderButtons, "Female") }
        btnOther.setOnClickListener { updateGenderSelection(btnOther, genderButtons, "Other") }

        btnContinue.setOnClickListener { createFamilyMember() }
    }

    private fun updateGenderSelection(selectedBtn: Button, allButtons: List<Button>, gender: String) {
        allButtons.forEach { it.isSelected = false }
        selectedBtn.isSelected = true
        selectedGender = gender

        // enable continue visual
        btnContinue.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1C57D9"))
    }

    private fun createFamilyMember() {
        val token = TokenManager.getToken(this)
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        val name = etFullName.text?.toString()?.trim().orEmpty()
        val ageStr = etAge.text?.toString()?.trim().orEmpty()
        val age = ageStr.toIntOrNull()

        if (name.isEmpty()) { etFullName.error = "Enter full name"; return }
        if (selectedGender.isNullOrBlank()) { Toast.makeText(this, "Select gender", Toast.LENGTH_SHORT).show(); return }
        if (age == null || age <= 0) { etAge.error = "Enter valid age"; return }

        btnContinue.isEnabled = false

        val req = FamilyMemberCreateRequest(
            full_name = name,
            gender = selectedGender!!,
            age = age
        )

        ApiClient.getApi(this).createFamilyMember(req).enqueue(object : Callback<FamilyMemberCreateResponse> {

            override fun onResponse(
                call: Call<FamilyMemberCreateResponse>,
                response: Response<FamilyMemberCreateResponse>
            ) {
                btnContinue.isEnabled = true

                if (response.code() == 401) {
                    Toast.makeText(this@AddFamilyMemberActivity, "Session expired. Login again.", Toast.LENGTH_SHORT).show()
                    TokenManager.clearToken(this@AddFamilyMemberActivity)
                    startActivity(Intent(this@AddFamilyMemberActivity, SignInActivity::class.java))
                    finish()
                    return
                }

                if (!response.isSuccessful) {
                    val err = response.errorBody()?.string()
                    Log.e("FAMILY_CREATE", "HTTP ${response.code()} err=$err")
                    Toast.makeText(this@AddFamilyMemberActivity, "HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                    return
                }

                val body = response.body()
                if (body?.status == true && body.member?.id != null) {

                    //  go next with MEMBER_ID
                    val i = Intent(this@AddFamilyMemberActivity, SelectRelationshipActivity::class.java)
                    i.putExtra("MEMBER_ID", body.member.id)
                    startActivity(i)

                } else {
                    Toast.makeText(this@AddFamilyMemberActivity, body?.message ?: "Failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<FamilyMemberCreateResponse>, t: Throwable) {
                btnContinue.isEnabled = true
                Log.e("FAMILY_CREATE", "FAIL ${t.message}", t)
                Toast.makeText(this@AddFamilyMemberActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}