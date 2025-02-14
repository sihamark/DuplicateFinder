package eu.heha.duplicatesfinder.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import duplicatesfinder.composeapp.generated.resources.Res
import duplicatesfinder.composeapp.generated.resources.folder_selection_input_label
import duplicatesfinder.composeapp.generated.resources.folder_selection_title
import duplicatesfinder.composeapp.generated.resources.folder_selection_use_folder_action
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderSelectionPane(
    state: FolderSelectionViewModel.FolderSelectionState,
    onChangePathToScan: (String) -> Unit,
    onClickStart: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.folder_selection_title)) }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(
                value = state.path,
                onValueChange = onChangePathToScan,
                label = { Text(stringResource(Res.string.folder_selection_input_label)) },
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )

            if (state.error.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(state.error)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClickStart
            ) {
                Text(stringResource(Res.string.folder_selection_use_folder_action))
            }
        }
    }
}