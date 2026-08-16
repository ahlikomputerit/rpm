package com.ahlikomputerit.lumentransfer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B5E),
    onPrimary = Color.White,
    secondary = Color(0xFF4D635D),
    background = Color(0xFFF9FBF8),
    surface = Color(0xFFF9FBF8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF55DBC6),
    onPrimary = Color(0xFF00382F),
    secondary = Color(0xFFB1CCC4),
    background = Color(0xFF101413),
    surface = Color(0xFF101413),
)

@Composable
fun LumenTransferApp(
    sendViewModel: SendViewModel,
    receiveViewModel: ReceiveViewModel,
) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(PRIVACY_PREFERENCES, android.content.Context.MODE_PRIVATE)
    }
    var route by rememberSaveable { mutableStateOf(AppRoute.HOME.name) }
    var privacyAccepted by remember {
        mutableStateOf(preferences.getBoolean(PRIVACY_ACCEPTED_KEY, false))
    }
    var pendingRoute by rememberSaveable { mutableStateOf<String?>(null) }
    var showPrivacyNotice by rememberSaveable { mutableStateOf(false) }

    fun requestRoute(target: AppRoute) {
        if (privacyAccepted) {
            route = target.name
        } else {
            pendingRoute = target.name
            showPrivacyNotice = true
        }
    }

    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (AppRoute.valueOf(route)) {
                AppRoute.HOME -> HomeScreen(
                    onSend = { requestRoute(AppRoute.SEND) },
                    onReceive = { requestRoute(AppRoute.RECEIVE) },
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

        if (showPrivacyNotice) {
            PrivacyNoticeDialog(
                onAccept = {
                    privacyAccepted = true
                    preferences.edit().putBoolean(PRIVACY_ACCEPTED_KEY, true).apply()
                    route = pendingRoute ?: AppRoute.HOME.name
                    pendingRoute = null
                    showPrivacyNotice = false
                },
                onDismiss = {
                    pendingRoute = null
                    showPrivacyNotice = false
                },
            )
        }
    }
}

private const val PRIVACY_PREFERENCES = "lumen_transfer_preferences"
private const val PRIVACY_ACCEPTED_KEY = "privacy_notice_accepted"
