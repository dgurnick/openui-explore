package com.dgurnick.openuiexplore.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dgurnick.openuiexplore.data.repository.ConnectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SplashState {
    data object Connecting : SplashState()
    data object Connected : SplashState()
    data class Error(val message: String) : SplashState()
}

class SplashViewModel(
    private val connectionRepository: ConnectionRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SplashState>(SplashState.Connecting)
    val state: StateFlow<SplashState> = _state.asStateFlow()

    init {
        connect()
    }

    fun connect() {
        _state.value = SplashState.Connecting
        viewModelScope.launch {
            val ok = runCatching {
                connectionRepository.checkConnection()
            }.getOrElse { false }
            _state.value = if (ok) {
                SplashState.Connected
            } else {
                SplashState.Error("Cannot reach OpenUI backend")
            }
        }
    }
}
