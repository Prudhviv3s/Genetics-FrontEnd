package com.simats.genetics

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.responses.DoctorPatientReportDetailResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DoctorInheritancePatternActivity : AppCompatActivity() {

    private var fallbackAnalysisId = 0
    private var fallbackTopPattern = "Unknown"
    private var fallbackTopConfidence = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_inheritance_pattern)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val name = intent.getStringExtra("PATIENT_NAME") ?: "-"
        val patientId = intent.getIntExtra("PATIENT_ID", -1)
        val displayId = intent.getStringExtra("PATIENT_DISPLAY_ID") ?: "-"
        val age = intent.getIntExtra("PATIENT_AGE", 0)
        val gender = intent.getStringExtra("PATIENT_GENDER") ?: "-"
        val email = intent.getStringExtra("PATIENT_EMAIL") ?: ""

        val analysisId = intent.getIntExtra("ANALYSIS_ID", 0)
        val topPattern = intent.getStringExtra("TOP_PATTERN") ?: "Unknown"
        val topConfidence = intent.getIntExtra("TOP_CONFIDENCE", 0)
        val probsString = intent.getStringExtra("PATTERN_PROBS") ?: ""

        findViewById<TextView>(R.id.tv_patient_name).text = name
        findViewById<TextView>(R.id.tv_patient_id_age).text =
            if (age > 0) "ID: $displayId · $age years" else "ID: $displayId"

        val cardAD = findViewById<MaterialCardView>(R.id.btn_autosomal_dominant)
        val cardAR = findViewById<MaterialCardView>(R.id.btn_autosomal_recessive)
        val cardXL = findViewById<MaterialCardView>(R.id.btn_x_linked)
        val cardYL = findViewById<MaterialCardView>(R.id.btn_y_linked)
        val cardMT = findViewById<MaterialCardView>(R.id.btn_mitochondrial)

        val pbAD = findViewById<ProgressBar>(R.id.pb_autosomal_dominant)
        val pbAR = findViewById<ProgressBar>(R.id.pb_autosomal_recessive)
        val pbXL = findViewById<ProgressBar>(R.id.pb_x_linked)
        val pbYL = findViewById<ProgressBar>(R.id.pb_y_linked)
        val pbMT = findViewById<ProgressBar>(R.id.pb_mitochondrial)

        val tvAD = findViewById<TextView>(R.id.tv_percent_autosomal_dominant)
        val tvAR = findViewById<TextView>(R.id.tv_percent_autosomal_recessive)
        val tvXL = findViewById<TextView>(R.id.tv_percent_x_linked)
        val tvYL = findViewById<TextView>(R.id.tv_percent_y_linked)
        val tvMT = findViewById<TextView>(R.id.tv_percent_mitochondrial)

        val btnViewSummaryReport = findViewById<MaterialButton>(R.id.btn_view_summary_report)

        val probs = parseProbabilities(probsString)

        val ad = probs["Autosomal Dominant"]
            ?: if (topPattern == "Autosomal Dominant") topConfidence else 0

        val ar = probs["Autosomal Recessive"]
            ?: if (topPattern == "Autosomal Recessive") topConfidence else 0

        val xl = probs["X-Linked"]
            ?: probs["X-Linked Dominant"]
            ?: probs["X-Linked Recessive"]
            ?: if (
                topPattern == "X-Linked" ||
                topPattern == "X-Linked Dominant" ||
                topPattern == "X-Linked Recessive"
            ) topConfidence else 0

        val yl = probs["Y-Linked"]
            ?: if (topPattern == "Y-Linked") topConfidence else 0

        val mt = probs["Mitochondrial"]
            ?: if (topPattern == "Mitochondrial") topConfidence else 0

        setBar(pbAD, tvAD, ad)
        setBar(pbAR, tvAR, ar)
        setBar(pbXL, tvXL, xl)
        setBar(pbYL, tvYL, yl)
        setBar(pbMT, tvMT, mt)

        btnViewSummaryReport.setOnClickListener {
            val intent = Intent(this, DoctorReportDetailActivity::class.java)
            intent.putExtra("PATIENT_NAME", name)
            intent.putExtra("PATIENT_ID", patientId)
            intent.putExtra("PATIENT_DISPLAY_ID", displayId)
            intent.putExtra("PATIENT_EMAIL", email)
            intent.putExtra("PATIENT_AGE", age)
            intent.putExtra("PATIENT_GENDER", gender)
            intent.putExtra("ANALYSIS_ID", if (analysisId > 0) analysisId else fallbackAnalysisId)
            intent.putExtra(
                "TOP_PATTERN",
                if (fallbackTopPattern != "Unknown") fallbackTopPattern else topPattern
            )
            intent.putExtra(
                "TOP_CONFIDENCE",
                if (fallbackTopConfidence > 0) fallbackTopConfidence else topConfidence
            )
            startActivity(intent)
        }

        cardAD.setOnClickListener { openPatternDetail("autosomal-dominant") }

        cardAR.setOnClickListener {
            startActivity(Intent(this, DoctorAutosomalRecessiveDetailActivity::class.java))
        }

        cardXL.setOnClickListener {
            startActivity(Intent(this, DoctorXLinkedDetailActivity::class.java))
        }

        cardYL.setOnClickListener {
            startActivity(Intent(this, DoctorYLinkedDetailActivity::class.java))
        }

        cardMT.setOnClickListener {
            startActivity(Intent(this, DoctorMitochondrialDetailActivity::class.java))
        }

        if (probsString.isBlank()) {
            if (patientId > 0) {
                fetchPatientAnalysis(
                    patientId,
                    pbAD, tvAD,
                    pbAR, tvAR,
                    pbXL, tvXL,
                    pbYL, tvYL,
                    pbMT, tvMT
                )
            } else {
                Toast.makeText(this, "Missing patient data.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun fetchPatientAnalysis(
        patientId: Int,
        pbAD: ProgressBar, tvAD: TextView,
        pbAR: ProgressBar, tvAR: TextView,
        pbXL: ProgressBar, tvXL: TextView,
        pbYL: ProgressBar, tvYL: TextView,
        pbMT: ProgressBar, tvMT: TextView
    ) {
        val api = ApiClient.getApi(this)

        api.getDoctorPatientReportDetail(patientId)
            .enqueue(object : Callback<DoctorPatientReportDetailResponse> {
                override fun onResponse(
                    call: Call<DoctorPatientReportDetailResponse>,
                    response: Response<DoctorPatientReportDetailResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.status == true && body.inheritance != null) {
                        val inheritance = body.inheritance

                        fallbackTopPattern = inheritance.pattern ?: "Unknown"
                        fallbackTopConfidence = inheritance.confidence ?: 0

                        val probs = inheritance.patternProbabilities ?: emptyMap()

                        val ad = probs["Autosomal Dominant"] ?: 0
                        val ar = probs["Autosomal Recessive"] ?: 0
                        val xl = probs["X-Linked"]
                            ?: probs["X-Linked Dominant"]
                            ?: probs["X-Linked Recessive"]
                            ?: 0
                        val yl = probs["Y-Linked"] ?: 0
                        val mt = probs["Mitochondrial"] ?: 0

                        setBar(pbAD, tvAD, ad)
                        setBar(pbAR, tvAR, ar)
                        setBar(pbXL, tvXL, xl)
                        setBar(pbYL, tvYL, yl)
                        setBar(pbMT, tvMT, mt)
                    } else {
                        showNoAnalysisState(
                            pbAD, tvAD,
                            pbAR, tvAR,
                            pbXL, tvXL,
                            pbYL, tvYL,
                            pbMT, tvMT
                        )
                    }
                }

                override fun onFailure(call: Call<DoctorPatientReportDetailResponse>, t: Throwable) {
                    Log.e("INHERITANCE_PAGE", "fetch failed: ${t.message}", t)
                    showNoAnalysisState(
                        pbAD, tvAD,
                        pbAR, tvAR,
                        pbXL, tvXL,
                        pbYL, tvYL,
                        pbMT, tvMT
                    )
                }
            })
    }

    private fun showNoAnalysisState(
        pbAD: ProgressBar, tvAD: TextView,
        pbAR: ProgressBar, tvAR: TextView,
        pbXL: ProgressBar, tvXL: TextView,
        pbYL: ProgressBar, tvYL: TextView,
        pbMT: ProgressBar, tvMT: TextView
    ) {
        fallbackAnalysisId = 0
        fallbackTopPattern = "Unknown"
        fallbackTopConfidence = 0

        setBar(pbAD, tvAD, 0)
        setBar(pbAR, tvAR, 0)
        setBar(pbXL, tvXL, 0)
        setBar(pbYL, tvYL, 0)
        setBar(pbMT, tvMT, 0)

        Toast.makeText(
            this,
            "No inheritance analysis available for this patient yet",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun openPatternDetail(patternKey: String) {
        val intent = Intent(this, DoctorPatternDetailActivity::class.java)
        intent.putExtra("PATTERN_KEY", patternKey)
        startActivity(intent)
    }

    private fun setBar(pb: ProgressBar, tv: TextView, value: Int) {
        val safe = value.coerceIn(0, 100)
        pb.max = 100
        pb.progress = safe
        tv.text = getString(R.string.percentage_format, safe)
    }

    private fun parseProbabilities(raw: String): Map<String, Int> {
        if (raw.isBlank()) return emptyMap()

        return try {
            raw.split(";")
                .mapNotNull { part ->
                    val p = part.trim()
                    if (!p.contains(":")) return@mapNotNull null
                    val pieces = p.split(":", limit = 2)
                    if (pieces.size < 2) return@mapNotNull null

                    val key = pieces[0].trim()
                    val value = pieces[1].trim().toIntOrNull() ?: 0
                    key to value
                }
                .toMap()
        } catch (e: Exception) {
            Log.e("PATTERN_PARSE", "parse error: ${e.message}", e)
            emptyMap()
        }
    }
}