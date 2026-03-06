package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class DoctorPatientDetailResponse(
    val status: Boolean,
    val patient: DoctorPatientResponse?
)