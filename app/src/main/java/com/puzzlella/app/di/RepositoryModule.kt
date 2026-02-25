package com.puzzlella.app.di

import com.puzzlella.app.data.repository.ImageRepository
import com.puzzlella.app.data.repository.PuzzleRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    single { PuzzleRepository(get()) }
    single { ImageRepository(androidContext()) }
}
