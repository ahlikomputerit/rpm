package com.ahlikomputerit.lumentransfer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ahlikomputerit.lumentransfer.presentation.home.HomeScreen
import com.ahlikomputerit.lumentransfer.presentation.receive.ReceiveScreen
import com.ahlikomputerit.lumentransfer.presentation.receive.ReceiveViewModel
import com.ahlikomputerit.lumentransfer.presentation.send.SendScreen
import com.ahlikomputerit.lumentransfer.presentation.send.SendViewModel

private enum class AppRoute {
    HOME,
    SEND,
    RECEIVE,
}

@Composable
fun LumenTransferApp(
    sendViewModel: SendViewModel,
    receiveViewModel: ReceiveViewModel,
) {
    var route by rememberSaveable { mutableStateOf(AppRoute.HOME.name) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (AppRoute.valueOf(route)) {
                AppRoute.HOME -> HomeScreen(
                    onSend = { route = AppRoute.SEND.name },
                    onReceive = { route = AppRoute.RECEIVE.name },
                )
                AppRoute.SEND -> SendScreen(
                    viewModel = sendViewModel,
                    onBack = { route = AppRoute.HOME.name },
                )
                AppRoute.RECEIVE -> ReceiveScreen(
                    viewModel = receiveViewModel,
                    onBack = { route = AppRoute.HOME.name },
                )
            }
        }
    }
}
