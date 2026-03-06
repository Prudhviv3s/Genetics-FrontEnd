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

class DoctorInheritancePatternActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_inheritance_pattern)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // =========================
        // INTENT DATA (from DoctorDnaReportActivity)
        // =========================
        val name = intent.getStringExtra("PATIENT_NAME") ?: "-"
        val patientId = intent.getIntExtra("PATIENT_ID", -1)
        val displayId = intent.getStringExtra("PATIENT_DISPLAY_ID") ?: "-"
        val age = intent.getIntExtra("PATIENT_AGE", 0)
        val gender = intent.getStringExtra("PATIENT_GENDER") ?: "-"

        val analysisId = intent.getIntExtra("ANALYSIS_ID", 0)
        val topPattern = intent.getStringExtra("TOP_PATTERN") ?: "Unknown"
        val topConfidence = intent.getIntExtra("TOP_CONFIDENCE", 0)

        // pattern probs sent as: "Autosomal Dominant:78;Autosomal Recessive:42;X-Linked:15;Y-Linked:5;Mitochondrial:8"
        val probsString = intent.getStringExtra("PATTERN_PROBS") ?: ""

        // =========================
        // HEADER
        // =========================
        findViewById<TextView>(R.id.tv_patient_name).text = name
        findViewById<TextView>(R.id.tv_patient_id_age).text =
            if (age > 0) "ID: $displayId · $age years" else "ID: $displayId"

        // =========================
        // UI REFERENCES (must exist in your XML)
        // If any id is missing, it will crash. Keep the same ids as below.
        // =========================
        val cardAD = findViewById<MaterialCardView>(R.id.btn_autosomal_dominant)
        val cardAR = findViewById<MaterialCardView>(R.id.btn_autosomal_recessive)
        val cardXL = findViewById<MaterialCardView>(R.id.btn_x_linked)
        val cardYL = findViewById<MaterialCardView>(R.id.btn_y_linked)
        val cardMT = findViewById<MaterialCardView>(R.id.btn_mitochondrial)

        // Progress + percent TextView ids (set these in your XML if not present)
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

        val btnViewReport = findViewById<MaterialButton>(R.id.btn_view_full_report)

        // =========================
        // PARSE PROBABILITIES
        // =========================
        val probs = parseProbabilities(probsString)

        val ad = probs["Autosomal Dominant"] ?: (if (topPattern == "Autosomal Dominant") topConfidence else 0)
        val ar = probs["Autosomal Recessive"] ?: (if (topPattern == "Autosomal Recessive") topConfidence else 0)
        val xl = probs["X-Linked"] ?: (if (topPattern == "X-Linked") topConfidence else 0)
        val yl = probs["Y-Linked"] ?: (if (topPattern == "Y-Linked") topConfidence else 0)
        val mt = probs["Mitochondrial"] ?: (if (topPattern == "Mitochondrial") topConfidence else 0)

        // Find the highest probability pattern to ensure it's passed as TOP_PATTERN
        val allProbs = mapOf(
            "Autosomal Dominant" to ad,
            "Autosomal Recessive" to ar,
            "X-Linked" to xl,
            "Y-Linked" to yl,
            "Mitochondrial" to mt
        )
        val highestEntry = allProbs.maxByOrNull { it.value }
        val finalTopPattern = highestEntry?.key ?: topPattern
        val finalTopConfidence = highestEntry?.value ?: topConfidence

        // =========================
        // SET UI (progress + text)
        // =========================
        setBar(pbAD, tvAD, ad)
        setBar(pbAR, tvAR, ar)
        setBar(pbXL, tvXL, xl)
        setBar(pbYL, tvYL, yl)
        setBar(pbMT, tvMT, mt)

        // =========================
        // VIEW FULL REPORT
        // =========================
        // =========================
        // VIEW FULL REPORT
        // =========================
        btnViewReport.setOnClickListener {
            if (analysisId <= 0) {
                Toast.makeText(this, "Analysis ID not available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val i = Intent(this, DoctorReportDetailActivity::class.java)
            i.putExtra("PATIENT_NAME", name)
            i.putExtra("PATIENT_ID", patientId)
            i.putExtra("PATIENT_DISPLAY_ID", displayId)
            i.putExtra("PATIENT_AGE", age)
            i.putExtra("PATIENT_GENDER", gender)
            i.putExtra("ANALYSIS_ID", analysisId)
            i.putExtra("TOP_PATTERN", finalTopPattern)
            i.putExtra("TOP_CONFIDENCE", finalTopConfidence)
            startActivity(i)
        }

        // =========================
        // NAVIGATION TO DETAIL PAGES
        // =========================
        cardAD.setOnClickListener { openPatternDetail("autosomal-dominant") }
        
        cardAR.setOnClickListener {
            val intent = Intent(this, DoctorAutosomalRecessiveDetailActivity::class.java)
            startActivity(intent)
        }

        cardXL.setOnClickListener {
            val intent = Intent(this, DoctorXLinkedDetailActivity::class.java)
            startActivity(intent)
        }
        
        cardYL.setOnClickListener {
            val intent = Intent(this, DoctorYLinkedDetailActivity::class.java)
            startActivity(intent)
        }
        
        cardMT.setOnClickListener {
            val intent = Intent(this, DoctorMitochondrialDetailActivity::class.java)
            startActivity(intent)
        }

        // =========================
        // FETCH DATA IF MISSING
        // =========================
        if (analysisId == 0 || topPattern == "Unknown" || probsString.isBlank()) {
           if (patientId > 0) {
               fetchDataFromApi(patientId, pbAD, tvAD, pbAR, tvAR, pbXL, tvXL, pbYL, tvYL, pbMT, tvMT)
           } else {
               Toast.makeText(this, "Missing patient data to load pattern.", Toast.LENGTH_LONG).show()
           }
        }
    }

    private var fallbackAnalysisId = 0
    private var fallbackTopPattern = "Unknown"
    private var fallbackTopConfidence = 0

    private fun fetchDataFromApi(
        pId: Int,
        pbAD: ProgressBar, tvAD: TextView,
        pbAR: ProgressBar, tvAR: TextView,
        pbXL: ProgressBar, tvXL: TextView,
        pbYL: ProgressBar, tvYL: TextView,
        pbMT: ProgressBar, tvMT: TextView
    ) {
        val api = com.simats.genetics.network.ApiClient.getApi(this)
        
        // 1. Fetch recent analyses to get the latest analysisId for this patient
        api.getRecentAnalyses().enqueue(object : retrofit2.Callback<com.simats.genetics.network.responses.RecentAnalysesResponse> {
            override fun onResponse(call: retrofit2.Call<com.simats.genetics.network.responses.RecentAnalysesResponse>, response: retrofit2.Response<com.simats.genetics.network.responses.RecentAnalysesResponse>) {
                val body = response.body()
                val recentItem = body?.results?.firstOrNull { it.patientId == pId }
                
                if (recentItem != null) {
                    fallbackAnalysisId = recentItem.analysisId
                    fallbackTopPattern = recentItem.patternTitle
                    fallbackTopConfidence = (recentItem.confidence * 100).toInt()
                    
                    // Update button reference if initialized later
                    intent.putExtra("ANALYSIS_ID", fallbackAnalysisId)
                    intent.putExtra("TOP_PATTERN", fallbackTopPattern)
                    intent.putExtra("TOP_CONFIDENCE", fallbackTopConfidence)
                    
                    // 2. Fetch detailed results for probabilities
                    fetchAnalysisResult(fallbackAnalysisId, pbAD, tvAD, pbAR, tvAR, pbXL, tvXL, pbYL, tvYL, pbMT, tvMT)
                } else {
                    Toast.makeText(this@DoctorInheritancePatternActivity, "No analysis found for this patient.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<com.simats.genetics.network.responses.RecentAnalysesResponse>, t: Throwable) {
                Toast.makeText(this@DoctorInheritancePatternActivity, "Failed to fetch analysis info", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchAnalysisResult(
        aId: Int,
        pbAD: ProgressBar, tvAD: TextView,
        pbAR: ProgressBar, tvAR: TextView,
        pbXL: ProgressBar, tvXL: TextView,
        pbYL: ProgressBar, tvYL: TextView,
        pbMT: ProgressBar, tvMT: TextView
    ) {
        com.simats.genetics.network.ApiClient.getApi(this).getAnalysisResult(aId).enqueue(object : retrofit2.Callback<com.simats.genetics.network.responses.AnalysisResultResponse> {
            override fun onResponse(call: retrofit2.Call<com.simats.genetics.network.responses.AnalysisResultResponse>, response: retrofit2.Response<com.simats.genetics.network.responses.AnalysisResultResponse>) {
                val body = response.body()
                if (body?.status == true && body.result != null) {
                    val resultData = body.result
                    
                    val pMap = mutableMapOf<String, Int>()
                    pMap[resultData.primaryPattern.title] = (resultData.primaryPattern.confidence * 100).toInt()
                    
                    resultData.alternatives.forEach { alt ->
                        pMap[alt.title] = (alt.confidence * 100).toInt()
                    }
                    
                    val ad = pMap["Autosomal Dominant"] ?: (if (fallbackTopPattern == "Autosomal Dominant") fallbackTopConfidence else 0)
                    val ar = pMap["Autosomal Recessive"] ?: (if (fallbackTopPattern == "Autosomal Recessive") fallbackTopConfidence else 0)
                    val xl = pMap["X-Linked"] ?: (if (fallbackTopPattern == "X-Linked") fallbackTopConfidence else 0)
                    val yl = pMap["Y-Linked"] ?: (if (fallbackTopPattern == "Y-Linked") fallbackTopConfidence else 0)
                    val mt = pMap["Mitochondrial"] ?: (if (fallbackTopPattern == "Mitochondrial") fallbackTopConfidence else 0)

                    setBar(pbAD, tvAD, ad)
                    setBar(pbAR, tvAR, ar)
                    setBar(pbXL, tvXL, xl)
                    setBar(pbYL, tvYL, yl)
                    setBar(pbMT, tvMT, mt)
                }
            }

            override fun onFailure(call: retrofit2.Call<com.simats.genetics.network.responses.AnalysisResultResponse>, t: Throwable) {
                Log.e("INHERITANCE_API", "Failed to fetch detailed result: ${t.message}")
            }
        })
    }

    private fun openPatternDetail(patternKey: String) {
        val i = Intent(this, DoctorPatternDetailActivity::class.java)
        i.putExtra("PATTERN_KEY", patternKey)
        startActivity(i)
    }

    private fun setBar(pb: ProgressBar, tv: TextView, value: Int) {
        val safe = value.coerceIn(0, 100)
        pb.max = 100
        pb.progress = safe
        tv.text = "$safe%"
    }

    private fun parseProbabilities(raw: String): Map<String, Int> {
        // raw = "Autosomal Dominant:78;Autosomal Recessive:42;X-Linked:15;Y-Linked:5;Mitochondrial:8"
        if (raw.isBlank()) return emptyMap()
        return try {
            raw.split(";")
                .mapNotNull { part ->
                    val p = part.trim()
                    if (!p.contains(":")) return@mapNotNull null
                    val (k, v) = p.split(":", limit = 2)
                    val key = k.trim()
                    val value = v.trim().toIntOrNull() ?: 0
                    key to value
                }
                .toMap()
        } catch (e: Exception) {
            Log.e("PATTERN_PARSE", "parse error: ${e.message}")
            emptyMap()
        }
    }
}
