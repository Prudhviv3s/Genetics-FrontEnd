// DoctorReportPatientsListResponse.kt  (NEW)
package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class DoctorReportPatientDto(
    val id: Int,

    @SerializedName("patient_id")
    val patientId: String,

    @SerializedName("full_name")
    val fullName: String,

    val email: String,
    val age: Int,

    @SerializedName("report_ready")
    val reportReady: Boolean,

    @SerializedName("inheritance_pattern")
    val inheritancePattern: String,

    @SerializedName("last_analysis")
    val lastAnalysis: String?, // null => Pending

    @SerializedName("analysis_id")
    val analysisId: Int? // Add analysisId
)

data class DoctorReportPatientsListResponse(
    val status: Boolean,
    val count: Int,
    val patients: List<DoctorReportPatientDto>
)

fun DoctorReportPatientDto.toDoctorReportPatientItem() = DoctorReportPatientItem(
    id = id,
    patientId = patientId,
    fullName = fullName,
    age = age,
    inheritancePattern = inheritancePattern,
    lastAnalysis = lastAnalysis,
    analysisId = analysisId
)
