package com.Android_kotlin_study.simplegithub.ui.main

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.arch.lifecycle.ViewModelProviders
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
import com.Android_kotlin_study.simplegithub.rx.AutoActivatedDisposable
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
    internal val searchHistoryDao by lazy { provideSearchHistoryDao(this) }

    //디스포저블을 관리하는 프로퍼티 추가
    internal val disposables = AutoClearedDisposable(this)

    //MainViewModel을 생성하기 위해 필요한 뷰모델 팩토리 클래스의 인스턴스를 생성
    internal val viewModelFactory by lazy {
        MainViewModelFactory(provideSearchHistoryDao(this))
    }

    //뷰모델의 인스턴스는 onCreate()에서 받으므로, lateinit으로 선언
    lateinit var viewModel: MainViewModel

    //액티비티가 완전히 종료되기 전까지 이벤트를 계속 받기 위해 추가
    internal val viewDisposables = AutoClearedDisposable(lifecycleOwner = this, alwaysClearOnStop = false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProviders.of(this, viewModelFactory)[MainViewModel::class.java]

        lifecycle += disposables

        //viewDisposables에서 이 액티비티의 생명주기 이벤트를 받도록 합니다.
        lifecycle += viewDisposables

        //Activity가 활성 상태인 경우에만 DB에 저장된 저장소 조회 기록을 받도록 함.
        lifecycle += AutoActivatedDisposable(this) {
            viewModel.searchHistory
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { items ->
                        with(adapter) {
                            if (items.isEmpty) {
                                clearItems()
                            } else {
                                setItems(items.value)
                            }
                            notifyDataSetChanged()
                        }
                    }
        }

        btnActivityMainSearch.setOnClickListener {
            startActivity<SearchActivity>()
        }

        with(rvActivityMainList) {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        //메시지 이벤트를 구독
        viewDisposables += viewModel.message
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { message ->
                    if (message.isEmpty) {
//                        빈 메시지를 받은 경우 표시되고 있는 메시지를 화면에서 숨김.
                        hideMessage()
                    } else {
                        showMessage(message.value)
                    }
                }
    }

    private fun hideMessage() {
        with(tvActivityMainMessage) {
            text = ""
            visibility = View.GONE
        }
    }

    private fun showMessage(message: String?) {
        with(tvActivityMainMessage) {
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
        if (R.id.menu_activity_main_clear_all == item.itemId) {
            //DB에 저장된 저장소 조회 기록 데이터를 모두 삭제
            disposables += viewModel.clearSearchHistory()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(repository: GithubRepo) {
        startActivity<RepositoryActivity>(
                RepositoryActivity.KEY_USER_LOGIN to repository.owner.login,
                RepositoryActivity.KEY_REPO_NAME to repository.name
        )
    }
}
