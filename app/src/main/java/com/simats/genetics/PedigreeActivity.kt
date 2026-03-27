package com.simats.genetics

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.TokenManager
import com.simats.genetics.network.responses.PedigreeChartResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PedigreeActivity : AppCompatActivity() {

    private lateinit var tvZoomPercent: TextView
    private lateinit var chartContainer: ConstraintLayout
    private lateinit var pedigreeView: PedigreeChartView

    private var currentZoom = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pedigree)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Pedigree Chart"

        // Bind UI
        tvZoomPercent = findViewById(R.id.tv_zoom_percent)
        chartContainer = findViewById(R.id.chart_container)
        pedigreeView = findViewById(R.id.pedigree_chart_view)

        // Bottom nav
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_pedigree

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, PatientHomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    })
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_pedigree -> true
                R.id.navigation_analysis -> {
                    startActivity(Intent(this, AnalysisActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    })
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    })
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }

        // Zoom buttons
        findViewById<MaterialCardView>(R.id.btn_zoom_in).setOnClickListener {
            if (currentZoom < 200) {
                currentZoom += 10
                updateZoom()
            }
        }

        findViewById<MaterialCardView>(R.id.btn_zoom_out).setOnClickListener {
            if (currentZoom > 50) {
                currentZoom -= 10
                updateZoom()
            }
        }

        updateZoom()
    }

    /**
     * ✅ THIS is the key for auto-refresh.
     * After adding family member and coming back, onResume runs again.
     */
    override fun onResume() {
        super.onResume()
        fetchPedigreeData()
    }

    private fun fetchPedigreeData() {
        // token check (avoid 401 confusion)
        val token = TokenManager.getToken(this)
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            goToLogin()
            return
        }

        ApiClient.getApi(this).getPedigreeChart().enqueue(object : Callback<PedigreeChartResponse> {

            override fun onResponse(call: Call<PedigreeChartResponse>, response: Response<PedigreeChartResponse>) {

                if (response.code() == 401) {
                    Toast.makeText(this@PedigreeActivity, "Session expired. Login again.", Toast.LENGTH_SHORT).show()
                    goToLogin()
                    return
                }

                if (!response.isSuccessful) {
                    val err = response.errorBody()?.string()
                    Log.e("PEDIGREE", "HTTP ${response.code()} err=$err")
                    Toast.makeText(this@PedigreeActivity, "HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                    return
                }

                val body = response.body()
                if (body?.status == true && body.pedigree != null) {
                    pedigreeView.setData(body.pedigree)
                } else {
                    Toast.makeText(this@PedigreeActivity, body?.message ?: "No pedigree data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PedigreeChartResponse>, t: Throwable) {
                Log.e("PEDIGREE", "FAIL ${t.message}", t)
                Toast.makeText(this@PedigreeActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateZoom() {
        tvZoomPercent.text = "${currentZoom}%"
        val scale = currentZoom / 100f
        chartContainer.scaleX = scale
        chartContainer.scaleY = scale
        chartContainer.pivotX = 0f
        chartContainer.pivotY = 0f
    }

    private fun goToLogin() {
        TokenManager.clearToken(this)
        val i = Intent(this, SignInActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(i)
        finish()
    }
}