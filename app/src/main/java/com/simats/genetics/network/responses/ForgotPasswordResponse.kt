package com.simats.genetics.network.responses

data class ForgotPasswordResponse(
    val status: Boolean,
    val message: String? = null,
    val otp: String? = null,
    val errors: Any? = null
)
