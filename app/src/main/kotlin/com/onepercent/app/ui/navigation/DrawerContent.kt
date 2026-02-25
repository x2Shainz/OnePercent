package com.onepercent.app.ui.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onepercent.app.R
import com.onepercent.app.navigation.Routes
import com.onepercent.app.util.WeekCalculator
import com.onepercent.app.util.WeekRange
import java.time.LocalDate

/**
 * Content of the [ModalNavigationDrawer] sidebar.
 *
 * Renders 5 items in order: Today, Index, then one entry per week in [fourWeeks].
 * The currently active item is highlighted by comparing [currentRoute] against each
 * item's route constant. Week items are separated from the fixed items by a divider.
 */
@Composable
fun DrawerContent(
    fourWeeks: List<WeekRange>,
    currentRoute: String?,
    onTodayClick: () -> Unit,
    onIndexClick: () -> Unit,
    onWeekClick: (WeekRange) -> Unit
) {
    ModalDrawerSheet {
        Spacer(Modifier.height(16.dp))

        NavigationDrawerItem(
            label = { Text(stringResource(R.string.today)) },
            selected = currentRoute == Routes.TODAY_TASKS,
            onClick = onTodayClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        NavigationDrawerItem(
            label = { Text(stringResource(R.string.index)) },
            selected = currentRoute == Routes.INDEX,
            onClick = onIndexClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        fourWeeks.forEach { weekRange ->
            val epochDay = WeekCalculator.weekStartEpochDay(weekRange.sunday)
            NavigationDrawerItem(
                label = { Text(WeekCalculator.formatWeekLabel(weekRange)) },
                selected = currentRoute == Routes.weeklyPager(epochDay),
                onClick = { onWeekClick(weekRange) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}
