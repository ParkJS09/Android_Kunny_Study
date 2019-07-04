package com.Android_kotlin_study.simplegithub.ui.search

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
import com.Android_kotlin_study.simplegithub.extensions.plusAssign
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
//    //여러 디스포저블 객체를 관리할 수 있는 CompositeDisposable 객체를 초기화.
//    internal val disposable = CompositeDisposable()
//    //액티비티가 종료되기 전까지 뷰에서 발생하는 이벤트를 처리하기 위해 viewDisposables프로퍼티 추가.
//    internal val viewDisposables = CompositeDisposable()
    //CompositeDisposable에서 AutoClearedDisposable로 변경
    internal val disposables = AutoClearedDisposable(this)
    //onStop() 콜백 함수가 호출되더라도 액티비티가 종료되는 시점에만 관리하고 있는 디스포저블을 해제하도록 구현되어 있으므로
    //alwaysClearOnStop 프로퍼티를 false로 설정한 생성자를 사용하여 AutoClearedDisposable 객체를 생성하도록 변경
    internal val viewDisposables = AutoClearedDisposable(this, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        lifecycle += disposables
        lifecycle += viewDisposables

        with(rvActivitySearchList) {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = this@SearchActivity.adapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_activity_search, menu)
        menuSearch = menu.findItem(R.id.menu_activity_search_query)
//        searchView = (menuSearch.actionView as SearchView).apply {
//            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//                override fun onQueryTextSubmit(query: String): Boolean {
//                    updateTitle(query)
//                    hideSoftKeyboard()
//                    collapseSearchView()
//                    searchRepository(query)
//                    return true
//                }
//
//                override fun onQueryTextChange(newText: String): Boolean {
//                    return false
//                }
//            })
//        }
        searchView = (menuSearch.actionView as SearchView)
        viewDisposables += searchView.queryTextChangeEvents()

                //검색을 수행했을 때 발생한 이벤트 처리
                .filter { it.isSubmitted }
                //이벤트에서 검색어 텍스트(CharSequence)추출
                .map{it.queryText()}
                //빈 무자열이 아닌 검색어 처리
                .filter{it.isNotEmpty()}
                //검색어를 String 형태로 변환
                .map{it.toString()}
                //Android의 mainThread()를 사용하여 메인스레드에서 실행 처리
                .observeOn(AndroidSchedulers.mainThread())
                //업서버블을 구독
                .subscribe{query->
                    updateTitle(query)
                    hideSoftKeyboard()
                    collapseSearchView()
                    searchRepository(query)
                }

        menuSearch.expandActionView()

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (R.id.menu_activity_search_query == item.itemId) {
            item.expandActionView()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

//    override fun onStop() {
//        super.onStop()
        //관리하고 있는 디스포저블 객체를 모두 해제
//        searchCall?.run { cancel() }
//        disposable.clear()
//        if(isFinishing){
//            viewDisposables.clear()
//        }
    //}

    override fun onItemClick(repository: GithubRepo) {
        startActivity<RepositoryActivity>(
                RepositoryActivity.KEY_USER_LOGIN to repository.owner.login,
                RepositoryActivity.KEY_REPO_NAME to repository.name)
    }

    private fun searchRepository(query: String) {
        //clearResults()
//        hideError()
//        showProgress()

//        searchCall = api.searchRepository(query)
//        searchCall!!.enqueue(object : Callback<RepoSearchResponse> {
//            override fun onResponse(call: Call<RepoSearchResponse>,
//                    response: Response<RepoSearchResponse>) {
//                hideProgress()
//
//                val searchResult = response.body()
//                if (response.isSuccessful && null != searchResult) {
//                    with(adapter) {
//                        setItems(searchResult.items)
//                        notifyDataSetChanged()
//                    }
//
//                    if (0 == searchResult.totalCount) {
//                        showError(getString(R.string.no_search_result))
//                    }
//                } else {
//                    showError("Not successful: " + response.message())
//                }
//            }
//
//            override fun onFailure(call: Call<RepoSearchResponse>, t: Throwable) {
//                hideProgress()
//                showError(t.message)
//            }
//        })

        disposables += api.searchRepository(query)
                //Observable 형태로 결과를 바꿔주기 위해 flatMap을 사용
                .flatMap {
                    if (0 == it.totalCount) {
                        //검색 결과가 없을 경우 에러를 발생시켜 에러 메시지를 표시
                        Observable.error(IllegalStateException("No search Result"))
                    } else {
                        Observable.just(it.items)
                    }
                }
                //스케줄러 지정
                .observeOn(AndroidSchedulers.mainThread())
                //구독할 때 수행할 작업
                .doOnSubscribe {
                    clearResults()
                    hideError()
                    showProgress()
                }
                //스트림이 종료될 때 수행할 작업
                .doOnTerminate {
                    hideProgress()
                }
                //옵서버블을 구독
                .subscribe({ items ->
                    with(adapter) {
                        setItems(items)
                        notifyDataSetChanged()
                    }
                }) {
                    //에러블록
                    showError(it.message)
                }

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
