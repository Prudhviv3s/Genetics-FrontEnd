package com.simats.genetics

import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.responses.ExportInfoResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ExportReportActivity : AppCompatActivity() {

    private var analysisId: Int = 0
    private var loader: View? = null

    // optional: show report info on UI if you have these ids
    private var tvReportType: TextView? = null
    private var tvFormat: TextView? = null
    private var tvSize: TextView? = null
    private var tvGenerated: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_report)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        analysisId = intent.getIntExtra("ANALYSIS_ID", 0)
        if (analysisId == 0) {
            Toast.makeText(this, "ANALYSIS_ID missing", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        loader = findViewById(R.id.loader)

        // if your XML has these ids, it will auto fill, else it's fine
        tvReportType = findViewById(R.id.tv_report_type)
        tvFormat = findViewById(R.id.tv_report_format)
        tvSize = findViewById(R.id.tv_report_size)
        tvGenerated = findViewById(R.id.tv_report_generated)

        setupExportMethods()
        loadExportInfo()

        findViewById<View>(R.id.btn_cancel).setOnClickListener { finish() }
    }

    private fun setupExportMethods() {
        // Download
        findViewById<View>(R.id.card_download).apply {
            findViewById<TextView>(R.id.tv_method_title).text = "Download to Device"
            findViewById<TextView>(R.id.tv_method_description).text = "Save PDF to your device"
            findViewById<ImageView>(R.id.iv_method_icon).setImageResource(R.drawable.ic_download)
            setOnClickListener { downloadPdf() }
        }

        // Email
        findViewById<View>(R.id.card_email).apply {
            findViewById<TextView>(R.id.tv_method_title).text = "Send via Email"
            findViewById<TextView>(R.id.tv_method_description).text = "Email report to yourself or others"
            findViewById<ImageView>(R.id.iv_method_icon).setImageResource(R.drawable.ic_email)
            setOnClickListener { openShareScreen(mode = "email") }
        }

        // Print
        findViewById<View>(R.id.card_print).apply {
            findViewById<TextView>(R.id.tv_method_title).text = "Print Report"
            findViewById<TextView>(R.id.tv_method_description).text = "Print physical copy"
            findViewById<ImageView>(R.id.iv_method_icon).setImageResource(R.drawable.ic_print)
            setOnClickListener {
                Toast.makeText(this@ExportReportActivity, "Printing not implemented yet", Toast.LENGTH_SHORT).show()
            }
        }

        // Share
        findViewById<View>(R.id.card_share).apply {
            findViewById<TextView>(R.id.tv_method_title).text = "Share Report"
            findViewById<TextView>(R.id.tv_method_description).text = "Share with healthcare provider"
            findViewById<ImageView>(R.id.iv_method_icon).setImageResource(R.drawable.ic_share)
            setOnClickListener { openShareScreen(mode = "share") }
        }
    }

    private fun loadExportInfo() {
        showLoading(true)
        ApiClient.getApi(this).getExportInfo(analysisId).enqueue(object : Callback<ExportInfoResponse> {
            override fun onResponse(
                call: Call<ExportInfoResponse>,
                response: Response<ExportInfoResponse>
            ) {
                showLoading(false)
                if (!response.isSuccessful) {
                    Toast.makeText(this@ExportReportActivity, "Failed (${response.code()})", Toast.LENGTH_LONG).show()
                    return
                }

                val body = response.body()
                val exportData = body?.export
                if (body?.status != true || exportData == null) {
                    Toast.makeText(this@ExportReportActivity, body?.error ?: "No info", Toast.LENGTH_LONG).show()
                    return
                }

                // Fill if textviews exist
                tvReportType?.text = exportData.report_type
                tvFormat?.text = exportData.format
                tvSize?.text = exportData.file_size
                tvGenerated?.text = exportData.generated
            }

            override fun onFailure(call: Call<ExportInfoResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@ExportReportActivity, t.message ?: "Network error", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun downloadPdf() {
        val url = ApiClient.BASE_URL + "reports/download/?analysis_id=$analysisId"
        // NOTE: If BASE_URL already ends with /api/, above works.
        // If not, just hardcode your full endpoint.

        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Genetic_Report_$analysisId.pdf")
                .setDescription("Downloading report...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Genetic_Report_$analysisId.pdf")

            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)

            Toast.makeText(this, "Download started ", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "Download failed", Toast.LENGTH_LONG).show()
        }
    }

    private fun openShareScreen(mode: String) {
        val i = Intent(this, ShareReportActivity::class.java)
        i.putExtra("ANALYSIS_ID", analysisId)
        i.putExtra("MODE", mode) // optional
        startActivity(i)
    }

    private fun showLoading(isLoading: Boolean) {
        loader?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}