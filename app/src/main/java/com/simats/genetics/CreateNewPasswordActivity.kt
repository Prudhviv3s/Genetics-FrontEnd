package com.simats.genetics

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.util.Log
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.requests.ResetPasswordRequest
import com.simats.genetics.network.responses.ResetPasswordResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class CreateNewPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_new_password)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val email = intent.getStringExtra("EMAIL").orEmpty()
        val otpFromIntent = intent.getStringExtra("OTP").orEmpty()
        
        val otpInput = findViewById<android.widget.EditText>(R.id.otp_input)
        if (otpFromIntent.isNotEmpty()) {
            otpInput.setText(otpFromIntent)
        }
        val passwordInput = findViewById<android.widget.EditText>(R.id.password_input)
        val confirmPasswordInput = findViewById<android.widget.EditText>(R.id.confirm_password_input)

        // Password visibility toggle 1
        val passwordToggle1 = findViewById<android.widget.ImageView>(R.id.password_toggle_1)
        var isPasswordVisible1 = false
        passwordToggle1.setOnClickListener {
            isPasswordVisible1 = !isPasswordVisible1
            if (isPasswordVisible1) {
                passwordInput.inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                passwordToggle1.setColorFilter(getColor(R.color.solidblue))
            } else {
                passwordInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordToggle1.setColorFilter(getColor(R.color.light_gray))
            }
            passwordInput.setSelection(passwordInput.text.length)
        }

        // Password visibility toggle 2
        val passwordToggle2 = findViewById<android.widget.ImageView>(R.id.password_toggle_2)
        var isPasswordVisible2 = false
        passwordToggle2.setOnClickListener {
            isPasswordVisible2 = !isPasswordVisible2
            if (isPasswordVisible2) {
                confirmPasswordInput.inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                passwordToggle2.setColorFilter(getColor(R.color.solidblue))
            } else {
                confirmPasswordInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordToggle2.setColorFilter(getColor(R.color.light_gray))
            }
            confirmPasswordInput.setSelection(confirmPasswordInput.text.length)
        }

        val resetPasswordButton = findViewById<Button>(R.id.reset_password_button)
        resetPasswordButton.setOnClickListener {
            val otp = otpInput.text.toString().trim()
            if (otp.isEmpty()) {
                otpInput.error = "OTP is required"
                otpInput.requestFocus()
                return@setOnClickListener
            }
            if (otp.length < 6) {
                otpInput.error = "Enter a valid 6-digit OTP"
                otpInput.requestFocus()
                return@setOnClickListener
            }

            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (password.isEmpty()) {
                passwordInput.error = "Password is required"
                passwordInput.requestFocus()
                return@setOnClickListener
            }
            val missingRequirements = com.simats.genetics.utils.PasswordValidator.validate(password)
            if (missingRequirements.isNotEmpty()) {
                passwordInput.error = "Missing: " + missingRequirements.joinToString(", ")
                passwordInput.requestFocus()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                confirmPasswordInput.error = "Passwords do not match"
                confirmPasswordInput.requestFocus()
                return@setOnClickListener
            }

            // Call API
            val request = ResetPasswordRequest(
                email = email,
                otp = otp,
                new_password = password,
                confirm_password = confirmPassword
            )

            ApiClient.getApi(this).resetPassword(request).enqueue(object : Callback<ResetPasswordResponse> {
                override fun onResponse(call: Call<ResetPasswordResponse>, response: Response<ResetPasswordResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.status == true) {
                            Toast.makeText(this@CreateNewPasswordActivity, body.message ?: "Password reset successfully!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@CreateNewPasswordActivity, SignInActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@CreateNewPasswordActivity, body?.message ?: "Reset failed", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("RESET_PASSWORD", "Error: ${response.code()}")
                        val errorMessage = if (response.code() == 400) {
                            "Incorrect OTP"
                        } else {
                            "Error: ${response.code()}"
                        }
                        Toast.makeText(this@CreateNewPasswordActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResetPasswordResponse>, t: Throwable) {
                    Log.e("RESET_PASSWORD", "Failure: ${t.message}")
                    Toast.makeText(this@CreateNewPasswordActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
