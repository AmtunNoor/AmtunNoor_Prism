package com.noor.prism
import com.google.gson.annotations.SerializedName

data class MenuModel(
    @SerializedName("title") val title: String,
    @SerializedName("url") val url: String
)
