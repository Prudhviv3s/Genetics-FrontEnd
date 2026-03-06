package com.simats.genetics

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.requests.RegisterRequest
import com.simats.genetics.network.responses.RegisterResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var fullNameInput: TextInputEditText
    private lateinit var dobInput: TextInputEditText
    private lateinit var genderInput: AutoCompleteTextView
    private lateinit var ageInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var termsCheckbox: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Initialize views
        fullNameInput = findViewById(R.id.full_name_input)
        dobInput = findViewById(R.id.dob_input)
        genderInput = findViewById(R.id.gender_input)
        ageInput = findViewById(R.id.age_input)
        emailInput = findViewById(R.id.email_input)
        phoneInput = findViewById(R.id.phone_input)
        passwordInput = findViewById(R.id.password_input)
        confirmPasswordInput = findViewById(R.id.confirm_password_input)
        termsCheckbox = findViewById(R.id.terms_checkbox)

        setupDatePicker()
        setupGenderDropdown()

        findViewById<Button>(R.id.create_account_button).setOnClickListener {
            registerUser()
        }

        findViewById<android.widget.TextView>(R.id.sign_in_text).setOnClickListener {
            val i = Intent(this, SignInActivity::class.java)
            i.putExtra("ROLE", intent.getStringExtra("ROLE"))
            startActivity(i)
        }

        // header image based on role (kept as-is)
        val role = intent.getStringExtra("ROLE")
        if (role == "DOCTOR") {
            val headerImage = findViewById<android.widget.ImageView>(R.id.header_image)
            headerImage.setImageResource(R.drawable.ic_stethoscope)
            headerImage.visibility = android.view.View.VISIBLE
        }
    }

    // DOB MUST BE YYYY-MM-DD for Django DateField
    private fun setupDatePicker() {
        dobInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val mm = (selectedMonth + 1).toString().padStart(2, '0')
                    val dd = selectedDay.toString().padStart(2, '0')
                    dobInput.setText("$selectedYear-$mm-$dd") // YYYY-MM-DD
                },
                year, month, day
            ).show()
        }
    }

    private fun setupGenderDropdown() {
        val genders = arrayOf("Male", "Female", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders)
        genderInput.setAdapter(adapter)
    }

    private fun registerUser() {
        val fullName = fullNameInput.text?.toString()?.trim().orEmpty()
        val dob = dobInput.text?.toString()?.trim().orEmpty()
        val gender = genderInput.text?.toString()?.trim().orEmpty()
        val ageStr = ageInput.text?.toString()?.trim().orEmpty()
        val email = emailInput.text?.toString()?.trim().orEmpty()
        val phone = phoneInput.text?.toString()?.trim().orEmpty()
        val password = passwordInput.text?.toString().orEmpty()
        val confirmPassword = confirmPasswordInput.text?.toString().orEmpty()

        // Validate inputs
        if (fullName.isEmpty()) { fullNameInput.error = "Full name is required"; fullNameInput.requestFocus(); return }
        if (dob.isEmpty()) { dobInput.error = "Date of birth is required"; dobInput.requestFocus(); return }
        if (gender.isEmpty()) { genderInput.error = "Gender is required"; genderInput.requestFocus(); return }
        if (ageStr.isEmpty()) { ageInput.error = "Age is required"; ageInput.requestFocus(); return }

        val age = ageStr.toIntOrNull()
        if (age == null || age <= 0) { ageInput.error = "Please enter a valid age"; ageInput.requestFocus(); return }

        if (email.isEmpty()) { emailInput.error = "Email is required"; emailInput.requestFocus(); return }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Please enter a valid email"; emailInput.requestFocus(); return
        }

        if (phone.isEmpty()) { phoneInput.error = "Phone number is required"; phoneInput.requestFocus(); return }

        if (password.isEmpty()) { passwordInput.error = "Password is required"; passwordInput.requestFocus(); return }
        if (password.length < 6) { passwordInput.error = "Password must be at least 6 characters"; passwordInput.requestFocus(); return }

        if (password != confirmPassword) {
            confirmPasswordInput.error = "Passwords do not match"; confirmPasswordInput.requestFocus(); return
        }

        if (!termsCheckbox.isChecked) {
            Toast.makeText(this, "Please agree to the Terms of Service", Toast.LENGTH_SHORT).show()
            return
        }

        //  Role comes from previous screen, but backend expects URL role as lowercase: doctor/patient
        val rolePath = (intent.getStringExtra("ROLE") ?: "PATIENT").lowercase() // "doctor" or "patient"

        //  Keep role in request body because your RegisterSerializer validates role
        val roleBody = if (rolePath == "doctor") "Doctor" else "Patient"

        val request = RegisterRequest(
            full_name = fullName,
            email = email,
            phone = phone,
            dob = dob,
            gender = gender,
            age = age,
            password = password,
            confirm_password = confirmPassword,
            terms_accepted = true
        )

        ApiClient.getApi(this).registerUser(rolePath, request).enqueue(object : Callback<RegisterResponse> {

            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status == true) {
                        Toast.makeText(this@CreateAccountActivity, body.message ?: "Account created!", Toast.LENGTH_SHORT).show()
                        val i = Intent(this@CreateAccountActivity, SignInActivity::class.java)
                        i.putExtra("ROLE", intent.getStringExtra("ROLE"))
                        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(i)
                        finish()
                    } else {
                        Toast.makeText(this@CreateAccountActivity, body?.message ?: "Registration failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errText = response.errorBody()?.string()
                    Log.e("REGISTER", "HTTP ${response.code()} error=$errText")
                    Toast.makeText(
                        this@CreateAccountActivity,
                        "HTTP ${response.code()}: ${errText ?: "Bad Request"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                Log.e("REGISTER", "FAIL: ${t.javaClass.name} ${t.message}", t)
                Toast.makeText(this@CreateAccountActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}