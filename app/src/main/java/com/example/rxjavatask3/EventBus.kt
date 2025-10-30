package com.example.rxjavatask3

import io.reactivex.rxjava3.subjects.ReplaySubject

object EventBus {
    val itemClickSubject = ReplaySubject.create<Int>()
}