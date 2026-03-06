package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class PedigreeChartResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("patient") val patient: PedigreePatient?,
    @SerializedName("pedigree") val pedigree: PedigreeData?
)

data class PedigreePatient(
    @SerializedName("id") val id: Int,
    @SerializedName("full_name") val fullName: String
)

data class PedigreeData(
    @SerializedName("nodes") val nodes: List<PedigreeNode>,
    @SerializedName("links") val links: List<PedigreeLink>
)

data class PedigreeNode(
    @SerializedName("node_id") val nodeId: String,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("relationship") val relationship: String?,
    @SerializedName("health_status") val healthStatus: String?,
    @SerializedName("is_proband") val isProband: Boolean = false,
    @SerializedName("age") val age: Int? = null
)

data class PedigreeLink(
    @SerializedName("type") val type: String, // "partner" / "parent"
    @SerializedName("from") val from: String,
    @SerializedName("to") val to: String
)