package com.simats.genetics

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.requests.ShareReportRequest
import com.simats.genetics.network.responses.ApiResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ShareReportActivity : AppCompatActivity() {

    private lateinit var etRecipientEmail: EditText
    private lateinit var etMessage: EditText

    private var analysisId: Int = 0
    private var loader: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_report)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        analysisId = intent.getIntExtra("ANALYSIS_ID", 0)
        if (analysisId == 0) {
            Toast.makeText(this, "ANALYSIS_ID missing", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        loader = findViewById(R.id.loader)

        etRecipientEmail = findViewById(R.id.et_recipient_email)
        etMessage = findViewById(R.id.et_message)

        setupQuickContacts()

        findViewById<View>(R.id.btn_cancel).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btn_send_report).setOnClickListener {
            sendReport()
        }

        findViewById<View>(R.id.btn_logout).setOnClickListener {
            com.simats.genetics.network.TokenManager.clearToken(this)
            val intent = android.content.Intent(this, MainActivity::class.java)
            intent.addFlags(
                android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            )
            startActivity(intent)
            finish()
        }
    }

    private fun sendReport() {
        val email = etRecipientEmail.text.toString().trim()
        val message = etMessage.text.toString().trim()

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid recipient email", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        val request = ShareReportRequest(
            recipient_email = email,
            message = message
        )

        ApiClient.getApi(this).shareReport(analysisId, request)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(
                    call: Call<ApiResponse>,
                    response: Response<ApiResponse>
                ) {
                    showLoading(false)

                    if (!response.isSuccessful) {
                        Toast.makeText(
                            this@ShareReportActivity,
                            "Failed (${response.code()})",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    val body = response.body()
                    if (body?.status == true) {
                        findViewById<View>(R.id.cv_success_banner).visibility = View.VISIBLE
                        Toast.makeText(
                            this@ShareReportActivity,
                            "Report shared successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@ShareReportActivity,
                            body?.message ?: "Share failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(
                        this@ShareReportActivity,
                        t.message ?: "Network error",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun showLoading(isLoading: Boolean) {
        loader?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun setupQuickContacts() {

        findViewById<View>(R.id.contact_sarah).apply {
            val name = "Dr. Sarah Johnson"
            val email = "sarah.johnson@clinic.com"
            val role = "Genetic Counselor"

            findViewById<TextView>(R.id.tv_contact_name).text = name
            findViewById<TextView>(R.id.tv_contact_email).text = email
            findViewById<TextView>(R.id.tv_contact_role).text = role

            setOnClickListener { etRecipientEmail.setText(email) }
        }

        findViewById<View>(R.id.contact_michael).apply {
            val name = "Dr. Michael Chen"
            val email = "michael.chen@hospital.com"
            val role = "Clinical Geneticist"

            findViewById<TextView>(R.id.tv_contact_name).text = name
            findViewById<TextView>(R.id.tv_contact_email).text = email
            findViewById<TextView>(R.id.tv_contact_role).text = role

            setOnClickListener { etRecipientEmail.setText(email) }
        }
    }
}