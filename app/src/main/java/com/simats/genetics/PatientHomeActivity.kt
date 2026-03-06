package com.simats.genetics

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.TokenManager
import com.simats.genetics.network.responses.MyProfileResponse
import com.simats.genetics.network.responses.PatientHomeResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PatientHomeActivity : AppCompatActivity() {

    private lateinit var tvWelcomeBack: TextView
    private lateinit var tvFamilyMembersCount: TextView
    private lateinit var tvGenerationsCount: TextView
    private lateinit var tvActiveTraitsCount: TextView
    private lateinit var tvPendingResultsCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_home)

        // Bind UI (match layout IDs exactly)
        tvWelcomeBack = findViewById(R.id.tvWelcomeBack)
        tvFamilyMembersCount = findViewById(R.id.tvFamilyMembersCount)
        tvGenerationsCount = findViewById(R.id.tvGenerationsCount)
        tvActiveTraitsCount = findViewById(R.id.tvActiveTraitsCount)
        tvPendingResultsCount = findViewById(R.id.tvPendingResultsCount)

        // Show saved name quickly
        TokenManager.getUserName(this)?.let {
            tvWelcomeBack.text = "Welcome Back,$it"
        }

        // Check token
        val token = TokenManager.getToken(this)
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            goToLogin()
            return
        }

        // Notification icon
        findViewById<ImageView>(R.id.notification_icon).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        // Quick actions
        findViewById<android.view.View>(R.id.btn_add_family).setOnClickListener {
            startActivity(Intent(this, AddFamilyMemberActivity::class.java))
        }




        findViewById<android.view.View>(R.id.btn_family_overview).setOnClickListener {
            startActivity(Intent(this, FamilyOverviewActivity::class.java))
        }

        // Bottom navigation
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_home

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_pedigree -> {
                    val intent = Intent(this, PedigreeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }


                R.id.navigation_analysis -> {
                    val intent = Intent(this, AnalysisActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.navigation_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }

                else -> false
            }
        }

        // Load from backend
        fetchMyProfileName()
        fetchPatientDashboard()
    }

    private fun fetchMyProfileName() {
        ApiClient.getApi(this).getMyProfile().enqueue(object : Callback<MyProfileResponse> {
            override fun onResponse(call: Call<MyProfileResponse>, response: Response<MyProfileResponse>) {

                if (response.code() == 401) {
                    Toast.makeText(this@PatientHomeActivity, "Session expired. Login again.", Toast.LENGTH_SHORT).show()
                    goToLogin()
                    return
                }

                if (!response.isSuccessful) {
                    Log.e("PAT_PROFILE", "HTTP ${response.code()} err=${response.errorBody()?.string()}")
                    return
                }

                val body = response.body()
                if (body?.status == true) {
                    val name = body.profile?.fullName ?: "Patient"
                    tvWelcomeBack.text = "Welcome Back,$name"
                    TokenManager.saveUserName(this@PatientHomeActivity, name)
                }
            }

            override fun onFailure(call: Call<MyProfileResponse>, t: Throwable) {
                Log.e("PAT_PROFILE", "FAIL ${t.message}", t)
            }
        })
    }

    private fun fetchPatientDashboard() {
        ApiClient.getApi(this).getPatientHome().enqueue(object : Callback<PatientHomeResponse> {
            override fun onResponse(call: Call<PatientHomeResponse>, response: Response<PatientHomeResponse>) {

                if (response.code() == 401) {
                    Toast.makeText(this@PatientHomeActivity, "Session expired. Login again.", Toast.LENGTH_SHORT).show()
                    goToLogin()
                    return
                }

                if (!response.isSuccessful) {
                    Log.e("PAT_HOME", "HTTP ${response.code()} err=${response.errorBody()?.string()}")
                    Toast.makeText(this@PatientHomeActivity, "HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                    return
                }

                val body = response.body()
                if (body?.status != true) {
                    Toast.makeText(this@PatientHomeActivity, "Failed to load dashboard", Toast.LENGTH_SHORT).show()
                    return
                }

                val d = body.dashboard
                tvFamilyMembersCount.text = (d?.familyMembers ?: 0).toString()
                tvGenerationsCount.text = (d?.generations ?: 0).toString()
                tvActiveTraitsCount.text = (d?.activeTraits ?: 0).toString()
                tvPendingResultsCount.text = (d?.pendingResults ?: 0).toString()
            }

            override fun onFailure(call: Call<PatientHomeResponse>, t: Throwable) {
                Log.e("PAT_HOME", "FAIL ${t.message}", t)
                Toast.makeText(this@PatientHomeActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun goToLogin() {
        TokenManager.clearToken(this)
        val i = Intent(this, SignInActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(i)
        finish()
    }
}