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
import com.google.android.material.card.MaterialCardView
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.responses.PedigreeChartResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DoctorPedigreeAnalysisActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_pedigree_analysis)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pedigree Analysis"

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // ✅ Get patient data properly
        val patientName = intent.getStringExtra("PATIENT_NAME")
        val patientId = intent.getIntExtra("PATIENT_ID", -1)

        findViewById<TextView>(R.id.tv_patient_name_header).text = patientName ?: "Unknown"

        // Zoom
        var currentZoom = 100
        val tvZoomPercent = findViewById<TextView>(R.id.tv_zoom_percent)
        val chartContainer = findViewById<ConstraintLayout>(R.id.chart_container)

        findViewById<MaterialCardView>(R.id.btn_zoom_in).setOnClickListener {
            if (currentZoom < 200) {
                currentZoom += 10
                updateZoom(currentZoom, tvZoomPercent, chartContainer)
            }
        }

        findViewById<MaterialCardView>(R.id.btn_zoom_out).setOnClickListener {
            if (currentZoom > 50) {
                currentZoom -= 10
                updateZoom(currentZoom, tvZoomPercent, chartContainer)
            }
        }

        // DNA Detection button
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_run_detection)
            .setOnClickListener {
                val intent = Intent(this, DoctorDnaReportActivity::class.java)
                intent.putExtra("PATIENT_ID", patientId)
                intent.putExtra("PATIENT_NAME", patientName)
                startActivity(intent)
            }

        // ✅ Fetch pedigree chart
        if (patientId != -1) {
            fetchPedigreeData(patientId)
        } else {
            Toast.makeText(this, "Invalid patient ID", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchPedigreeData(patientId: Int) {

        ApiClient.getApi(this)
            .getDoctorPatientPedigreeChart(patientId)
            .enqueue(object : Callback<PedigreeChartResponse> {

                override fun onResponse(
                    call: Call<PedigreeChartResponse>,
                    response: Response<PedigreeChartResponse>
                ) {

                    if (!response.isSuccessful) {
                        Log.e("DOC_PED_CHART", "HTTP ${response.code()} err=${response.errorBody()?.string()}")
                        Toast.makeText(
                            this@DoctorPedigreeAnalysisActivity,
                            "HTTP ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    val body = response.body()

                    if (body?.status == true && body.pedigree != null) {

                        if (body.pedigree.nodes.isNullOrEmpty()) {
                            Toast.makeText(
                                this@DoctorPedigreeAnalysisActivity,
                                "No pedigree nodes found",
                                Toast.LENGTH_SHORT
                            ).show()
                            return
                        }

                        findViewById<PedigreeChartView>(R.id.pedigree_chart_view)
                            .setData(body.pedigree)

                    } else {
                        Toast.makeText(
                            this@DoctorPedigreeAnalysisActivity,
                            body?.message ?: "No pedigree data available",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<PedigreeChartResponse>, t: Throwable) {
                    Log.e("DOC_PED_CHART", "FAIL ${t.message}", t)
                    Toast.makeText(
                        this@DoctorPedigreeAnalysisActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun updateZoom(zoom: Int, tvPercent: TextView, container: View) {
        tvPercent.text = "$zoom%"
        val scale = zoom / 100f
        container.scaleX = scale
        container.scaleY = scale
        container.pivotX = 0f
        container.pivotY = 0f
    }
}