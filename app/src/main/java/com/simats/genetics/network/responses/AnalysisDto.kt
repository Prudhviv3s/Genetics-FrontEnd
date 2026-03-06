package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class AnalysisDto(
    val id: Int? = null,
    @SerializedName("inheritance_pattern") val inheritancePattern: String? = null,
    val confidence: Int? = null,
    val description: String? = null,
    @SerializedName("analysis_date") val analysisDate: String? = null,

    // Your backend sends "pattern_probabilities"
    @SerializedName("pattern_probabilities")
    val patternProbabilities: Map<String, Int>? = null
)
