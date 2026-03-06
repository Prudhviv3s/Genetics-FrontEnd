package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class DoctorPatientResponse(
    val id: Int,

    @SerializedName("patient_id")
    val patientId: String,

    @SerializedName("full_name")
    val fullName: String,

    val email: String,
    val age: Int,
    val gender: String
)