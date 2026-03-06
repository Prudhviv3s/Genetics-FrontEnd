package com.simats.genetics.network.responses

data class LoginResponse(
    val status: Boolean,
    val message: String?,
    val token: String?,
    val role: String?,
    val errors: Map<String, List<String>>? = null
)