package com.simats.genetics

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.requests.UpdateProfileRequest
import com.simats.genetics.network.responses.MyProfileResponse
import com.simats.genetics.network.responses.UpdateProfileResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class EditProfileActivity : AppCompatActivity() {

    private lateinit var profileImage: ShapeableImageView
    private lateinit var tvUserId: TextView

    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText
    private lateinit var dobInput: TextInputEditText
    private lateinit var genderInput: MaterialAutoCompleteTextView
    private lateinit var genderInputLayout: com.google.android.material.textfield.TextInputLayout

    private lateinit var saveButton: MaterialButton

    private var myRole: String = ""
    private var myId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // views
        profileImage = findViewById(R.id.profile_image)
        tvUserId = findViewById(R.id.tv_user_id)

        nameInput = findViewById(R.id.name_input)
        emailInput = findViewById(R.id.email_input)
        phoneInput = findViewById(R.id.phone_input)
        dobInput = findViewById(R.id.dob_input)
        genderInput = findViewById(R.id.gender_input)
        genderInputLayout = findViewById(R.id.gender_input_layout)

        saveButton = findViewById(R.id.save_button)

        // gender dropdown
        val genders = arrayOf("Male", "Female", "Other")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, genders)
        genderInput.setAdapter(adapter)

        genderInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateProfileIcon(s?.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        dobInput.setOnClickListener { showDatePicker() }
        saveButton.setOnClickListener { saveProfile() }

        // Populate from intent extras if available
        intent.getStringExtra("FULL_NAME")?.let { nameInput.setText(it) }
        intent.getStringExtra("EMAIL")?.let { emailInput.setText(it) }
        intent.getStringExtra("PHONE")?.let { phoneInput.setText(it) }
        intent.getStringExtra("DOB")?.let { dobInput.setText(it) }
        intent.getStringExtra("GENDER")?.let { 
            genderInput.setText(it, false)
            updateProfileIcon(it)
        }

        loadProfile()
    }

    private fun updateProfileIcon(gender: String?) {
        val isFemale = (gender ?: "").lowercase() == "female"
        
        if (isFemale) {
            profileImage.setImageResource(R.drawable.ic_female_avatar)
            genderInputLayout.setStartIconDrawable(R.drawable.ic_female_silhouette)
            profileImage.setPadding(0, 0, 0, 0)
            profileImage.strokeWidth = 0f
            profileImage.imageTintList = null
            genderInputLayout.setStartIconTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.text_secondary)))
        } else {
            profileImage.setImageResource(R.drawable.ic_person)
            genderInputLayout.setStartIconDrawable(R.drawable.ic_person)
            profileImage.setPadding(30, 30, 30, 30)
            profileImage.strokeWidth = 2f
            profileImage.imageTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.solidblue))
            genderInputLayout.setStartIconTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.text_secondary)))
        }
    }

    // =========================
    // LOAD PROFILE (GET /me/)
    // =========================
    private fun loadProfile() {
        ApiClient.getApi(this).getMyProfile().enqueue(object : Callback<MyProfileResponse> {
            override fun onResponse(call: Call<MyProfileResponse>, response: Response<MyProfileResponse>) {
                if (!response.isSuccessful) {
                    Log.e("EDIT_PROFILE", "HTTP ${response.code()} ${response.errorBody()?.string()}")
                    Toast.makeText(this@EditProfileActivity, "Failed to load profile", Toast.LENGTH_SHORT).show()
                    return
                }

                val body = response.body()
                if (body?.status != true || body.profile == null) {
                    Toast.makeText(this@EditProfileActivity, "Profile not found", Toast.LENGTH_SHORT).show()
                    return
                }

                val p = body.profile

                myRole = (p.role ?: "").trim()
                myId = p.id ?: 0

                // Same ID format
                val displayId = when (myRole.lowercase()) {
                    "doctor" -> "DR${1000 + myId}"
                    "patient" -> "PT${1000 + myId}"
                    else -> "#$myId"
                }
                tvUserId.text = "${if (myRole.isNotBlank()) myRole else "User"} ID: #$displayId"

                nameInput.setText(p.fullName ?: "")
                emailInput.setText(p.email ?: "")
                phoneInput.setText(p.phone ?: "")
                dobInput.setText(p.dob ?: "")
                genderInput.setText(p.gender ?: "", false)
                updateProfileIcon(p.gender)
            }

            override fun onFailure(call: Call<MyProfileResponse>, t: Throwable) {
                Log.e("EDIT_PROFILE", "FAIL ${t.message}", t)
                Toast.makeText(this@EditProfileActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // =========================
    // SAVE PROFILE (PUT /me/)
    // =========================
    private fun saveProfile() {
        val fullName = nameInput.text?.toString()?.trim().orEmpty()
        val email = emailInput.text?.toString()?.trim().orEmpty()
        val phone = phoneInput.text?.toString()?.trim().orEmpty()
        val dob = dobInput.text?.toString()?.trim().orEmpty()
        val gender = genderInput.text?.toString()?.trim().orEmpty()

        if (fullName.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email are required", Toast.LENGTH_SHORT).show()
            return
        }

        val req = UpdateProfileRequest(
            fullName = fullName,
            email = email,
            phone = phone,
            dob = dob,
            gender = gender
        )

        ApiClient.getApi(this).updateMyProfile(req).enqueue(object : Callback<UpdateProfileResponse> {
            override fun onResponse(call: Call<UpdateProfileResponse>, response: Response<UpdateProfileResponse>) {
                if (!response.isSuccessful) {
                    Log.e("EDIT_PROFILE", "SAVE HTTP ${response.code()} ${response.errorBody()?.string()}")
                    Toast.makeText(this@EditProfileActivity, "Failed to save", Toast.LENGTH_SHORT).show()
                    return
                }

                val body = response.body()
                if (body?.status == true) {
                    Toast.makeText(this@EditProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@EditProfileActivity, body?.message ?: "Error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UpdateProfileResponse>, t: Throwable) {
                Log.e("EDIT_PROFILE", "SAVE FAIL ${t.message}", t)
                Toast.makeText(this@EditProfileActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val date = "%02d/%02d/%04d".format(selectedDay, selectedMonth + 1, selectedYear)
                dobInput.setText(date)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }
}
