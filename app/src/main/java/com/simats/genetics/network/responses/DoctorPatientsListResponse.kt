package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class DoctorPatientsListResponse(

    @SerializedName("status")
    val status: Boolean,

    @SerializedName("patients")
    val patients: List<DoctorPatientResponse>
)