package eu.heha.duplicatesfinder.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.heha.duplicatesfinder.model.Duplicate
import eu.heha.duplicatesfinder.model.DuplicateFinder
import eu.heha.duplicatesfinder.model.PathWithMetaData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DuplicatesResolutionViewModel : ViewModel() {
    var state: ScanResultState by mutableStateOf(ScanResultState())
        private set

    fun findDuplicates(path: PathWithMetaData) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                state = ScanResultState(path = path, isScanning = true)
                val duplicates = findDuplicatesPerFolder(path)
                state = state.copy(duplicatesPerFolder = duplicates)
            } catch (e: Exception) {
                println("error: $e")
            }
            state = state.copy(isScanning = false)
        }
    }

    private fun findDuplicatesPerFolder(path: PathWithMetaData): List<Pair<PathWithMetaData, List<Duplicate>>> {
        val duplicates = DuplicateFinder.findDuplicates(path)
        return duplicates.fold<Duplicate, MutableList<Pair<PathWithMetaData, List<Duplicate>>>>(
            mutableListOf()
        ) { acc, duplicate ->
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

    fun selectPath(duplicate: Duplicate, path: PathWithMetaData?) {
        val currentSelections = state.selections
        val newSelections = if (path == null) {
            currentSelections - duplicate
        } else {
            currentSelections + (duplicate to path)
        }
        state = state.copy(selections = newSelections)
    }

    fun clearSelection(folder: PathWithMetaData?) {
        val newSelections = if (folder == null) {
            emptyMap()
        } else {
            state.selections.filter { (_, path) -> path.parent != folder }
        }
        state = state.copy(selections = newSelections)
    }

    fun deleteNotSelected() {
        val path = state.path ?: return
        val duplicatesPerFolder = state.duplicatesPerFolder
        val selections = state.selections
        if (selections.isEmpty()) return
        if (duplicatesPerFolder.isEmpty()) return
        viewModelScope.launch(Dispatchers.Default) {
            try {
                state = state.copy(isDeleting = true)
                val allDuplicates = duplicatesPerFolder.flatMap { (_, duplicates) -> duplicates }
                val toDelete = selections.mapNotNull { (duplicate, path) ->
                    allDuplicates.find { it == duplicate }?.duplicates?.filter { it != path }
                }.flatten()
                toDelete.forEach { it.delete() }
                val newDuplicates = findDuplicatesPerFolder(path)
                state = state.copy(
                    duplicatesPerFolder = newDuplicates,
                    selections = emptyMap(),
                    isDeleting = false
                )
            } catch (e: Exception) {
                println("error: $e")
            }
        }
    }

    fun selectBest(folder: PathWithMetaData?) {
        viewModelScope.launch(Dispatchers.Default) {
            val existingSelections = state.selections
            val duplicates = if (folder != null) {
                state.duplicatesPerFolder.find { (folderI, _) -> folderI == folder }?.second
                    ?: emptyList()
            } else {
                state.duplicatesPerFolder.flatMap { (_, duplicates) -> duplicates }
            }
            val selections = duplicates.mapNotNull { duplicate ->
                duplicate.duplicates.filter { it.name.endsWith(".mp3") }
                    .firstOrNull { !it.name.contains("(") }
                    ?.let { duplicate to it }
            }.toMap()
            state = state.copy(selections = existingSelections + selections)
        }
    }

    data class ScanResultState(
        val path: PathWithMetaData? = null,
        val isScanning: Boolean = false,
        val isDeleting: Boolean = false,
        val duplicatesPerFolder: List<Pair<PathWithMetaData, List<Duplicate>>> = emptyList(),
        val selections: Map<Duplicate, PathWithMetaData> = emptyMap()
    )
}