package com.simats.genetics.network.responses

data class ExportReportInfoResponse(
    val status: Boolean,
    val report: ExportReportInfo? = null,
    val message: String? = null
)

data class ExportReportInfo(
    val report_type: String,
    val format: String,
    val file_size: String,
    val generated: String
)