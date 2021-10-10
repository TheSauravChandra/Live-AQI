package com.saurav.sauravcaqi.di

import com.saurav.sauravcaqi.adapter.AqiCityAdapter
import com.saurav.sauravcaqi.vm.AqiVM
import okhttp3.OkHttpClient
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
  factory { AqiCityAdapter(get()) }
  factory { OkHttpClient() }
}

val viewModelModule = module {
  viewModel { AqiVM(get()) }
}