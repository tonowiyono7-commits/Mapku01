package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.MarkerFilter
import com.example.ui.theme.BrandBlue

@Composable
fun FilterMarkersDialog(
    initialFilter: MarkerFilter,
    onDismiss: () -> Unit,
    onConfirm: (showHistory: Boolean, showFavorites: Boolean) -> Unit,
) {
    var showHistory by remember(initialFilter) { mutableStateOf(initialFilter.showHistory) }
    var showFavorites by remember(initialFilter) { mutableStateOf(initialFilter.showFavorites) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraSmall,
        title = {
            Text(
                text = stringResource(R.string.filter_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilterCheckboxRow(
                    label = stringResource(R.string.filter_history),
                    checked = showHistory,
                    onCheckedChange = { showHistory = it },
                    testTag = "filter_history_checkbox",
                )
                FilterCheckboxRow(
                    label = stringResource(R.string.filter_favorites),
                    checked = showFavorites,
                    onCheckedChange = { showFavorites = it },
                    testTag = "filter_favorites_checkbox",
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(showHistory, showFavorites) },
                modifier = Modifier.testTag("filter_ok_button"),
            ) {
                Text(
                    text = stringResource(R.string.dialog_ok),
                    color = BrandBlue,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("filter_cancel_button"),
            ) {
                Text(
                    text = stringResource(R.string.dialog_cancel),
                    color = BrandBlue,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    )
}

@Composable
private fun FilterCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = BrandBlue,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
