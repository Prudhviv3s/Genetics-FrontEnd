package com.simats.genetics

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.TokenManager
import com.simats.genetics.network.responses.DoctorPatientReportDetailResponse
import com.simats.genetics.network.responses.PedigreeChartResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DoctorReportDetailActivity : AppCompatActivity() {


    private var loader: View? = null
    private var analysisId: Int = 0
    private var patientId: Int = 0
    private var patientDisplayId: String? = null
    
    // Hold extras from intent as priorities
    private var topPatternFromIntent: String? = null
    private var topConfidenceFromIntent: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_report_detail)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Inheritance Pattern Report"
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        loader = findViewById(R.id.loader)

        analysisId = intent.getIntExtra("ANALYSIS_ID", 0)
        patientId = intent.getIntExtra("PATIENT_ID", 0)
        patientDisplayId = intent.getStringExtra("PATIENT_DISPLAY_ID")
        
        topPatternFromIntent = intent.getStringExtra("TOP_PATTERN")
        topConfidenceFromIntent = intent.getIntExtra("TOP_CONFIDENCE", 0)

        setupButtons()

        if (patientId > 0) {
            loadReport(patientId)
        } else {
            Toast.makeText(this, "No patient ID provided", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_doctor_report_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == R.id.action_logout) {
            performLogout()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupButtons() {
        findViewById<View>(R.id.btn_download)?.setOnClickListener {
            if (patientId > 0) {
                downloadPdf()
            } else {
                Toast.makeText(this, "Analysis report not available", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<View>(R.id.btn_logout_bottom)?.setOnClickListener {
            performLogout()
        }
    }

    private fun performLogout() {
        TokenManager.clearToken(this)
        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun loadReport(patientId: Int) {
        showLoading(true)

        // 1. Fetch Report Details
        ApiClient.getApi(this).getDoctorPatientReportDetail(patientId)
            .enqueue(object : Callback<DoctorPatientReportDetailResponse> {
                override fun onResponse(
                    call: Call<DoctorPatientReportDetailResponse>,
                    response: Response<DoctorPatientReportDetailResponse>
                ) {
                    if (response.isSuccessful && response.body()?.status == true) {
                        bindReport(response.body()!!)
                    } else {
                        Log.e("REPORT_DETAIL", "Report fetch failed")
                    }
                    
                    // 2. Always try to fetch pedigree stats live for accuracy
                    fetchLivePedigreeStats(patientId)
                }

                override fun onFailure(call: Call<DoctorPatientReportDetailResponse>, t: Throwable) {
                    showLoading(false)
                    Log.e("REPORT_DETAIL", "FAIL ${t.message}", t)
                    Toast.makeText(this@DoctorReportDetailActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun fetchLivePedigreeStats(patientId: Int) {
        ApiClient.getApi(this).getDoctorPatientPedigreeChart(patientId)
            .enqueue(object : Callback<PedigreeChartResponse> {
                override fun onResponse(
                    call: Call<PedigreeChartResponse>,
                    response: Response<PedigreeChartResponse>
                ) {
                    showLoading(false)
                    val body = response.body()
                    if (response.isSuccessful && body?.status == true) {
                        val nodes = body.pedigree?.nodes ?: return
                        
                        val totalMembers = nodes.size
                        val affectedCount = nodes.count { it.healthStatus?.lowercase() == "affected" }
                        
                        // Calculate generations depth logic improved
                        val generationsSet = mutableSetOf<Int>()
                        nodes.forEach { node ->
                            val rel = node.relationship?.lowercase() ?: ""
                            when {
                                rel.contains("grand") -> generationsSet.add(1)
                                rel.contains("father") || rel.contains("mother") || rel.contains("uncle") || rel.contains("aunt") -> generationsSet.add(2)
                                rel.contains("patient") || rel.contains("proband") || rel.contains("brother") || rel.contains("sister") || rel.contains("cousin") -> generationsSet.add(3)
                                rel.contains("son") || rel.contains("daughter") -> generationsSet.add(4)
                                else -> generationsSet.add(3)
                            }
                        }
                        
                        // If we have "grand", we have at least 3 generations (Grand -> Parent -> Child)
                        // If we have "father", we have at least 2 generations
                        var genCount = 1
                        if (generationsSet.contains(1)) genCount = 3
                        else if (generationsSet.contains(2)) genCount = 2
                        
                        // Check if children exist to add one more
                        if (generationsSet.contains(4)) {
                            if (genCount < 3) genCount = 3 // if parents + kids but no grandparents
                            if (generationsSet.contains(1) || generationsSet.contains(2)) {
                                genCount = if (generationsSet.contains(1)) 4 else 3
                            } else {
                                genCount = 2 // only proband + kids
                            }
                        }
                        
                        // Actually, let's just use the max level found
                        val maxLevel = generationsSet.maxOrNull() ?: 3
                        val minLevel = generationsSet.minOrNull() ?: 3
                        genCount = (maxLevel - minLevel + 1).coerceAtLeast(1)

                        findViewById<TextView?>(R.id.tv_family_members)?.text = totalMembers.toString()
                        findViewById<TextView?>(R.id.tv_affected)?.text = affectedCount.toString()
                        findViewById<TextView?>(R.id.tv_generations)?.text = genCount.toString()
                    }
                }

                override fun onFailure(call: Call<PedigreeChartResponse>, t: Throwable) {
                    showLoading(false)
                }
            })
    }

    private fun bindReport(data: DoctorPatientReportDetailResponse) {
        val patient = data.patient ?: return

        findViewById<TextView>(R.id.tv_patient_name).text = patient.fullName ?: "-"
        findViewById<TextView>(R.id.tv_patient_id).text =
            "Patient ID: ${patient.patientId ?: patientDisplayId ?: "-"}"
        findViewById<TextView>(R.id.tv_patient_age).text =
            patient.age?.let { "$it years" } ?: "-"
        findViewById<TextView>(R.id.tv_patient_gender).text = patient.gender ?: "-"
        findViewById<TextView?>(R.id.tv_analysis_date)?.text = patient.analysisDate ?: "-"

        val inheritance = data.inheritance
        if (inheritance != null) {
            // FIND HIGHEST PROBABILITY
            val probs = inheritance.patternProbabilities ?: emptyMap()
            var topP = inheritance.pattern ?: "Unknown"
            var topC = inheritance.confidence ?: 0
            
            if (probs.isNotEmpty()) {
                val maxEntry = probs.maxByOrNull { it.value }
                if (maxEntry != null && maxEntry.value > topC) {
                    topP = maxEntry.key
                    topC = maxEntry.value
                }
            }
            
            // Prioritize Intent extras if they are stronger or to ensure consistency
            if (!topPatternFromIntent.isNullOrBlank()) {
                topP = topPatternFromIntent!!
                topC = if (topConfidenceFromIntent > 0) topConfidenceFromIntent else topC
            }

            findViewById<TextView?>(R.id.tv_pattern_title)?.text = topP
            findViewById<TextView?>(R.id.tv_confidence)?.text =
                "Confidence: $topC% (${getConfidenceLabel(topC)})"
            findViewById<TextView?>(R.id.tv_pattern_desc)?.text =
                inheritance.description ?: "-"
        }

        val report = data.report
        findViewById<TextView?>(R.id.tv_report_generated)?.text =
            report?.generatedDate?.let { "Report Generated: $it" } ?: ""
    }

    private fun getConfidenceLabel(conf: Int): String {
        return when {
            conf >= 85 -> "Very High"
            conf >= 70 -> "High"
            conf >= 40 -> "Medium"
            else -> "Low"
        }
    }

    private fun downloadPdf() {
        val url = if (analysisId > 0) {
            ApiClient.BASE_URL + "reports/download/?analysis_id=$analysisId"
        } else {
            ApiClient.BASE_URL + "reports/download/?patient_id=$patientId"
        }

        try {
            val fileName = "Genetic_Report_${patientDisplayId ?: patientId}.pdf"

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription("Downloading genetic analysis report...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

            val token = TokenManager.getToken(this)
            if (!token.isNullOrBlank()) {
                request.addRequestHeader("Authorization", "Token $token")
            }

            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)

            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("REPORT_DOWNLOAD", "Download failed: ${e.message}", e)
            Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        loader?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}