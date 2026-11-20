package com.example.tiktak_app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tiktak_app.presentation.viewmodel.AuthViewModel
import com.example.tiktak_app.presentation.viewmodel.CurrentGameViewModel
import com.example.tiktak_app.presentation.viewmodel.GamesViewModel
import com.example.tiktak_app.presentation.viewmodel.HistoryViewModel
import com.example.tiktak_app.presentation.viewmodel.LeaderboardViewModel
import com.example.tiktak_app.presentation.viewmodel.MenuViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {

    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(AuthViewModel::class)
    abstract fun bindAuthViewModel(viewModel: AuthViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MenuViewModel::class)
    abstract fun bindMenuViewModel(viewModel: MenuViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(GamesViewModel::class)
    abstract fun bindGamesViewModel(viewModel: GamesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CurrentGameViewModel::class)
    abstract fun bindCurrentGameViewModel(viewModel: CurrentGameViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(HistoryViewModel::class)
    abstract fun bindHistoryViewModel(viewModel: HistoryViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LeaderboardViewModel::class)
    abstract fun bindLeaderboardViewModel(viewModel: LeaderboardViewModel): ViewModel
}
