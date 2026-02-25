package com.onepercent.app.ui.index

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onepercent.app.R
import com.onepercent.app.util.WeekCalculator
import java.time.LocalDate

/**
 * Placeholder Index screen showing links to the 4 current/upcoming week views.
 * Week ranges are computed once at composition time and do not update while the
 * screen remains in the back stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndexScreen(
    onOpenDrawer: () -> Unit,
    onNavigateToWeek: (weekStartEpochDay: Long) -> Unit
) {
    val fourWeeks = remember { WeekCalculator.fourWeekRanges(LocalDate.now()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.index)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.open_drawer)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.weeks),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )
            fourWeeks.forEach { weekRange ->
                val epochDay = WeekCalculator.weekStartEpochDay(weekRange.sunday)
                NavigationDrawerItem(
                    label = { Text(WeekCalculator.formatWeekLabel(weekRange)) },
                    selected = false,
                    onClick = { onNavigateToWeek(epochDay) }
                )
            }
        }
    }
}
