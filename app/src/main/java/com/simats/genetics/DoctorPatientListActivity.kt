package com.simats.genetics

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.responses.DoctorPatientResponse
import com.simats.genetics.network.responses.DoctorPatientsListResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DoctorPatientListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var resultsCount: TextView

    private lateinit var etSearch: EditText

    private val allPatients = mutableListOf<DoctorPatientResponse>()
    private val shownPatients = mutableListOf<DoctorPatientResponse>()
    private lateinit var adapter: PatientAdapter

    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_patient_list)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        recyclerView = findViewById(R.id.rv_patients)
        recyclerView.layoutManager = LinearLayoutManager(this)

        resultsCount = findViewById(R.id.tv_results_count)
        etSearch = findViewById(R.id.et_search)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                currentQuery = (s?.toString() ?: "").trim()
                applyFilters()
            }
        })

        adapter = PatientAdapter(shownPatients)
        recyclerView.adapter = adapter

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_home

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> true

                R.id.navigation_pedigree -> {
                    val intent = Intent(this, DoctorPedigreeListActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.navigation_analysis -> {
                    val intent = Intent(this, DoctorAnalysisActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.navigation_settings -> {
                    val intent = Intent(this, DoctorSettingsActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }

                else -> false
            }
        }

        fetchPatients()
    }

    private fun fetchPatients() {
        ApiClient.getApi(this).getDoctorPatients()
            .enqueue(object : Callback<DoctorPatientsListResponse> {
                override fun onResponse(
                    call: Call<DoctorPatientsListResponse>,
                    response: Response<DoctorPatientsListResponse>
                ) {
                    if (!response.isSuccessful) {
                        val err = response.errorBody()?.string()
                        Log.e("DOC_PAT_LIST", "HTTP ${response.code()} err=$err")
                        Toast.makeText(
                            this@DoctorPatientListActivity,
                            "HTTP ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    val body = response.body()
                    if (body?.status == true) {
                        allPatients.clear()
                        allPatients.addAll(body.patients)
                        applyFilters()
                    } else {
                        Toast.makeText(
                            this@DoctorPatientListActivity,
                            "Failed to load patients",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<DoctorPatientsListResponse>, t: Throwable) {
                    Log.e("DOC_PAT_LIST", "FAIL ${t.message}", t)
                    Toast.makeText(
                        this@DoctorPatientListActivity,
                        "Network error",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun applyFilters() {
        val q = currentQuery.lowercase()

        val bySearch = if (q.isBlank()) {
            allPatients
        } else {
            allPatients.filter { patient ->
                (patient.fullName ?: "").lowercase().contains(q) ||
                        (patient.email ?: "").lowercase().contains(q) ||
                        (patient.patientId ?: "").lowercase().contains(q)
            }
        }

        shownPatients.clear()
        shownPatients.addAll(bySearch)
        adapter.notifyDataSetChanged()
        resultsCount.text = "${shownPatients.size} patients found"
    }

    inner class PatientAdapter(
        private val patients: List<DoctorPatientResponse>
    ) : RecyclerView.Adapter<PatientAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tv_patient_name)
            val tvEmail: TextView = view.findViewById(R.id.tv_patient_email)
            val tvDetails: TextView = view.findViewById(R.id.tv_patient_details)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_patient_pedigree, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val patient = patients[position]

            val name = patient.fullName ?: "-"
            val email = patient.email ?: "-"
            val age = patient.age?.toString() ?: "-"
            val gender = patient.gender ?: "-"
            val displayId = patient.patientId ?: "-"

            holder.tvName.text = name
            holder.tvEmail.text = email
            holder.tvDetails.text = "Age: $age  Gender: $gender  ID: $displayId"

            holder.itemView.setOnClickListener {
                val intent = Intent(this@DoctorPatientListActivity, PatientDetailsActivity::class.java)
                intent.putExtra("PATIENT_NAME", name)
                intent.putExtra("PATIENT_ID", patient.id) // integer DB id for backend
                intent.putExtra("PATIENT_DISPLAY_ID", displayId) // PT1001 for UI
                intent.putExtra("PATIENT_EMAIL", email)
                intent.putExtra("PATIENT_AGE", patient.age ?: 0)
                intent.putExtra("PATIENT_GENDER", gender)
                startActivity(intent)
            }
        }

        override fun getItemCount(): Int = patients.size
    }
}