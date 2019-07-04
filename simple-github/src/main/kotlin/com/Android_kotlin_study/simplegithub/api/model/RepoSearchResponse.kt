package com.Android_kotlin_study.simplegithub.api.model

import com.google.gson.annotations.SerializedName

class RepoSearchResponse(
        @SerializedName("total_count") val totalCount: Int,
        val items: List<GithubRepo>)
