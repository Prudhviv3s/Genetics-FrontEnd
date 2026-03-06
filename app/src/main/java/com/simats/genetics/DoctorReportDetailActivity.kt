package com.simats.genetics

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.responses.AnalysisReportResponse
import com.simats.genetics.network.responses.PedigreeChartResponse
import com.simats.genetics.network.responses.PedigreeNode
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.DecimalFormat

class DoctorReportDetailActivity : AppCompatActivity() {

    private var loader: View? = null
    private var pdfUrl: String? = null
    private var analysisId: Int = 0
    private var patientId: Int = 0

    private var intentPatientName: String? = null
    private var intentPatientDisplayId: String? = null
    private var intentPatientAge: Int = 0
    private var intentPatientGender: String? = null
    
    private var intentTopPattern: String? = null
    private var intentTopConfidence: Int? = null

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

        // Correct patient details from intent
        intentPatientName = intent.getStringExtra("PATIENT_NAME")
        intentPatientDisplayId = intent.getStringExtra("PATIENT_DISPLAY_ID")
        intentPatientAge = intent.getIntExtra("PATIENT_AGE", 0)
        intentPatientGender = intent.getStringExtra("PATIENT_GENDER")

        // Get highest percentage pattern from intent if passed
        intentTopPattern = intent.getStringExtra("TOP_PATTERN")
        if (intent.hasExtra("TOP_CONFIDENCE")) {
            intentTopConfidence = intent.getIntExtra("TOP_CONFIDENCE", 0)
        }

        if (analysisId == 0) {
            if (patientId > 0) {
                fetchAnalysisIdFromRecent(patientId)
            } else {
                Toast.makeText(this, "ANALYSIS_ID missing", Toast.LENGTH_LONG).show()
                finish()
            }
            return
        }

        setupButtonsAndLoad()
    }

    private fun setupButtonsAndLoad() {
        findViewById<LinearLayout>(R.id.btn_download).setOnClickListener {
            val intent = Intent(this, DoctorReportExportActivity::class.java)
            intent.putExtra("ANALYSIS_ID", analysisId)
            intent.putExtra("PDF_URL", pdfUrl)
            startActivity(intent)
        }
        
        findViewById<LinearLayout>(R.id.btn_share).setOnClickListener {
            val intent = Intent(this, DoctorReportShareActivity::class.java)
            intent.putExtra("ANALYSIS_ID", analysisId)
            intent.putExtra("PDF_URL", pdfUrl)
            startActivity(intent)
        }
        
        findViewById<LinearLayout>(R.id.btn_print).setOnClickListener { printPdf() }

        loadReport(analysisId)
        
        // Fetch correct pedigree details from family data
        if (patientId != 0) {
            fetchCorrectPedigreeDetails(patientId)
        }
    }

    private fun fetchAnalysisIdFromRecent(pId: Int) {
        showLoading(true)
        ApiClient.getApi(this).getRecentAnalyses().enqueue(object : retrofit2.Callback<com.simats.genetics.network.responses.RecentAnalysesResponse> {
            override fun onResponse(call: Call<com.simats.genetics.network.responses.RecentAnalysesResponse>, response: Response<com.simats.genetics.network.responses.RecentAnalysesResponse>) {
                val body = response.body()
                val item = body?.results?.firstOrNull { it.patientId == pId }
                if (item != null) {
                    analysisId = item.analysisId
                    setupButtonsAndLoad()
                } else {
                    showLoading(false)
                    Toast.makeText(this@DoctorReportDetailActivity, "No analysis found for this patient", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            override fun onFailure(call: Call<com.simats.genetics.network.responses.RecentAnalysesResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@DoctorReportDetailActivity, "Network error fetching analysis", Toast.LENGTH_LONG).show()
                finish()
            }
        })
    }

    private fun loadReport(analysisId: Int) {
        showLoading(true)

        ApiClient.getApi(this).getAnalysisReport(analysisId).enqueue(object : Callback<AnalysisReportResponse> {
            override fun onResponse(call: Call<AnalysisReportResponse>, response: Response<AnalysisReportResponse>) {
                showLoading(false)

                if (!response.isSuccessful) {
                    Toast.makeText(this@DoctorReportDetailActivity, "Failed (${response.code()})", Toast.LENGTH_LONG).show()
                    return
                }

                val body = response.body()
                if (body?.status != true || body.report == null) {
                    Toast.makeText(this@DoctorReportDetailActivity, body?.error ?: "No report data", Toast.LENGTH_LONG).show()
                    return
                }

                bindReport(body.report)
            }

            override fun onFailure(call: Call<AnalysisReportResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@DoctorReportDetailActivity, t.message ?: "Network error", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun bindReport(r: com.simats.genetics.network.responses.AnalysisReportData) {
        // Use intent values for patient details if available, else use report data
        findViewById<TextView>(R.id.tv_patient_name).text = intentPatientName ?: r.patient.name
        findViewById<TextView>(R.id.tv_patient_id).text = "Patient ID: ${intentPatientDisplayId ?: r.patient.display_id}"
        findViewById<TextView>(R.id.tv_patient_age).text = if (intentPatientAge > 0) "$intentPatientAge years" else (r.patient.age?.let { "$it years" } ?: "-")
        findViewById<TextView>(R.id.tv_patient_gender).text = intentPatientGender ?: (r.patient.gender ?: "-")

        findViewById<TextView?>(R.id.tv_analysis_date)?.text = (r.patient.analysis_date ?: "-")

        // Use highest percentage pattern if available from intent, otherwise use from report
        val displayPattern = intentTopPattern ?: r.pattern.title
        findViewById<TextView?>(R.id.tv_pattern_title)?.text = displayPattern

        val displayConf = if (intentTopConfidence != null) {
            intentTopConfidence!!.toDouble() / 100.0
        } else {
            r.pattern.confidence ?: 0.0
        }
        
        val confPct = displayConf * 100.0
        val df = DecimalFormat("##")
        val label = r.pattern.confidence_label ?: ""
        val confText = if (label.isNotBlank() && intentTopPattern == null) {
            "Confidence: ${df.format(confPct)}% ($label)"
        } else {
            "Confidence: ${df.format(confPct)}%"
        }
        findViewById<TextView?>(R.id.tv_confidence)?.text = confText
        
        findViewById<TextView?>(R.id.tv_pattern_desc)?.text = (r.pattern.description ?: "-")

        // These might be overwritten by fetchCorrectPedigreeDetails
        findViewById<TextView?>(R.id.tv_family_members)?.text = (r.pedigree.family_members?.toString() ?: "-")
        findViewById<TextView?>(R.id.tv_affected)?.text = (r.pedigree.affected?.toString() ?: "-")
        findViewById<TextView?>(R.id.tv_generations)?.text = (r.pedigree.generations?.toString() ?: "-")

        findViewById<TextView?>(R.id.tv_report_generated)?.text =
            r.generated_at?.let { "Report Generated: $it" } ?: ""

        pdfUrl = r.report_pdf_url

        // The downstream activities/methods handle the null URL case with user feedback,
        // so we keep the buttons enabled to allow navigation and provide that feedback.
    }

    private fun fetchCorrectPedigreeDetails(patientId: Int) {
        ApiClient.getApi(this).getDoctorPatientPedigreeChart(patientId).enqueue(object : Callback<PedigreeChartResponse> {
            override fun onResponse(call: Call<PedigreeChartResponse>, response: Response<PedigreeChartResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status == true && body.pedigree != null) {
                        calculateAndSetPedigreeSummary(body.pedigree.nodes)
                    }
                }
            }
            override fun onFailure(call: Call<PedigreeChartResponse>, t: Throwable) {
                Log.e("PEDIGREE_FIX", "Error: ${t.message}")
            }
        })
    }

    private fun calculateAndSetPedigreeSummary(nodes: List<PedigreeNode>) {
        if (nodes.isEmpty()) return
        
        val familyMembersCount = nodes.size
        val affectedCount = nodes.count { (it.healthStatus ?: "").lowercase() == "affected" }
        
        // Logic similar to PedigreeChartView to determine generations
        val gen1 = mutableListOf<PedigreeNode>()
        val gen2 = mutableListOf<PedigreeNode>()
        val others = mutableListOf<PedigreeNode>()

        nodes.forEach { n ->
            val rel = (n.relationship ?: "").lowercase()
            when {
                rel.contains("father") || rel.contains("mother") || rel.contains("grandfather") || rel.contains("grandmother") -> gen1.add(n)
                n.isProband || rel.contains("brother") || rel.contains("sister") || rel.contains("son") || rel.contains("daughter") -> gen2.add(n)
                else -> others.add(n)
            }
        }
        
        var generationsCount = 0
        if (gen1.isNotEmpty()) generationsCount++
        if (gen2.isNotEmpty()) generationsCount++
        if (others.isNotEmpty()) generationsCount++
        
        if (generationsCount == 0 && nodes.isNotEmpty()) generationsCount = 1

        findViewById<TextView?>(R.id.tv_family_members)?.text = familyMembersCount.toString()
        findViewById<TextView?>(R.id.tv_affected)?.text = affectedCount.toString()
        findViewById<TextView?>(R.id.tv_generations)?.text = generationsCount.toString()
    }

    private fun printPdf() {
        val url = pdfUrl
        if (url.isNullOrBlank()) {
            Toast.makeText(this, "PDF not available", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Preparing print...", Toast.LENGTH_SHORT).show()

        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, urlFinished: String) {
                val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
                val jobName = "Inheritance_Report_$analysisId"

                val printAdapter = view.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadUrl(url)
    }

    private fun showLoading(isLoading: Boolean) {
        loader?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
