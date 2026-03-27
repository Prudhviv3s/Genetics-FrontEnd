package com.simats.genetics

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.TokenManager
import com.simats.genetics.network.requests.FamilyMemberUpdateRequest
import com.simats.genetics.network.responses.FamilyMemberUpdateResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HealthStatusActivity : AppCompatActivity() {

    private var selectedPosition: Int = -1
    private var memberId: Int = -1
    private var relationship: String? = null
    private var side: String? = null

    data class HealthStatus(val title: String, val description: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_status)

        memberId = intent.getIntExtra("MEMBER_ID", -1)
        relationship = intent.getStringExtra("RELATIONSHIP")
        side = intent.getStringExtra("SIDE")
        
        if (memberId == -1) {
            Toast.makeText(this, "Member ID missing. Please add family member again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // ... (rest of onCreate remains same)
        val token = TokenManager.getToken(this)
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val statuses = listOf(
            HealthStatus("Affected", "Shows symptoms of the genetic condition"),
            HealthStatus("Unaffected", "Does not show symptoms"),
            HealthStatus("Carrier", "Carries the gene but no symptoms"),
            HealthStatus("Unknown", "Status not yet determined")
        )

        val recyclerView = findViewById<RecyclerView>(R.id.rv_health_status)
        val btnContinue = findViewById<Button>(R.id.btn_continue)

        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = HealthStatusAdapter(statuses) { position ->
            selectedPosition = position
            btnContinue.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1C57D9"))
        }
        recyclerView.adapter = adapter

        btnContinue.setOnClickListener {
            if (selectedPosition == -1) {
                Toast.makeText(this, "Please select a health status", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val chosenStatus = statuses[selectedPosition].title // must match backend values
            saveHealthStatusToBackend(memberId, chosenStatus, btnContinue)
        }
    }

    private fun saveHealthStatusToBackend(memberId: Int, healthStatus: String, btnContinue: Button) {
        btnContinue.isEnabled = false

        val req = FamilyMemberUpdateRequest(
            relationship = relationship,
            side_of_family = side ?: "None",
            health_status = healthStatus,
            medical_notes = "" // Send empty string instead of null to match web
        )

        ApiClient.getApi(this).updateFamilyMember(memberId, req)
            .enqueue(object : Callback<FamilyMemberUpdateResponse> {
                // ... (rest remains same)
                override fun onResponse(
                    call: Call<FamilyMemberUpdateResponse>,
                    response: Response<FamilyMemberUpdateResponse>
                ) {
                    btnContinue.isEnabled = true

                    if (response.code() == 401) {
                        Toast.makeText(this@HealthStatusActivity, "Session expired. Login again.", Toast.LENGTH_SHORT).show()
                        TokenManager.clearToken(this@HealthStatusActivity)
                        startActivity(Intent(this@HealthStatusActivity, SignInActivity::class.java))
                        finish()
                        return
                    }

                    if (!response.isSuccessful) {
                        val err = response.errorBody()?.string()
                        Log.e("HEALTH_UPDATE", "HTTP ${response.code()} err=$err")
                        Toast.makeText(this@HealthStatusActivity, "HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val body = response.body()
                    if (body?.status == true) {
                        Toast.makeText(this@HealthStatusActivity, "Family member saved", Toast.LENGTH_SHORT).show()

                        // Go back to Patient Home (clear intermediate screens)
                        val i = Intent(this@HealthStatusActivity, PatientHomeActivity::class.java)
                        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(i)
                        finish()
                    } else {
                        Toast.makeText(this@HealthStatusActivity, body?.message ?: "Failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<FamilyMemberUpdateResponse>, t: Throwable) {
                    btnContinue.isEnabled = true
                    Log.e("HEALTH_UPDATE", "FAIL ${t.message}", t)
                    Toast.makeText(this@HealthStatusActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    inner class HealthStatusAdapter(
        private val items: List<HealthStatus>,
        private val onItemSelected: (Int) -> Unit
    ) : RecyclerView.Adapter<HealthStatusAdapter.ViewHolder>() {

        private var lastSelectedPos = -1

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tv_status_title)
            val tvDescription: TextView = view.findViewById(R.id.tv_status_description)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_health_status, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTitle.text = item.title
            holder.tvDescription.text = item.description

            holder.itemView.isSelected = (position == lastSelectedPos)

            holder.itemView.setOnClickListener {
                val previous = lastSelectedPos
                lastSelectedPos = holder.adapterPosition
                notifyItemChanged(previous)
                notifyItemChanged(lastSelectedPos)
                onItemSelected(lastSelectedPos)
            }
        }

        override fun getItemCount() = items.size
    }
}