package com.simats.genetics

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.requests.RunPatternDetectionRequest
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class DoctorDnaReportActivity : AppCompatActivity() {

    private var selectedPdfUri: Uri? = null
    private var selectedPdfName: String? = null

    // Your layout should have a loader view with id "loader" (ProgressBar or full-screen overlay)
    private var loader: View? = null

    private val pickPdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Some devices may throw, it's okay
            }

            selectedPdfUri = uri
            selectedPdfName = getFileName(uri) ?: "dna_report.pdf"

            Toast.makeText(
                this,
                "PDF selected: $selectedPdfName\nNow click 'Run Pattern Detection'",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(this, "No PDF selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_dna_report)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        loader = findViewById(R.id.loader)

        val btnUpload = findViewById<MaterialCardView>(R.id.btn_upload)
        val btnScan = findViewById<MaterialCardView>(R.id.btn_scan)
        val btnRun = findViewById<MaterialButton>(R.id.btn_run_detection)

        btnUpload.setOnClickListener { openPdfPicker() }

        btnScan.setOnClickListener {
            Toast.makeText(this, "Camera scan not implemented yet", Toast.LENGTH_SHORT).show()
        }

        btnRun.setOnClickListener {
            val uri = selectedPdfUri
            if (uri == null) {
                Toast.makeText(this, "Please upload a DNA PDF first", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            uploadThenRunDetection(uri)
        }
    }

    private fun openPdfPicker() {
        pickPdf.launch(arrayOf("application/pdf"))
    }

    private fun uploadThenRunDetection(uri: Uri) {
        val patientId = intent.getIntExtra("PATIENT_ID", 0)
        val patientName = intent.getStringExtra("PATIENT_NAME") ?: ""
        val patientDisplayId = intent.getStringExtra("PATIENT_DISPLAY_ID") ?: ""

        if (patientId <= 0) {
            Toast.makeText(this, "Invalid patient id. Please open this page from patient details.", Toast.LENGTH_LONG).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@DoctorDnaReportActivity)

                // 1) Convert Uri -> temp file
                val pdfFile = uriToCacheFile(uri, selectedPdfName ?: "dna_report.pdf")

                // 2) Build multipart
                val fileBody = pdfFile.asRequestBody("application/pdf".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", pdfFile.name, fileBody)

                val patientIdBody: RequestBody =
                    patientId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                // 3) Upload
                val uploadRes = api.uploadDnaReportSuspend(filePart, patientIdBody)

                if (!uploadRes.isSuccessful) {
                    val err = uploadRes.errorBody()?.string()
                    Log.e("DNA_UPLOAD", "HTTP ${uploadRes.code()} err=$err")
                    Toast.makeText(
                        this@DoctorDnaReportActivity,
                        "Upload failed (HTTP ${uploadRes.code()})",
                        Toast.LENGTH_LONG
                    ).show()
                    showLoading(false)
                    return@launch
                }

                val uploadBody = uploadRes.body()
                if (uploadBody?.status != true || uploadBody.upload?.id == null) {
                    Toast.makeText(
                        this@DoctorDnaReportActivity,
                        uploadBody?.message ?: "Upload failed",
                        Toast.LENGTH_LONG
                    ).show()
                    showLoading(false)
                    return@launch
                }

                val uploadId = uploadBody.upload.id
                Toast.makeText(
                    this@DoctorDnaReportActivity,
                    "Upload complete. Running detection...",
                    Toast.LENGTH_SHORT
                ).show()

                // 4) Run detection
                val runReq = RunPatternDetectionRequest(
                    patientId = patientId,
                    dnaUploadId = uploadId
                )

                val runRes = api.runPatternDetectionSuspend(runReq)
                if (!runRes.isSuccessful) {
                    val err = runRes.errorBody()?.string()
                    Log.e("DNA_DETECT", "HTTP ${runRes.code()} err=$err")
                    Toast.makeText(
                        this@DoctorDnaReportActivity,
                        "Detection failed (HTTP ${runRes.code()})",
                        Toast.LENGTH_LONG
                    ).show()
                    showLoading(false)
                    return@launch
                }

                val runBody = runRes.body()
                if (runBody?.status != true) {
                    Toast.makeText(
                        this@DoctorDnaReportActivity,
                        runBody?.message ?: "Detection failed",
                        Toast.LENGTH_LONG
                    ).show()
                    showLoading(false)
                    return@launch
                }

                // ✅ SUCCESS → Navigate to Inheritance Pattern Page
                val i = Intent(this@DoctorDnaReportActivity, DoctorInheritancePatternActivity::class.java)
                i.putExtra("PATIENT_NAME", patientName)
                i.putExtra("PATIENT_ID", patientId)
                i.putExtra("PATIENT_DISPLAY_ID", patientDisplayId)

                // Pass the data needed by DoctorInheritancePatternActivity
                val result = runBody.result
                i.putExtra("ANALYSIS_ID", result?.id ?: 0)
                i.putExtra("TOP_PATTERN", result?.inheritancePattern ?: "Unknown")
                i.putExtra("TOP_CONFIDENCE", result?.confidence ?: 0)

                // Format probabilities as expected by DoctorInheritancePatternActivity
                val probsString = result?.patternProbabilities?.entries?.joinToString(";") { "${it.key}:${it.value}" } ?: ""
                i.putExtra("PATTERN_PROBS", probsString)

                startActivity(i)

            } catch (e: Exception) {
                Log.e("DNA_FLOW", "Exception ${e.message}", e)
                Toast.makeText(
                    this@DoctorDnaReportActivity,
                    e.message ?: "Network error",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun uriToCacheFile(uri: Uri, fallbackName: String): File {
        val safeName = fallbackName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val outFile = File(cacheDir, "upload_${System.currentTimeMillis()}_$safeName")

        contentResolver.openInputStream(uri).use { input ->
            if (input == null) throw IllegalStateException("Cannot open selected PDF")
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return outFile
    }

    private fun getFileName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun showLoading(isLoading: Boolean) {
        loader?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
