package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class DoctorPatientReportDetailResponse(
    val status: Boolean,
    val patient: PatientReportInfo? = null,
    val inheritance: InheritanceInfo? = null,
    @SerializedName("pedigree_stats") val pedigreeStats: PedigreeStats? = null,
    val report: ReportInfo? = null
)

data class PatientReportInfo(
    val id: Int,
    @SerializedName("patient_id") val patientId: String,
    @SerializedName("full_name") val fullName: String,
    val age: Int?,
    val gender: String?,
    @SerializedName("analysis_date") val analysisDate: String?
)

data class InheritanceInfo(
    val pattern: String,
    val confidence: Int,
    @SerializedName("confidence_label") val confidenceLabel: String,
    val description: String,
    @SerializedName("pattern_probabilities") val patternProbabilities: Map<String, Int>?
)

data class PedigreeStats(
    @SerializedName("family_members") val familyMembers: Int,
    val affected: Int,
    val generations: Int
)

data class ReportInfo(
    val title: String,
    val summary: String,
    @SerializedName("generated_date") val generatedDate: String?
)
