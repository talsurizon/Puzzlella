package com.puzzlella.app.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.puzzlella.app.ui.history.HistoryScreen
import com.puzzlella.app.ui.home.HomeScreen
import com.puzzlella.app.ui.preview.PreviewScreen
import com.puzzlella.app.ui.puzzle.PuzzleScreen

@Composable
fun PuzzlellaNavHost(windowSizeClass: WindowSizeClass) {
    val navController = rememberNavController()
    val animDuration = 400

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(animDuration)
            ) + fadeIn(tween(animDuration))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(animDuration)
            ) + fadeOut(tween(animDuration))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(animDuration)
            ) + fadeIn(tween(animDuration))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(animDuration)
            ) + fadeOut(tween(animDuration))
        }
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                windowSizeClass = windowSizeClass,
                onImageSelected = { uri ->
                    navController.navigate(Routes.preview(uri.toString()))
                },
                onHistoryClick = {
                    navController.navigate(Routes.HISTORY)
                }
            )
        }

        composable(
            route = Routes.PREVIEW,
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val imageUri = Uri.decode(backStackEntry.arguments?.getString("imageUri") ?: "")
            PreviewScreen(
                imageUri = imageUri,
                onStartPuzzle = { imagePath, pieceCount ->
                    navController.navigate(Routes.puzzle(imagePath, pieceCount)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.PUZZLE,
            arguments = listOf(
                navArgument("imagePath") { type = NavType.StringType },
                navArgument("pieceCount") { type = NavType.IntType },
                navArgument("historyId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val imagePath = Uri.decode(backStackEntry.arguments?.getString("imagePath") ?: "")
            val pieceCount = backStackEntry.arguments?.getInt("pieceCount") ?: 12
            val historyId = backStackEntry.arguments?.getLong("historyId") ?: -1L
            PuzzleScreen(
                imagePath = imagePath,
                pieceCount = pieceCount,
                historyId = historyId,
                windowSizeClass = windowSizeClass,
                onBack = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                onNewPuzzle = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                windowSizeClass = windowSizeClass,
                onBack = { navController.popBackStack() },
                onResumePuzzle = { historyId, imagePath, pieceCount ->
                    navController.navigate(Routes.puzzle(imagePath, pieceCount, historyId))
                }
            )
        }
    }
}
