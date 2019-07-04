package com.Android_kotlin_study.simplegithub.ui.main

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.Android_kotlin_study.simplegithub.R
import com.Android_kotlin_study.simplegithub.ui.search.SearchActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.startActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnActivityMainSearch.setOnClickListener {
            startActivity<SearchActivity>()
        }
    }
}
