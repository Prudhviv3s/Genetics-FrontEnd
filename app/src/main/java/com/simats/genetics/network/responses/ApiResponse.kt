package com.simats.genetics.network.responses

data class ApiResponse(
    val status: Boolean,
    val message: String,
    val token: String? = null,
    val role: String? = null,
    val user_id: Int? = null,
    val full_name: String? = null,
    val email: String? = null
)