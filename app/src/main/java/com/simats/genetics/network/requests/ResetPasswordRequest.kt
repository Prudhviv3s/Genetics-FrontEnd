package com.simats.genetics.network.requests

data class ResetPasswordRequest(
    val email: String,
    val otp: String,
    val new_password: String,
    val confirm_password: String
)
