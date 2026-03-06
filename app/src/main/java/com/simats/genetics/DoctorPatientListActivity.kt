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

    // ADD THESE (must exist in XML with same ids)
    private lateinit var etSearch: EditText
    private lateinit var chipAll: Chip
    private lateinit var chipPending: Chip
    private lateinit var chipAnalyzed: Chip
    private lateinit var chipPriority: Chip

    private val allPatients = mutableListOf<DoctorPatientResponse>()
    private val shownPatients = mutableListOf<DoctorPatientResponse>()
    private lateinit var adapter: PatientAdapter

    private enum class Tab { ALL, PENDING, ANALYZED, PRIORITY }
    private var currentTab: Tab = Tab.ALL
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

        // ====== SEARCH + TABS ======
        etSearch = findViewById(R.id.et_search)

        chipAll = findViewById(R.id.chip_all)
        chipPending = findViewById(R.id.chip_pending)
        chipAnalyzed = findViewById(R.id.chip_analyzed)
        chipPriority = findViewById(R.id.chip_priority)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentQuery = (s?.toString() ?: "").trim()
                applyFilters()
            }
        })

        chipAll.setOnClickListener { setTab(Tab.ALL) }
        chipPending.setOnClickListener { setTab(Tab.PENDING) }
        chipAnalyzed.setOnClickListener { setTab(Tab.ANALYZED) }
        chipPriority.setOnClickListener { setTab(Tab.PRIORITY) }

        // Adapter uses shownPatients (filtered list)
        adapter = PatientAdapter(shownPatients)
        recyclerView.adapter = adapter

        // Bottom navigation (your same code)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_home

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> {
                    val intent = Intent(this, DoctorHomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
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

        // default tab
        setTab(Tab.ALL)

        fetchPatients()
    }

    private fun setTab(tab: Tab) {
        currentTab = tab
        chipAll.isChecked = tab == Tab.ALL
        chipPending.isChecked = tab == Tab.PENDING
        chipAnalyzed.isChecked = tab == Tab.ANALYZED
        chipPriority.isChecked = tab == Tab.PRIORITY
        applyFilters()
    }

    private fun fetchPatients() {
        ApiClient.getApi(this).getDoctorPatients().enqueue(object : Callback<DoctorPatientsListResponse> {
            override fun onResponse(
                call: Call<DoctorPatientsListResponse>,
                response: Response<DoctorPatientsListResponse>
            ) {
                if (!response.isSuccessful) {
                    val err = response.errorBody()?.string()
                    Log.e("DOC_PAT_LIST", "HTTP ${response.code()} err=$err")
                    Toast.makeText(this@DoctorPatientListActivity, "HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                    return
                }

                val body = response.body()
                if (body?.status == true) {
                    allPatients.clear()
                    allPatients.addAll(body.patients)
                    applyFilters()
                } else {
                    Toast.makeText(this@DoctorPatientListActivity, "Failed to load patients", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<DoctorPatientsListResponse>, t: Throwable) {
                Log.e("DOC_PAT_LIST", "FAIL ${t.message}", t)
                Toast.makeText(this@DoctorPatientListActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun applyFilters() {
        val q = currentQuery.lowercase()

        val byTab = allPatients.filter { p ->
            when (currentTab) {
                Tab.ALL -> true
                Tab.PENDING -> isPending(p)
                Tab.ANALYZED -> !isPending(p)
                Tab.PRIORITY -> isPriority(p)
            }
        }

        val bySearch = if (q.isBlank()) {
            byTab
        } else {
            byTab.filter { p ->
                p.fullName.lowercase().contains(q) ||
                        p.email.lowercase().contains(q) ||
                        p.patientId.lowercase().contains(q)
            }
        }

        shownPatients.clear()
        shownPatients.addAll(bySearch)
        adapter.notifyDataSetChanged()
        resultsCount.text = "${shownPatients.size} patients found"
    }

    // ====== CHANGE THESE RULES IF YOU WANT ======
    private fun isPending(p: DoctorPatientResponse): Boolean {
        // Example rule because API does not send status:
        // Pending if missing important fields
        return p.email.isBlank() || p.gender.isBlank() || p.age <= 0
    }

    private fun isPriority(p: DoctorPatientResponse): Boolean {
        // Example: pending + older patients
        return isPending(p) && p.age >= 40
    }

    inner class PatientAdapter(private val patients: List<DoctorPatientResponse>) :
        RecyclerView.Adapter<PatientAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tv_patient_name)
            val tvEmail: TextView = view.findViewById(R.id.tv_patient_email)
            val tvDetails: TextView = view.findViewById(R.id.tv_patient_details)

            val tvBadge: TextView = view.findViewById(R.id.tv_status_badge)
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
            holder.tvDetails.text = "Age: ${patient.age}  Gender: ${patient.gender}  ID: ${patient.patientId}"

            // If you added a badge TextView in XML (tv_status_badge), this shows it.
            holder.tvBadge?.let { badge ->
                if (isPending(patient)) {
                    badge.visibility = View.VISIBLE
                    badge.text = "Pending"
                } else {
                    badge.visibility = View.VISIBLE
                    badge.text = "Analyzed"
                }
            }

            holder.itemView.setOnClickListener {
                val intent = Intent(this@DoctorPatientListActivity, PatientDetailsActivity::class.java)
                intent.putExtra("PATIENT_NAME", patient.fullName)
                intent.putExtra("PATIENT_ID", patient.id)
                intent.putExtra("PATIENT_DISPLAY_ID", patient.patientId)
                intent.putExtra("PATIENT_EMAIL", patient.email)
                intent.putExtra("PATIENT_AGE", patient.age)
                intent.putExtra("PATIENT_GENDER", patient.gender)
                startActivity(intent)
            }
        }

        override fun getItemCount() = patients.size
    }
}