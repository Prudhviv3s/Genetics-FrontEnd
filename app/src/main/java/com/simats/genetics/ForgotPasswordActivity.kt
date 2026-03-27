package com.simats.genetics

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.requests.ForgotPasswordRequest
import com.simats.genetics.network.responses.ForgotPasswordResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var emailInput: TextInputEditText
    private lateinit var resetPasswordButton: Button
    private lateinit var backToSignInText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        emailInput = findViewById(R.id.email_input)
        resetPasswordButton = findViewById(R.id.reset_password_button)
        backToSignInText = findViewById(R.id.back_to_sign_in_text)

        resetPasswordButton.setOnClickListener {
            callForgotPasswordApi()
        }

        backToSignInText.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun callForgotPasswordApi() {
        val email = emailInput.text?.toString()?.trim().orEmpty()

        if (email.isEmpty()) {
            emailInput.error = "Email is required"
            emailInput.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Enter a valid email"
            emailInput.requestFocus()
            return
        }
        if (!email.lowercase().endsWith(".com") && !email.lowercase().endsWith(".in")) {
            emailInput.error = "Email must end with .com or .in"
            emailInput.requestFocus()
            return
        }

        resetPasswordButton.isEnabled = false

        val request = ForgotPasswordRequest(email = email)
        ApiClient.getApi(this).forgotPassword(request).enqueue(object : Callback<ForgotPasswordResponse> {
            override fun onResponse(call: Call<ForgotPasswordResponse>, response: Response<ForgotPasswordResponse>) {
                resetPasswordButton.isEnabled = true
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status == true) {
                        Toast.makeText(this@ForgotPasswordActivity, body.message ?: "OTP sent!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@ForgotPasswordActivity, CreateNewPasswordActivity::class.java)
                        intent.putExtra("EMAIL", email)
                        intent.putExtra("OTP", body.otp ?: "") // Pass OTP if backend provides it for dev/testing
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@ForgotPasswordActivity, body?.message ?: "Failed to send OTP", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errText = response.errorBody()?.string()
                    Log.e("FORGOT_PASSWORD", "HTTP ${response.code()} error=$errText")
                    Toast.makeText(this@ForgotPasswordActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ForgotPasswordResponse>, t: Throwable) {
                resetPasswordButton.isEnabled = true
                Log.e("FORGOT_PASSWORD", "Failure: ${t.message}")
                Toast.makeText(this@ForgotPasswordActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}