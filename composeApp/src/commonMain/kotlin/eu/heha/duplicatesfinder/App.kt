package eu.heha.duplicatesfinder

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
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
                        navEntry = NavEntry.ScanResult(Path(model.state.path))
                        model.reset()
                    }
                }
                FolderSelectionPane(
                    state = model.state,
                    onChangePathToScan = model::onPathChanged,
                    onClickStart = model::scan
                )
            }

            is NavEntry.ScanResult -> {
                val model = viewModel { ScanResultViewModel(entry.path) }
                LaunchedEffect(model) {
                    model.findDuplicates()
                }
                ScanResultPane(
                    state = model.state,
                    onSelectPath = model::selectPath,
                    onClickClearSelection = model::clearSelection,
                    onClickDeleteSelected = model::deleteNotSelected,
                    onClickSelectBest = model::selectBest,
                    onClickBack = { navEntry = NavEntry.FolderSelection }
                )
            }
        }
    }
}

sealed interface NavEntry {
    data object FolderSelection : NavEntry
    data class ScanResult(val path: Path) : NavEntry
}