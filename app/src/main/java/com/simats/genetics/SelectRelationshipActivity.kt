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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.TokenManager
import com.simats.genetics.network.requests.FamilyMemberUpdateRequest
import com.simats.genetics.network.responses.FamilyMemberUpdateResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SelectRelationshipActivity : AppCompatActivity() {

    private var selectedPosition: Int = -1
    private var memberId: Int = -1

    data class Relationship(val label: String, val icon: String, val imageRes: Int? = null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_relationship)

        memberId = intent.getIntExtra("MEMBER_ID", -1)
        if (memberId == -1) {
            Toast.makeText(this, "Member ID missing. Please add family member again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

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

        val relationships = listOf(
            Relationship("Father", "👦"),
            Relationship("Mother", "👩"),
            Relationship("Brother", "👦"),
            Relationship("Sister", "👧"),
            Relationship("Son", "👶"),
            Relationship("Daughter", "👶"),
            Relationship("Grandfather", "👴"),
            Relationship("Grandmother", "👵"),
            Relationship("Uncle", "👨"),
            Relationship("Aunt", "👩"),
            Relationship("Cousin", "", R.drawable.ic_person),
            Relationship("Other Relative", "", R.drawable.ic_pedigree)
        )

        val recyclerView = findViewById<RecyclerView>(R.id.rv_relationships)
        val btnContinue = findViewById<Button>(R.id.btn_continue)

        recyclerView.layoutManager = GridLayoutManager(this, 2)

        val adapter = RelationshipAdapter(relationships) { position ->
            selectedPosition = position
            btnContinue.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1C57D9"))
        }
        recyclerView.adapter = adapter

        btnContinue.setOnClickListener {
            if (selectedPosition == -1) {
                Toast.makeText(this, "Please select a relationship", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val relationship = relationships[selectedPosition].label
            navigateNext(relationship)
        }
    }

    private fun navigateNext(relationship: String) {
        val needsSide = listOf("Grandfather", "Grandmother", "Uncle", "Aunt", "Cousin").contains(relationship)
        
        if (needsSide) {
            val i = Intent(this, SelectFamilySideActivity::class.java)
            i.putExtra("MEMBER_ID", memberId)
            i.putExtra("RELATIONSHIP", relationship)
            startActivity(i)
        } else {
            val i = Intent(this, HealthStatusActivity::class.java)
            i.putExtra("MEMBER_ID", memberId)
            i.putExtra("RELATIONSHIP", relationship)
            i.putExtra("SIDE", "None")
            startActivity(i)
        }
    }

    inner class RelationshipAdapter(
        private val items: List<Relationship>,
        private val onItemSelected: (Int) -> Unit
    ) : RecyclerView.Adapter<RelationshipAdapter.ViewHolder>() {

        private var lastSelectedPos = -1

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvIcon: TextView = view.findViewById(R.id.tv_relationship_icon)
            val ivIcon: ImageView = view.findViewById(R.id.iv_relationship_icon)
            val tvLabel: TextView = view.findViewById(R.id.tv_relationship_label)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_relationship, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvLabel.text = item.label

            if (item.imageRes != null) {
                holder.tvIcon.visibility = View.GONE
                holder.ivIcon.visibility = View.VISIBLE
                holder.ivIcon.setImageResource(item.imageRes)
            } else {
                holder.tvIcon.visibility = View.VISIBLE
                holder.ivIcon.visibility = View.GONE
                holder.tvIcon.text = item.icon
            }

            holder.itemView.isSelected = (position == lastSelectedPos)

            holder.itemView.setOnClickListener {
                val prev = lastSelectedPos
                lastSelectedPos = holder.adapterPosition
                notifyItemChanged(prev)
                notifyItemChanged(lastSelectedPos)
                onItemSelected(lastSelectedPos)
            }
        }

        override fun getItemCount() = items.size
    }
}