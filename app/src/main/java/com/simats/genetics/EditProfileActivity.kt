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
    private lateinit var ageInput: TextInputEditText
    private lateinit var genderInput: MaterialAutoCompleteTextView
    private lateinit var genderInputLayout: com.google.android.material.textfield.TextInputLayout

    private lateinit var saveButton: MaterialButton

    private var myRole: String = ""
    private var myId: Int = 0
    private var doctorIdFromApi: String = ""
    private var patientIdFromApi: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        profileImage = findViewById(R.id.profile_image)
        tvUserId = findViewById(R.id.tv_user_id)

        nameInput = findViewById(R.id.name_input)
        emailInput = findViewById(R.id.email_input)
        phoneInput = findViewById(R.id.phone_input)
        dobInput = findViewById(R.id.dob_input)
        ageInput = findViewById(R.id.age_input)
        genderInput = findViewById(R.id.gender_input)
        genderInputLayout = findViewById(R.id.gender_input_layout)

        saveButton = findViewById(R.id.save_button)

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
            genderInputLayout.setStartIconTintList(
                android.content.res.ColorStateList.valueOf(getColor(R.color.text_secondary))
            )
        } else {
            profileImage.setImageResource(R.drawable.ic_person)
            genderInputLayout.setStartIconDrawable(R.drawable.ic_person)
            profileImage.setPadding(30, 30, 30, 30)
            profileImage.strokeWidth = 2f
            profileImage.imageTintList =
                android.content.res.ColorStateList.valueOf(getColor(R.color.solidblue))
            genderInputLayout.setStartIconTintList(
                android.content.res.ColorStateList.valueOf(getColor(R.color.text_secondary))
            )
        }
    }

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
                doctorIdFromApi = p.doctorId ?: ""
                patientIdFromApi = p.patientId ?: ""

                val displayId = when (myRole.lowercase()) {
                    "doctor" -> if (doctorIdFromApi.isNotBlank()) doctorIdFromApi else "DT${1000 + myId}"
                    "patient" -> if (patientIdFromApi.isNotBlank()) patientIdFromApi else "PT${1000 + myId}"
                    else -> "#$myId"
                }

                tvUserId.text = "${if (myRole.isNotBlank()) myRole else "User"} ID: $displayId"

                nameInput.setText(p.fullName ?: "")
                emailInput.setText(p.email ?: "")
                phoneInput.setText(p.phone ?: "")
                dobInput.setText(p.dob ?: "")
                ageInput.setText(p.age?.toString() ?: "")
                genderInput.setText(p.gender ?: "", false)
                updateProfileIcon(p.gender)
            }

            override fun onFailure(call: Call<MyProfileResponse>, t: Throwable) {
                Log.e("EDIT_PROFILE", "FAIL ${t.message}", t)
                Toast.makeText(this@EditProfileActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveProfile() {
        val fullName = nameInput.text?.toString()?.trim().orEmpty()
        val email = emailInput.text?.toString()?.trim().orEmpty()
        val phone = phoneInput.text?.toString()?.trim().orEmpty()
        val dob = dobInput.text?.toString()?.trim().orEmpty()
        val ageStr = ageInput.text?.toString()?.trim().orEmpty()
        val gender = genderInput.text?.toString()?.trim().orEmpty()

        if (fullName.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email are required", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Please enter a valid email"
            emailInput.requestFocus()
            return
        }
        if (!email.lowercase().endsWith(".com") && !email.lowercase().endsWith(".in")) {
            emailInput.error = "Email must end with .com or .in"
            emailInput.requestFocus()
            return
        }

        if (!fullName.matches(Regex("^[a-zA-Z\\s]+$"))) {
            nameInput.error = "Full name must contain only alphabets"
            nameInput.requestFocus()
            return
        }

        if (phone.isNotBlank() && phone.length != 10) {
            phoneInput.error = "Phone number must be 10 digits"
            phoneInput.requestFocus()
            return
        }

        val req = UpdateProfileRequest(
            fullName = fullName,
            email = email,
            phone = phone,
            dob = dob,
            age = ageStr.toIntOrNull(),
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
                val mm = (selectedMonth + 1).toString().padStart(2, '0')
                val dd = selectedDay.toString().padStart(2, '0')
                dobInput.setText("$selectedYear-$mm-$dd")

                // Calculate age automatically
                val birthCalendar = Calendar.getInstance()
                birthCalendar.set(selectedYear, selectedMonth, selectedDay)
                
                val today = Calendar.getInstance()
                var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
                
                if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
                    age--
                }
                
                if (age >= 0) {
                    ageInput.setText(age.toString())
                }
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }
}