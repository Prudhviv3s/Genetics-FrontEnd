package com.simats.genetics.network.responses

data class ResetPasswordResponse(
    val status: Boolean,
    val message: String? = null,
    val errors: Any? = null
)
