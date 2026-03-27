package com.simats.genetics

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.responses.DoctorDashboardResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DoctorHomeActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvActivePatients: TextView
    private lateinit var tvPendingAnalysis: TextView
    private lateinit var tvTotalPedigrees: TextView
    private lateinit var tvReportsGenerated: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_home)

        tvWelcome = findViewById(R.id.tvWelcome)
        tvActivePatients = findViewById(R.id.tvActivePatients)
        tvPendingAnalysis = findViewById(R.id.tvPendingAnalysis)
        tvTotalPedigrees = findViewById(R.id.tvTotalPedigrees)
        tvReportsGenerated = findViewById(R.id.tvReportsGenerated)

        findViewById<ImageView>(R.id.notification_icon).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        findViewById<View>(R.id.btn_patient_list).setOnClickListener {
            startActivity(Intent(this, DoctorPatientListActivity::class.java))
        }

        findViewById<View>(R.id.btn_reports).setOnClickListener {
            startActivity(Intent(this, DoctorPatientReportsActivity::class.java))
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_home

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_pedigree -> {
                    val intent = Intent(this, DoctorPedigreeListActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_analysis -> {
                    val intent = Intent(this, DoctorAnalysisActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_settings -> {
                    val intent = Intent(this, DoctorSettingsActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }

    }

    override fun onResume() {
        super.onResume()
        fetchDoctorDashboard()
    }

    private fun fetchDoctorDashboard() {
        ApiClient.getApi(this).getDoctorDashboard().enqueue(object : Callback<DoctorDashboardResponse> {
            override fun onResponse(
                call: Call<DoctorDashboardResponse>,
                response: Response<DoctorDashboardResponse>
            ) {
                if (!response.isSuccessful) {
                    val err = response.errorBody()?.string()
                    Log.e("DOCTOR_DASH", "HTTP ${response.code()} err=$err")
                    Toast.makeText(this@DoctorHomeActivity, "HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                    return
                }

                val body = response.body()
                if (body?.status != true) {
                    Toast.makeText(this@DoctorHomeActivity, "Failed to load dashboard", Toast.LENGTH_SHORT).show()
                    return
                }

                val name = body.doctorName ?: "Doctor"
                tvWelcome.text = "Welcome, $name"

                tvActivePatients.text = (body.stats?.activePatients ?: 0).toString()
                tvPendingAnalysis.text = (body.stats?.pendingAnalysis ?: 0).toString()
                tvTotalPedigrees.text = (body.stats?.totalPedigrees ?: 0).toString()
                tvReportsGenerated.text = (body.stats?.reportsGenerated ?: 0).toString()
            }

            override fun onFailure(call: Call<DoctorDashboardResponse>, t: Throwable) {
                Log.e("DOCTOR_DASH", "FAIL ${t.message}", t)
                Toast.makeText(this@DoctorHomeActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}