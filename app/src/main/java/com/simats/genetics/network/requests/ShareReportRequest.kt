package com.simats.genetics.network.requests

data class ShareReportRequest(
    val recipient_email: String,
    val message: String? = null
)