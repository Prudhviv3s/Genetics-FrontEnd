package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class PredictPdfResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("patterns") val patterns: List<PatternProbabilityDto>? = null,
    @SerializedName("extracted") val extracted: Map<String, Any>? = null,
    @SerializedName("error") val error: String? = null
)

data class PatternProbabilityDto(
    @SerializedName("name") val name: String,
    @SerializedName("score") val score: Int
)
