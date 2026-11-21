package com.example.dailycheck.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dailycheck.ui.screens.home.HomeScreen
import com.example.dailycheck.ui.screens.home.HomeViewModel

/**
 * P0 只有一个页面，但保留 NavHost 作为后续 P1/P2 的骨架。
 * 通知点击带来的 taskId 从这里下发给 HomeScreen，用于滚动 + 高亮。
 */
@Composable
fun AppNavHost(
    highlightTaskId: Long?,
    onHighlightConsumed: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = {
            fadeIn(animationSpec = tween(220)) +
                slideInHorizontally(animationSpec = tween(220)) { fullWidth -> fullWidth / 16 }
        },
        exitTransition = { fadeOut(animationSpec = tween(160)) },
        popEnterTransition = { fadeIn(animationSpec = tween(220)) },
        popExitTransition = { fadeOut(animationSpec = tween(160)) }
    ) {
        composable(route = Routes.HOME) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = viewModel,
                highlightTaskId = highlightTaskId,
                onHighlightConsumed = onHighlightConsumed
            )
        }
    }
}
