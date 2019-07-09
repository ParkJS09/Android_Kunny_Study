package com.Android_kotlin_study.simplegithub.ui.signin

import android.arch.lifecycle.ViewModel
import com.Android_kotlin_study.simplegithub.api.AuthApi
import com.Android_kotlin_study.simplegithub.data.AuthTokenProvider
import com.Android_kotlin_study.simplegithub.util.SupportOptional
import com.Android_kotlin_study.simplegithub.util.optionalOf
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class SignInViewModel(
        val api: AuthApi,
        val authTokenProvider: AuthTokenProvider)
    : ViewModel() {

    //액세스 토큰을 전달할 서브젝트
    val accessToken: BehaviorSubject<SupportOptional<String>> = BehaviorSubject.create()

    //에러 메시지를 전달할 서브젝트
    val message: PublishSubject<String> = PublishSubject.create()

    //작업 진행 상태를 전달할 서브젝트. 초기값으로  false를 지정
    val isLoading: BehaviorSubject<Boolean>
            = BehaviorSubject.createDefault(false)


    //기기에 저장된 액세스 토큰을 불러옴
    fun loadAccessToken(): Disposable
            = Single.fromCallable { optionalOf(authTokenProvider.token) }
            .subscribeOn(Schedulers.io())
            .subscribe(Consumer<SupportOptional<String>> {
                accessToken.onNext(it)
            })

    fun requestAccessToken(clientId: String, clientSecret: String, code: String): Disposable
            = api.getAccessToken(clientId, clientSecret, code)
            .map { it.accessToken }
            .doOnSubscribe { isLoading.onNext(true) }
            .doOnTerminate { isLoading.onNext(false) }
            .subscribe({ token ->
                authTokenProvider.updateToken(token)
                accessToken.onNext(optionalOf(token))
            }) {
                message.onNext(it.message ?: "Unexpected error")
            }
}