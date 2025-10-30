package com.example.rxjavatask3

import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Path

data class Post(val id: Int, val title: String)
data class DiscountCard(val id: Int, val name: String, val discount: Int)

interface ApiService {
    @GET("posts/{id}")
    fun getPost(@Path("id") id: Int): Single<Post>

    @GET("server1/discounts")
    fun getServer1Discounts(): Single<List<DiscountCard>>

    @GET("server2/discounts")
    fun getServer2Discounts(): Single<List<DiscountCard>>
}