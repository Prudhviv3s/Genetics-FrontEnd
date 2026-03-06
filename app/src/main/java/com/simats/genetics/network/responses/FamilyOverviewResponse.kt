package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class FamilyOverviewResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("members") val members: List<FamilyMemberItemResponse> = emptyList()
)

data class FamilyMemberItemResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("relationship") val relationship: String?,
    @SerializedName("age") val age: Int?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("health_status") val healthStatus: String?
)
