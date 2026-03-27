package com.simats.genetics.network.requests

data class FamilyMemberUpdateRequest(
    val relationship: String? = null,
    val side_of_family: String? = null,
    val health_status: String? = null,
    val medical_notes: String? = null
)
