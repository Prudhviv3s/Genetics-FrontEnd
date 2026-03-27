package com.simats.genetics

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.TokenManager
import com.simats.genetics.network.responses.LatestAnalysisResponse
import com.simats.genetics.network.responses.LatestAnalysisDto
import com.simats.genetics.network.responses.MyProfileResponse
import com.simats.genetics.network.responses.ProfileDto
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AnalysisActivity : AppCompatActivity() {

    private var loader: View? = null
    private var pdfUrl: String? = null
    private var analysisId: Int = 0

    // Since this is opened from Patient side usually we fetch the info from API
    // but just in case we have passed intents
    private var intentPatientName: String? = null
    private var intentPatientDisplayId: String? = null
    private var intentPatientAge: Int = 0
    private var intentPatientGender: String? = null
    private var intentTopPattern: String? = null
    private var intentTopConfidence: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        // hide back button since it's a top level tab on Patient Home
        // supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Analysis Results"
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        loader = findViewById(R.id.loader)
        setupBottomNav()

        analysisId = intent.getIntExtra("ANALYSIS_ID", 0)

        intentPatientName = intent.getStringExtra("PATIENT_NAME")
        intentPatientDisplayId = intent.getStringExtra("PATIENT_DISPLAY_ID")
        intentPatientAge = intent.getIntExtra("PATIENT_AGE", 0)
        intentPatientGender = intent.getStringExtra("PATIENT_GENDER")

        intentTopPattern = intent.getStringExtra("TOP_PATTERN")
        if (intent.hasExtra("TOP_CONFIDENCE")) {
            intentTopConfidence = intent.getIntExtra("TOP_CONFIDENCE", 0)
        }

        if (analysisId != 0) {
            setupButtons()
        }
    }

    override fun onResume() {
        super.onResume()
        if (analysisId == 0) {
            fetchMyProfile()
            fetchLatestAnalysis()
        }
    }

    private fun setupButtons() {
        findViewById<LinearLayout>(R.id.btn_download).setOnClickListener {
            downloadPdf()
        }
    }

    private fun setupBottomNav() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_analysis

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> {
                    val intent = Intent(this, PatientHomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_pedigree -> {
                    val intent = Intent(this, PedigreeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_analysis -> true
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
    }

    private fun fetchMyProfile() {
        ApiClient.getApi(this).getMyProfile().enqueue(object : Callback<MyProfileResponse> {
            override fun onResponse(call: Call<MyProfileResponse>, response: Response<MyProfileResponse>) {
                val profile = response.body()?.profile
                if (profile != null) {
                    bindProfile(profile)
                }
            }
            override fun onFailure(call: Call<MyProfileResponse>, t: Throwable) {
                Log.e("ANALYSIS", "Profile fetch failed")
            }
        })
    }

    private fun bindProfile(p: ProfileDto) {
        findViewById<TextView>(R.id.tv_patient_name).text = p.fullName
        findViewById<TextView>(R.id.tv_patient_id).text = "Patient ID: ${p.patientId}"
        findViewById<TextView>(R.id.tv_patient_age).text = p.age?.let { "$it years" } ?: "-"
        findViewById<TextView>(R.id.tv_patient_gender).text = p.gender ?: "-"
    }

    private fun fetchLatestAnalysis() {
        showLoading(true)
        ApiClient.getApi(this).getLatestAnalysis().enqueue(object : Callback<LatestAnalysisResponse> {
            override fun onResponse(call: Call<LatestAnalysisResponse>, response: Response<LatestAnalysisResponse>) {
                showLoading(false)
                val body = response.body()
                if (body?.status == true && body.analysis != null) {
                    val analysis = body.analysis
                    analysisId = analysis.id ?: 0
                    bindReport(analysis)
                    setupButtons()
                } else {
                    Toast.makeText(this@AnalysisActivity, "No analysis found", Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<LatestAnalysisResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@AnalysisActivity, "Network error", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun bindReport(r: LatestAnalysisDto) {
        findViewById<TextView?>(R.id.tv_analysis_date)?.text = r.analysisDate ?: "-"

        findViewById<TextView?>(R.id.tv_pattern_title)?.text = r.inheritancePattern
        
        val conf = r.confidence ?: 0
        val label = if (conf >= 85) "Very High" else if (conf >= 70) "High" else if (conf >= 40) "Medium" else "Low"
        findViewById<TextView?>(R.id.tv_confidence)?.text = "Confidence: $conf% ($label)"

        findViewById<TextView?>(R.id.tv_pattern_desc)?.text = r.description ?: "-"

        findViewById<TextView?>(R.id.tv_family_members)?.text = r.familyMembers?.toString() ?: "-"
        findViewById<TextView?>(R.id.tv_affected)?.text = r.affected?.toString() ?: "-"
        findViewById<TextView?>(R.id.tv_generations)?.text = r.generations?.toString() ?: "-"

        findViewById<TextView?>(R.id.tv_report_generated)?.text =
            r.analysisDate?.let { "Report Generated: $it" } ?: ""

        pdfUrl = ApiClient.BASE_URL + "reports/download/?analysis_id=${r.id}"
    }


    private fun downloadPdf() {
        if (analysisId <= 0) {
            Toast.makeText(this, "Analysis report not available", Toast.LENGTH_SHORT).show()
            return
        }
        val url = ApiClient.BASE_URL + "reports/download/?analysis_id=$analysisId"
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Genetic_Report_$analysisId.pdf")
                .setDescription("Downloading report...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Genetic_Report_$analysisId.pdf")

            val token = TokenManager.getToken(this)
            if (token != null) {
                request.addRequestHeader("Authorization", "Token $token")
            }

            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)

            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "Download failed", Toast.LENGTH_LONG).show()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        loader?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
