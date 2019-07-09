package com.Android_kotlin_study.simplegithub.ui.signin

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.Android_kotlin_study.simplegithub.api.AuthApi
import com.Android_kotlin_study.simplegithub.data.AuthTokenProvider

class SignInViewModelFactory(val api : AuthApi, val authTokenProvider : AuthTokenProvider) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SignInViewModel(api, authTokenProvider) as T
    }
}
