package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class FamilyMemberUpdateResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String? = null
)