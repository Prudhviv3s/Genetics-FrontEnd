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
            if (password.length < 8) {
                passwordInput.error = "Password must be at least 8 characters"
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
                        Toast.makeText(this@CreateNewPasswordActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
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
