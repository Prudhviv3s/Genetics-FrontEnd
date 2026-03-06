package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class PatientDto(
    val id: Int,

    @SerializedName("patient_id")
    val patientCode: String, // e.g. "P1" or "#P1" depending backend

    @SerializedName("full_name")
    val fullName: String,

    val email: String,
    val age: Int,
    val gender: String
)

data class PatientsListResponse(
    val status: Boolean,
    val message: String,
    val data: List<PatientDto>
)
