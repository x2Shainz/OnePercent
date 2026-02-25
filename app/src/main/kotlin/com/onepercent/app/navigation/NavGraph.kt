package com.onepercent.app.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.onepercent.app.ui.addtask.AddTaskScreen
import com.onepercent.app.ui.futurelog.FutureLogScreen
import com.onepercent.app.ui.index.IndexScreen
import com.onepercent.app.ui.months.NextMonthsScreen
import com.onepercent.app.ui.months.PastMonthsScreen
import com.onepercent.app.ui.navigation.DrawerContent
import com.onepercent.app.ui.todaytasks.TodayTasksScreen
import com.onepercent.app.ui.weeklypager.WeeklyPagerScreen
import com.onepercent.app.util.WeekCalculator
import kotlinx.coroutines.launch
import java.time.LocalDate

object Routes {
    const val TODAY_TASKS   = "today_tasks"
    const val ADD_TASK      = "add_task"
    const val INDEX         = "index"
    const val WEEKLY_PAGER  = "week/{weekStartEpochDay}"
    const val FUTURE_LOG    = "future_log"
    const val PAST_MONTHS   = "past_months"
    const val NEXT_MONTHS   = "next_months"

    fun weeklyPager(weekStartEpochDay: Long) = "week/$weekStartEpochDay"
}

@Preview
@Composable
fun OnePercentNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val fourWeeks = remember { WeekCalculator.fourWeekRanges(LocalDate.now()) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    fun openDrawer()  = scope.launch { drawerState.open() }
    fun closeDrawer() = scope.launch { drawerState.close() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                fourWeeks = fourWeeks,
                currentRoute = currentRoute,
                onTodayClick = {
                    closeDrawer()
                    navController.navigate(Routes.TODAY_TASKS) {
                        popUpTo(Routes.TODAY_TASKS) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onIndexClick = {
                    closeDrawer()
                    navController.navigate(Routes.INDEX) { launchSingleTop = true }
                },
                onWeekClick = { weekRange ->
                    closeDrawer()
                    val epochDay = WeekCalculator.weekStartEpochDay(weekRange.sunday)
                    navController.navigate(Routes.weeklyPager(epochDay)) {
                        launchSingleTop = true
                    }
                }
            )
        }
    ) {
        NavHost(navController = navController, startDestination = Routes.TODAY_TASKS) {
            composable(Routes.TODAY_TASKS) {
                TodayTasksScreen(
                    onNavigateToAddTask = { navController.navigate(Routes.ADD_TASK) },
                    onOpenDrawer = { openDrawer() }
                )
            }
            composable(Routes.ADD_TASK) {
                // AddTaskScreen is a modal form — no drawer access needed
                AddTaskScreen(onTaskSaved = { navController.popBackStack() })
            }
            composable(Routes.INDEX) {
                IndexScreen(
                    onOpenDrawer = { openDrawer() },
                    onNavigateToWeek = { epochDay ->
                        navController.navigate(Routes.weeklyPager(epochDay)) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToFutureLog = {
                        navController.navigate(Routes.FUTURE_LOG) { launchSingleTop = true }
                    },
                    onNavigateToPastMonths = {
                        navController.navigate(Routes.PAST_MONTHS) { launchSingleTop = true }
                    },
                    onNavigateToNextMonths = {
                        navController.navigate(Routes.NEXT_MONTHS) { launchSingleTop = true }
                    }
                )
            }
            composable(Routes.FUTURE_LOG) {
                FutureLogScreen(onOpenDrawer = { openDrawer() })
            }
            composable(Routes.PAST_MONTHS) {
                PastMonthsScreen(onOpenDrawer = { openDrawer() })
            }
            composable(Routes.NEXT_MONTHS) {
                NextMonthsScreen(onOpenDrawer = { openDrawer() })
            }
            composable(
                route = Routes.WEEKLY_PAGER,
                arguments = listOf(navArgument("weekStartEpochDay") { type = NavType.LongType })
            ) { backStackEntry ->
                val epochDay = backStackEntry.arguments!!.getLong("weekStartEpochDay")
                WeeklyPagerScreen(
                    weekStartEpochDay = epochDay,
                    onOpenDrawer = { openDrawer() },
                    onNavigateToAddTask = { navController.navigate(Routes.ADD_TASK) }
                )
            }
        }
    }
}
