package com.ahlikomputerit.lumentransfer.presentation.send

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ahlikomputerit.lumentransfer.data.file.AndroidDocumentReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SendStatus {
    data object Idle : SendStatus
    data object Reading : SendStatus
    data object Ready : SendStatus
    data class Failed(val message: String) : SendStatus
}

data class SendUiState(
    val status: SendStatus = SendStatus.Idle,
    val selected: AndroidDocumentReader.SelectedDocument? = null,
)

class SendViewModel(contentResolver: ContentResolver) : ViewModel() {
    private val reader = AndroidDocumentReader(contentResolver)
    private val _uiState = MutableStateFlow(SendUiState())
    val uiState: StateFlow<SendUiState> = _uiState.asStateFlow()

    fun onFileSelected(uri: Uri) {
        _uiState.value = SendUiState(status = SendStatus.Reading)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { reader.read(uri) }
                .onSuccess { document ->
                    _uiState.value = SendUiState(status = SendStatus.Ready, selected = document)
                }
                .onFailure { error ->
                    _uiState.value = SendUiState(
                        status = SendStatus.Failed(error.message ?: "Unable to read selected file"),
                    )
                }
        }
    }

    fun clearSelection() {
        _uiState.value = SendUiState()
    }

    companion object {
        fun factory(contentResolver: ContentResolver): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(SendViewModel::class.java))
                    return SendViewModel(contentResolver) as T
                }
            }
    }
}
