package com.example.rxjavatask3

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var networkText: TextView
    private lateinit var timerText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var btnMerge: Button
    private lateinit var btnZip: Button
    private lateinit var discountsText: TextView

    private val disposables = CompositeDisposable()

    private val apiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl("")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .build()

        retrofit.create<ApiService>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupNetworkCall()
        setupTimer()
        setupRecyclerView()
        setupSearchDebounce()
        setupDiscountButtons()
    }

    private fun initViews() {
        networkText = findViewById(R.id.networkText)
        timerText = findViewById(R.id.timerText)
        recyclerView = findViewById(R.id.recyclerView)
        searchEditText = findViewById(R.id.searchEditText)
        btnMerge = findViewById(R.id.btnMerge)
        btnZip = findViewById(R.id.btnZip)
        discountsText = findViewById(R.id.discountsText)
    }

    // 1. Сетевой запрос
    private fun setupNetworkCall() {
        disposables.add(
            apiService.getPost(1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { post ->
                        networkText.text = post.title
                    },
                    { error ->
                        networkText.text = error.message
                    }
                )
        )
    }

    // 2. Таймер
    private fun setupTimer() {
        disposables.add(
            Observable.interval(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { seconds ->
                    timerText.text = "Прошло: $seconds сек"
                }
        )
    }

    // 3. RecyclerView с Subject
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = Adapter()

        disposables.add(
            EventBus.itemClickSubject
                .subscribe { position ->
                    Toast.makeText(this, "Клик на: $position", Toast.LENGTH_SHORT).show()
                }
        )
    }

    // 4. EditText с debounce
    private fun setupSearchDebounce() {
        disposables.add(
            Observable.create { emitter ->
                val textWatcher = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        s?.let { emitter.onNext(it) }
                    }
                    override fun afterTextChanged(s: Editable?) {}
                }

                searchEditText.addTextChangedListener(textWatcher)
                emitter.setCancellable { searchEditText.removeTextChangedListener(textWatcher) }
            }
                .debounce(3, TimeUnit.SECONDS) // Ждем 3 секунды без ввода
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe { text ->
                    Log.d("SEARCH_DEBOUNCE", "Поиск: $text")
                    Toast.makeText(this, "Ищем: $text", Toast.LENGTH_SHORT).show()
                }
        )
    }

    // 5. Два сервера с разными стратегиями
    private fun setupDiscountButtons() {
        // а) Если один падает - ВСЕ РАВНО выводим
        btnMerge.setOnClickListener {
            loadDiscountsMerge()
        }

        // б) Если один падает - НИЧЕГО не выводим
        btnZip.setOnClickListener {
            loadDiscountsZip()
        }
    }

    private fun loadDiscountsMerge() {
        discountsText.text = "Загружаем скидки (стратегия MERGE)"

        disposables.add(
        Observable.merge(
            apiService.getServer1Discounts()
                .onErrorReturnItem(emptyList())
                .toObservable(),
            apiService.getServer2Discounts()
                .onErrorReturnItem(emptyList())
                .toObservable()
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { discounts ->
                    updateDiscountsText("MERGE: Загружено ${discounts.size} карт")
                },
                { error ->
                    updateDiscountsText("MERGE ошибка: ${error.message}")
                }
            )
        )
    }

    private fun loadDiscountsZip() {
        discountsText.text = "Загружаем скидки (стратегия ZIP)..."

        disposables.add(
        Single.zip(
            apiService.getServer1Discounts(),
            apiService.getServer2Discounts()
        ) { list1, list2 -> list1 + list2 }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { discounts ->
                    updateDiscountsText("ZIP: Загружено ${discounts.size} карт")
                },
                { error ->
                    updateDiscountsText("ZIP ошибка: ${error.message}")
                }
            )
        )
    }

    private fun updateDiscountsText(message: String) {
        discountsText.text = message
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }
}