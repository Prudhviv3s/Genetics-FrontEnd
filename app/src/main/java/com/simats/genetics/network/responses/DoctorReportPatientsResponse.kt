package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class DoctorReportPatientsResponse(
    val status: Boolean,
    val count: Int = 0,
    val patients: List<DoctorReportPatientItem> = emptyList()
)

data class DoctorReportPatientItem(
    val id: Int,
    @SerializedName("patient_id") val patientId: String?,          // like "PT1001"
    @SerializedName("full_name") val fullName: String?,
    val age: Int? = null,
    @SerializedName("report_ready") val reportReady: Boolean = false,
    @SerializedName("inheritance_pattern") val inheritancePattern: String? = null,
    @SerializedName("last_analysis") val lastAnalysis: String? = null, // "2026-02-10"
    @SerializedName("analysis_id") val analysisId: Int? = null
)