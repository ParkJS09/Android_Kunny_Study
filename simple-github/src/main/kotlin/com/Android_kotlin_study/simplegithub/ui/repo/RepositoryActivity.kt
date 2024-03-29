package com.Android_kotlin_study.simplegithub.ui.repo

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repository)

        val login = intent.getStringExtra(KEY_USER_LOGIN) ?: throw IllegalArgumentException(
                "No login info exists in extras")
        val repo = intent.getStringExtra(KEY_REPO_NAME) ?: throw IllegalArgumentException(
                "No repo info exists in extras")

        lifecycle += disposable
        showRepositoryInfo(login, repo)
    }

//    override fun onStop() {
//        super.onStop()
//        //repoCall?.run { cancel() }
//        disposable.clear()
//    }

    private fun showRepositoryInfo(login: String, repoName: String) {
        showProgress()

//        repoCall = api.getRepository(login, repoName)
//        repoCall!!.enqueue(object : Callback<GithubRepo> {
//            override fun onResponse(call: Call<GithubRepo>, response: Response<GithubRepo>) {
//                hideProgress(true)
//
//                val repo = response.body()
//                if (response.isSuccessful && null != repo) {
//                    GlideApp.with(this@RepositoryActivity)
//                            .load(repo.owner.avatarUrl)
//                            .into(ivActivityRepositoryProfile)
//
//                    tvActivityRepositoryName.text = repo.fullName
//                    tvActivityRepositoryStars.text = resources
//                            .getQuantityString(R.plurals.star, repo.stars, repo.stars)
//                    if (null == repo.description) {
//                        tvActivityRepositoryDescription.setText(R.string.no_description_provided)
//                    } else {
//                        tvActivityRepositoryDescription.text = repo.description
//                    }
//                    if (null == repo.language) {
//                        tvActivityRepositoryLanguage.setText(R.string.no_language_specified)
//                    } else {
//                        tvActivityRepositoryLanguage.text = repo.language
//                    }
//
//                    try {
//                        val lastUpdate = dateFormatInResponse.parse(repo.updatedAt)
//                        tvActivityRepositoryLastUpdate.text = dateFormatToShow.format(lastUpdate)
//                    } catch (e: ParseException) {
//                        tvActivityRepositoryLastUpdate.text = getString(R.string.unknown)
//                    }
//
//                } else {
//                    showError("Not successful: " + response.message())
//                }
//            }
//
//            override fun onFailure(call: Call<GithubRepo>, t: Throwable) {
//                hideProgress(false)
//                showError(t.message)
//            }
//        })

        disposable += (api.getRepository(login, repoName)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { showProgress() }
                .doOnError { hideProgress(false) }
                .doOnComplete { hideProgress(true) }
                .subscribe({ repo ->
                    GlideApp.with(this@RepositoryActivity)
                            .load(repo.owner.avatarUrl)
                            .into(ivActivityRepositoryProfile)

                    tvActivityRepositoryName.text = repo.fullName
                    tvActivityRepositoryStars.text = resources.getQuantityString(R.plurals.star, repo.stars, repo.stars)
                    if (null == repo.description) {
                        tvActivityRepositoryDescription.setText(R.string.no_description_provided)
                    } else {
                        tvActivityRepositoryDescription.text = repo.description
                    }


                    if (null == repo.language) {
                        tvActivityRepositoryLanguage.setText(R.string.no_language_specified)
                    } else {
                        tvActivityRepositoryLanguage.text = repo.language
                    }

                    try{
                        val lastUpdate = dateFormatInResponse.parse(repo.updatedAt)
                        tvActivityRepositoryLastUpdate.text = dateFormatToShow.format(lastUpdate)
                    }catch(e:ParseException){
                        tvActivityRepositoryLastUpdate.text = getString(R.string.unknown)
                    }
                }){
                    showError(it.message)
                }

        )

    }

    private fun showProgress() {
        llActivityRepositoryContent.visibility = View.GONE
        pbActivityRepository.visibility = View.VISIBLE
    }

    private fun hideProgress(isSucceed: Boolean) {
        llActivityRepositoryContent.visibility = if (isSucceed) View.VISIBLE else View.GONE
        pbActivityRepository.visibility = View.GONE
    }

    private fun showError(message: String?) {
        with(tvActivityRepositoryMessage) {
            text = message ?: "Unexpected error."
            visibility = View.VISIBLE
        }
    }
}
