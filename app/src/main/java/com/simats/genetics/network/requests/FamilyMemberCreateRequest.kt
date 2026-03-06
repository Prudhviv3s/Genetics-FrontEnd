package com.simats.genetics.network.requests

data class FamilyMemberCreateRequest(
    val full_name: String,
    val gender: String,   // "Male" / "Female" / "Other"
    val age: Int
)
