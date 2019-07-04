package com.Android_kotlin_study.simplegithub.ui.signin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.Android_kotlin_study.simplegithub.BuildConfig
import com.Android_kotlin_study.simplegithub.R
import com.Android_kotlin_study.simplegithub.api.provideAuthApi
import com.Android_kotlin_study.simplegithub.data.AuthTokenProvider
import com.Android_kotlin_study.simplegithub.extensions.plusAssign
import com.Android_kotlin_study.simplegithub.rx.AutoClearedDisposable
import com.Android_kotlin_study.simplegithub.ui.main.MainActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_sign_in.*
import org.jetbrains.anko.clearTask
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.longToast
import org.jetbrains.anko.newTask

class SignInActivity : AppCompatActivity() {

    internal val api by lazy { provideAuthApi() }

    internal val authTokenProvider by lazy { AuthTokenProvider(this) }

    //    internal val disposables = CompositeDisposable()
    internal val disposables = AutoClearedDisposable(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        //Lifecycle.addObserver() 함수를 사용하여 AutoClearedDisposable 객체를 옵서버로 등록
        lifecycle += disposables

        btnActivitySignInStart.setOnClickListener {
            val authUri = Uri.Builder().scheme("https").authority("github.com")
                    .appendPath("login")
                    .appendPath("oauth")
                    .appendPath("authorize")
                    .appendQueryParameter("client_id", BuildConfig.GITHUB_CLIENT_ID)
                    .build()

            val intent = CustomTabsIntent.Builder().build()
            intent.launchUrl(this@SignInActivity, authUri)
        }

        if (null != authTokenProvider.token) {
            launchMainActivity()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        showProgress()

        val code = intent.data?.getQueryParameter("code")
                ?: throw IllegalStateException("No code exists")

        getAccessToken(code)
    }

    //Lifecycle를 활용하여서 더 이상 onStop()을 오버라이드 하지 않아도됨.
//    override fun onStop() {
//        super.onStop()
////        accessTokenCall?.run { cancel() }
////        관리하고 있던 디스포저블 객체를 모두 해제
//        //onSt
//        //disposables.clear()
//    }

    private fun getAccessToken(code: String) {
//        showProgress()
//        accessTokenCall = api.getAccessToken(
//                BuildConfig.GITHUB_CLIENT_ID, BuildConfig.GITHUB_CLIENT_SECRET, code)
//        accessTokenCall = api.getAccessToken(BuildConfig.GITHUB_CLIENT_ID, BuildConfig.GITHUB_CLIENT_SECRET, code)
//
//        accessTokenCall!!.enqueue(object : Callback<GithubAccessToken> {
//            override fun onResponse(call: Call<GithubAccessToken>,
//                                    response: Response<GithubAccessToken>) {
//                hideProgress()
//
//                val token = response.body()
//                if (response.isSuccessful && null != token) {
//                    authTokenProvider.updateToken(token.accessToken)
//
//                    launchMainActivity()
//                } else {
//                    showError(IllegalStateException(
//                            "Not successful: " + response.message()))
//                }
//            }
//
//            override fun onFailure(call: Call<GithubAccessToken>, t: Throwable) {
//                hideProgress()
//                showError(t)
//            }
//        })
//        생성된 디스포저블 객체는 CompositeDisposable에서 관리하도록 CompositeDisposable.add()함수를 사용하여 추가.
        disposables += api.getAccessToken(
                BuildConfig.GITHUB_CLIENT_ID, BuildConfig.GITHUB_CLIENT_SECRET, code)
//                REST API를 통해 받은 응답에서 액세스 토큰만 추출
                .map { it.accessToken }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { showProgress() }
                .doOnTerminate { hideProgress() }
                .subscribe({ token ->
                    authTokenProvider.updateToken(token)
                    launchMainActivity()
                }) {
                    showError(it)
                }
    }

    private fun showProgress() {
        btnActivitySignInStart.visibility = View.GONE
        pbActivitySignIn.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        btnActivitySignInStart.visibility = View.VISIBLE
        pbActivitySignIn.visibility = View.GONE
    }

    private fun showError(throwable: Throwable) {
        longToast(throwable.message ?: "No message available")
    }

    private fun launchMainActivity() {
        startActivity(intentFor<MainActivity>().clearTask().newTask())
    }
}
