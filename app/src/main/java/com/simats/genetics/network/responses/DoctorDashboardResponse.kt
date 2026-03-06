package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class DoctorDashboardResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("doctor_name") val doctorName: String?,
    @SerializedName("stats") val stats: DoctorStats?
)

data class DoctorStats(
    @SerializedName("active_patients") val activePatients: Int,
    @SerializedName("pending_analysis") val pendingAnalysis: Int,
    @SerializedName("total_pedigrees") val totalPedigrees: Int,
    @SerializedName("reports_generated") val reportsGenerated: Int
)