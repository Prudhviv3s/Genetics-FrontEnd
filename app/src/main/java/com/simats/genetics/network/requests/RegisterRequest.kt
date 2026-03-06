package com.simats.genetics.network.requests

data class RegisterRequest(
    val full_name: String,
    val email: String,
    val phone: String,
    val dob: String,      // "YYYY-MM-DD"
    val gender: String,
    val age: Int,
    val password: String,
    val confirm_password: String,
    val terms_accepted: Boolean = true   // always true
)