package com.dgurnick.openuiexplore.di

import com.dgurnick.openuiexplore.presentation.chat.ChatViewModel
import com.dgurnick.openuiexplore.presentation.login.LoginViewModel
import com.dgurnick.openuiexplore.presentation.splash.SplashViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { SplashViewModel(get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { ChatViewModel(get(), get()) }
}
