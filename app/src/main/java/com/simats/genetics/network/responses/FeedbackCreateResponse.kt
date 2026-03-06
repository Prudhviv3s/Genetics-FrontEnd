package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class FeedbackCreateResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String?
)