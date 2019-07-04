package com.Android_kotlin_study.simplegithub.extensions

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver

operator fun Lifecycle.plusAssign(observer: LifecycleObserver) = this.addObserver(observer)