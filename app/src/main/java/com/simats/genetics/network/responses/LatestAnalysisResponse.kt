package com.simats.genetics.network.responses

data class LatestAnalysisResponse(
    val status: Boolean,
    val analysis: AnalysisDto? = null
)
