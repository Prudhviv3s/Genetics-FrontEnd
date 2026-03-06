package com.simats.genetics.network.responses

data class ClinicalNoteCreateResponse(
    val status: Boolean,
    val message: String? = null,
    val note: ClinicalNoteResponse? = null,
    val errors: Any? = null
)
