package eu.heha.duplicatesfinder.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.heha.duplicatesfinder.model.PathWithMetaData
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

class FolderSelectionViewModel : ViewModel() {

    var state by mutableStateOf(FolderSelectionState())
        private set

    fun onPathChanged(path: String) {
        state = state.copy(path = path)
    }

    fun scan() {
        viewModelScope.launch {
            try {
                state = state.copy(isScanning = true)
                val path = PathWithMetaData(state.path)
                state = if (!path.isDirectory) {
                    state.copy(isScanning = false, error = "Path is not a directory")
                } else {
                    state.copy(isScanning = false, isSuccess = true, error = "")
                }
            } catch (e: Exception) {
                state = state.copy(isScanning = false, error = e.message ?: "Unknown error")
            }
        }
    }

    fun reset() {
        state = FolderSelectionState()
    }

    data class FolderSelectionState(
        val path: String = "",
        val isScanning: Boolean = false,
        val error: String = "",
        val isSuccess: Boolean = false
    )
}