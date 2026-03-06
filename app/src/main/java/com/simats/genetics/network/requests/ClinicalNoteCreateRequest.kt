package com.simats.genetics.network.requests

import com.google.gson.annotations.SerializedName

data class ClinicalNoteCreateRequest(

    @SerializedName("observations")
    val observations: String,

    @SerializedName("recommendations")
    val recommendations: String
)