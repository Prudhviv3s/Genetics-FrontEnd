package com.simats.genetics.network.responses

data class ExportInfoResponse(
    val status: Boolean,
    val export: ExportInfo?,
    val error: String? = null
)

data class ExportInfo(
    val report_type: String,
    val format: String,
    val file_size: String,
    val generated: String,
    val pdf_url: String? = null
)
