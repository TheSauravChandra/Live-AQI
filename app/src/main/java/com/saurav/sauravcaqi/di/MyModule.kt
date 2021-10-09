package com.saurav.sauravcaqi.di

import com.saurav.sauravcaqi.adapter.AqiCityAdapter
import com.saurav.sauravcaqi.vm.AqiVM
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
  factory { AqiCityAdapter(get()) }
}

val viewModelModule = module {
  viewModel { AqiVM() }
}