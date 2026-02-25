package com.onepercent.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.onepercent.app.ui.addtask.AddTaskScreen
import com.onepercent.app.ui.todaytasks.TodayTasksScreen

object Routes {
    const val TODAY_TASKS = "today_tasks"
    const val ADD_TASK = "add_task"
}

@Preview
@Composable
fun OnePercentNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.TODAY_TASKS
    ) {
        composable(Routes.TODAY_TASKS) {
            TodayTasksScreen(
                onNavigateToAddTask = { navController.navigate(Routes.ADD_TASK) }
            )
        }
        composable(Routes.ADD_TASK) {
            AddTaskScreen(
                onTaskSaved = { navController.popBackStack() }
            )
        }
    }
}
