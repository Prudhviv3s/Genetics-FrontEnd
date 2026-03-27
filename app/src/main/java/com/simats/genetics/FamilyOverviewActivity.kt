package com.simats.genetics

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.text.Editable
import android.text.TextWatcher
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.TokenManager
import com.simats.genetics.network.responses.ApiResponse
import com.simats.genetics.network.responses.FamilyMemberItemResponse
import com.simats.genetics.network.responses.FamilyOverviewResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FamilyOverviewActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var btnAdd: Button
    private lateinit var tvCount: TextView
    private lateinit var etSearch: EditText

    private val allMembers = mutableListOf<FamilyMemberItemResponse>()
    private lateinit var adapter: FamilyMemberAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_family_overview)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        rv = findViewById(R.id.rv_family_members)
        btnAdd = findViewById(R.id.btn_add_member)

        // Match layout IDs
        tvCount = findViewById(R.id.tv_member_count)
        etSearch = findViewById(R.id.et_search_members)

        // Token check
        if (TokenManager.getToken(this).isNullOrBlank()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            goToLogin()
            return
        }

        adapter = FamilyMemberAdapter(mutableListOf())
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        btnAdd.setOnClickListener {
            startActivity(Intent(this, AddFamilyMemberActivity::class.java))
        }

        // Search
        etSearch.addTextChangedListener(SimpleTextWatcher { q ->
            val query = q.trim().lowercase()
            val filtered = if (query.isEmpty()) allMembers else allMembers.filter {
                it.fullName?.lowercase()?.contains(query) == true ||
                        it.relationship?.lowercase()?.contains(query) == true ||
                        it.healthStatus?.lowercase()?.contains(query) == true
            }
            adapter.setData(filtered)
        })
    }

    override fun onResume() {
        super.onResume()
        fetchFamilyMembers()
    }

    private fun fetchFamilyMembers() {
        ApiClient.getApi(this).getFamilyOverview().enqueue(object : Callback<FamilyOverviewResponse> {
            override fun onResponse(call: Call<FamilyOverviewResponse>, response: Response<FamilyOverviewResponse>) {

                if (response.code() == 401) {
                    Toast.makeText(this@FamilyOverviewActivity, "Session expired. Login again.", Toast.LENGTH_SHORT).show()
                    goToLogin()
                    return
                }

                if (!response.isSuccessful) {
                    Log.e("FAMILY_OVERVIEW", "HTTP ${response.code()} err=${response.errorBody()?.string()}")
                    Toast.makeText(this@FamilyOverviewActivity, "HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                    return
                }

                val body = response.body()
                Log.d("FAMILY_OVERVIEW", "Response received: status=${body?.status}, count=${body?.count}, membersSize=${body?.familyMembers?.size}")
                
                if (body?.status != true) {
                    Toast.makeText(this@FamilyOverviewActivity, body?.message ?: "Failed to load", Toast.LENGTH_SHORT).show()
                    return
                }

                allMembers.clear()
                val membersToAdd = body.familyMembers ?: emptyList()
                allMembers.addAll(membersToAdd)
                Log.d("FAMILY_OVERVIEW", "allMembers updated: size=${allMembers.size}")
                membersToAdd.forEachIndexed { index, m ->
                    Log.d("FAMILY_OVERVIEW", "Member[$index]: name=${m.fullName}, rel=${m.relationship}, age=${m.age}")
                }

                // Count label
                tvCount.text = "${body.count} members added"

                // Apply search text if any
                val query = etSearch.text?.toString()?.trim()?.lowercase().orEmpty()
                val filtered = if (query.isEmpty()) allMembers else allMembers.filter {
                    it.fullName?.lowercase()?.contains(query) == true ||
                            it.relationship?.lowercase()?.contains(query) == true ||
                            it.healthStatus?.lowercase()?.contains(query) == true
                }
                Log.d("FAMILY_OVERVIEW", "filtered list size: ${filtered.size} (query='$query')")

                val membersCount = filtered.size
                Toast.makeText(this@FamilyOverviewActivity, "Showing $membersCount members", Toast.LENGTH_SHORT).show()
                adapter.setData(filtered)
            }

            override fun onFailure(call: Call<FamilyOverviewResponse>, t: Throwable) {
                Toast.makeText(this@FamilyOverviewActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("FAMILY_OVERVIEW", "FAIL ${t.message}", t)
            }
        })
    }

    private fun showDeleteConfirmationDialog(member: FamilyMemberItemResponse) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_family_member, null)
        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.tv_dialog_message).text =
            """Are you sure you want to delete ${member.fullName ?: "this member"}?
This action cannot be undone."""

        dialogView.findViewById<Button>(R.id.btn_delete_confirm).setOnClickListener {
            deleteMember(member.id)
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btn_delete_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun deleteMember(memberId: Int) {
        ApiClient.getApi(this).deleteFamilyMember(memberId).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {

                if (response.code() == 401) {
                    Toast.makeText(this@FamilyOverviewActivity, "Session expired. Login again.", Toast.LENGTH_SHORT).show()
                    goToLogin()
                    return
                }

                if (!response.isSuccessful) {
                    Log.e("DELETE_MEMBER", "HTTP ${response.code()} err=${response.errorBody()?.string()}")
                    Toast.makeText(this@FamilyOverviewActivity, "Delete failed (HTTP ${response.code()})", Toast.LENGTH_SHORT).show()
                    return
                }

                val body = response.body()
                if (body?.status == true) {
                    Toast.makeText(this@FamilyOverviewActivity, "Deleted", Toast.LENGTH_SHORT).show()
                    fetchFamilyMembers()
                } else {
                    Toast.makeText(this@FamilyOverviewActivity, body?.message ?: "Delete failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(this@FamilyOverviewActivity, "Delete failed: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("DELETE_MEMBER", "FAIL ${t.message}", t)
            }
        })
    }

    private fun goToLogin() {
        TokenManager.clearToken(this)
        val i = Intent(this, SignInActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(i)
        finish()
    }

    // ---------------- ADAPTER ----------------

    inner class FamilyMemberAdapter(private val members: MutableList<FamilyMemberItemResponse>) :
        RecyclerView.Adapter<FamilyMemberAdapter.VH>() {

        fun setData(newData: List<FamilyMemberItemResponse>) {
            Log.d("FAMILY_OVERVIEW", "adapter.setData called with ${newData.size} items")
            members.clear()
            members.addAll(newData)
            notifyDataSetChanged()
        }

        inner class VH(view: android.view.View) : RecyclerView.ViewHolder(view) {
            val ivIcon: ImageView = view.findViewById(R.id.iv_member_icon)
            val tvName: TextView = view.findViewById(R.id.tv_member_name)
            val tvRelationship: TextView = view.findViewById(R.id.tv_relationship)
            val tvAge: TextView = view.findViewById(R.id.tv_age)
            val tvStatus: TextView = view.findViewById(R.id.tv_status_badge)
            val ivDelete: ImageView = view.findViewById(R.id.iv_delete)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val view = layoutInflater.inflate(R.layout.item_family_member, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val member = members[position]
            Log.d("FAMILY_OVERVIEW", "onBindViewHolder at pos $position: ${member.fullName}")

            holder.tvName.text = member.fullName ?: "Unknown Name"
            
            val displaySide = if (member.sideOfFamily != null && member.sideOfFamily != "None") {
                " (${member.sideOfFamily})"
            } else ""
            
            holder.tvRelationship.text = "${member.relationship ?: "Unknown"}$displaySide •"
            holder.tvAge.text = "${member.age ?: 0} years"
            holder.tvStatus.text = member.healthStatus ?: "Unknown"

            // Badge style colors
            when (member.healthStatus) {
                "Affected" -> {
                    holder.tvStatus.setBackgroundResource(R.drawable.badge_affected)
                    holder.tvStatus.setTextColor(Color.parseColor("#F5222D"))
                }
                "Carrier" -> {
                    holder.tvStatus.setBackgroundResource(R.drawable.badge_carrier)
                    holder.tvStatus.setTextColor(Color.parseColor("#FA8C16"))
                }
                "Unaffected" -> {
                    holder.tvStatus.setBackgroundResource(R.drawable.badge_unaffected)
                    holder.tvStatus.setTextColor(Color.parseColor("#52C41A"))
                }
                else -> {
                    holder.tvStatus.setBackgroundResource(R.drawable.badge_unknown)
                    holder.tvStatus.setTextColor(Color.parseColor("#595959"))
                }
            }

            // Pedigree Icon Selection
            val isFemale = member.gender.equals("female", true)
            val iconRes = when (member.healthStatus) {
                "Affected" -> if (isFemale) R.drawable.ped_female_affected else R.drawable.ped_male_affected
                "Carrier" -> if (isFemale) R.drawable.ped_female_carrier else R.drawable.ped_male_carrier
                "Unaffected" -> if (isFemale) R.drawable.ped_female_unaffected else R.drawable.ped_male_unaffected
                else -> if (isFemale) R.drawable.ped_female_unaffected else R.drawable.ped_male_unaffected // Default to unaffected style for unknown
            }
            holder.ivIcon.setImageResource(iconRes)

            holder.ivDelete.setOnClickListener { showDeleteConfirmationDialog(member) }
        }

        override fun getItemCount(): Int = members.size
    }
}