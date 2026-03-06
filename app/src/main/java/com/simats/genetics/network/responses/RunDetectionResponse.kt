package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class RunDetectionResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("upload_id") val uploadId: Int? = null,
    @SerializedName("result") val result: LatestAnalysisDto? = null
)