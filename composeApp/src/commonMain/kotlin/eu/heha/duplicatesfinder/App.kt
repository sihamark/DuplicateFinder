package eu.heha.duplicatesfinder

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.heha.duplicatesfinder.model.PathWithMetaData
import eu.heha.duplicatesfinder.ui.FolderSelectionViewModel
import eu.heha.duplicatesfinder.ui.DuplicatesResolutionPane
import eu.heha.duplicatesfinder.ui.DuplicatesResolutionViewModel
import eu.heha.duplicatesfinder.ui.FolderSelectionPane
import kotlinx.io.files.Path
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        var navEntry by remember { mutableStateOf<NavEntry>(NavEntry.FolderSelection) }
        when (val entry = navEntry) {
            NavEntry.FolderSelection -> {
                val model = viewModel { FolderSelectionViewModel() }
                LaunchedEffect(model.state.isSuccess) {
                    if (model.state.isSuccess) {
                        navEntry = NavEntry.DuplicateResolution(PathWithMetaData(model.state.path))
                        model.reset()
                    }
                }
                FolderSelectionPane(
                    state = model.state,
                    onChangePathToScan = model::onPathChanged,
                    onClickStart = model::scan
                )
            }

            is NavEntry.DuplicateResolution -> {
                val model = viewModel { DuplicatesResolutionViewModel() }
                LaunchedEffect(model) {
                    model.findDuplicates(entry.path)
                }
                DuplicatesResolutionPane(
                    state = model.state,
                    onSelectPath = model::selectPath,
                    onClickClearSelection = model::clearSelection,
                    onClickResolveDuplicates = model::deleteNotSelected,
                    onClickSelectBest = model::selectBest,
                    onClickBack = { navEntry = NavEntry.FolderSelection }
                )
            }
        }
    }
}

sealed interface NavEntry {
    data object FolderSelection : NavEntry
    data class DuplicateResolution(val path: PathWithMetaData) : NavEntry
}