package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class RecentAnalysesResponse(
    val status: Boolean,
    val results: List<RecentAnalysisItem> = emptyList(),
    val error: String? = null
)

data class RecentAnalysisItem(
    @SerializedName("analysis_id") val analysisId: Int,
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("patient_name") val patientName: String,
    @SerializedName("pattern_title") val patternTitle: String,
    @SerializedName("confidence") val confidence: Double,
    @SerializedName("created_at") val createdAt: String
)
