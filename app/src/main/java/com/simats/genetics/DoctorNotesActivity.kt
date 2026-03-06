package com.simats.genetics

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.genetics.databinding.ActivityDoctorNotesBinding
import com.simats.genetics.databinding.ItemPreviousNoteBinding
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.requests.ClinicalNoteCreateRequest
import com.simats.genetics.network.responses.ClinicalNoteCreateResponse
import com.simats.genetics.network.responses.ClinicalNoteResponse
import com.simats.genetics.network.responses.ClinicalNotesListResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DoctorNotesActivity : AppCompatActivity() {

    private var patientId: Int = 0
    private val notesList = mutableListOf<ClinicalNoteResponse>()
    private lateinit var adapter: NotesAdapter
    private lateinit var binding: ActivityDoctorNotesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        patientId = intent.getIntExtra("PATIENT_ID", 0)

        adapter = NotesAdapter(notesList)
        binding.rvPreviousNotes.layoutManager = LinearLayoutManager(this)
        binding.rvPreviousNotes.adapter = adapter

        binding.btnCancel.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnSave.setOnClickListener {

            val observations = binding.etObservations.text.toString().trim()
            val recommendations = binding.etRecommendations.text.toString().trim()

            if (observations.isEmpty() || recommendations.isEmpty()) {
                Toast.makeText(this, "Fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveClinicalNote(observations, recommendations) {
                binding.etObservations.setText("")
                binding.etRecommendations.setText("")
                loadNotes()
            }
        }

        loadNotes()
    }

    // =========================
    // LOAD PREVIOUS NOTES
    // =========================
    private fun loadNotes() {
        if (patientId == 0) return

        ApiClient.getApi(this)
            .getClinicalNotes(patientId)
            .enqueue(object : Callback<ClinicalNotesListResponse> {

                override fun onResponse(
                    call: Call<ClinicalNotesListResponse>,
                    response: Response<ClinicalNotesListResponse>
                ) {
                    if (!response.isSuccessful) {
                        Log.e("NOTES", "HTTP ${response.code()}")
                        return
                    }

                    val body = response.body()
                    if (body?.status == true) {
                        notesList.clear()
                        notesList.addAll(body.notes)
                        adapter.notifyDataSetChanged()
                    }
                }

                override fun onFailure(call: Call<ClinicalNotesListResponse>, t: Throwable) {
                    Log.e("NOTES", "Error ${t.message}")
                }
            })
    }

    // =========================
    // SAVE NOTE
    // =========================
    private fun saveClinicalNote(
        observations: String,
        recommendations: String,
        onSuccess: () -> Unit
    ) {

        val request = ClinicalNoteCreateRequest(
            observations = observations,
            recommendations = recommendations
        )

        ApiClient.getApi(this)
            .createClinicalNote(patientId, request)
            .enqueue(object : Callback<ClinicalNoteCreateResponse> {

                override fun onResponse(
                    call: Call<ClinicalNoteCreateResponse>,
                    response: Response<ClinicalNoteCreateResponse>
                ) {
                    if (!response.isSuccessful) {
                        Toast.makeText(this@DoctorNotesActivity, "Failed to save", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val body = response.body()
                    if (body?.status == true) {
                        Toast.makeText(this@DoctorNotesActivity, "Clinical notes saved successfully", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    } else {
                        Toast.makeText(this@DoctorNotesActivity, body?.message ?: "Error", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ClinicalNoteCreateResponse>, t: Throwable) {
                    Toast.makeText(this@DoctorNotesActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // =========================
    // ADAPTER
    // =========================
    inner class NotesAdapter(private val items: List<ClinicalNoteResponse>) :
        RecyclerView.Adapter<NotesAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemPreviousNoteBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemPreviousNoteBinding.inflate(layoutInflater, parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val note = items[position]

            holder.binding.tvNoteTime.text = note.created_at ?: "-"

            holder.binding.tvNoteContent.text = "${note.observations ?: ""}\n\n${note.recommendations ?: ""}"
        }

        override fun getItemCount() = items.size
    }
}
