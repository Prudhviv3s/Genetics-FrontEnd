package com.simats.genetics.network.requests

import com.google.gson.annotations.SerializedName

data class FeedbackCreateRequest(
    @SerializedName("rating") val rating: Int,
    @SerializedName("feedback_type") val feedbackType: String,
    @SerializedName("message") val message: String
)