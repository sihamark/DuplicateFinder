package eu.heha.duplicatesfinder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

class ScanResultViewModel(
    private val path: Path
) : ViewModel() {
    var state: ScanResultState by mutableStateOf(ScanResultState())
        private set

    fun findDuplicates() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                state = state.copy(isScanning = true)
                val duplicates = findDuplicatesPerFolder()
                state = state.copy(duplicatesPerFolder = duplicates)
            } catch (e: Exception) {
                println("error: $e")
            }
            state = state.copy(isScanning = false)
        }
    }

    private fun findDuplicatesPerFolder(): List<Pair<Path, List<Duplicate>>> {
        val duplicates = DuplicateFinder.findDuplicates(path)
        return duplicates.fold<Duplicate, MutableList<Pair<Path, List<Duplicate>>>>(mutableListOf()) { acc, duplicate ->
            val indexOfFolder = acc.indexOfFirst { (folder, _) -> folder == duplicate.parent }
            acc.apply {
                if (indexOfFolder == -1) {
                    add(duplicate.parent to listOf(duplicate))
                } else {
                    val (_, existingList) = acc[indexOfFolder]
                    acc[indexOfFolder] = duplicate.parent to (existingList + duplicate)
                }
            }
        }.toList()
    }

    fun selectPath(duplicate: Duplicate, path: Path?) {
        val currentSelections = state.selections
        val newSelections = if (path == null) {
            currentSelections - duplicate
        } else {
            currentSelections + (duplicate to path)
        }
        state = state.copy(selections = newSelections)
    }

    fun clearSelection() {
        state = state.copy(selections = emptyMap())
    }

    fun deleteNotSelected() {
        val duplicatesPerFolder = state.duplicatesPerFolder
        val selections = state.selections
        if (selections.isEmpty()) return
        if (duplicatesPerFolder.isEmpty()) return
        viewModelScope.launch(Dispatchers.Default) {
            state = state.copy(isDeleting = true)
            val allDuplicates = duplicatesPerFolder.flatMap { (_, duplicates) -> duplicates }
            val toDelete = selections.mapNotNull { (duplicate, path) ->
                allDuplicates.find { it == duplicate }?.duplicates?.filter { it != path }
            }.flatten()
            toDelete.forEach { SystemFileSystem.delete(it) }
            val newDuplicates = findDuplicatesPerFolder()
            state = state.copy(
                duplicatesPerFolder = newDuplicates,
                selections = emptyMap(),
                isDeleting = false
            )
        }
    }

    fun selectBest() {
        viewModelScope.launch(Dispatchers.Default) {
            val duplicates = state.duplicatesPerFolder
            val selections = duplicates.flatMap { (_, duplicates) -> duplicates }
                .mapNotNull { duplicate ->
                    duplicate.duplicates.filter { it.name.endsWith(".mp3") }
                        .firstOrNull { !it.name.contains("(") }
                        ?.let { duplicate to it }
                }.toMap()
            state = state.copy(selections = selections)
        }
    }

    data class ScanResultState(
        val isScanning: Boolean = false,
        val isDeleting: Boolean = false,
        val duplicatesPerFolder: List<Pair<Path, List<Duplicate>>> = emptyList(),
        val selections: Map<Duplicate, Path> = emptyMap()
    )
}