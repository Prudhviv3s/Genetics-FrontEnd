package com.simats.genetics

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.Button
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.responses.AnalysisResultResponse
import com.simats.genetics.network.responses.PatternItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.roundToInt

class AnalysisActivity : AppCompatActivity() {

    private var analysisId: Int = 0
    private var loader: View? = null

    // Primary
    private lateinit var tvPrimaryTitle: TextView
    private lateinit var tvPrimaryPercent: TextView
    private lateinit var pbPrimary: ProgressBar
    private lateinit var tvPrimaryLabel: TextView

    // Alt 1
    private var alt1Container: View? = null
    private var tvAlt1Title: TextView? = null
    private var tvAlt1Percent: TextView? = null
    private var pbAlt1: ProgressBar? = null

    // Alt 2
    private var alt2Container: View? = null
    private var tvAlt2Title: TextView? = null
    private var tvAlt2Percent: TextView? = null
    private var pbAlt2: ProgressBar? = null

    private lateinit var btnShareReport: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // ✅ Fetch latest analysis if ID is 0
        analysisId = intent.getIntExtra("ANALYSIS_ID", 0)
        
        loader = findViewById(R.id.loader)

        // ----- Bind required views (ensure these ids exist in your XML) -----
        tvPrimaryTitle = findViewById(R.id.tv_primary_pattern_title)
        tvPrimaryPercent = findViewById(R.id.tv_primary_percent)
        pbPrimary = findViewById(R.id.pb_primary)
        tvPrimaryLabel = findViewById(R.id.tv_confidence_label) // you can show "High Confidence"

        // Alternative rows (optional ids)
        alt1Container = findViewById(R.id.alt_row_1)
        tvAlt1Title = findViewById(R.id.tv_alt1_title)
        tvAlt1Percent = findViewById(R.id.tv_alt1_percent)
        pbAlt1 = findViewById(R.id.pb_alt1)

        alt2Container = findViewById(R.id.alt_row_2)
        tvAlt2Title = findViewById(R.id.tv_alt2_title)
        tvAlt2Percent = findViewById(R.id.tv_alt2_percent)
        pbAlt2 = findViewById(R.id.pb_alt2)

        btnShareReport = findViewById(R.id.btn_share_report)
        btnShareReport.setOnClickListener {
            val i = Intent(this, ExportReportActivity::class.java)
            i.putExtra("ANALYSIS_ID", analysisId)
            startActivity(i)
        }

        setupBottomNav()
        if (analysisId == 0) {
            fetchLatestAnalysisAndLoad()
        } else {
            loadPatientAnalysisResult()
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

    private fun loadPatientAnalysisResult() {
        showLoading(true)

        ApiClient.getApi(this).getAnalysisResult(analysisId).enqueue(object : Callback<AnalysisResultResponse> {
            override fun onResponse(call: Call<AnalysisResultResponse>, response: Response<AnalysisResultResponse>) {
                showLoading(false)

                if (!response.isSuccessful) {
                    Toast.makeText(this@AnalysisActivity, "Failed (${response.code()})", Toast.LENGTH_LONG).show()
                    return
                }

                val body = response.body()
                if (body?.status != true || body.result == null) {
                    Toast.makeText(this@AnalysisActivity, body?.error ?: "No analysis result", Toast.LENGTH_LONG).show()
                    return
                }

                // Combine primary + alternatives, sort desc, take top 3
                val all = mutableListOf<PatternItem>()
                all.add(body.result.primaryPattern)
                all.addAll(body.result.alternatives)

                val top3 = all
                    .distinctBy { it.title.lowercase() }
                    .sortedByDescending { it.confidence }
                    .take(3)

                bindTop3(top3)
            }

            override fun onFailure(call: Call<AnalysisResultResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@AnalysisActivity, t.message ?: "Network error", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun fetchLatestAnalysisAndLoad() {
        showLoading(true)
        ApiClient.getApi(this).getLatestAnalysis().enqueue(object : Callback<com.simats.genetics.network.responses.LatestAnalysisResponse> {
            override fun onResponse(
                call: Call<com.simats.genetics.network.responses.LatestAnalysisResponse>,
                response: Response<com.simats.genetics.network.responses.LatestAnalysisResponse>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status == true && body.analysis?.id != null) {
                        analysisId = body.analysis.id
                        loadPatientAnalysisResult()
                    } else {
                        showLoading(false)
                        val errorTxt = response.errorBody()?.string() ?: "Unknown error"
                        Toast.makeText(this@AnalysisActivity, "Failed ${response.code()}: $errorTxt", Toast.LENGTH_LONG).show()
                    }
                } else {
                    showLoading(false)
                    val errorTxt = response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(this@AnalysisActivity, "Failed to load latest analysis: ${response.code()} $errorTxt", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<com.simats.genetics.network.responses.LatestAnalysisResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@AnalysisActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun bindTop3(top3: List<PatternItem>) {
        if (top3.isEmpty()) {
            Toast.makeText(this, "No patterns found", Toast.LENGTH_SHORT).show()
            return
        }

        // Primary = top1
        val p0 = top3[0]
        val pct0 = (p0.confidence * 100.0).roundToInt().coerceIn(0, 100)
        tvPrimaryTitle.text = p0.title
        tvPrimaryPercent.text = "$pct0%"
        pbPrimary.max = 100
        pbPrimary.progress = pct0
        tvPrimaryLabel.text = p0.confidenceLabel ?: confidenceLabel(pct0)

        // Alt = top2
        if (top3.size >= 2) {
            val p1 = top3[1]
            val pct1 = (p1.confidence * 100.0).roundToInt().coerceIn(0, 100)
            alt1Container?.visibility = View.VISIBLE
            tvAlt1Title?.text = p1.title
            tvAlt1Percent?.text = "$pct1%"
            pbAlt1?.max = 100
            pbAlt1?.progress = pct1
        } else {
            alt1Container?.visibility = View.GONE
        }

        // Alt = top3
        if (top3.size >= 3) {
            val p2 = top3[2]
            val pct2 = (p2.confidence * 100.0).roundToInt().coerceIn(0, 100)
            alt2Container?.visibility = View.VISIBLE
            tvAlt2Title?.text = p2.title
            tvAlt2Percent?.text = "$pct2%"
            pbAlt2?.max = 100
            pbAlt2?.progress = pct2
        } else {
            alt2Container?.visibility = View.GONE
        }
    }

    private fun confidenceLabel(pct: Int): String {
        return when {
            pct >= 85 -> "High Confidence"
            pct >= 60 -> "Medium Confidence"
            else -> "Low Confidence"
        }
    }

    private fun showLoading(isLoading: Boolean) {
        loader?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}