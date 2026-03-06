package com.simats.genetics

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.responses.DoctorReportPatientItem
import com.simats.genetics.network.responses.DoctorReportPatientsListResponse
import com.simats.genetics.network.responses.toDoctorReportPatientItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DoctorPatientReportsActivity : AppCompatActivity() {

    private lateinit var rvPatientReports: RecyclerView
    private lateinit var adapter: PatientReportAdapter

    // Optional search (ONLY if your XML has it)
    private var etSearch: EditText? = null
    private var tvCount: TextView? = null

    private val allPatients = mutableListOf<DoctorReportPatientItem>()
    private val filteredPatients = mutableListOf<DoctorReportPatientItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_patient_reports)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        rvPatientReports = findViewById(R.id.rv_patient_reports)
        rvPatientReports.layoutManager = LinearLayoutManager(this)

        adapter = PatientReportAdapter(filteredPatients) { patient ->
            val intent = Intent(this, DoctorReportDetailActivity::class.java)
            intent.putExtra("PATIENT_ID", patient.id)                       // DB id
            intent.putExtra("PATIENT_DISPLAY_ID", patient.patientId ?: "")  // PTxxxx
            intent.putExtra("PATIENT_NAME", patient.fullName ?: "")
            intent.putExtra("PATIENT_AGE", patient.age ?: 0)
            intent.putExtra("PATTERN", patient.inheritancePattern ?: "Unknown")
            intent.putExtra("LAST_ANALYSIS", patient.lastAnalysis ?: "")
            intent.putExtra("ANALYSIS_ID", patient.analysisId ?: 0)
            startActivity(intent)
        }

        rvPatientReports.adapter = adapter

        // If you have these ids in XML, it will work. If not, remove these lines.
        etSearch = findViewById(R.id.et_search)
        // tvCount = findViewById(R.id.tv_results_count)

        etSearch?.addTextChangedListener(SimpleTextWatcher { q ->
            applyFilter(q)
        })

        fetchPatients()
    }

    private fun fetchPatients() {
        ApiClient.getApi(this).getDoctorReportPatients()
            .enqueue(object : Callback<DoctorReportPatientsListResponse> {

                override fun onResponse(
                    call: Call<DoctorReportPatientsListResponse>,
                    response: Response<DoctorReportPatientsListResponse>
                ) {
                    if (!response.isSuccessful) {
                        val err = response.errorBody()?.string()
                        Log.e("DOC_REPORTS", "HTTP ${response.code()} err=$err")
                        Toast.makeText(this@DoctorPatientReportsActivity, "HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val body = response.body()
                    if (body?.status == true) {
                        allPatients.clear()
                        allPatients.addAll(body.patients.map { it.toDoctorReportPatientItem() })
                        applyFilter(etSearch?.text?.toString().orEmpty())
                    } else {
                        Toast.makeText(this@DoctorPatientReportsActivity, "Failed to load patients", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<DoctorReportPatientsListResponse>, t: Throwable) {
                    Log.e("DOC_REPORTS", "FAIL ${t.message}", t)
                    Toast.makeText(this@DoctorPatientReportsActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun applyFilter(queryRaw: String) {
        val q = queryRaw.trim().lowercase()

        filteredPatients.clear()
        if (q.isEmpty()) {
            filteredPatients.addAll(allPatients)
        } else {
            filteredPatients.addAll(
                allPatients.filter {
                    (it.fullName ?: "").lowercase().contains(q) ||
                            (it.patientId ?: "").lowercase().contains(q) ||
                            (it.inheritancePattern ?: "").lowercase().contains(q)
                }
            )
        }

        adapter.notifyDataSetChanged()
        tvCount?.text = "${filteredPatients.size} patients found"
    }

    // =========================
    // ADAPTER
    // =========================
    inner class PatientReportAdapter(
        private val items: List<DoctorReportPatientItem>,
        private val onItemClick: (DoctorReportPatientItem) -> Unit
    ) : RecyclerView.Adapter<PatientReportAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_patient_report, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]

            holder.tvName.text = item.fullName ?: "-"
            holder.tvAge.text = "Age: ${item.age ?: "-"}"
            holder.tvPatientId.text = "ID: ${item.patientId ?: "-"}"

            holder.itemView.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tv_patient_name)
            val tvAge: TextView = view.findViewById(R.id.tv_patient_age)
            val tvPatientId: TextView = view.findViewById(R.id.tv_patient_id)
        }
    }
}
