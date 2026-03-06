package com.simats.genetics.network.requests

import com.google.gson.annotations.SerializedName

data class RunPatternDetectionRequest(
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("dna_upload_id") val dnaUploadId: Int
)
