package com.Android_kotlin_study.simplegithub.data.RoomDB

import android.arch.persistence.room.Room
import android.content.Context

private var instance : SimpleGithubDatabase ?= null
fun provideSearchHistoryDao(context : Context) :SearchHistoryDao = provideDatabase(context).searchHistoryDao()

private fun provideDatabase(context : Context) : SimpleGithubDatabase{
    if(null == instance){
        instance = Room.databaseBuilder(context.applicationContext, SimpleGithubDatabase::class.java, "simlple_github.db").build()
    }
    return instance!!
}