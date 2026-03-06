package com.simats.genetics.network.requests

import com.google.gson.annotations.SerializedName

data class UpdateProfileRequest(
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("dob") val dob: String? = null,      // "YYYY-MM-DD"
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("age") val age: Int? = null
)