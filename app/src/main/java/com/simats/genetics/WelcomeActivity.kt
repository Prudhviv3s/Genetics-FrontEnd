package com.simats.genetics

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val getStartedButton = findViewById<MaterialButton>(R.id.get_started_button)
        getStartedButton.setOnClickListener {
            val intent = Intent(this, RoleSelectionActivity::class.java)
            startActivity(intent)
        }

        val signInButton = findViewById<MaterialButton>(R.id.sign_in_button)
        signInButton.setOnClickListener {
            val intent = Intent(this, SignInRoleActivity::class.java)
            startActivity(intent)
        }
    }
}