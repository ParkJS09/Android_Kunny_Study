package com.Android_kotlin_study.simplegithub.api.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.google.gson.annotations.SerializedName

//GithubRepo 엔티티의 데이터가 저장될 테이블 이름을 repositories로 지정
@Entity(tableName = "repositories")
class GithubRepo(
        val name: String,
        @SerializedName("full_name")
        //fullName 프로퍼티를 주요 키로 사용하며, 테이블 내 필드 이름은 full_name으로 지정
        @PrimaryKey @ColumnInfo(name = "full_name")
        val fullName: String,

        //GithubOwner 내 필드를 테이블에 함께 저장.
        @Embedded
        val owner: GithubOwner,

        val description: String?,
        val language: String?,

        @SerializedName("updated_at")
        @ColumnInfo(name = "updated_At")
        val updatedAt: String,

        @SerializedName("stargazers_count")
        val stars: Int
)
