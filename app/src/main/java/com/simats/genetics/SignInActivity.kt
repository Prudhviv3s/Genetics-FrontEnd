package com.simats.genetics

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.TokenManager
import com.simats.genetics.network.requests.LoginRequest
import com.simats.genetics.network.responses.ApiResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignInActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var signInButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Forgot password
        findViewById<TextView>(R.id.forgot_password_text).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // Sign up
        findViewById<TextView>(R.id.sign_up_text).setOnClickListener {
            startActivity(Intent(this, RoleSelectionActivity::class.java))
        }

        // Header image based on role (kept as-is)
        val roleFromIntent = intent.getStringExtra("ROLE")
        if (roleFromIntent == "DOCTOR") {
            val headerImage = findViewById<android.widget.ImageView>(R.id.header_image)
            headerImage.setImageResource(R.drawable.ic_stethoscope)
            headerImage.visibility = android.view.View.VISIBLE
        }

        // Bind inputs
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        signInButton = findViewById(R.id.sign_in_button)

        // Password visibility toggle
        val passwordToggle = findViewById<android.widget.ImageView>(R.id.password_toggle_icon)
        var isPasswordVisible = false
        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                passwordInput.inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                passwordToggle.setImageResource(R.drawable.ic_eye) // Using same icon or eye-off if exists
                passwordToggle.setColorFilter(getColor(R.color.solidblue))
            } else {
                passwordInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordToggle.setImageResource(R.drawable.ic_eye)
                passwordToggle.setColorFilter(getColor(R.color.light_gray))
            }
            passwordInput.setSelection(passwordInput.text.length)
        }

        signInButton.setOnClickListener {
            doLogin()
        }
    }

    private fun doLogin() {
        val email = emailInput.text?.toString()?.trim().orEmpty()
        val password = passwordInput.text?.toString().orEmpty()

        if (email.isEmpty()) {
            emailInput.error = "Email is required"
            emailInput.requestFocus()
            return
        }
        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            passwordInput.requestFocus()
            return
        }

        signInButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = ApiClient.getApi(this@SignInActivity).login(LoginRequest(email = email, password = password))

                withContext(Dispatchers.Main) {
                    signInButton.isEnabled = true

                    if (!res.isSuccessful) {
                        val err = res.errorBody()?.string()
                        Log.e("LOGIN", "HTTP ${res.code()} err=$err")
                        Toast.makeText(this@SignInActivity, "HTTP ${res.code()}", Toast.LENGTH_LONG).show()
                        return@withContext
                    }

                    val body: ApiResponse? = res.body()
                    if (body?.status != true || body.token.isNullOrBlank()) {
                        Toast.makeText(
                            this@SignInActivity,
                            body?.message ?: "Login failed",
                            Toast.LENGTH_LONG
                        ).show()
                        return@withContext
                    }

                    //  SAVE TOKEN (this makes Doctor dashboard work)
                    TokenManager.saveToken(this@SignInActivity, body.token!!)
                    body.full_name?.let { TokenManager.saveUserName(this@SignInActivity, it) }

                    //  Navigate by role returned from backend (not by intent ROLE)
                    val role = (body.role ?: "").trim()
                    if (role.equals("Doctor", ignoreCase = true)) {
                        startActivity(Intent(this@SignInActivity, DoctorHomeActivity::class.java))
                    } else {
                        startActivity(Intent(this@SignInActivity, PatientHomeActivity::class.java))
                    }
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    signInButton.isEnabled = true
                    Log.e("LOGIN", "FAIL ${e.message}", e)
                    Toast.makeText(this@SignInActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}