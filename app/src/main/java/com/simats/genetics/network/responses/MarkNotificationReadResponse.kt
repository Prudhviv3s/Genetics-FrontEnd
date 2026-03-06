package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class MarkNotificationReadResponse(
    val status: Boolean,
    val message: String? = null,
    @SerializedName("new_count")
    val newCount: Int = 0
)
