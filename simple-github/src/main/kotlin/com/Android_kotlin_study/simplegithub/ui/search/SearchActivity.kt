package com.Android_kotlin_study.simplegithub.ui.search

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.Android_kotlin_study.simplegithub.R
import com.Android_kotlin_study.simplegithub.api.GithubApi
import com.Android_kotlin_study.simplegithub.api.model.GithubRepo
import com.Android_kotlin_study.simplegithub.api.provideGithubApi
import com.Android_kotlin_study.simplegithub.data.RoomDB.provideSearchHistoryDao
import com.Android_kotlin_study.simplegithub.extensions.plusAssign
import com.Android_kotlin_study.simplegithub.extensions.runOnIoScheduler
import com.Android_kotlin_study.simplegithub.rx.AutoClearedDisposable
import com.Android_kotlin_study.simplegithub.ui.repo.RepositoryActivity
import com.jakewharton.rxbinding2.support.v7.widget.queryTextChangeEvents
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_search.*
import org.jetbrains.anko.startActivity
import java.lang.IllegalStateException

class SearchActivity : AppCompatActivity(), SearchAdapter.ItemClickListener {

    internal lateinit var menuSearch: MenuItem
    internal lateinit var searchView: SearchView
    internal val adapter by lazy {
        SearchAdapter().apply { setItemClickListener(this@SearchActivity) }
    }
    internal val api: GithubApi by lazy { provideGithubApi(this) }
    internal val disposables = AutoClearedDisposable(this)
    internal val viewDisposables = AutoClearedDisposable(this, false)
    //저장을 하기 위한 Dao의 인스턴스를 받음.
    internal val searchHistoryDao by lazy { provideSearchHistoryDao(this) }

    //액티비티가 완전히 종료되기 전까지 이벤트를 계속 받기위해 추가
    internal val viewDisposable = AutoClearedDisposable(this, false)

    //SearchViewModel을 생성할 때 필요한 뷰모델 팩토리 클래스의 인스턴스를 생성
    internal val viewModelFactory by lazy {
        SearchViewModelFactory(
                provideGithubApi(this),
                provideSearchHistoryDao(this))
    }

    //뷰모델의 인스턴스는 onCreate에서 생성하므로 lateinit으로 선언
    private lateinit var viewModel: SearchViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        viewModel = ViewModelProviders.of(this, viewModelFactory)[SearchViewModel::class.java]
        lifecycle += disposables
        lifecycle += viewDisposables

        with(rvActivitySearchList) {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = this@SearchActivity.adapter
        }

        //검색 결과 이벤트 구독
        viewDisposables += viewModel.searchResult
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { items ->
                    with(adapter) {
                        if (items.isEmpty) {
                            //빈 이벤트를 받으면 표시되고있던 항목을 제거
                            clearItems()
                        } else {
                            //유효한 이벤트를 받으면 데이터를 화면에 표시
                            setItems(items.value)
                        }
                        notifyDataSetChanged()
                    }
                }

        //메시지 이벤트를 구독
        viewDisposables += viewModel.message
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { message ->
                    if (message.isEmpty) {
                        //빈 이벤트를 받으면 화면에 표시되고 있떤 메시지를 숨김
                        hideError()
                    } else {
                        //유효한 이벤트를 받으면 화면에 메시지를 표시
                        showError(message.value)
                    }
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
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_activity_search, menu)
        menuSearch = menu.findItem(R.id.menu_activity_search_query)
        searchView = (menuSearch.actionView as SearchView)

        viewDisposables += searchView.queryTextChangeEvents()
                //검색을 수행했을 때 발생한 이벤트 처리
                .filter { it.isSubmitted }
                //이벤트에서 검색어 텍스트(CharSequence)추출
                .map { it.queryText() }
                //빈 무자열이 아닌 검색어 처리
                .filter { it.isNotEmpty() }
                //검색어를 String 형태로 변환
                .map { it.toString() }
                //Android의 mainThread()를 사용하여 메인스레드에서 실행 처리
                .observeOn(AndroidSchedulers.mainThread())
                //업서버블을 구독
                .subscribe { query ->
                    updateTitle(query)
                    hideSoftKeyboard()
                    collapseSearchView()
                    searchRepository(query)
                }

        viewDisposables += viewModel.lastSearchKeyword
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { keyword ->
                    if (keyword.isEmpty) {
                        menuSearch.expandActionView()
                    } else {
                        updateTitle(keyword.value)
                    }
                }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (R.id.menu_activity_search_query == item.itemId) {
            item.expandActionView()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(repository: GithubRepo) {
        disposables += viewModel.addToSearchHisory(repository)
        startActivity<RepositoryActivity>(
                RepositoryActivity.KEY_USER_LOGIN to repository.owner.login,
                RepositoryActivity.KEY_REPO_NAME to repository.name
        )
    }

    private fun searchRepository(query: String) {
        disposables += viewModel.searchRepository(query)
    }

    private fun updateTitle(query: String) {
        supportActionBar?.run { subtitle = query }
    }

    private fun hideSoftKeyboard() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).run {
            hideSoftInputFromWindow(searchView.windowToken, 0)
        }
    }

    private fun collapseSearchView() {
        menuSearch.collapseActionView()
    }

    private fun clearResults() {
        with(adapter) {
            clearItems()
            notifyDataSetChanged()
        }
    }

    private fun showProgress() {
        pbActivitySearch.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        pbActivitySearch.visibility = View.GONE
    }

    private fun showError(message: String?) {
        with(tvActivitySearchMessage) {
            text = message ?: "Unexpected error."
            visibility = View.VISIBLE
        }
    }

    private fun hideError() {
        with(tvActivitySearchMessage) {
            text = ""
            visibility = View.GONE
        }
    }
}
