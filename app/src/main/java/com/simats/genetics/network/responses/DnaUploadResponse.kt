package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class DnaUploadResponse(
    val status: Boolean,
    val message: String? = null,
    val upload: DnaUploadDto? = null
)

data class DnaUploadDto(
    val id: Int? = null,
    @SerializedName("original_name") val originalName: String? = null,
    val file: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)