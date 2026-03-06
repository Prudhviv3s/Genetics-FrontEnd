package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class PatientHomeResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("dashboard") val dashboard: PatientDashboard?
)

data class PatientDashboard(
    @SerializedName("family_members") val familyMembers: Int,
    @SerializedName("generations") val generations: Int,
    @SerializedName("active_traits") val activeTraits: Int,
    @SerializedName("pending_results") val pendingResults: Int
)