package io.github.flassuu.talex.ui.component

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.navigation.RouteKeys

/** A main destination that can occupy a bottom-bar slot (ADR-0034). */
enum class NavDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    OVERVIEW(RouteKeys.OVERVIEW, R.string.overview_title, Icons.Outlined.Home),
    TRANSACTIONS(RouteKeys.TRANSACTIONS, R.string.transactions_title, Icons.Outlined.SwapVert),
    ACCOUNTS(RouteKeys.ACCOUNTS, R.string.accounts_title, Icons.Outlined.AccountBalanceWallet),
    BUDGETS(RouteKeys.BUDGETS, R.string.budgets_title, Icons.Outlined.Savings),
    GOALS(RouteKeys.GOALS, R.string.goals_title, Icons.Outlined.Flag),
    ASSETS(RouteKeys.ASSETS, R.string.assets_title, Icons.Outlined.AccountBalance),
    ANALYTICS(RouteKeys.ANALYTICS, R.string.analytics_title, Icons.Outlined.Insights),
    RECURRING(RouteKeys.RECURRING, R.string.recurring_title, Icons.Outlined.Repeat),
    CATEGORIES(RouteKeys.CATEGORIES, R.string.categories_title, Icons.Outlined.Category),
    SETTINGS(RouteKeys.SETTINGS, R.string.settings_title, Icons.Outlined.Settings),
    ;

    companion object {
        /** Every main destination, in the order shown in the More menu. */
        val allowed: List<NavDestination> = entries.toList()

        val defaults: List<NavDestination> = listOf(OVERVIEW, TRANSACTIONS, ACCOUNTS)

        fun fromRoute(route: String?): NavDestination? = entries.firstOrNull { it.route == route }
    }
}
