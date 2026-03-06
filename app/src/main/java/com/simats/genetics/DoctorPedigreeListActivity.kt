package com.simats.genetics

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.responses.DoctorPatientResponse
import com.simats.genetics.network.responses.DoctorPatientsListResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DoctorPedigreeListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var resultsCount: TextView
    private lateinit var adapter: PatientAdapter

    private val allPatients = mutableListOf<DoctorPatientResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_pedigree_list)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        recyclerView = findViewById(R.id.rv_patients)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = PatientAdapter()
        recyclerView.adapter = adapter

        resultsCount = findViewById(R.id.tv_results_count)

        val bottomNavigationView = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_pedigree

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> {
                    val intent = Intent(this, DoctorHomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_pedigree -> true
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
        val etSearch = findViewById<android.widget.EditText>(R.id.et_search)
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        fetchPatients()
    }

    private fun filter(query: String) {
        val filteredList = if (query.isEmpty()) {
            allPatients
        } else {
            allPatients.filter {
                it.fullName.contains(query, ignoreCase = true) ||
                        it.email.contains(query, ignoreCase = true) ||
                        it.patientId.contains(query, ignoreCase = true)
            }
        }
        adapter.setData(filteredList)
        resultsCount.text = "${filteredList.size} patients found"
    }

    private fun fetchPatients() {
        ApiClient.getApi(this)
            .getDoctorPedigreePatients()
            .enqueue(object : Callback<DoctorPatientsListResponse> {

                override fun onResponse(
                    call: Call<DoctorPatientsListResponse>,
                    response: Response<DoctorPatientsListResponse>
                ) {

                    if (!response.isSuccessful) {
                        Log.e("DOC_PED_LIST", "HTTP ${response.code()} err=${response.errorBody()?.string()}")
                        Toast.makeText(this@DoctorPedigreeListActivity, "HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val body = response.body()

                    if (body?.status == true) {
                        allPatients.clear()
                        allPatients.addAll(body.patients)

                        adapter.setData(allPatients)
                        resultsCount.text = "${allPatients.size} patients found"
                    } else {
                        Toast.makeText(this@DoctorPedigreeListActivity, "Failed to load patients", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<DoctorPatientsListResponse>, t: Throwable) {
                    Log.e("DOC_PED_LIST", "FAIL ${t.message}", t)
                    Toast.makeText(this@DoctorPedigreeListActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ================= ADAPTER =================

    inner class PatientAdapter :
        RecyclerView.Adapter<PatientAdapter.ViewHolder>() {

        private val patients = mutableListOf<DoctorPatientResponse>()

        fun setData(newList: List<DoctorPatientResponse>) {
            patients.clear()
            patients.addAll(newList)
            notifyDataSetChanged()
        }

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

            holder.tvName.text = patient.fullName
            holder.tvEmail.text = patient.email

            val age = patient.age ?: 0
            val gender = patient.gender ?: "Unknown"

            holder.tvDetails.text =
                "Age: $age    Gender: $gender    ID: ${patient.patientId}"

            holder.itemView.setOnClickListener {

                val intent = Intent(
                    this@DoctorPedigreeListActivity,
                    DoctorPedigreeAnalysisActivity::class.java
                )

                intent.putExtra("PATIENT_ID", patient.id) // PASS INT (REQUIRED BY RECEIVER)
                intent.putExtra("PATIENT_NAME", patient.fullName)
                intent.putExtra("PATIENT_DISPLAY_ID", patient.patientId)

                startActivity(intent)
            }
        }

        override fun getItemCount() = patients.size
    }
}