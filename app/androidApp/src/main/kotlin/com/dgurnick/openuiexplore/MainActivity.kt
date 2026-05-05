package com.dgurnick.openuiexplore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.dgurnick.openuiexplore.presentation.chat.ChatViewModel
import com.dgurnick.openuiexplore.presentation.login.LoginViewModel
import com.dgurnick.openuiexplore.presentation.splash.SplashViewModel
import com.dgurnick.openuiexplore.ui.chat.ChatScreen
import com.dgurnick.openuiexplore.ui.login.LoginScreen
import com.dgurnick.openuiexplore.ui.splash.SplashScreen
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme { AppNavigation() } }
    }
}

private enum class Screen {
    SPLASH,
    LOGIN,
    CHAT
}

@Composable
private fun AppNavigation() {
    var screen by rememberSaveable { mutableStateOf(Screen.SPLASH) }

    when (screen) {
        Screen.SPLASH -> {
            val vm: SplashViewModel = koinViewModel()
            val state by vm.state.collectAsState()
            SplashScreen(
                    state = state,
                    onRetry = vm::connect,
                    onConnected = { screen = Screen.LOGIN }
            )
        }
        Screen.LOGIN -> {
            val vm: LoginViewModel = koinViewModel()
            val state by vm.state.collectAsState()
            LoginScreen(
                    state = state,
                    onLogin = vm::login,
                    onDismissError = vm::dismissError,
                    onSuccess = { screen = Screen.CHAT }
            )
        }
        Screen.CHAT -> {
            val vm: ChatViewModel = koinViewModel()
            val state by vm.state.collectAsState()
            ChatScreen(
                    state = state,
                    onSend = vm::sendMessage,
                    onDismissError = vm::dismissError,
                    onReset = vm::resetChat,
                    onQuickAction = vm::sendMessage
            )
        }
    }
}
