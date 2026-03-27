package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class MyProfileResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("profile") val profile: ProfileDto?
)

data class ProfileDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("doctor_id") val doctorId: String?,
    @SerializedName("patient_id") val patientId: String?,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("dob") val dob: String?,
    @SerializedName("age") val age: Int?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("role") val role: String?
)