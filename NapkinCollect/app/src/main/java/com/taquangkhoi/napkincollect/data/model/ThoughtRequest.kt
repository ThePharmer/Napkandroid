package com.taquangkhoi.napkincollect.data.model

import com.google.gson.annotations.SerializedName

data class ThoughtRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("token")
    val token: String,

    @SerializedName("thought")
    val thought: String,

    @SerializedName("sourceUrl")
    val sourceUrl: String
)
