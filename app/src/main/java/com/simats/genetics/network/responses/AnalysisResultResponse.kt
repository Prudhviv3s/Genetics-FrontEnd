package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class AnalysisResultResponse(
    val status: Boolean,
    val result: AnalysisResultData? = null,
    val error: String? = null
)

data class AnalysisResultData(
    @SerializedName("analysis_id") val analysisId: Int,
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("patient_name") val patientName: String,
    @SerializedName("primary_pattern") val primaryPattern: PatternItem,
    @SerializedName("alternatives") val alternatives: List<PatternItem> = emptyList(),
    @SerializedName("summary_text") val summaryText: String? = null
)

data class PatternItem(
    @SerializedName("title") val title: String,
    @SerializedName("confidence") val confidence: Double, // 0.78
    @SerializedName("confidence_label") val confidenceLabel: String? = null // "High Confidence"
)