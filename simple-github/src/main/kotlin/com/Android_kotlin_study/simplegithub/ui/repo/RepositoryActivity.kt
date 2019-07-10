package com.Android_kotlin_study.simplegithub.ui.repo

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.Android_kotlin_study.simplegithub.R
import com.Android_kotlin_study.simplegithub.api.provideGithubApi
import com.Android_kotlin_study.simplegithub.extensions.plusAssign
import com.Android_kotlin_study.simplegithub.rx.AutoClearedDisposable
import com.Android_kotlin_study.simplegithub.ui.GlideApp
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_repository.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class RepositoryActivity : AppCompatActivity() {

    companion object {
        const val KEY_USER_LOGIN = "user_login"
        const val KEY_REPO_NAME = "repo_name"
    }

    internal val api by lazy { provideGithubApi(this) }

    //internal var repoCall: Call<GithubRepo>? = null
    //internal val disposable = CompositeDisposable()
    internal val disposable = AutoClearedDisposable(this)

    internal val dateFormatInResponse = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssX", Locale.getDefault())

    internal val dateFormatToShow = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    //액티비티가 완전히 종료되기 전까지 이벤트를 계속 받기 위해 추가
    internal val viewDisposables = AutoClearedDisposable(this, false)

    //RepositoryViewModel을 생성하기 위해 필요한 뷰모델 팩토리 클래스의 인스턴스를 생성
    internal val viewModelFactory by lazy {
        RepositoryViewModelFactory(provideGithubApi(this))
    }

    //뷰모델의 인스턴스는 onCreate()에서 생성 되므로 lateinit으로 선언
    private lateinit var viewModel: RepositoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repository)

        viewModel = ViewModelProviders.of(this, viewModelFactory)[RepositoryViewModel::class.java]
        lifecycle += disposable
        //viewDisposables에서 이 액티비티의 생명주기 이벤트를 받도록 함.
        lifecycle += viewDisposables
        //저장소 정보 이벤트를 구독
        viewDisposables += viewModel.repository
                //유효한 저장소 이벤트만 받도록 처리
                .filter { !it.isEmpty }
                .map { it.value }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { repository ->
                    GlideApp.with(this@RepositoryActivity)
                            .load(repository.owner.avatarUrl)
                            .into(ivActivityRepositoryProfile)

                    tvActivityRepositoryName.text = repository.fullName
                    tvActivityRepositoryStars.text = resources.getQuantityString(R.plurals.star, repository.stars, repository.stars)

                    if (null == repository.description) {
                        tvActivityRepositoryDescription.setText(R.string.no_description_provided)
                    } else {
                        tvActivityRepositoryDescription.text = repository.description
                    }

                    if (null == repository.language) {
                        tvActivityRepositoryLanguage.setText(R.string.no_language_specified)
                    } else {
                        tvActivityRepositoryLanguage.text = repository.language
                    }

                    try {
                        val lastUpdate = dateFormatInResponse.parse(repository.updatedAt)
                        tvActivityRepositoryLastUpdate.text = dateFormatToShow.format(lastUpdate)
                    } catch (e: ParseException) {
                        tvActivityRepositoryLastUpdate.text = getString(R.string.unknown)
                    }
                }

        //메시지 이벤트를 구독
        viewDisposables += viewModel.message
                .observeOn(AndroidSchedulers.mainThread())
                //메시지를 이벤트를 받으면 화면에 해당 이벤트를 구독
                .subscribe { message -> showError(message) }
        //저장소 정보를 보여주는 뷰의 표시 유무를 결정하는 이벤트를 구독
        viewDisposables += viewModel.isContentVisible
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { visible ->
                    setContentVisibility(visible)
                }

        //작업 진행 여부 이벤트를 구독
        viewDisposables += viewModel.isLoading
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { isLoading ->
                    if (isLoading) {
                        showProgress()
                    } else {
                        hideProgress()
                    }
                }

        val login = intent.getStringExtra(KEY_USER_LOGIN) ?: throw IllegalArgumentException(
                "No login info exists in extras")
        val repo = intent.getStringExtra(KEY_REPO_NAME) ?: throw IllegalArgumentException(
                "No repo info exists in extras")


        disposable += viewModel.requestRepositoryInfo(login, repo)
    }

    private fun showProgress() {
        llActivityRepositoryContent.visibility = View.GONE
        pbActivityRepository.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        pbActivityRepository.visibility = View.GONE
    }

    private fun showError(message: String?) {
        with(tvActivityRepositoryMessage) {
            text = message ?: "Unexpected error."
            visibility = View.VISIBLE
        }
    }

    private fun setContentVisibility(show: Boolean) {
        llActivityRepositoryContent.visibility = if (show) View.VISIBLE else View.GONE
    }
}
