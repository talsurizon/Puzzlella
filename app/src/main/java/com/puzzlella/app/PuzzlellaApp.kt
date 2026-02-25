package com.puzzlella.app

import android.app.Application
import com.puzzlella.app.di.appModule
import com.puzzlella.app.di.databaseModule
import com.puzzlella.app.di.repositoryModule
import com.puzzlella.app.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class PuzzlellaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@PuzzlellaApp)
            modules(appModule, databaseModule, repositoryModule, viewModelModule)
        }
    }
}
