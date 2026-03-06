package com.simats.genetics.network.requests

data class FamilyMemberUpdateRequest(
    val relationship: String?,
    val health_status: String?,
    val medical_notes: String?
)
