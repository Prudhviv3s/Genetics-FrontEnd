package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class FamilyMemberCreateResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("member_id") val memberId: Int? = null
)