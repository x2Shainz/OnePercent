package com.onepercent.app.ui.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onepercent.app.R

/**
 * Full-screen page for viewing and editing a single [com.onepercent.app.data.model.Entry].
 *
 * The title is editable in the [TopAppBar]; the body occupies the rest of the screen.
 * Changes are auto-saved via a debounce in [EntryViewModel]; [DisposableEffect] calls
 * [EntryViewModel.saveNow] as a safety net when the screen leaves the composition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun EntryScreen(
    entryId: Long = -1L,
    onNavigateBack: () -> Unit = {}
) {
    val viewModel: EntryViewModel = hiltViewModel<EntryViewModel, EntryViewModel.Factory>(
        creationCallback = { factory -> factory.create(entryId) }
    )

    val title by viewModel.title.collectAsStateWithLifecycle()
    val body  by viewModel.body.collectAsStateWithLifecycle()

    // Safety-net save when the composable leaves the composition (e.g., back navigation).
    // The debounce in the ViewModel covers normal typing pauses; this catches cases where
    // the user navigates back before the 500 ms debounce fires.
    DisposableEffect(Unit) {
        onDispose { viewModel.saveNow() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    BasicTextField(
                        value = title,
                        onValueChange = viewModel::onTitleChange,
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (title.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.untitled),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            BasicTextField(
                value = body,
                onValueChange = viewModel::onBodyChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                decorationBox = { innerTextField ->
                    if (body.isEmpty()) {
                        Text(
                            text = stringResource(R.string.write_something),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
