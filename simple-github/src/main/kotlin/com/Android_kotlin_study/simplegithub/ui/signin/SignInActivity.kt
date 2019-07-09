package com.Android_kotlin_study.simplegithub.ui.signin

import android.arch.lifecycle.ViewModelProviders
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
    internal val disposables = AutoClearedDisposable(this)
    //액티비티가 완전히 종료되기 전까지 이벤트를 계속 받기 위해 추가
    internal val viewDisposables = AutoClearedDisposable(lifecycleOwner = this, alwaysClearOnStop = false)
    //signInViewModel을 생성할 때 필요한 뷰모델 팩토리 클래스의 인스턴스를 생성
    internal val viewModelFactory by lazy {
        SignInViewModelFactory(provideAuthApi(), AuthTokenProvider(this))
    }
    //viewModel의 인스턴스는 onCreate()에서 받으므로, lateinit으로 선언
    lateinit var viewModel: SignInViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        //SignInViewModel의 인스턴스를 받는다
        viewModel = ViewModelProviders.of(this, viewModelFactory)[SignInViewModel::class.java]
        //Lifecycle.addObserver() 함수를 사용하여 AutoClearedDisposable 객체를 옵서버로 등록
        lifecycle += disposables
        //viewDisposables에서 이 액티비티의 생명주기 이벤트를 받도록 처리
        lifecycle += viewDisposables

        //액세스 토큰 이벤트 구독
        viewDisposables += viewModel.accessToken
                //액세스 토근이 없는 경우는 무시
                .filter { !it.isEmpty }
                .observeOn(AndroidSchedulers.mainThread())
                //액세스 토큰이 있는 것을 확인했다면 메인으로 이동
                .subscribe { launchMainActivity() }


        //에러 메시지 이벤트 구독
        viewDisposables += viewModel.message
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{ message -> showError(message)}

        viewDisposables += viewModel.isLoading
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { isLoading ->
                    if (isLoading) {
                        showProgress()
                    } else {
                        hideProgress()
                    }
                }

        disposables += viewModel.loadAccessToken()

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

    private fun getAccessToken(code: String) {
        //ViewModel에 정의된 함수를 사용하여 새로운 액세스 토큰을 요청
        disposables += viewModel.requestAccessToken(
                BuildConfig.GITHUB_CLIENT_ID,
                BuildConfig.GITHUB_CLIENT_SECRET, code)
    }

    private fun showProgress() {
        btnActivitySignInStart.visibility = View.GONE
        pbActivitySignIn.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        btnActivitySignInStart.visibility = View.VISIBLE
        pbActivitySignIn.visibility = View.GONE
    }

    private fun showError(message: String) {
        longToast(message)
    }

    private fun launchMainActivity() {
        startActivity(intentFor<MainActivity>().clearTask().newTask())
    }
}
