package com.puzzlella.app.di

import com.puzzlella.app.ui.home.HomeViewModel
import com.puzzlella.app.ui.history.HistoryViewModel
import com.puzzlella.app.ui.preview.PreviewViewModel
import com.puzzlella.app.ui.puzzle.PuzzleViewModel
import com.puzzlella.app.ui.samples.SamplesViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { HomeViewModel(get()) }
    viewModel { HistoryViewModel(get()) }
    viewModel { PreviewViewModel() }
    viewModel { SamplesViewModel(get()) }
    viewModel { params -> PuzzleViewModel(params.get(), params.get(), params.get(), get(), get()) }
}
