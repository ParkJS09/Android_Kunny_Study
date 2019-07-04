package com.Android_kotlin_study.simplegithub.api.model

import com.google.gson.annotations.SerializedName

class GithubOwner(
        val login: String,
        @SerializedName("avatar_url") val avatarUrl: String)
