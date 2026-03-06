package com.simats.genetics

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.responses.MarkNotificationReadResponse
import com.simats.genetics.network.responses.NotificationItem
import com.simats.genetics.network.responses.NotificationsListResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationsActivity : AppCompatActivity() {

    private lateinit var cardAnalysis: View
    private lateinit var cardFamily: View
    private lateinit var cardPedigree: View
    private lateinit var cardAppointment: View
    private lateinit var cardReview: View

    private lateinit var filterAll: TextView
    private lateinit var filterUnread: TextView

    private val cards by lazy {
        listOf(cardAnalysis, cardFamily, cardPedigree, cardAppointment, cardReview)
    }

    private var currentTab = "all" // "all" or "unread"
    private var latestNotifications: List<NotificationItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // cards
        cardAnalysis = findViewById(R.id.card_analysis)
        cardFamily = findViewById(R.id.card_family)
        cardPedigree = findViewById(R.id.card_pedigree)
        cardAppointment = findViewById(R.id.card_appointment)
        cardReview = findViewById(R.id.card_review)

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
                        latestNotifications = body.notifications
                        renderToCards(latestNotifications)
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
    // SHOW DATA IN YOUR 5 CARDS
    // =========================
    private fun renderToCards(notifs: List<NotificationItem>) {
        // hide all first
        cards.forEach { it.visibility = View.GONE }

        // show max 5
        val showList = notifs.take(5)

        showList.forEachIndexed { index, notif ->
            val card = cards[index]
            card.visibility = View.VISIBLE
            bindCard(card, notif)

            // click -> mark read
            card.setOnClickListener {
                if (!notif.isRead) markRead(notif.id)
                // later you can open screen based on notifType/data
            }
        }
    }

    private fun bindCard(card: View, notif: NotificationItem) {
        val viewGroup = card as? android.view.ViewGroup
        if (viewGroup == null || viewGroup.childCount == 0) return
        val root = viewGroup.getChildAt(0) as? android.widget.RelativeLayout ?: return

        if (root.childCount < 2) return
        val contentLayout = root.getChildAt(1) as? android.widget.LinearLayout ?: return

        if (contentLayout.childCount < 3) return
        val titleLayout = contentLayout.getChildAt(0) as? android.widget.RelativeLayout ?: return
        val tvMsg = contentLayout.getChildAt(1) as? android.widget.TextView ?: return
        val tvTime = contentLayout.getChildAt(2) as? android.widget.TextView ?: return

        if (titleLayout.childCount < 2) return
        val tvTitle = titleLayout.getChildAt(0) as? android.widget.TextView ?: return
        val dot = titleLayout.getChildAt(1) as? android.view.View ?: return

        tvTitle.text = notif.title ?: "-"
        tvMsg.text = notif.message ?: ""
        tvTime.text = notif.createdAt ?: ""  // if backend sends created_at, else blank

        dot.visibility = if (notif.isRead) View.INVISIBLE else View.VISIBLE
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