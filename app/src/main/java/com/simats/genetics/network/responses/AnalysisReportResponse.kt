package com.simats.genetics.network.responses

data class AnalysisReportResponse(
    val status: Boolean,
    val report: AnalysisReportData? = null,
    val error: String? = null
)

data class AnalysisReportData(
    val analysis_id: Int,
    val patient: ReportPatient,
    val pattern: ReportPattern,
    val pedigree: PedigreeSummary,
    val key_findings: List<String> = emptyList(),
    val clinical_recommendations: List<String> = emptyList(),
    val generated_at: String? = null,     // "2026-02-11"
    val report_pdf_url: String? = null    // full URL to PDF
)

data class ReportPatient(
    val name: String,
    val display_id: String,
    val age: Int? = null,
    val gender: String? = null,
    val analysis_date: String? = null     // "21/01/2026" or ISO
)

data class ReportPattern(
    val title: String,
    val confidence: Double? = null,       // 0.92
    val confidence_label: String? = null, // "Very High"
    val description: String? = null
)

data class PedigreeSummary(
    val family_members: Int? = null,
    val affected: Int? = null,
    val generations: Int? = null
)