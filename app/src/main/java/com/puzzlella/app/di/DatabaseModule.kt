package com.puzzlella.app.di

import androidx.room.Room
import com.puzzlella.app.data.db.PuzzlellaDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            PuzzlellaDatabase::class.java,
            "puzzlella_db"
        ).build()
    }

    single { get<PuzzlellaDatabase>().puzzleHistoryDao() }
}
