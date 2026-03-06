package com.simats.genetics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class DoctorReportShareActivity : AppCompatActivity() {

    private var pdfUrl: String? = null
    private var analysisId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_report_share)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Retrieve data from intent
        pdfUrl = intent.getStringExtra("PDF_URL")
        analysisId = intent.getIntExtra("ANALYSIS_ID", 0)

        val btnWhatsapp = findViewById<LinearLayout>(R.id.btn_share_whatsapp)
        val btnEmail = findViewById<LinearLayout>(R.id.btn_share_email)
        val btnCopy = findViewById<LinearLayout>(R.id.btn_copy_link)
        val btnMore = findViewById<MaterialButton>(R.id.btn_more_options)

        btnWhatsapp.setOnClickListener { shareToWhatsApp() }
        btnEmail.setOnClickListener { shareToEmail() }
        btnCopy.setOnClickListener { copyToClipboard() }

        // "More Options" navigates to Export Report page
        btnMore.setOnClickListener {
            val intent = Intent(this, DoctorReportExportActivity::class.java)
            intent.putExtra("ANALYSIS_ID", analysisId)
            intent.putExtra("PDF_URL", pdfUrl)
            startActivity(intent)
        }

        if (pdfUrl.isNullOrBlank()) {
            Toast.makeText(this, "Report URL is not available for direct sharing.", Toast.LENGTH_LONG).show()
            btnWhatsapp.isEnabled = false
            btnEmail.isEnabled = false
            btnCopy.isEnabled = false

            // Visual feedback for disabled buttons
            btnWhatsapp.alpha = 0.5f
            btnEmail.alpha = 0.5f
            btnCopy.alpha = 0.5f
        }
    }

    private fun shareToWhatsApp() {
        val url = pdfUrl ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Inheritance Pattern Report: $url")
            setPackage("com.whatsapp")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp not installed.", Toast.LENGTH_SHORT).show()
            // Fallback to generic chooser
            val genericIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
            }
            startActivity(Intent.createChooser(genericIntent, "Share Report"))
        }
    }

    private fun shareToEmail() {
        val url = pdfUrl ?: return
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, "Inheritance Pattern Report")
            putExtra(Intent.EXTRA_TEXT, "Please find the report link here: $url")
        }
        startActivity(Intent.createChooser(intent, "Send Email"))
    }

    private fun copyToClipboard() {
        val url = pdfUrl ?: return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Report URL", url)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}
