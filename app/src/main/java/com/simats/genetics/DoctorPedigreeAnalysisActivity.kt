package com.simats.genetics

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
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

    private lateinit var pedigreeView: PedigreeChartView
    private lateinit var loader: ProgressBar

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

        val patientName      = intent.getStringExtra("PATIENT_NAME")
        val patientId        = intent.getIntExtra("PATIENT_ID", -1)
        val patientDisplayId = intent.getStringExtra("PATIENT_DISPLAY_ID")

        findViewById<TextView>(R.id.tv_patient_name_header).text = patientName ?: "Unknown"
        findViewById<TextView>(R.id.tv_patient_id_header).text   = "ID: ${patientDisplayId ?: "Unknown"}"

        // Bind views
        pedigreeView = findViewById(R.id.pedigree_chart_view)
        loader       = findViewById(R.id.loader)

        // Zoom
        var currentZoom = 100
        val tvZoomPercent  = findViewById<TextView>(R.id.tv_zoom_percent)
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


        // Initial variables set but fetching moved to onResume
    }

    override fun onResume() {
        super.onResume()
        val patientId = intent.getIntExtra("PATIENT_ID", -1)
        if (patientId != -1) {
            fetchPedigreeData(patientId)
        } else {
            Toast.makeText(this, "Invalid patient ID", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchPedigreeData(patientId: Int) {
        loader.visibility = View.VISIBLE

        ApiClient.getApi(this)
            .getDoctorPatientPedigreeChart(patientId)
            .enqueue(object : Callback<PedigreeChartResponse> {

                override fun onResponse(
                    call: Call<PedigreeChartResponse>,
                    response: Response<PedigreeChartResponse>
                ) {
                    loader.visibility = View.GONE

                    if (!response.isSuccessful) {
                        Log.e("DOC_PED_CHART", "HTTP ${response.code()} err=${response.errorBody()?.string()}")
                        Toast.makeText(
                            this@DoctorPedigreeAnalysisActivity,
                            "Failed to load chart (HTTP ${response.code()}). Is the server running?",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    val body = response.body()
                    if (body?.status == true && body.pedigree != null) {
                        val nodes = body.pedigree.nodes
                        if (nodes.isNullOrEmpty()) {
                            // PedigreeChartView already shows a placeholder
                            Toast.makeText(
                                this@DoctorPedigreeAnalysisActivity,
                                "No family members found for this patient. Please add family members first.",
                                Toast.LENGTH_LONG
                            ).show()
                            return
                        }
                        // Set data — PedigreeChartView will redraw automatically
                        pedigreeView.setData(body.pedigree)
                    } else {
                        Toast.makeText(
                            this@DoctorPedigreeAnalysisActivity,
                            body?.message ?: "No pedigree data available",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<PedigreeChartResponse>, t: Throwable) {
                    loader.visibility = View.GONE
                    Log.e("DOC_PED_CHART", "FAIL ${t.message}", t)
                    Toast.makeText(
                        this@DoctorPedigreeAnalysisActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_LONG
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