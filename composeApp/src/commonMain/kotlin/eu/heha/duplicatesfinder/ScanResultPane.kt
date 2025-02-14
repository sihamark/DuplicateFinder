package eu.heha.duplicatesfinder

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.io.files.Path

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultPane(
    state: ScanResultViewModel.ScanResultState,
    onSelectPath: (Duplicate, Path?) -> Unit,
    onClickBack: () -> Unit,
    onClickClearSelection: () -> Unit,
    onClickDeleteSelected: () -> Unit,
    onClickSelectBest: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duplicates") },
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (state.duplicatesPerFolder.isNotEmpty()) {
                BottomAppBar {
                    if (state.selections.isNotEmpty()) {
                        Button(
                            onClick = onClickDeleteSelected
                        ) {
                            Text("Remove (${state.selections.size}) Duplicates")
                        }
                        TextButton(
                            onClick = onClickClearSelection
                        ) {
                            Text("Clear selection")
                        }
                    }
                    TextButton(
                        onClick = onClickSelectBest
                    ) {
                        Text("Select Best")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.isScanning) {
                CircularProgressIndicator()
            } else if (state.duplicatesPerFolder.isEmpty()) {
                Text("No duplicates found")
            } else {
                val isExpandedMap = remember {
                    List(state.duplicatesPerFolder.size) { index: Int -> index to false }
                        .toMutableStateMap()
                }
                LazyColumn(Modifier.fillMaxWidth()) {
                    state.duplicatesPerFolder.forEachIndexed { index, (folder, duplicates) ->
                        section(
                            folder = folder,
                            duplicates = duplicates,
                            isExpanded = isExpandedMap[index] ?: false,
                            selections = state.selections,
                            onSelectPath = { duplicate, path -> onSelectPath(duplicate, path) },
                            onClick = { isExpandedMap[index] = !(isExpandedMap[index] ?: false) }
                        )
                    }
                }
            }

            if (state.isDeleting) {
                Dialog(onDismissRequest = { }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Deleting duplicates")
                    }
                }
            }
        }
    }
}

private fun LazyListScope.section(
    folder: Path,
    duplicates: List<Duplicate>,
    isExpanded: Boolean,
    selections: Map<Duplicate, Path>,
    onSelectPath: (Duplicate, Path?) -> Unit,
    onClick: () -> Unit
) {

    item {
        FolderItem(
            folder = folder,
            isExpanded = isExpanded,
            onClick = onClick
        )
    }

    if (isExpanded) {
        itemsIndexed(duplicates) { index, duplicate ->
            Column {
                if (index != 0) HorizontalDivider()
                DuplicateItem(
                    duplicate = duplicate,
                    selectedPath = selections[duplicate],
                    onSelectPath = { path -> onSelectPath(duplicate, path) }
                )
            }
        }
    }
}

@Composable
private fun FolderItem(
    folder: Path,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Surface(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(folder.name)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onClick) {
                val rotation by animateFloatAsState(if (isExpanded) 90f else 0f)
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Expand",
                    modifier = Modifier.rotate(rotation)
                )
            }
        }
    }
}

@Composable
private fun DuplicateItem(
    duplicate: Duplicate,
    selectedPath: Path?,
    onSelectPath: (Path?) -> Unit
) {
    Column(modifier = Modifier.padding(8.dp)) {
        duplicate.duplicates.forEach { path ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = path == selectedPath,
                    onCheckedChange = { isChecked ->
                        onSelectPath(path.takeIf { isChecked })
                    }
                )
                Text(path.name)
            }
        }
    }
}
