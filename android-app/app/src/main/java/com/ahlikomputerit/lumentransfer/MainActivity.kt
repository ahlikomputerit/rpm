package com.ahlikomputerit.lumentransfer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.ahlikomputerit.lumentransfer.presentation.receive.ReceiveViewModel
import com.ahlikomputerit.lumentransfer.presentation.send.SendViewModel

class MainActivity : ComponentActivity() {
    private val sendViewModel by viewModels<SendViewModel> {
        SendViewModel.factory(contentResolver)
    }
    private val receiveViewModel by viewModels<ReceiveViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LumenTransferApp(
                sendViewModel = sendViewModel,
                receiveViewModel = receiveViewModel,
            )
        }
    }
}
