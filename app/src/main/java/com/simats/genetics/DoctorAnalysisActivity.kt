package com.simats.genetics

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.responses.DoctorPatientResponse
import com.simats.genetics.network.responses.DoctorPatientsListResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DoctorAnalysisActivity : AppCompatActivity() {

    private var recycler: RecyclerView? = null
    private lateinit var adapter: DoctorAnalysisPatientAdapter
    private var loader: View? = null
    private var tvNoAnalyses: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_analysis)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Inheritance Analysis"
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_analysis

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    val intent = Intent(this, DoctorHomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    true
                }
                R.id.navigation_pedigree -> {
                    val intent = Intent(this, DoctorPedigreeListActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    true
                }
                R.id.navigation_analysis -> true
                R.id.navigation_settings -> {
                    val intent = Intent(this, DoctorSettingsActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        loader = findViewById(R.id.loader)
        recycler = findViewById(R.id.recycler_recent_analyses)
        tvNoAnalyses = findViewById(R.id.tv_no_analyses)

        recycler?.layoutManager = LinearLayoutManager(this)

        adapter = DoctorAnalysisPatientAdapter(emptyList()) { patient ->
            val intent = Intent(this, DoctorInheritancePatternActivity::class.java)
            intent.putExtra("PATIENT_ID", patient.id)
            intent.putExtra("PATIENT_NAME", patient.fullName)
            intent.putExtra("PATIENT_DISPLAY_ID", patient.patientId)
            startActivity(intent)
        }

        recycler?.adapter = adapter

        loadPatientsForAnalysis()
    }

    private fun loadPatientsForAnalysis() {
        showLoading(true)
        recycler?.visibility = View.GONE
        tvNoAnalyses?.visibility = View.GONE

        ApiClient.getApi(this).getDoctorPatients().enqueue(object : Callback<DoctorPatientsListResponse> {
            override fun onResponse(
                call: Call<DoctorPatientsListResponse>,
                response: Response<DoctorPatientsListResponse>
            ) {
                showLoading(false)

                if (!response.isSuccessful) {
                    Toast.makeText(
                        this@DoctorAnalysisActivity,
                        "Failed to load patients (${response.code()})",
                        Toast.LENGTH_LONG
                    ).show()
                    tvNoAnalyses?.text = "No patients found."
                    tvNoAnalyses?.visibility = View.VISIBLE
                    return
                }

                val body = response.body()
                val patients = body?.patients ?: emptyList()

                if (body?.status != true || patients.isEmpty()) {
                    adapter.submit(emptyList())
                    tvNoAnalyses?.text = "No patients found."
                    tvNoAnalyses?.visibility = View.VISIBLE
                    recycler?.visibility = View.GONE
                    return
                }

                adapter.submit(patients)
                recycler?.visibility = View.VISIBLE
                tvNoAnalyses?.visibility = View.GONE
            }

            override fun onFailure(call: Call<DoctorPatientsListResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(
                    this@DoctorAnalysisActivity,
                    t.message ?: "Network error",
                    Toast.LENGTH_LONG
                ).show()
                tvNoAnalyses?.text = "No patients found."
                tvNoAnalyses?.visibility = View.VISIBLE
                recycler?.visibility = View.GONE
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        loader?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}

private class DoctorAnalysisPatientAdapter(
    private var items: List<DoctorPatientResponse>,
    private val onClick: (DoctorPatientResponse) -> Unit
) : RecyclerView.Adapter<DoctorAnalysisPatientAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.txtName)
        val meta: TextView = v.findViewById(R.id.txtMeta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_analysis, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text = item.fullName
        holder.meta.text = item.patientId
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<DoctorPatientResponse>) {
        items = newItems
        notifyDataSetChanged()
    }
}