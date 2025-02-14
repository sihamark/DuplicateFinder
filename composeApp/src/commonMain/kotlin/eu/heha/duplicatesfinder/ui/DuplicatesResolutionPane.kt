package eu.heha.duplicatesfinder.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import duplicatesfinder.composeapp.generated.resources.Res
import duplicatesfinder.composeapp.generated.resources.duplicates_resolution_back_action_description
import duplicatesfinder.composeapp.generated.resources.duplicates_resolution_clear_selection_action
import duplicatesfinder.composeapp.generated.resources.duplicates_resolution_folder_collapse_action_description
import duplicatesfinder.composeapp.generated.resources.duplicates_resolution_folder_expand_action_description
import duplicatesfinder.composeapp.generated.resources.duplicates_resolution_no_duplicates_found_message
import duplicatesfinder.composeapp.generated.resources.duplicates_resolution_resolve_duplicates_action
import duplicatesfinder.composeapp.generated.resources.duplicates_resolution_resolving_duplicates_message
import duplicatesfinder.composeapp.generated.resources.duplicates_resolution_select_best_action
import duplicatesfinder.composeapp.generated.resources.duplicates_resolution_title
import duplicatesfinder.composeapp.generated.resources.duplicates_resolution_to_folder_selection_action
import eu.heha.duplicatesfinder.model.Duplicate
import eu.heha.duplicatesfinder.model.PathWithMetaData
import eu.heha.duplicatesfinder.ui.scroll.VerticalScrollbar
import eu.heha.duplicatesfinder.ui.scroll.rememberScrollbarAdapter
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesResolutionPane(
    state: DuplicatesResolutionViewModel.ScanResultState,
    onSelectPath: (Duplicate, PathWithMetaData?) -> Unit,
    onClickBack: () -> Unit,
    onClickResolveDuplicates: () -> Unit,
    onClickClearSelection: (folder: PathWithMetaData?) -> Unit,
    onClickSelectBest: (folder: PathWithMetaData?) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.duplicates_resolution_title)) },
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.duplicates_resolution_back_action_description)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (state.duplicatesPerFolder.isNotEmpty()) {
                BottomAppBar {
                    if (state.selections.isNotEmpty()) {
                        Button(
                            onClick = onClickResolveDuplicates
                        ) {
                            Text(
                                stringResource(
                                    Res.string.duplicates_resolution_resolve_duplicates_action,
                                    state.selections.size
                                )
                            )
                        }
                        TextButton(
                            onClick = { onClickClearSelection(null) }
                        ) {
                            Text(stringResource(Res.string.duplicates_resolution_clear_selection_action))
                        }
                    }
                    TextButton(
                        onClick = { onClickSelectBest(null) }
                    ) {
                        Text(stringResource(Res.string.duplicates_resolution_select_best_action))
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
                Scanning()
            } else if (state.duplicatesPerFolder.isEmpty()) {
                EmptyMessage(state, onClickBack)
            } else {
                val isExpandedMap = remember {
                    List(state.duplicatesPerFolder.size) { index: Int -> index to false }
                        .toMutableStateMap()
                }
                Box {
                    val lazyListState = rememberLazyListState()
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        state = lazyListState
                    ) {
                        state.duplicatesPerFolder.forEachIndexed { index, (folder, duplicates) ->
                            section(
                                folder = folder,
                                duplicates = duplicates,
                                isExpanded = isExpandedMap[index] ?: false,
                                selections = state.selections,
                                onClickSelectBest = { onClickSelectBest(it) },
                                onClickClearSelection = { onClickClearSelection(it) },
                                onSelectPath = { duplicate, path -> onSelectPath(duplicate, path) },
                                onClick = {
                                    isExpandedMap[index] = !(isExpandedMap[index] ?: false)
                                }
                            )
                        }
                    }
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(lazyListState)
                    )
                }
            }

            if (state.isDeleting) {
                Dialog(onDismissRequest = { }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(Res.string.duplicates_resolution_resolving_duplicates_message))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage(
    state: DuplicatesResolutionViewModel.ScanResultState,
    onClickBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(
                    Res.string.duplicates_resolution_no_duplicates_found_message,
                    state.path?.path?.toString() ?: ""
                ),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onClickBack) {
                Text(stringResource(Res.string.duplicates_resolution_to_folder_selection_action))
            }
        }
    }
}

@Composable
private fun Scanning() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(Res.string.duplicates_resolution_resolving_duplicates_message),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private fun LazyListScope.section(
    folder: PathWithMetaData,
    duplicates: List<Duplicate>,
    isExpanded: Boolean,
    selections: Map<Duplicate, PathWithMetaData>,
    onClickSelectBest: (PathWithMetaData) -> Unit,
    onClickClearSelection: (PathWithMetaData) -> Unit,
    onSelectPath: (Duplicate, PathWithMetaData?) -> Unit,
    onClick: () -> Unit
) {

    item {
        FolderItem(
            folder = folder,
            isExpanded = isExpanded,
            numberOfSelections = selections.filter { (duplicate, _) -> duplicate.parent == folder }.size,
            onClickSelectBest = { onClickSelectBest(folder) },
            onClickClearSelection = { onClickClearSelection(folder) },
            onClickToggleExpand = onClick
        )
    }

    if (isExpanded) {
        itemsIndexed(duplicates) { index, duplicate ->
            Column(modifier = Modifier.animateItem()) {
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
    folder: PathWithMetaData,
    isExpanded: Boolean,
    numberOfSelections: Int,
    onClickSelectBest: () -> Unit,
    onClickClearSelection: () -> Unit,
    onClickToggleExpand: () -> Unit
) {
    Surface(onClick = onClickToggleExpand) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val numberOfSelectionsText =
                if (numberOfSelections > 0) " ($numberOfSelections)" else ""
            Text(
                folder.name + numberOfSelectionsText,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.width(8.dp).weight(1f))
            OutlinedButton(onClick = onClickSelectBest) {
                Text(
                    stringResource(Res.string.duplicates_resolution_select_best_action)
                )
            }
            Spacer(Modifier.width(8.dp))
            if (numberOfSelections > 0) {
                OutlinedButton(onClick = onClickClearSelection) {
                    Text(
                        stringResource(Res.string.duplicates_resolution_clear_selection_action)
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = onClickToggleExpand) {
                val rotation by animateFloatAsState(if (isExpanded) 90f else 0f)
                val description = if (isExpanded) {
                    Res.string.duplicates_resolution_folder_collapse_action_description
                } else {
                    Res.string.duplicates_resolution_folder_expand_action_description
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = stringResource(description),
                    modifier = Modifier.rotate(rotation)
                )
            }
        }
    }
}

@Composable
private fun DuplicateItem(
    duplicate: Duplicate,
    selectedPath: PathWithMetaData?,
    onSelectPath: (PathWithMetaData?) -> Unit
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
                Text(path.name, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(16.dp))
                Text(
                    path.size.humanReadableByteCount(),
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace)
                )
            }
        }
    }
}