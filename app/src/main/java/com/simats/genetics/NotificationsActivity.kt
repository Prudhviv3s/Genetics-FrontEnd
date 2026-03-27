package com.simats.genetics

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.TokenManager
import com.simats.genetics.network.responses.MarkNotificationReadResponse
import com.simats.genetics.network.responses.NotificationItem
import com.simats.genetics.network.responses.NotificationsListResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationsActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var tvCount: TextView
    private lateinit var filterAll: TextView
    private lateinit var filterUnread: TextView

    private lateinit var adapter: NotificationAdapter
    private var currentTab = "all" // "all" or "unread"
    private val notificationList = mutableListOf<NotificationItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        tvCount = findViewById(R.id.notifications_count)
        rvNotifications = findViewById(R.id.rv_notifications)
        rvNotifications.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(notificationList)
        rvNotifications.adapter = adapter

        // filters
        filterAll = findViewById(R.id.filter_all)
        filterUnread = findViewById(R.id.filter_unread)

        // default selected
        selectFilter(all = true)

        filterAll.setOnClickListener {
            selectFilter(all = true)
            currentTab = "all"
            loadNotifications()
        }

        filterUnread.setOnClickListener {
            selectFilter(all = false)
            currentTab = "unread"
            loadNotifications()
        }

        loadNotifications()
    }

    private fun selectFilter(all: Boolean) {
        if (all) {
            filterAll.isSelected = true
            filterAll.setTextColor(Color.WHITE)

            filterUnread.isSelected = false
            filterUnread.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        } else {
            filterUnread.isSelected = true
            filterUnread.setTextColor(Color.WHITE)

            filterAll.isSelected = false
            filterAll.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }
    }

    // =========================
    // LOAD NOTIFICATIONS
    // =========================
    private fun loadNotifications() {
        ApiClient.getApi(this)
            .getNotifications(currentTab)
            .enqueue(object : Callback<NotificationsListResponse> {
                override fun onResponse(
                    call: Call<NotificationsListResponse>,
                    response: Response<NotificationsListResponse>
                ) {
                    if (!response.isSuccessful) {
                        Log.e("NOTIF", "HTTP ${response.code()} err=${response.errorBody()?.string()}")
                        Toast.makeText(this@NotificationsActivity, "HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val body = response.body()
                    if (body?.status == true) {
                        notificationList.clear()
                        notificationList.addAll(body.notifications)
                        adapter.notifyDataSetChanged()

                        // Update count text
                        val count = if (currentTab == "unread") body.notifications.size else body.newCount
                        tvCount.text = if (count > 0) "$count New Notifications" else "No New Notifications"
                    } else {
                        Toast.makeText(this@NotificationsActivity, "Failed to load", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<NotificationsListResponse>, t: Throwable) {
                    Log.e("NOTIF", "FAIL ${t.message}", t)
                    Toast.makeText(this@NotificationsActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // =========================
    // RECYCLER ADAPTER
    // =========================
    inner class NotificationAdapter(private val list: List<NotificationItem>) :
        RecyclerView.Adapter<NotificationAdapter.NotifViewHolder>() {

        inner class NotifViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val ivIcon: ImageView = v.findViewById(R.id.iv_notif_icon)
            val iconContainer: CardView = v.findViewById(R.id.icon_container)
            val tvTitle: TextView = v.findViewById(R.id.tv_notif_title)
            val tvMsg: TextView = v.findViewById(R.id.tv_notif_message)
            val tvTime: TextView = v.findViewById(R.id.tv_notif_time)
            val dot: View = v.findViewById(R.id.unread_dot)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
            return NotifViewHolder(v)
        }

        override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
            val item = list[position]
            holder.tvTitle.text = item.title ?: "Notification"
            holder.tvMsg.text = item.message ?: ""
            holder.tvTime.text = item.timeAgo ?: item.createdAt ?: ""
            holder.dot.visibility = if (item.isRead) View.INVISIBLE else View.VISIBLE

            // customize icon based on type (backend types are lowercase)
            when (item.notifType?.lowercase()) {
                "analysis" -> {
                    holder.ivIcon.setImageResource(R.drawable.ic_document)
                    holder.iconContainer.setCardBackgroundColor(Color.parseColor("#F0F4FF"))
                }
                "family" -> {
                    holder.ivIcon.setImageResource(R.drawable.ic_family_members)
                    holder.iconContainer.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                }
                "pedigree" -> {
                    holder.ivIcon.setImageResource(R.drawable.ic_check_circle)
                    holder.iconContainer.setCardBackgroundColor(Color.parseColor("#F3E5F5"))
                }
                "appointment" -> {
                    holder.ivIcon.setImageResource(R.drawable.ic_calendar)
                    holder.iconContainer.setCardBackgroundColor(Color.parseColor("#FFF3E0"))
                }
                "review" -> {
                    holder.ivIcon.setImageResource(R.drawable.ic_info)
                    holder.iconContainer.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
                }
                else -> {
                    holder.ivIcon.setImageResource(R.drawable.ic_notifications)
                    holder.iconContainer.setCardBackgroundColor(Color.parseColor("#F5F5F5"))
                }
            }

            holder.itemView.setOnClickListener {
                if (!item.isRead) markRead(item.id)
                handleNavigation(item)
            }
        }

        override fun getItemCount() = list.size
    }

    private fun handleNavigation(item: NotificationItem) {
        val type = item.notifType?.lowercase()
        val data = item.data ?: emptyMap<String, Any>()
        val role = TokenManager.getRole(this) ?: "Patient"
        val isDoctor = role.equals("Doctor", ignoreCase = true)

        when (type) {
            "analysis" -> {
                val analysisId = (data["analysis_id"] as? Number)?.toInt() ?: 0
                val patientId = (data["patient_id"] as? Number)?.toInt() ?: 0
                val patientName = data["patient_name"] as? String ?: "Unknown"
                val displayId = data["patient_display_id"] as? String ?: ""

                if (isDoctor) {
                    // Doctors go to DoctorReportDetailActivity
                    val intent = Intent(this, DoctorReportDetailActivity::class.java)
                    intent.putExtra("ANALYSIS_ID", analysisId)
                    intent.putExtra("PATIENT_ID", patientId)
                    intent.putExtra("PATIENT_NAME", patientName)
                    intent.putExtra("PATIENT_DISPLAY_ID", displayId)
                    startActivity(intent)
                } else {
                    // Patients go to AnalysisActivity
                    if (analysisId > 0) {
                        val intent = Intent(this, AnalysisActivity::class.java)
                        intent.putExtra("ANALYSIS_ID", analysisId)
                        startActivity(intent)
                    }
                }
            }
            "family", "pedigree" -> {
                // Navigate to Family Overview
                val intent = Intent(this, FamilyOverviewActivity::class.java)
                startActivity(intent)
            }
            "general" -> {
                val screen = data["screen"] as? String
                if (screen == "my_results") {
                    val intent = Intent(this, AnalysisActivity::class.java)
                    startActivity(intent)
                } else if (screen == "patient_detail" && isDoctor) {
                    // New Patient Registration notification for Doctors
                    val patientId = (data["patient_id"] as? Number)?.toInt() ?: 0
                    val patientName = data["patient_name"] as? String ?: "Unknown"
                    val patientEmail = data["patient_email"] as? String ?: ""
                    val displayId = data["patient_display_id"] as? String ?: ""
                    
                    val intent = Intent(this, PatientDetailsActivity::class.java)
                    intent.putExtra("PATIENT_ID", patientId)
                    intent.putExtra("PATIENT_NAME", patientName)
                    intent.putExtra("PATIENT_EMAIL", patientEmail)
                    intent.putExtra("PATIENT_DISPLAY_ID", displayId)
                    startActivity(intent)
                }
            }
        }
    }

    // =========================
    // MARK SINGLE NOTIFICATION READ
    // =========================
    private fun markRead(notifId: Int) {
        ApiClient.getApi(this)
            .markNotificationRead(notifId)
            .enqueue(object : Callback<MarkNotificationReadResponse> {
                override fun onResponse(
                    call: Call<MarkNotificationReadResponse>,
                    response: Response<MarkNotificationReadResponse>
                ) {
                    if (!response.isSuccessful) {
                        Toast.makeText(this@NotificationsActivity, "Failed to mark read", Toast.LENGTH_SHORT).show()
                        return
                    }
                    // refresh current tab
                    loadNotifications()
                }

                override fun onFailure(call: Call<MarkNotificationReadResponse>, t: Throwable) {
                    Toast.makeText(this@NotificationsActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}