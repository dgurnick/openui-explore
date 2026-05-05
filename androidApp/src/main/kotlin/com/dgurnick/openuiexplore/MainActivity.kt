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
import com.dgurnick.openuiexplore.presentation.splash.SplashViewModel
import com.dgurnick.openuiexplore.ui.chat.ChatScreen
import com.dgurnick.openuiexplore.ui.splash.SplashScreen
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
private fun AppNavigation() {
    var showChat by rememberSaveable { mutableStateOf(false) }

    if (showChat) {
        val chatViewModel: ChatViewModel = koinViewModel()
        val chatState by chatViewModel.state.collectAsState()
        ChatScreen(
            state = chatState,
            onSend = chatViewModel::sendMessage,
            onDismissError = chatViewModel::dismissError
        )
    } else {
        val splashViewModel: SplashViewModel = koinViewModel()
        val splashState by splashViewModel.state.collectAsState()
        SplashScreen(
            state = splashState,
            onRetry = splashViewModel::connect,
            onConnected = { showChat = true }
        )
    }
}
