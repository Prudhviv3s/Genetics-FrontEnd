package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class LatestAnalysisDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("inheritance_pattern") val inheritancePattern: String? = null,
    @SerializedName("confidence") val confidence: Int? = null,
    @SerializedName("description") val description: String? = null,

    @SerializedName("pattern_probabilities")
    val patternProbabilities: Map<String, Int>? = null,

    @SerializedName("key_findings")
    val keyFindings: List<String>? = null,

    @SerializedName("clinical_recommendations")
    val clinicalRecommendations: List<String>? = null
)