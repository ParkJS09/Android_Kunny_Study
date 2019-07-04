package com.Android_kotlin_study.simplegithub.data.RoomDB

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.Android_kotlin_study.simplegithub.api.model.GithubRepo
import io.reactivex.Flowable

@Dao
interface SearchHistoryDao {
    //데이터베이스에 저장소를 추가
    //이미 저장된 항목이 있을 경우 데이터를 덮어 씌움
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(repo:GithubRepo)

    //저장되어 있는 저장소 목록을 반환
    //Flowable 형태의 자료르 반환, 데이터베이스가 변경되면 알림을 받아 새로운 자료를 사져옴.
    //항상 최산 자료를 유지할 수 있음.
    @Query("SELECT * FROM repositories")
    fun getHistory(): Flowable<List<GithubRepo>>

    //repositories 테이블의 모든 데이터를 삭제.
    @Query("DELETE FROM repositories")
    fun clearAll()
}