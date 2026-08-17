package com.manageyourmoney.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.manageyourmoney.app.ui.screens.home.HomeScreen
import com.manageyourmoney.app.ui.screens.month.MonthDetailScreen
import com.manageyourmoney.app.ui.screens.split.SplitMoneyScreen

/**
 * The nav graph replacing the web app's `State.view` single-page router. Each
 * destination is a real back-stack entry (not a manually-tracked `State.view` string),
 * so Android 16's predictive back animates between them automatically — Home ->
 * MonthDetail -> (a form's own `BackHandler`, see [MonthDetailScreen]'s bottom sheet,
 * which intercepts back to close the sheet before it would pop the whole screen,
 * mirroring the web app's layered "form panel closes before the page navigates back").
 */
@Composable
fun MoneyNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = MoneyRoute.Home) {
        composable<MoneyRoute.Home> {
            HomeScreen(
                onOpenMonth = { monthKey -> navController.navigate(MoneyRoute.MonthDetail(monthKey)) },
                onOpenSplitMoney = { navController.navigate(MoneyRoute.SplitMoney) },
            )
        }
        composable<MoneyRoute.MonthDetail> {
            MonthDetailScreen(onBack = { navController.popBackStack() })
        }
        composable<MoneyRoute.SplitMoney> {
            SplitMoneyScreen(onBack = { navController.popBackStack() })
        }
    }
}
