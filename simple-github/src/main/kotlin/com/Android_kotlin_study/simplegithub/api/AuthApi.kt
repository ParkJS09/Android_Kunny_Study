package com.Android_kotlin_study.simplegithub.api

import com.Android_kotlin_study.simplegithub.api.model.GithubAccessToken

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST
import io.reactivex.Observable

interface AuthApi {

    @FormUrlEncoded
    @POST("login/oauth/access_token")
    @Headers("Accept: application/json")
    fun getAccessToken(
            @Field("client_id") clientId: String,
            @Field("client_secret") clientSecret: String,
            @Field("code") code: String): Observable<GithubAccessToken>
}
