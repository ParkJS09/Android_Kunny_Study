package com.Android_kotlin_study.simplegithub.api

import com.Android_kotlin_study.simplegithub.api.model.GithubRepo
import com.Android_kotlin_study.simplegithub.api.model.RepoSearchResponse

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import io.reactivex.Observable

interface GithubApi {

    @GET("search/repositories")
    fun searchRepository(@Query("q") query: String): Observable<RepoSearchResponse>

    @GET("repos/{owner}/{name}")
    fun getRepository(
            @Path("owner") ownerLogin: String,
            @Path("name") repoName: String): Observable<GithubRepo>
}
