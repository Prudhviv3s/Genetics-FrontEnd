package com.simats.genetics.network.responses

data class ClinicalNotesListResponse(
    val status: Boolean,
    val count: Int = 0,
    val notes: List<ClinicalNoteResponse> = emptyList(),
    val message: String? = null
)

data class ClinicalNoteResponse(
    val id: Int,
    val observations: String? = null,
    val recommendations: String? = null,
    val created_at: String? = null
)