package com.mardous.projectmusic.ui.component.compose.preferences

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mardous.projectmusic.ui.component.compose.DialogListItemWithRadio

@Composable
fun <T> SingleChoiceDialog(
    title: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    onDismissRequest: () -> Unit,
    optionTitle: @Composable (T) -> String = { it.toString() }
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(options) { _, option ->
                    DialogListItemWithRadio(
                        title = optionTitle(option),
                        isSelected = option == selectedOption,
                        onClick = {
                            onOptionSelected(option)
                            onDismissRequest()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
fun TextEditDialog(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismissRequest: () -> Unit,
    label: String = ""
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text); onDismissRequest() }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
