package com.Android_kotlin_study.simplegithub.extensions

import com.Android_kotlin_study.simplegithub.rx.AutoClearedDisposable
import io.reactivex.disposables.Disposable

////CompositeDisposableㅡ이 '+=' 연산자 뒤에 Disposable 타입이 오는 경우 재정의
//operator fun CompositeDisposable.plusAssign(disposable : Disposable){
//    //CompositeDisposable.add() 함수를 호출
//    this.add(disposable)
//}


operator fun AutoClearedDisposable.plusAssign(disposable: Disposable) = this.add(disposable)