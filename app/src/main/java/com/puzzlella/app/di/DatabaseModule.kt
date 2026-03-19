package com.puzzlella.app.di

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.puzzlella.app.data.db.PuzzlellaDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE puzzle_history ADD COLUMN boardState TEXT")
    }
}

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            PuzzlellaDatabase::class.java,
            "puzzlella_db"
        ).addMigrations(MIGRATION_1_2).build()
    }

    single { get<PuzzlellaDatabase>().puzzleHistoryDao() }
}
