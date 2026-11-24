package io.github.flassuu.talex.feature.tags

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.flassuu.talex.R
import io.github.flassuu.talex.ui.component.TalexScreen
import io.github.flassuu.talex.ui.component.errorMessage

@Composable
fun TagsScreen(viewModel: TagsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hostState = remember { SnackbarHostState() }
    val messageText = state.message?.let { message ->
        message.res?.let { stringResource(it) } ?: message.error?.let { errorMessage(it) }
    }
    LaunchedEffect(state.message?.id) {
        messageText?.let {
            hostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    TalexScreen(
        title = stringResource(R.string.tags_title),
        snackbarHost = { SnackbarHost(hostState) },
    ) { padding ->
        if (state.tags.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.tags_empty),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.tags_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
            ) {
                items(state.tags, key = { it.id }) { tag ->
                    ListItem(
                        headlineContent = { Text(tag.name) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { viewModel.startRename(tag) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = stringResource(R.string.tag_rename),
                                    )
                                }
                                IconButton(onClick = { viewModel.requestDelete(tag) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = stringResource(R.string.delete_action),
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    state.rename?.let { rename ->
        RenameTagDialog(
            state = rename,
            onNameChange = viewModel::onNameChange,
            onConfirm = viewModel::confirmRename,
            onDismiss = viewModel::cancelRename,
        )
    }

    state.delete?.let { tag ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(stringResource(R.string.tag_delete_title)) },
            text = { Text(stringResource(R.string.tag_delete_confirm, tag.name)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text(stringResource(R.string.delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        )
    }
}

@Composable
private fun RenameTagDialog(
    state: TagsViewModel.RenameState,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tag_rename)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.tag_name_label)) },
                    singleLine = true,
                    isError = state.error != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.error?.let { error ->
                    Text(
                        text = errorMessage(error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !state.busy && state.name.isNotBlank()) {
                Text(stringResource(R.string.rename_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_action)) }
        },
    )
}
