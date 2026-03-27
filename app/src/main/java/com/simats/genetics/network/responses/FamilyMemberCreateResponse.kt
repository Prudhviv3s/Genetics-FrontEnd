package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class FamilyMemberCreateResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("member") val member: MemberDto? = null
)

data class MemberDto(
    @SerializedName("id") val id: Int
)