package com.saurav.sauravcaqi.app

import android.app.Application
import com.saurav.sauravcaqi.di.appModule
import com.saurav.sauravcaqi.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApp : Application() {
  override fun onCreate() {
    super.onCreate()
    startKoin {
      // declare used Android context
      androidContext(this@MyApp)
      // declare modules
      modules(listOf(appModule, viewModelModule))
    }
  }
}