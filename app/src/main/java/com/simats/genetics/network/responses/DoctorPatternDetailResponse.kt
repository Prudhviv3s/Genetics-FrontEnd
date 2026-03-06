package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class DoctorPatternDetailResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("pattern") val pattern: PatternDetailDto
)

data class PatternDetailDto(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("symptoms") val symptoms: List<String>,
    @SerializedName("treatments") val treatments: List<String>
)
