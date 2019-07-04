package com.Android_kotlin_study.simplegithub.ui.main

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.Android_kotlin_study.simplegithub.R
import com.Android_kotlin_study.simplegithub.api.model.GithubRepo
import com.Android_kotlin_study.simplegithub.data.RoomDB.provideSearchHistoryDao
import com.Android_kotlin_study.simplegithub.extensions.plusAssign
import com.Android_kotlin_study.simplegithub.extensions.runOnIoScheduler
import com.Android_kotlin_study.simplegithub.rx.AutoClearedDisposable
import com.Android_kotlin_study.simplegithub.ui.repo.RepositoryActivity
import com.Android_kotlin_study.simplegithub.ui.search.SearchActivity
import com.Android_kotlin_study.simplegithub.ui.search.SearchAdapter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.startActivity

class MainActivity : AppCompatActivity(), SearchAdapter.ItemClickListener {

    //어댑터 프로퍼티 추가
    internal val adapter by lazy {
        SearchAdapter().apply {
            setItemClickListener(this@MainActivity)
        }
    }

    //최근 조회한 저장소를 담당하는 데이터 접근 객체 프로퍼티 추가
    internal val searchHistoryDao by lazy { provideSearchHistoryDao(this)}

    //디스포저블을 관리하는 프로퍼티 추가
    internal val disposables = AutoClearedDisposable(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycle += disposables
        lifecycle += object : LifecycleObserver {
            //onStart() 콜백 함수가 호출되면 fetchSearchHistory() 함수를 호출
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun fetch(){
                fetchSearchHistory()
            }
        }

        btnActivityMainSearch.setOnClickListener {
            startActivity<SearchActivity>()
        }

        with(rvActivityMainList){
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun fetchSearchHistory() : Disposable = searchHistoryDao.getHistory()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({items ->
                with(adapter){
                    setItems(items)
                    notifyDataSetChanged()
                }

                if(items.isEmpty()) {
                    showMessage(getString(R.string.no_recent_repositories))
                } else {
                    hideMessage()
                }
            }){
                showMessage(it.message)
            }

    private fun hideMessage() {
        with(tvActivityMainMessage){
            text = ""
            visibility = View.GONE
        }
    }

    private fun showMessage(message: String?) {
        with(tvActivityMainMessage){
            text = message ?: "Unexpected error"
            visibility = View.VISIBLE
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_activity_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
        if(R.id.menu_activity_main_clear_all==item.itemId){
            clearAll()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun clearAll() {
        disposables += runOnIoScheduler { searchHistoryDao.clearAll() }
    }

    override fun onItemClick(repository: GithubRepo) {
        startActivity<RepositoryActivity>(
                RepositoryActivity.KEY_USER_LOGIN to repository.owner.login,
                RepositoryActivity.KEY_REPO_NAME to repository.name
        )
    }
}
