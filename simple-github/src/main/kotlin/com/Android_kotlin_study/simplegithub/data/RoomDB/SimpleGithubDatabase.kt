package com.Android_kotlin_study.simplegithub.data.RoomDB

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.Android_kotlin_study.simplegithub.api.model.GithubRepo

@Database(entities = arrayOf(GithubRepo::class), version =1 )
abstract class SimpleGithubDatabase : RoomDatabase(){
    abstract fun searchHistoryDao() : SearchHistoryDao
}